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
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
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

    private static final int NUMBER_OF_WORKER_THREADS = 200;
    private static final int MAX_TASKS_FOR_POLLING = 50;
    private static final long DATA_ROUTER_MAX_RETRIES = 5;
    private static final Duration DATA_ROUTER_INITIAL_RETRY_TIMEOUT = Duration.ofSeconds(2);
    private static final long FILE_TRANSFER_MAX_RETRIES = 3;
    private static final Duration FILE_TRANSFER_INITIAL_RETRY_TIMEOUT = Duration.ofSeconds(5);

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    private final AppConfig applicationConfiguration;
    private final AtomicInteger currentNumberOfTasks = new AtomicInteger();
    private final AtomicInteger threadPoolQueueSize = new AtomicInteger();
    private final AtomicInteger currentNumberOfSubscriptions = new AtomicInteger();
    private final Scheduler scheduler = Schedulers.newParallel("FileCollectorWorker", NUMBER_OF_WORKER_THREADS);
    PublishedFileCache publishedFilesCache = new PublishedFileCache();

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
            if (getCurrentNumberOfTasks() > MAX_TASKS_FOR_POLLING || this.threadPoolQueueSize.get() > 0) {
                logger.info(
                        "Skipping consuming new files; current number of tasks: {}, number of subscriptions: {}, "
                                + "published files: {}, number of queued VES events: {}",
                        getCurrentNumberOfTasks(), this.currentNumberOfSubscriptions.get(), publishedFilesCache.size(),
                        threadPoolQueueSize.get());
                return;
            }
            if (this.applicationConfiguration.getDmaapConsumerConfiguration() == null) {
                logger.warn("No configuration loaded, skipping polling for messages");
                return;
            }

            currentNumberOfSubscriptions.incrementAndGet();
            Map<String, String> context = MappedDiagnosticContext.initializeTraceContext();
            logger.trace("Execution of tasks was registered");
            createMainTask(context) //
                    .subscribe(ScheduledTasks::onSuccess, //
                            throwable -> {
                                onError(throwable, context);
                                currentNumberOfSubscriptions.decrementAndGet();
                            }, //
                            () -> {
                                onComplete(context);
                                currentNumberOfSubscriptions.decrementAndGet();
                            });
        } catch (Exception e) {
            logger.error("Unexpected exception: {}", e.toString(), e);
        }
    }

    Flux<FilePublishInformation> createMainTask(Map<String, String> context) {
        return fetchMoreFileReadyMessages() //
                .doOnNext(fileReadyMessage -> threadPoolQueueSize.incrementAndGet()) //
                .parallel(NUMBER_OF_WORKER_THREADS) // Each FileReadyMessage in a separate thread
                .runOn(scheduler) //
                .doOnNext(fileReadyMessage -> threadPoolQueueSize.decrementAndGet()) //
                .flatMap(fileReadyMessage -> Flux.fromIterable(fileReadyMessage.files())) //
                .flatMap(fileData -> createMdcContext(fileData, context)) //
                .filter(this::isFeedConfigured) //
                .filter(this::shouldBePublished) //
                .doOnNext(fileData -> currentNumberOfTasks.incrementAndGet()) //
                .flatMap(this::fetchFile, false, 1, 1) //
                .flatMap(this::publishToDataRouter, false, 1, 1) //
                .doOnNext(publishInfo -> deleteFile(publishInfo.getInternalLocation(), publishInfo.getContext())) //
                .doOnNext(publishInfo -> currentNumberOfTasks.decrementAndGet()) //
                .sequential();
    }

    private class FileDataWithContext {
        public final FileData fileData;
        public final Map<String, String> context;

        public FileDataWithContext(FileData fileData, Map<String, String> context) {
            this.fileData = fileData;
            this.context = context;
        }
    }

    /**
     * called in regular intervals to remove out-dated cached information.
     */
    public void purgeCachedInformation(Instant now) {
        publishedFilesCache.purge(now);
    }

    protected PublishedChecker createPublishedChecker() {
        return new PublishedChecker(applicationConfiguration);
    }

    public int getCurrentNumberOfTasks() {
        return currentNumberOfTasks.get();
    }

    public int publishedFilesCacheSize() {
        return publishedFilesCache.size();
    }

    public int getCurrentNumberOfSubscriptions() {
        return currentNumberOfSubscriptions.get();
    }

    public int getThreadPoolQueueSize() {
        return this.threadPoolQueueSize.get();
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

    private static void onComplete(Map<String, String> contextMap) {
        MDC.setContextMap(contextMap);
        logger.trace("Datafile tasks have been completed");
    }

    private static synchronized void onSuccess(FilePublishInformation publishInfo) {
        MDC.setContextMap(publishInfo.getContext());
        logger.info("Datafile file published {}", publishInfo.getInternalLocation());
    }

    private static void onError(Throwable throwable, Map<String, String> context) {
        MDC.setContextMap(context);
        logger.error("Chain of tasks have been aborted due to errors in Datafile workflow {}", throwable.toString());
    }

    private Mono<FileDataWithContext> createMdcContext(FileData fileData, Map<String, String> context) {
        MDC.setContextMap(context);
        context = MappedDiagnosticContext.setRequestId(fileData.name());
        FileDataWithContext pair = new FileDataWithContext(fileData, context);
        return Mono.just(pair);
    }

    private boolean isFeedConfigured(FileDataWithContext fileData) {
        if (applicationConfiguration.isFeedConfigured(fileData.fileData.messageMetaData().changeIdentifier())) {
            return true;
        } else {
            logger.info("No feed is configured for: {}, file ignored: {}",
                    fileData.fileData.messageMetaData().changeIdentifier(), fileData.fileData.name());
            return false;
        }
    }

    private boolean shouldBePublished(FileDataWithContext fileData) {
        Path localFilePath = fileData.fileData.getLocalFilePath();
        if (publishedFilesCache.put(localFilePath) == null) {
            try {
                boolean result = !createPublishedChecker().isFilePublished(fileData.fileData.name(),
                        fileData.fileData.messageMetaData().changeIdentifier(), fileData.context);
                return result;
            } catch (DatafileTaskException e) {
                logger.error("Cannot check if a file {} is published", fileData.fileData.name(), e);
                return true; // Publish it then
            }
        } else {
            return false;
        }
    }

    private Mono<FilePublishInformation> fetchFile(FileDataWithContext fileData) {
        MDC.setContextMap(fileData.context);
        return createFileCollector() //
                .collectFile(fileData.fileData, FILE_TRANSFER_MAX_RETRIES, FILE_TRANSFER_INITIAL_RETRY_TIMEOUT,
                        fileData.context) //
                .onErrorResume(exception -> handleFetchFileFailure(fileData));
    }

    private Mono<FilePublishInformation> handleFetchFileFailure(FileDataWithContext fileData) {
        MDC.setContextMap(fileData.context);
        Path localFilePath = fileData.fileData.getLocalFilePath();
        logger.error("File fetching failed, fileData {}", fileData.fileData);
        deleteFile(localFilePath, fileData.context);
        publishedFilesCache.remove(localFilePath);
        currentNumberOfTasks.decrementAndGet();
        return Mono.empty();
    }

    private Mono<FilePublishInformation> publishToDataRouter(FilePublishInformation publishInfo) {
        MDC.setContextMap(publishInfo.getContext());

        return createDataRouterPublisher()
                .publishFile(publishInfo, DATA_ROUTER_MAX_RETRIES, DATA_ROUTER_INITIAL_RETRY_TIMEOUT)
                .onErrorResume(exception -> handlePublishFailure(publishInfo));
    }

    private Mono<FilePublishInformation> handlePublishFailure(FilePublishInformation publishInfo) {
        MDC.setContextMap(publishInfo.getContext());
        logger.error("File publishing failed: {}", publishInfo);
        Path internalFileName = publishInfo.getInternalLocation();
        deleteFile(internalFileName, publishInfo.getContext());
        publishedFilesCache.remove(internalFileName);
        currentNumberOfTasks.decrementAndGet();
        return Mono.empty();
    }

    /**
     * Fetch more messages from the message router. This is done in a polling/blocking fashion.
     */
    Flux<FileReadyMessage> fetchMoreFileReadyMessages() {
        logger.info(
                "Consuming new file ready messages, current number of tasks: {}, published files: {}, "
                        + "number of subscriptions: {}",
                getCurrentNumberOfTasks(), publishedFilesCache.size(), this.currentNumberOfSubscriptions.get());

        Map<String, String> context = MDC.getCopyOfContextMap();
        try {
            return createConsumerTask() //
                    .getMessageRouterResponse() //
                    .onErrorResume(exception -> handleConsumeMessageFailure(exception, context));
        } catch (Exception e) {
            logger.error("Could not create message consumer task", e);
            return Flux.empty();
        }
    }

    private Flux<FileReadyMessage> handleConsumeMessageFailure(Throwable exception, Map<String, String> context) {
        MDC.setContextMap(context);
        logger.error("Polling for file ready message failed, exception: {}, config: {}", exception.toString(),
                this.applicationConfiguration.getDmaapConsumerConfiguration());
        return Flux.empty();
    }

    private static void deleteFile(Path localFile, Map<String, String> context) {
        MDC.setContextMap(context);
        logger.trace("Deleting file: {}", localFile);
        try {
            Files.delete(localFile);
        } catch (Exception e) {
            logger.trace("Could not delete file: {}", localFile, e);
        }
    }
}
