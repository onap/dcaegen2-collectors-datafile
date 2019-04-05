/*-
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
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.model.FileData;
import org.onap.dcaegen2.collectors.datafile.model.FilePublishInformation;
import org.onap.dcaegen2.collectors.datafile.model.FileReadyMessage;
import org.onap.dcaegen2.collectors.datafile.model.logging.MappedDiagnosticContext;
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
 * This implements the main flow of the data file collector. Fetch file ready events from the message router, fetch new
 * files from the PNF publish these in the data router.
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
    public void executeDatafileMainTask() {
        try {
            Map<String, String> context = MappedDiagnosticContext.initializeTraceContext();
            logger.trace("Execution of tasks was registered");
            applicationConfiguration.loadConfigurationFromFile();
            createMainTask(context) //
                    .subscribe(model -> onSuccess(model, context), //
                        thr -> onError(thr, context), //
                        () -> onComplete(context));
        } catch (Exception e) {
            logger.error("Unexpected exception: ", e);
        }
    }

    Flux<FilePublishInformation> createMainTask(Map<String, String> contextMap) {
        return fetchMoreFileReadyMessages() //
                .parallel(NUMBER_OF_WORKER_THREADS) // Each FileReadyMessage in a separate thread
                .runOn(scheduler) //
                .flatMap(fileReadyMessage -> Flux.fromIterable(fileReadyMessage.files())) //
                .filter(fileData -> shouldBePublished(fileData, contextMap)) //
                .doOnNext(fileData -> currentNumberOfTasks.incrementAndGet()) //
                .flatMap(fileData -> fetchFile(fileData, contextMap)) //
                .flatMap(model -> publishToDataRouter(model, contextMap)) //
                .doOnNext(model -> deleteFile(model.getInternalLocation(), contextMap)) //
                .doOnNext(model -> currentNumberOfTasks.decrementAndGet()) //
                .sequential();
    }

    /**
     * called in regular intervals to remove out-dated cached information.
     */
    public void purgeCachedInformation(Instant now) {
        alreadyPublishedFiles.purge(now);
    }

    protected PublishedChecker createPublishedChecker() {
        return new PublishedChecker(applicationConfiguration);
    }

    protected int getCurrentNumberOfTasks() {
        return currentNumberOfTasks.get();
    }

    protected DMaaPMessageConsumer createConsumerTask() {
        return new DMaaPMessageConsumer(this.applicationConfiguration);
    }

    protected FileCollector createFileCollector() {
        return new FileCollector(applicationConfiguration);
    }

    protected DataRouterPublisher createDataRouterPublisher() {
        return new DataRouterPublisher(applicationConfiguration);
    }

    private void onComplete(Map<String, String> contextMap) {
        MDC.setContextMap(contextMap);
        logger.trace("Datafile tasks have been completed");
    }

    private synchronized void onSuccess(FilePublishInformation model, Map<String, String> contextMap) {
        MDC.setContextMap(contextMap);
        logger.info("Datafile file published {}", model.getInternalLocation());
    }

    private void onError(Throwable throwable, Map<String, String> contextMap) {
        MDC.setContextMap(contextMap);
        logger.error("Chain of tasks have been aborted due to errors in Datafile workflow {}", throwable.toString());
    }

    private boolean shouldBePublished(FileData fileData, Map<String, String> contextMap) {
        boolean result = false;
        Path localFilePath = fileData.getLocalFilePath();
        if (alreadyPublishedFiles.put(localFilePath) == null) {
            result = !createPublishedChecker().isFilePublished(fileData.name(), contextMap);
        }
        return result;
    }

    private Mono<FilePublishInformation> fetchFile(FileData fileData, Map<String, String> contextMap) {
        MDC.setContextMap(contextMap);
        return createFileCollector()
                .collectFile(fileData, FILE_TRANSFER_MAX_RETRIES, FILE_TRANSFER_INITIAL_RETRY_TIMEOUT, contextMap)
                .onErrorResume(exception -> handleFetchFileFailure(fileData, contextMap));
    }

    private Mono<FilePublishInformation> handleFetchFileFailure(FileData fileData, Map<String, String> contextMap) {
        MDC.setContextMap(contextMap);
        Path localFilePath = fileData.getLocalFilePath();
        logger.error("File fetching failed, fileData {}", fileData);
        deleteFile(localFilePath, contextMap);
        alreadyPublishedFiles.remove(localFilePath);
        currentNumberOfTasks.decrementAndGet();
        return Mono.empty();
    }

    private Mono<FilePublishInformation> publishToDataRouter(FilePublishInformation model,
            Map<String, String> contextMap) {
        MDC.setContextMap(contextMap);

        return createDataRouterPublisher()
                .publishFile(model, DATA_ROUTER_MAX_RETRIES, DATA_ROUTER_INITIAL_RETRY_TIMEOUT, contextMap)
                .onErrorResume(exception -> handlePublishFailure(model, contextMap));
    }

    private Mono<FilePublishInformation> handlePublishFailure(FilePublishInformation model,
            Map<String, String> contextMap) {
        MDC.setContextMap(contextMap);
        logger.error("File publishing failed: {}", model);
        Path internalFileName = model.getInternalLocation();
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
                .getMessageRouterResponse() //
                .onErrorResume(exception -> handleConsumeMessageFailure(exception, contextMap));
    }

    private Flux<FileReadyMessage> handleConsumeMessageFailure(Throwable exception, Map<String, String> contextMap) {
        MDC.setContextMap(contextMap);
        logger.error("Polling for file ready message failed, exception: {}, config: {}", exception.toString(),
                this.applicationConfiguration.getDmaapConsumerConfiguration());
        return Flux.empty();
    }

    private void deleteFile(Path localFile, Map<String, String> contextMap) {
        MDC.setContextMap(contextMap);
        logger.trace("Deleting file: {}", localFile);
        try {
            Files.delete(localFile);
        } catch (Exception e) {
            logger.trace("Could not delete file: {}", localFile, e);
        }
    }
}
