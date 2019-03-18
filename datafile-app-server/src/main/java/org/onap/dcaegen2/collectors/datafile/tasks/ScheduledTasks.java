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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.ftp.FtpsClient;
import org.onap.dcaegen2.collectors.datafile.ftp.SftpClient;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.FeedData;
import org.onap.dcaegen2.collectors.datafile.model.FileData;
import org.onap.dcaegen2.collectors.datafile.model.FileReadyMessage;
import org.onap.dcaegen2.collectors.datafile.model.MessageMetaData;
import org.onap.dcaegen2.collectors.datafile.service.PublishedFileCache;
import org.onap.dcaegen2.services.sdk.rest.services.model.logging.MdcVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * This implements the main flow of the data file collector. Fetch file ready events from the message router, fetch new
 * files from the PNF publish these in the data router.
 *
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 3/23/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class ScheduledTasks {

    private static final int MAX_NUMBER_OF_CONCURRENT_TASKS = 200;
    private static final int MAX_ILDLE_THREAD_TIME_TO_LIVE_SECONDS = 10;

    /** Data needed for fetching of one file. */
    private class FileCollectionData {
        final FileData fileData;
        final FileCollector collectorTask;
        final MessageMetaData metaData;

        FileCollectionData(FileData fd, FileCollector collectorTask, MessageMetaData metaData) {
            this.fileData = fd;
            this.collectorTask = collectorTask;
            this.metaData = metaData;
        }
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final AppConfig applicationConfiguration;
    private final FeedData drFeedData;

    private final AtomicInteger currentNumberOfTasks = new AtomicInteger();
    private final Scheduler scheduler =
            Schedulers.newElastic("DataFileCollector", MAX_ILDLE_THREAD_TIME_TO_LIVE_SECONDS);
    PublishedFileCache alreadyPublishedFiles = new PublishedFileCache();


    /**
     * Constructor for task registration in Datafile Workflow.
     *
     * @param applicationConfiguration - application configuration
     * @param feedData the URLs to use to communicate with the DataRouter.
     */
    public ScheduledTasks(AppConfig applicationConfiguration, FeedData feedData) {
        this.applicationConfiguration = applicationConfiguration;
        this.drFeedData = feedData;
    }

    /**
     * Main function for scheduling for the file collection Workflow.
     *
     * @param contextMap context for logging.
     */
    public void scheduleMainDatafileEventTask(Map<String, String> contextMap) {
        MdcVariables.setMdcContextMap(contextMap);
        logger.trace("Execution of tasks was registered");
        applicationConfiguration.initFileStreamReader();
        createMainTask(contextMap) //
                .subscribe(model -> onSuccess(model, contextMap), thr -> onError(thr, contextMap),
                        () -> onComplete(contextMap));
    }

    Flux<ConsumerDmaapModel> createMainTask(Map<String, String> contextMap) {
        return fetchMoreFileReadyMessages() //
                .parallel(getParallelism()) // Each FileReadyMessage in a separate thread
                .runOn(scheduler) //
                .flatMap(this::createFileCollectionTask) //
                .filter(fileData -> shouldBePublished(fileData, contextMap)) //
                .doOnNext(fileData -> currentNumberOfTasks.incrementAndGet()) //
                .flatMap(fileData -> collectFileFromXnf(fileData, contextMap)) //
                .flatMap(model -> publishToDataRouter(model, contextMap)) //
                .doOnNext(model -> deleteFile(model.getInternalLocation(), contextMap)) //
                .doOnNext(model -> currentNumberOfTasks.decrementAndGet()) //
                .sequential();
    }

    /**
     * Called at regular intervals to remove out-dated cached information.
     *
     * @param now the time of the purge.
     */
    public void purgeCachedInformation(Instant now) {
        alreadyPublishedFiles.purge(now);
    }

    private void onComplete(Map<String, String> contextMap) {
        MdcVariables.setMdcContextMap(contextMap);
        logger.info("Datafile tasks have been completed");
    }

    private void onSuccess(ConsumerDmaapModel model, Map<String, String> contextMap) {
        MdcVariables.setMdcContextMap(contextMap);
        logger.info("Datafile consumed tasks {}", model.getInternalLocation());
    }

    private void onError(Throwable throwable, Map<String, String> contextMap) {
        MdcVariables.setMdcContextMap(contextMap);
        logger.error("Chain of tasks have been aborted due to errors in Datafile workflow {}", throwable);
    }

    private int getParallelism() {
        if (MAX_NUMBER_OF_CONCURRENT_TASKS - getCurrentNumberOfTasks() > 0) {
            return MAX_NUMBER_OF_CONCURRENT_TASKS - getCurrentNumberOfTasks();
        } else {
            return 1; // We need at least one rail/thread
        }
    }

    private Flux<FileCollectionData> createFileCollectionTask(FileReadyMessage availableFiles) {
        List<FileCollectionData> fileCollects = new ArrayList<>();

        for (FileData fileData : availableFiles.files()) {
            fileCollects.add(
                    new FileCollectionData(fileData, createFileCollector(fileData), availableFiles.messageMetaData()));
        }
        return Flux.fromIterable(fileCollects);
    }

    private boolean shouldBePublished(FileCollectionData task, Map<String, String> contextMap) {
        boolean result = false;
        Path localFileName = task.fileData.getLocalFileName();
        if (alreadyPublishedFiles.put(localFileName) == null) {
            result = !createPublishedChecker().execute(localFileName.getFileName().toString(), drFeedData, contextMap);
        }
        return result;
    }

    private Mono<ConsumerDmaapModel> collectFileFromXnf(FileCollectionData fileCollect,
            Map<String, String> contextMap) {
        final long maxNUmberOfRetries = 3;
        final Duration initialRetryTimeout = Duration.ofSeconds(5);

        MdcVariables.setMdcContextMap(contextMap);
        return fileCollect.collectorTask.execute(fileCollect.fileData, fileCollect.metaData, maxNUmberOfRetries,
                initialRetryTimeout, contextMap)
                .onErrorResume(exception -> handleCollectFailure(fileCollect.fileData, contextMap));
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
        final long maxNumberOfRetries = 3;
        final Duration initialRetryTimeout = Duration.ofSeconds(5);


        MdcVariables.setMdcContextMap(contextMap);
        return createDataRouterPublisher()
                .execute(model, drFeedData, maxNumberOfRetries, initialRetryTimeout, contextMap)
                .onErrorResume(exception -> handlePublishFailure(model, exception, contextMap));
    }

    private Mono<ConsumerDmaapModel> handlePublishFailure(ConsumerDmaapModel model, Throwable exception,
            Map<String, String> contextMap) {
        MdcVariables.setMdcContextMap(contextMap);
        logger.error("File publishing failed: {}, exception: {}", model.getName(), exception);
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
        logger.trace("Consuming new file ready messages, current number of tasks: {}", getCurrentNumberOfTasks());
        if (getCurrentNumberOfTasks() > MAX_NUMBER_OF_CONCURRENT_TASKS) {
            return Flux.empty();
        }

        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return createConsumerTask() //
                .execute() //
                .onErrorResume(exception -> handleConsumeMessageFailure(exception, contextMap));
    }

    private Flux<FileReadyMessage> handleConsumeMessageFailure(Throwable exception, Map<String, String> contextMap) {
        MdcVariables.setMdcContextMap(contextMap);
        logger.error("Polling for file ready message failed, exception: {}", exception);
        return Flux.empty();
    }

    private void deleteFile(Path localFile, Map<String, String> contextMap) {
        MdcVariables.setMdcContextMap(contextMap);
        logger.trace("Deleting file: {}", localFile);
        try {
            Files.delete(localFile);
        } catch (Exception e) {
            logger.trace("Could not delete file: {}", localFile, e);
        }
    }

    int getCurrentNumberOfTasks() {
        return currentNumberOfTasks.get();
    }

    DMaaPMessageConsumerTask createConsumerTask() {
        return new DMaaPMessageConsumerTask(this.applicationConfiguration);
    }

    PublishedChecker createPublishedChecker() {
        return new PublishedChecker();
    }

    DataRouterPublisher createDataRouterPublisher() {
        return new DataRouterPublisher();
    }

    FileCollector createFileCollector(FileData fileData) {
        return new FileCollector(applicationConfiguration, new FtpsClient(fileData.fileServerData()),
                new SftpClient(fileData.fileServerData()));
    }
}
