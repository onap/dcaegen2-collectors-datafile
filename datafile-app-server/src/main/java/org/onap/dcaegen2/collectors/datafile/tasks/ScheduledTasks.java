/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018-2019 Nordix Foundation. All rights reserved.
 * ===============================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 * ============LICENSE_END========================================================================
 */

package org.onap.dcaegen2.collectors.datafile.tasks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.FileData;
import org.onap.dcaegen2.collectors.datafile.model.FileReadyMessage;
import org.onap.dcaegen2.collectors.datafile.model.logging.MdcVariables;
import org.onap.dcaegen2.collectors.datafile.service.PublishedFileCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * This implements the main flow of the data file collector. Fetch file ready events from the
 * message router, fetch new files from the PNF publish these in the data router.
 */
@Component
public class ScheduledTasks {

    private static final int NUMBER_OF_WORKER_THREADS = 100;
    private static final int MAX_TASKS_FOR_POLLING = 50;
    private static final long DATA_ROUTER_MAX_RETRIES = 5;
    private static final Duration DATA_ROUTER_INITIAL_RETRY_TIMEOUT = Duration.ofSeconds(2);
    private static final long FILE_TRANSFER_MAX_RETRIES = 3;
    private static final Duration FILE_TRANSFER_INITIAL_RETRY_TIMEOUT = Duration.ofSeconds(5);

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
    private final AppConfig applicationConfiguration;
    private final AtomicInteger currentNumberOfTasks = new AtomicInteger();
    private final Scheduler scheduler = Schedulers.newParallel("FileCollectorWorker", NUMBER_OF_WORKER_THREADS);
    PublishedFileCache alreadyPublishedFiles = new PublishedFileCache();

    /**
     * Constructor for task registration in Datafile Workflow.
     *
     * @param applicationConfiguration - application configuration
     */
    @Autowired
    public ScheduledTasks(AppConfig applicationConfiguration) {
        this.applicationConfiguration = applicationConfiguration;
    }

    /**
     * Main function for scheduling for the file collection Workflow.
     */
    public void scheduleMainDatafileEventTask(Map<String, String> contextMap) {
        try {
            MdcVariables.setMdcContextMap(contextMap);
            logger.trace("Execution of tasks was registered");
            applicationConfiguration.loadConfigurationFromFile();
            createMainTask(contextMap).subscribe(model -> onSuccess(model, contextMap), thr -> onError(thr, contextMap),
                    () -> onComplete(contextMap));
        } catch (Exception e) {
            logger.error("Unexpected exception: ", e);
        }
    }

    Flux<ConsumerDmaapModel> createMainTask(Map<String, String> contextMap) {
        return fetchMoreFileReadyMessages() //
                .parallel(NUMBER_OF_WORKER_THREADS) // Each FileReadyMessage in a separate thread
                .runOn(scheduler) //
                .flatMap(fileReadyMessage -> Flux.fromIterable(fileReadyMessage.files())) //
                .filter(this::shouldBePublished) //
                .doOnNext(fileData -> currentNumberOfTasks.incrementAndGet()) //
                .flatMap(fileData -> fetchFile(fileData, contextMap)) //
                .flatMap(model -> publishToDataRouter(model, contextMap)) //
                .doOnNext(model -> deleteFile(Paths.get(model.getInternalLocation()), contextMap)) //
                .doOnNext(model -> currentNumberOfTasks.decrementAndGet()) //
                .sequential();
    }

    /**
     * called in regular intervals to remove out-dated cached information
     */
    public void purgeCachedInformation(Instant now) {
        alreadyPublishedFiles.purge(now);
    }

    private void onComplete(Map<String, String> contextMap) {
        MdcVariables.setMdcContextMap(contextMap);
        logger.trace("Datafile tasks have been completed");
    }

    private synchronized void onSuccess(ConsumerDmaapModel model, Map<String, String> contextMap) {
        MdcVariables.setMdcContextMap(contextMap);
        logger.info("Datafile file published {}", model.getInternalLocation());
    }

    private void onError(Throwable throwable, Map<String, String> contextMap) {
        MdcVariables.setMdcContextMap(contextMap);
        logger.error("Chain of tasks have been aborted due to errors in Datafile workflow {}", throwable.toString());
    }

    private boolean shouldBePublished(FileData fileData) {
        return alreadyPublishedFiles.put(fileData.getLocalFileName()) == null;
    }

    private Mono<ConsumerDmaapModel> fetchFile(FileData fileData, Map<String, String> contextMap) {
        MdcVariables.setMdcContextMap(contextMap);
        return createFileCollector()
                .execute(fileData, FILE_TRANSFER_MAX_RETRIES, FILE_TRANSFER_INITIAL_RETRY_TIMEOUT, contextMap)
                .onErrorResume(exception -> handleCollectFailure(fileData, contextMap));
    }

    private Mono<ConsumerDmaapModel> handleCollectFailure(FileData fileData, Map<String, String> contextMap) {
        MdcVariables.setMdcContextMap(contextMap);
        Path localFileName = fileData.getLocalFileName();
        logger.error("File fetching failed: {}", localFileName);
        deleteFile(localFileName, contextMap);
        alreadyPublishedFiles.remove(localFileName);
        currentNumberOfTasks.decrementAndGet();
        return Mono.empty();
    }

    private Mono<ConsumerDmaapModel> publishToDataRouter(ConsumerDmaapModel model, Map<String, String> contextMap) {

        DataRouterPublisher publisherTask = createDataRouterPublisher();

        MdcVariables.setMdcContextMap(contextMap);
        return publisherTask.execute(model, DATA_ROUTER_MAX_RETRIES, DATA_ROUTER_INITIAL_RETRY_TIMEOUT, contextMap)
                .onErrorResume(exception -> handlePublishFailure(model, contextMap));
    }

    private Mono<ConsumerDmaapModel> handlePublishFailure(ConsumerDmaapModel model, Map<String, String> contextMap) {
        MdcVariables.setMdcContextMap(contextMap);
        logger.error("File publishing failed: {}", model.getName());
        Path internalFileName = Paths.get(model.getInternalLocation());
        deleteFile(internalFileName, contextMap);
        alreadyPublishedFiles.remove(internalFileName);
        currentNumberOfTasks.decrementAndGet();
        return Mono.empty();
    }

    /**
     * Fetch more messages from the message router. This is done in a polling/blocking fashion.
     */
    private Flux<FileReadyMessage> fetchMoreFileReadyMessages() {
        logger.info("Consuming new file ready messages, current number of tasks: {}", getCurrentNumberOfTasks());
        if (getCurrentNumberOfTasks() > MAX_TASKS_FOR_POLLING) {
            logger.info("Skipping, current number of tasks: {}", getCurrentNumberOfTasks());
            return Flux.empty();
        }

        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return createConsumerTask() //
                .execute() //
                .onErrorResume(exception -> handleConsumeMessageFailure(exception, contextMap));
    }

    private Flux<FileReadyMessage> handleConsumeMessageFailure(Throwable exception, Map<String, String> contextMap) {
        MdcVariables.setMdcContextMap(contextMap);
        logger.error("Polling for file ready message failed, exception: {}", exception.toString());
        return Flux.empty();
    }

    private void deleteFile(Path localFile, Map<String, String> contextMap) {
        MdcVariables.setMdcContextMap(contextMap);
        logger.trace("Deleting file: {}", localFile);
        try {
            Files.delete(localFile);
        } catch (Exception e) {
            logger.trace("Could not delete file: {}", localFile);
        }
    }

    protected int getCurrentNumberOfTasks() {
        return currentNumberOfTasks.get();
    }

    protected DMaaPMessageConsumerTask createConsumerTask() {
        return new DMaaPMessageConsumerTask(this.applicationConfiguration);
    }

    protected FileCollector createFileCollector() {
        return new FileCollector(applicationConfiguration);
    }

    protected DataRouterPublisher createDataRouterPublisher() {
        return new DataRouterPublisher(applicationConfiguration);
    }

}
