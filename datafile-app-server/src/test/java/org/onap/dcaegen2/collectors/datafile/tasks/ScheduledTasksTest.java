/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.dcaegen2.collectors.datafile.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.ftp.Scheme;
import org.onap.dcaegen2.collectors.datafile.model.FileData;
import org.onap.dcaegen2.collectors.datafile.model.FilePublishInformation;
import org.onap.dcaegen2.collectors.datafile.model.FileReadyMessage;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFileData;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFilePublishInformation;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFileReadyMessage;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableMessageMetaData;
import org.onap.dcaegen2.collectors.datafile.model.MessageMetaData;
import org.onap.dcaegen2.collectors.datafile.utils.LoggingUtils;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.ImmutableDmaapPublisherConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class ScheduledTasksTest {

    private static final String PM_FILE_NAME = "A20161224.1030-1045.bin.gz";

    private AppConfig appConfig = mock(AppConfig.class);
    private ScheduledTasks testedObject = spy(new ScheduledTasks(appConfig));

    private int uniqueValue = 0;
    private DMaaPMessageConsumer consumerMock;
    private PublishedChecker publishedCheckerMock;
    private FileCollector fileCollectorMock;
    private DataRouterPublisher dataRouterMock;
    private Map<String, String> contextMap = new HashMap<String, String>();

    @BeforeEach
    private void setUp() {
        DmaapPublisherConfiguration dmaapPublisherConfiguration = new ImmutableDmaapPublisherConfiguration.Builder() //
                .dmaapContentType("application/json") //
                .dmaapHostName("54.45.33.2") //
                .dmaapPortNumber(1234) //
                .dmaapProtocol("https") //
                .dmaapUserName("DFC") //
                .dmaapUserPassword("DFC") //
                .dmaapTopicName("unauthenticated.VES_NOTIFICATION_OUTPUT") //
                .trustStorePath("trustStorePath") //
                .trustStorePasswordPath("trustStorePasswordPath") //
                .keyStorePath("keyStorePath") //
                .keyStorePasswordPath("keyStorePasswordPath") //
                .enableDmaapCertAuth(true) //
                .build(); //
        doReturn(dmaapPublisherConfiguration).when(appConfig).getDmaapPublisherConfiguration();

        consumerMock = mock(DMaaPMessageConsumer.class);
        publishedCheckerMock = mock(PublishedChecker.class);
        fileCollectorMock = mock(FileCollector.class);
        dataRouterMock = mock(DataRouterPublisher.class);

        doReturn(consumerMock).when(testedObject).createConsumerTask();
        doReturn(publishedCheckerMock).when(testedObject).createPublishedChecker();
        doReturn(fileCollectorMock).when(testedObject).createFileCollector();
        doReturn(dataRouterMock).when(testedObject).createDataRouterPublisher();
    }

    private MessageMetaData messageMetaData() {
        return ImmutableMessageMetaData.builder() //
                .productName("productName") //
                .vendorName("") //
                .lastEpochMicrosec("") //
                .sourceName("") //
                .startEpochMicrosec("") //
                .timeZoneOffset("") //
                .changeIdentifier("") //
                .changeType("") //
                .build();
    }

    private FileData fileData(int instanceNumber) {
        return ImmutableFileData.builder() //
                .name("name" + instanceNumber) //
                .fileFormatType("") //
                .fileFormatVersion("") //
                .location("ftpes://192.168.0.101/ftp/rop/" + PM_FILE_NAME + instanceNumber) //
                .scheme(Scheme.FTPS) //
                .compression("") //
                .messageMetaData(messageMetaData()) //
                .build();
    }

    private List<FileData> files(int size, boolean uniqueNames) {
        List<FileData> list = new LinkedList<FileData>();
        for (int i = 0; i < size; ++i) {
            if (uniqueNames) {
                ++uniqueValue;
            }
            list.add(fileData(uniqueValue));
        }
        return list;
    }

    private FileReadyMessage createFileReadyMessage(int numberOfFiles, boolean uniqueNames) {
        return ImmutableFileReadyMessage.builder().files(files(numberOfFiles, uniqueNames)).build();
    }

    private Flux<FileReadyMessage> fileReadyMessageFlux(int numberOfEvents, int filesPerEvent, boolean uniqueNames) {
        List<FileReadyMessage> list = new LinkedList<FileReadyMessage>();
        for (int i = 0; i < numberOfEvents; ++i) {
            list.add(createFileReadyMessage(filesPerEvent, uniqueNames));
        }
        return Flux.fromIterable(list);
    }

    private FilePublishInformation filePublishInformation() {
        return ImmutableFilePublishInformation //
                .builder() //
                .productName("") //
                .vendorName("") //
                .lastEpochMicrosec("") //
                .sourceName("") //
                .startEpochMicrosec("") //
                .timeZoneOffset("") //
                .name("") //
                .location("") //
                .internalLocation(Paths.get("internalLocation")) //
                .compression("") //
                .fileFormatType("") //
                .fileFormatVersion("") //
                .context(new HashMap<String, String>()).build();
    }

    @Test
    public void purgeFileCache() {
        testedObject.publishedFilesCache.put(Paths.get("file.xml"));

        testedObject.purgeCachedInformation(Instant.MAX);

        assertEquals(0, testedObject.publishedFilesCacheSize());
    }

    @Test
    public void nothingToConsume() {
        doReturn(consumerMock).when(testedObject).createConsumerTask();
        doReturn(Flux.empty()).when(consumerMock).getMessageRouterResponse();

        testedObject.executeDatafileMainTask();

        assertEquals(0, testedObject.getCurrentNumberOfTasks());
        verify(consumerMock, times(1)).getMessageRouterResponse();
        verifyNoMoreInteractions(consumerMock);
    }

    @Test
    public void skippingConsumeDueToCurrentNumberOfTasksGreaterThan50() {
        doReturn(51).when(testedObject).getCurrentNumberOfTasks();

        testedObject.executeDatafileMainTask();

        verifyNoMoreInteractions(consumerMock);
    }

    @Test
    public void mainTaskReturnsException() {
        doReturn(Flux.error(new Exception("Failed"))).when(testedObject).fetchMoreFileReadyMessages();

        ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(ScheduledTasks.class);
        testedObject.executeDatafileMainTask();

        Awaitility.await().pollDelay(org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS)
                .until(() -> logAppender.list.size() > 0);

        assertEquals("[ERROR] Chain of tasks have been aborted due to errors in Datafile workflow "
                + "java.lang.Exception: Failed", logAppender.list.get(0).toString());

        verifyNoMoreInteractions(consumerMock);
    }

    @Test
    public void executeDatafileMainTask_successfulCase() {
        final int noOfEvents = 1;
        final int noOfFilesPerEvent = 1;

        Flux<FileReadyMessage> fileReadyMessages = fileReadyMessageFlux(noOfEvents, noOfFilesPerEvent, true);
        doReturn(fileReadyMessages).when(consumerMock).getMessageRouterResponse();

        doReturn(false).when(publishedCheckerMock).isFilePublished(anyString(), any());

        Mono<FilePublishInformation> collectedFile = Mono.just(filePublishInformation());
        doReturn(collectedFile).when(fileCollectorMock).collectFile(notNull(), anyLong(), notNull(), notNull());
        doReturn(collectedFile).when(dataRouterMock).publishFile(notNull(), anyLong(), notNull());

        testedObject.executeDatafileMainTask();
    }

    @Test
    public void executeDatafileMainTask_unexpectedFail() {
        doReturn(Flux.error(new Exception("Failed"))).when(testedObject).createMainTask(contextMap);

        ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(ScheduledTasks.class);
        testedObject.executeDatafileMainTask();

        assertTrue(logAppender.list.toString().contains("[ERROR] Unexpected exception: "));
    }

    @Test
    public void executeDatafileMainTask_consumeFail() {
        doReturn(Flux.error(new Exception("Failed"))).when(consumerMock).getMessageRouterResponse();

        ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(ScheduledTasks.class);
        testedObject.executeDatafileMainTask();

        Awaitility.await().pollDelay(org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS)
                .until(() -> logAppender.list.size() > 0);

        assertTrue(logAppender.list.toString().contains(
                "[ERROR] Polling for file ready message failed, " + "exception: java.lang.Exception: Failed"));
    }

    @Test
    public void executeDatafileMainTask_publishFail() {
        final int noOfEvents = 1;
        final int noOfFilesPerEvent = 1;

        Flux<FileReadyMessage> fileReadyMessages = fileReadyMessageFlux(noOfEvents, noOfFilesPerEvent, true);
        doReturn(fileReadyMessages).when(consumerMock).getMessageRouterResponse();

        doReturn(false).when(publishedCheckerMock).isFilePublished(anyString(), any());

        Mono<FilePublishInformation> collectedFile = Mono.just(filePublishInformation());
        doReturn(collectedFile).when(fileCollectorMock).collectFile(notNull(), anyLong(), notNull(), notNull());
        doReturn(Mono.error(new Exception("Failed"))).when(dataRouterMock).publishFile(notNull(), anyLong(), notNull());

        ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(ScheduledTasks.class);
        testedObject.executeDatafileMainTask();

        Awaitility.await().pollDelay(org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS)
                .until(() -> logAppender.list.size() > 0);

        assertTrue(logAppender.list.toString().contains("[ERROR] File publishing failed: "));
    }

    @Test
    public void consume_successfulCase() {
        final int noOfEvents = 200;
        final int noOfFilesPerEvent = 200;
        final int noOfFiles = noOfEvents * noOfFilesPerEvent;

        Flux<FileReadyMessage> fileReadyMessages = fileReadyMessageFlux(noOfEvents, noOfFilesPerEvent, true);
        doReturn(fileReadyMessages).when(consumerMock).getMessageRouterResponse();

        doReturn(false).when(publishedCheckerMock).isFilePublished(anyString(), any());

        Mono<FilePublishInformation> collectedFile = Mono.just(filePublishInformation());
        doReturn(collectedFile).when(fileCollectorMock).collectFile(notNull(), anyLong(), notNull(), notNull());
        doReturn(collectedFile).when(dataRouterMock).publishFile(notNull(), anyLong(), notNull());

        StepVerifier //
                .create(testedObject.createMainTask(contextMap)) //
                .expectSubscription() //
                .expectNextCount(noOfFiles) //
                .expectComplete() //
                .verify(); //

        assertEquals(0, testedObject.getCurrentNumberOfTasks());
        verify(consumerMock, times(1)).getMessageRouterResponse();
        verify(fileCollectorMock, times(noOfFiles)).collectFile(notNull(), anyLong(), notNull(), notNull());
        verify(dataRouterMock, times(noOfFiles)).publishFile(notNull(), anyLong(), notNull());
        verifyNoMoreInteractions(dataRouterMock);
        verifyNoMoreInteractions(fileCollectorMock);
        verifyNoMoreInteractions(consumerMock);
    }

    @Test
    public void consume_fetchFailedOnce() {
        Flux<FileReadyMessage> fileReadyMessages = fileReadyMessageFlux(2, 2, true); // 4 files
        doReturn(fileReadyMessages).when(consumerMock).getMessageRouterResponse();

        doReturn(false).when(publishedCheckerMock).isFilePublished(anyString(), any());

        Mono<FilePublishInformation> collectedFile = Mono.just(filePublishInformation());
        Mono<Object> error = Mono.error(new Exception("problem"));

        // First file collect will fail, 3 will succeed
        doReturn(error, collectedFile, collectedFile, collectedFile) //
                .when(fileCollectorMock) //
                .collectFile(any(FileData.class), anyLong(), any(Duration.class), notNull());

        doReturn(collectedFile).when(dataRouterMock).publishFile(notNull(), anyLong(), notNull());
        doReturn(collectedFile).when(dataRouterMock).publishFile(notNull(), anyLong(), notNull());

        StepVerifier //
                .create(testedObject.createMainTask(contextMap)) //
                .expectSubscription() //
                .expectNextCount(3) //
                .expectComplete() //
                .verify(); //

        assertEquals(0, testedObject.getCurrentNumberOfTasks());
        verify(consumerMock, times(1)).getMessageRouterResponse();
        verify(fileCollectorMock, times(4)).collectFile(notNull(), anyLong(), notNull(), notNull());
        verify(dataRouterMock, times(3)).publishFile(notNull(), anyLong(), notNull());
        verifyNoMoreInteractions(dataRouterMock);
        verifyNoMoreInteractions(fileCollectorMock);
        verifyNoMoreInteractions(consumerMock);
    }

    @Test
    public void consume_publishFailedOnce() {

        Flux<FileReadyMessage> fileReadyMessages = fileReadyMessageFlux(2, 2, true); // 4 files
        doReturn(fileReadyMessages).when(consumerMock).getMessageRouterResponse();

        doReturn(false).when(publishedCheckerMock).isFilePublished(anyString(), any());

        Mono<FilePublishInformation> collectedFile = Mono.just(filePublishInformation());
        doReturn(collectedFile).when(fileCollectorMock).collectFile(notNull(), anyLong(), notNull(), notNull());

        Mono<Object> error = Mono.error(new Exception("problem"));
        // One publish will fail, the rest will succeed
        doReturn(collectedFile, error, collectedFile, collectedFile) //
                .when(dataRouterMock) //
                .publishFile(notNull(), anyLong(), notNull());

        StepVerifier //
                .create(testedObject.createMainTask(contextMap)) //
                .expectSubscription() //
                .expectNextCount(3) // 3 completed files
                .expectComplete() //
                .verify(); //

        assertEquals(0, testedObject.getCurrentNumberOfTasks());
        verify(consumerMock, times(1)).getMessageRouterResponse();
        verify(fileCollectorMock, times(4)).collectFile(notNull(), anyLong(), notNull(), notNull());
        verify(dataRouterMock, times(4)).publishFile(notNull(), anyLong(), notNull());
        verifyNoMoreInteractions(dataRouterMock);
        verifyNoMoreInteractions(fileCollectorMock);
        verifyNoMoreInteractions(consumerMock);
    }

    @Test
    public void consume_successfulCase_sameFileNames() {
        final int noOfEvents = 1;
        final int noOfFilesPerEvent = 100;

        // 100 files with the same name
        Flux<FileReadyMessage> fileReadyMessages = fileReadyMessageFlux(noOfEvents, noOfFilesPerEvent, false);
        doReturn(fileReadyMessages).when(consumerMock).getMessageRouterResponse();

        doReturn(false).when(publishedCheckerMock).isFilePublished(anyString(), any());

        Mono<FilePublishInformation> collectedFile = Mono.just(filePublishInformation());
        doReturn(collectedFile).when(fileCollectorMock).collectFile(notNull(), anyLong(), notNull(), notNull());
        doReturn(collectedFile).when(dataRouterMock).publishFile(notNull(), anyLong(), notNull());

        StepVerifier //
                .create(testedObject.createMainTask(contextMap)).expectSubscription() //
                .expectNextCount(1) // 99 is skipped
                .expectComplete() //
                .verify(); //

        assertEquals(0, testedObject.getCurrentNumberOfTasks());
        verify(consumerMock, times(1)).getMessageRouterResponse();
        verify(fileCollectorMock, times(1)).collectFile(notNull(), anyLong(), notNull(), notNull());
        verify(dataRouterMock, times(1)).publishFile(notNull(), anyLong(), notNull());
        verify(publishedCheckerMock, times(1)).isFilePublished(notNull(), notNull());
        verifyNoMoreInteractions(dataRouterMock);
        verifyNoMoreInteractions(fileCollectorMock);
        verifyNoMoreInteractions(consumerMock);
        verifyNoMoreInteractions(dataRouterMock);
    }
}
