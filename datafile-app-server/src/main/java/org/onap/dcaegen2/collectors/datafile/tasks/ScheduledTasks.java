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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.ftp.FtpsClient;
import org.onap.dcaegen2.collectors.datafile.ftp.SftpClient;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.FileData;
import org.onap.dcaegen2.collectors.datafile.model.FileReadyMessage;
import org.onap.dcaegen2.collectors.datafile.model.MessageMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 3/23/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
@Component
public class ScheduledTasks {

    private static final int MAX_NUMBER_OF_CONCURRENT_TASKS = 200;

    /** Data needed for fetching of files from one PNF */
    private class FileCollectionData {
        final FileData fileData;
        final FileCollector collectorTask; // Same object, ftp session etc. can be used for each file in one VES
                                                  // event
        final MessageMetaData metaData;

        FileCollectionData(FileData fd, FileCollector collectorTask, MessageMetaData metaData) {
            this.fileData = fd;
            this.collectorTask = collectorTask;
            this.metaData = metaData;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
    private final AppConfig applicationConfiguration;
    private final AtomicInteger taskCounter = new AtomicInteger();
    private final Set<Path> alreadyPublishedFiles = Collections.synchronizedSet(new HashSet<Path>());

    /**
     * Constructor for task registration in Datafile Workflow.
     *
     * @param applicationConfiguration - application configuration
     * @param xnfCollectorTask - second task
     * @param dmaapPublisherTask - third task
     */
    @Autowired
    public ScheduledTasks(AppConfig applicationConfiguration) {
        this.applicationConfiguration = applicationConfiguration;
    }

    /**
     * Main function for scheduling Datafile Workflow.
     */
    public void scheduleMainDatafileEventTask() {
        logger.trace("Execution of tasks was registered");
        applicationConfiguration.initFileStreamReader();
        //@formatter:off
        consumeMessagesFromDmaap()
            .parallel() // Each FileReadyMessage in a separate thread
            .runOn(Schedulers.parallel())
            .flatMap(this::createFileCollectionTask)
            .filter(this::shouldBePublished)
            .doOnNext(fileData -> taskCounter.incrementAndGet())
            .flatMap(this::collectFileFromXnf)
            .flatMap(this::publishToDataRouter)
            .doOnNext(model -> deleteFile(Paths.get(model.getInternalLocation())))
            .doOnNext(model -> taskCounter.decrementAndGet())
            .sequential()
            .subscribe(this::onSuccess, this::onError, this::onComplete);
        //@formatter:on
    }

    private void onComplete() {
        logger.info("Datafile tasks have been completed");
    }

    private void onSuccess(ConsumerDmaapModel model) {
        logger.info("Datafile consumed tasks." + model.getInternalLocation());
    }

    private void onError(Throwable throwable) {
        logger.error("Chain of tasks have been aborted due to errors in Datafile workflow {}", throwable);
    }

    private Flux<FileCollectionData> createFileCollectionTask(FileReadyMessage availableFiles) {
        List<FileCollectionData> fileCollects = new ArrayList<>();

        for (FileData fileData : availableFiles.files()) {
            FileCollector task = new FileCollector(applicationConfiguration,
                    new FtpsClient(fileData.fileServerData()), new SftpClient(fileData.fileServerData()));
            fileCollects.add(new FileCollectionData(fileData, task, availableFiles.messageMetaData()));
        }
        return Flux.fromIterable(fileCollects);
    }

    private boolean shouldBePublished(FileCollectionData task) {
        return alreadyPublishedFiles.add(task.fileData.getLocalFileName());
    }

    private Mono<ConsumerDmaapModel> collectFileFromXnf(FileCollectionData fileCollect) {
        final long maxNUmberOfRetries = 3;
        final Duration initialRetryTimeout = Duration.ofSeconds(5);

        return fileCollect.collectorTask
                .execute(fileCollect.fileData, fileCollect.metaData, maxNUmberOfRetries, initialRetryTimeout)
                .onErrorResume(exception -> handleCollectFailure(fileCollect.fileData, exception));
    }

    private Mono<ConsumerDmaapModel> handleCollectFailure(FileData fileData, Throwable exception) {
        logger.error("File fetching failed: {}, reason: {}", fileData.name(), exception.getMessage());
        deleteFile(fileData.getLocalFileName());
        alreadyPublishedFiles.remove(fileData.getLocalFileName());
        taskCounter.decrementAndGet();
        return Mono.empty();
    }

    private Mono<ConsumerDmaapModel> publishToDataRouter(ConsumerDmaapModel model) {
        final long maxNumberOfRetries = 3;
        final Duration initialRetryTimeout = Duration.ofSeconds(5);

        DataRouterPublisher publisherTask = new DataRouterPublisher(applicationConfiguration);

        return publisherTask.execute(model, maxNumberOfRetries, initialRetryTimeout)
                .onErrorResume(exception -> handlePublishFailure(model, exception));

    }

    private Mono<ConsumerDmaapModel> handlePublishFailure(ConsumerDmaapModel model, Throwable exception) {
        logger.error("File publishing failed: {}, exception: {}", model.getName(), exception);
        Path internalFileName = Paths.get(model.getInternalLocation());
        deleteFile(internalFileName);
        alreadyPublishedFiles.remove(internalFileName);
        taskCounter.decrementAndGet();
        return Mono.empty();
    }

    private Flux<FileReadyMessage> consumeMessagesFromDmaap() {
        final int currentNumberOfTasks = taskCounter.get();
        logger.trace("Consuming new file ready messages, current number of tasks: {}", currentNumberOfTasks);
        if (currentNumberOfTasks > MAX_NUMBER_OF_CONCURRENT_TASKS) {
            return Flux.empty();
        }

        final DMaaPMessageConsumerTask messageConsumerTask =
                new DMaaPMessageConsumerTask(this.applicationConfiguration);
        return messageConsumerTask.execute()
                .onErrorResume(exception -> handleConsumeMessageFailure(exception));
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
            logger.warn("Could not delete file: {}, {}", localFile, e);
        }
    }
}
