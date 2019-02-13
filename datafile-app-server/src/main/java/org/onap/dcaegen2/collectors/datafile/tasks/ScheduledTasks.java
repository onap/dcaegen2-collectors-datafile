/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018 Nordix Foundation. All rights reserved.
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

    private class SynchronizedCounter {
        private volatile int counter = 0;

        synchronized void inc() {
            counter += 1;
        }

        synchronized void dec() {
            counter -= 1;
        }

        synchronized int get() {
            return counter;
        }
    }

    // Data needed for fetching of files
    private class FileCollectionTask {
        final FileData fileData;
        final FileCollector collectorTask; // Same object, ftp session etc. can be used for each file in one VES
                                                  // event
        final MessageMetaData metaData;

        FileCollectionTask(FileData fd, FileCollector collectorTask, MessageMetaData metaData) {
            this.fileData = fd;
            this.collectorTask = collectorTask;
            this.metaData = metaData;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
    private final AppConfig applicationConfiguration;
    private final SynchronizedCounter taskCounter = new SynchronizedCounter();
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
            .parallel()
            .runOn(Schedulers.parallel())
            .flatMap(this::createFileCollectionTask)
            .filter(task -> shouldBePublished(task))
            .doOnEach(task -> taskCounter.inc())
            .flatMap(this::collectFileFromXnf)
            .flatMap(this::publishToDataRouter)
            .flatMap(model -> deleteFile(Paths.get(model.getInternalLocation())))
            .doOnEach(model -> taskCounter.dec())
            .sequential()
            .subscribe(this::onSuccess, this::onError, this::onComplete);
        //@formatter:on
    }

    private void onComplete() {
        logger.info("Datafile tasks have been completed");
    }

    private void onSuccess(Path localFile) {
        logger.info("Datafile consumed tasks." + localFile);
    }

    private void onError(Throwable throwable) {
        logger.error("Chain of tasks have been aborted due to errors in Datafile workflow {}", throwable);
    }

    private Flux<FileCollectionTask> createFileCollectionTask(FileReadyMessage availableFiles) {
        List<FileCollectionTask> fileCollects = new ArrayList<>();

        for (FileData fileData : availableFiles.files()) {
            FileCollector task = new FileCollector(applicationConfiguration,
                    new FtpsClient(fileData.fileServerData()), new SftpClient(fileData.fileServerData()));
            fileCollects.add(new FileCollectionTask(fileData, task, availableFiles.messageMetaData()));
        }
        return Flux.fromIterable(fileCollects);
    }

    private boolean shouldBePublished(FileCollectionTask task) {
        return alreadyPublishedFiles.add(task.fileData.getLocalFileName());
    }

    private Mono<ConsumerDmaapModel> collectFileFromXnf(FileCollectionTask fileCollect) {
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
        taskCounter.dec();
        return Mono.empty();
    }

    private Flux<ConsumerDmaapModel> publishToDataRouter(ConsumerDmaapModel model) {
        final long maxNumberOfRetries = 10;
        final Duration initialRetryTimeout = Duration.ofSeconds(5);

        DataRouterPublisher publisherTask = new DataRouterPublisher(applicationConfiguration);

        return publisherTask.execute(model, maxNumberOfRetries, initialRetryTimeout)
                .onErrorResume(exception -> handlePublishFailure(model, exception));

    }

    private Flux<ConsumerDmaapModel> handlePublishFailure(ConsumerDmaapModel model, Throwable exception) {
        logger.error("File publishing failed: {}, exception: {}", model.getName(), exception);
        Path internalFileName = Paths.get(model.getInternalLocation());
        deleteFile(internalFileName);
        alreadyPublishedFiles.remove(internalFileName);
        taskCounter.dec();
        return Flux.empty();
    }

    private Flux<FileReadyMessage> consumeMessagesFromDmaap() {
        final int currentNumberOfTasks = taskCounter.get();
        logger.trace("Consuming new file ready messages, current number of tasks: {}", currentNumberOfTasks);

        final DMaaPMessageConsumerTask messageConsumerTask =
                new DMaaPMessageConsumerTask(this.applicationConfiguration);
        return messageConsumerTask.execute();
    }

    private Flux<Path> deleteFile(Path localFile) {
        try {
            Files.delete(localFile);
        } catch (Exception e) {
            logger.warn("Could not remove file :{}, {}", localFile, e);
        }
        return Flux.just(localFile);
    }
}
