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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.ftp.FtpsClient;
import org.onap.dcaegen2.collectors.datafile.ftp.SftpClient;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.FileData;
import org.onap.dcaegen2.collectors.datafile.model.FileReadyMessage;
import org.onap.dcaegen2.collectors.datafile.model.MessageMetaData;
import org.onap.dcaegen2.collectors.datafile.service.PublishedFileCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final int MAX_NUMBER_OF_CONCURRENT_TASKS = 200;
    private static final int MAX_ILDLE_THREAD_TIME_TO_LIVE_SECONDS = 10;

    /** Data needed for fetching of one file */
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

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
    private final AppConfig applicationConfiguration;
    private final AtomicInteger currentNumberOfTasks = new AtomicInteger();
    private final Scheduler scheduler =
            Schedulers.newElastic("DataFileCollector", MAX_ILDLE_THREAD_TIME_TO_LIVE_SECONDS);
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
    public void scheduleMainDatafileEventTask() {
        logger.trace("Execution of tasks was registered");
        applicationConfiguration.initFileStreamReader();
        createMainTask().subscribe(this::onSuccess, this::onError, this::onComplete);
    }

    Flux<ConsumerDmaapModel> createMainTask() {
        return fetchMoreFileReadyMessages() //
                .parallel(getParallelism()) // Each FileReadyMessage in a separate thread
                .runOn(scheduler) //
                .flatMap(this::createFileCollectionTask) //
                .filter(this::shouldBePublished) //
                .doOnNext(fileData -> currentNumberOfTasks.incrementAndGet()) //
                .flatMap(this::collectFileFromXnf) //
                .flatMap(this::publishToDataRouter) //
                .doOnNext(model -> deleteFile(Paths.get(model.getInternalLocation()))) //
                .doOnNext(model -> currentNumberOfTasks.decrementAndGet()) //
                .sequential();
    }

    /**
     * called in regular intervals to remove out-dated cached information
     */
    public void purgeCachedInformation(Instant now) {
        alreadyPublishedFiles.purge(now);
    }

    private void onComplete() {
        logger.info("Datafile tasks have been completed");
    }

    private void onSuccess(ConsumerDmaapModel model) {
        logger.info("Datafile consumed tasks {}", model.getInternalLocation());
    }

    private void onError(Throwable throwable) {
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

    private boolean shouldBePublished(FileCollectionData task) {
        return alreadyPublishedFiles.put(task.fileData.getLocalFileName()) == null;
    }

    private Mono<ConsumerDmaapModel> collectFileFromXnf(FileCollectionData fileCollect) {
        final long maxNUmberOfRetries = 3;
        final Duration initialRetryTimeout = Duration.ofSeconds(5);

        return fileCollect.collectorTask
                .execute(fileCollect.fileData, fileCollect.metaData, maxNUmberOfRetries, initialRetryTimeout)
                .onErrorResume(exception -> handleCollectFailure(fileCollect.fileData));
    }

    private Mono<ConsumerDmaapModel> handleCollectFailure(FileData fileData) {
        Path localFileName = fileData.getLocalFileName();
        logger.error("File fetching failed: {}", localFileName);
        deleteFile(localFileName);
        alreadyPublishedFiles.remove(localFileName);
        currentNumberOfTasks.decrementAndGet();
        return Mono.empty();
    }

    private Mono<ConsumerDmaapModel> publishToDataRouter(ConsumerDmaapModel model) {
        final long maxNumberOfRetries = 3;
        final Duration initialRetryTimeout = Duration.ofSeconds(5);

        DataRouterPublisher publisherTask = createDataRouterPublisher();

        return publisherTask.execute(model, maxNumberOfRetries, initialRetryTimeout)
                .onErrorResume(exception -> handlePublishFailure(model, exception));
    }

    private Mono<ConsumerDmaapModel> handlePublishFailure(ConsumerDmaapModel model, Throwable exception) {
        logger.error("File publishing failed: {}, exception: {}", model.getName(), exception);
        Path internalFileName = Paths.get(model.getInternalLocation());
        deleteFile(internalFileName);
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

        return createConsumerTask() //
                .execute() //
                .onErrorResume(this::handleConsumeMessageFailure);
    }

    private Flux<FileReadyMessage> handleConsumeMessageFailure(Throwable exception) {
        logger.error("Polling for file ready message filed, exception: {}", exception);
        return Flux.empty();
    }

    private void deleteFile(Path localFile) {
        logger.trace("Deleting file: {}", localFile);
        try {
            Files.delete(localFile);
        } catch (Exception e) {
            logger.trace("Could not delete file: {}", localFile);
        }
    }

    int getCurrentNumberOfTasks() {
        return currentNumberOfTasks.get();
    }

    DMaaPMessageConsumerTask createConsumerTask() {
        return new DMaaPMessageConsumerTask(this.applicationConfiguration);
    }

    FileCollector createFileCollector(FileData fileData) {
        return new FileCollector(applicationConfiguration, new FtpsClient(fileData.fileServerData()),
                new SftpClient(fileData.fileServerData()));
    }

    DataRouterPublisher createDataRouterPublisher() {
        return new DataRouterPublisher(applicationConfiguration);
    }

}
