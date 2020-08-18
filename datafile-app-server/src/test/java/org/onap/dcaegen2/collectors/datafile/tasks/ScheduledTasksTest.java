/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019 Nordix Foundation.
 * Copyright (C) 2020 Nokia. All rights reserved.
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

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.configuration.ConsumerConfiguration;
import org.onap.dcaegen2.collectors.datafile.configuration.ImmutablePublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.configuration.PublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
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
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.api.MessageRouterSubscriber;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.MessageRouterSubscribeRequest;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.config.MessageRouterSubscriberConfig;
import org.onap.dcaegen2.services.sdk.rest.services.model.logging.MdcVariables;
import org.slf4j.MDC;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class ScheduledTasksTest {

    private static final String PM_FILE_NAME = "A20161224.1030-1045.bin.gz";
    private static final String CHANGE_IDENTIFIER = "PM_MEAS_FILES";

    private AppConfig appConfig = mock(AppConfig.class);
    private ScheduledTasks testedObject;

    private int uniqueValue = 0;
    private DMaaPMessageConsumer consumerMock;
    private PublishedChecker publishedCheckerMock;
    private FileCollector fileCollectorMock;
    private DataRouterPublisher dataRouterMock;
    private Map<String, String> contextMap = new HashMap<String, String>();

    private final String publishUrl = "https://54.45.33.2:1234/unauthenticated.VES_NOTIFICATION_OUTPUT";

    @BeforeEach
    private void setUp() throws DatafileTaskException {
        testedObject = spy(new ScheduledTasks(appConfig));

        consumerMock = mock(DMaaPMessageConsumer.class);
        publishedCheckerMock = mock(PublishedChecker.class);
        fileCollectorMock = mock(FileCollector.class);
        dataRouterMock = mock(DataRouterPublisher.class);

        doReturn(consumerMock).when(testedObject).createConsumerTask();
        doReturn(publishedCheckerMock).when(testedObject).createPublishedChecker();
        doReturn(fileCollectorMock).when(testedObject).createFileCollector();
        doReturn(dataRouterMock).when(testedObject).createDataRouterPublisher();
    }

    private void setUpConfiguration() throws DatafileTaskException {
        final PublisherConfiguration dmaapPublisherConfiguration = ImmutablePublisherConfiguration.builder() //
            .publishUrl(publishUrl) //
            .logUrl("") //
            .userName("userName") //
            .passWord("passWord") //
            .trustStorePath("trustStorePath") //
            .trustStorePasswordPath("trustStorePasswordPath") //
            .keyStorePath("keyStorePath") //
            .keyStorePasswordPath("keyStorePasswordPath") //
            .enableDmaapCertAuth(true) //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .build(); //
        final ConsumerConfiguration dmaapConsumerConfiguration =
            new ConsumerConfiguration(mock(MessageRouterSubscriberConfig.class), mock(MessageRouterSubscriber.class),
                mock(MessageRouterSubscribeRequest.class));


        doReturn(dmaapPublisherConfiguration).when(appConfig).getPublisherConfiguration(CHANGE_IDENTIFIER);
        doReturn(dmaapConsumerConfiguration).when(appConfig).getDmaapConsumerConfiguration();
        doReturn(true).when(appConfig).isFeedConfigured(CHANGE_IDENTIFIER);
    }

    private MessageMetaData messageMetaData() {
        return ImmutableMessageMetaData.builder() //
            .productName("productName") //
            .vendorName("") //
            .lastEpochMicrosec("") //
            .sourceName("") //
            .startEpochMicrosec("") //
            .timeZoneOffset("") //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .changeType("") //
            .build();
    }

    private FileData fileData(int instanceNumber) {
        return ImmutableFileData.builder() //
            .name(PM_FILE_NAME + instanceNumber) //
            .fileFormatType("") //
            .fileFormatVersion("") //
            .location("ftpes://192.168.0.101/ftp/rop/" + PM_FILE_NAME + instanceNumber) //
            .scheme(Scheme.FTPES) //
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
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .context(new HashMap<String, String>()).build();
    }

    @Test
    public void purgeFileCache() {
        testedObject.publishedFilesCache.put(Paths.get("file.xml"));

        testedObject.purgeCachedInformation(Instant.MAX);

        assertEquals(0, testedObject.publishedFilesCacheSize());
    }

    @Test
    public void nothingToConsume() throws DatafileTaskException {
        setUpConfiguration();

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
    public void executeDatafileMainTask_successfulCase() throws DatafileTaskException {
        setUpConfiguration();

        final int noOfEvents = 1;
        final int noOfFilesPerEvent = 1;

        Flux<FileReadyMessage> fileReadyMessages = fileReadyMessageFlux(noOfEvents, noOfFilesPerEvent, true);
        doReturn(fileReadyMessages).when(consumerMock).getMessageRouterResponse();

        doReturn(false).when(publishedCheckerMock).isFilePublished(anyString(), any(), any());

        Mono<FilePublishInformation> collectedFile = Mono.just(filePublishInformation());
        doReturn(collectedFile).when(fileCollectorMock).collectFile(notNull(), anyLong(), notNull(), notNull());
        doReturn(collectedFile).when(dataRouterMock).publishFile(notNull(), anyLong(), notNull());

        testedObject.executeDatafileMainTask();

        await().untilAsserted(() -> assertEquals("currentNumberOfSubscriptions should have been 0", 0,
            testedObject.getCurrentNumberOfSubscriptions()));

        assertFalse(StringUtils.isBlank(MDC.get(MdcVariables.REQUEST_ID)));

        verify(appConfig).getDmaapConsumerConfiguration();
        verify(appConfig).isFeedConfigured(CHANGE_IDENTIFIER);
        verifyNoMoreInteractions(appConfig);

        assertEquals("totalReceivedEvents should have been 1", 1, testedObject.getCounters().getTotalReceivedEvents());
    }

    @Test
    public void executeDatafileMainTask_unconfiguredChangeIdentifier() throws DatafileTaskException {
        final PublisherConfiguration dmaapPublisherConfiguration = ImmutablePublisherConfiguration.builder() //
            .publishUrl(publishUrl) //
            .logUrl("") //
            .userName("userName") //
            .passWord("passWord") //
            .trustStorePath("trustStorePath") //
            .trustStorePasswordPath("trustStorePasswordPath") //
            .keyStorePath("keyStorePath") //
            .keyStorePasswordPath("keyStorePasswordPath") //
            .enableDmaapCertAuth(true) //
            .changeIdentifier("Different changeIdentifier") //
            .build(); //
        final ConsumerConfiguration dmaapConsumerConfiguration =
            new ConsumerConfiguration(mock(MessageRouterSubscriberConfig.class), mock(MessageRouterSubscriber.class),
                mock(MessageRouterSubscribeRequest.class));

        doReturn(dmaapPublisherConfiguration).when(appConfig).getPublisherConfiguration(CHANGE_IDENTIFIER);
        doReturn(dmaapConsumerConfiguration).when(appConfig).getDmaapConsumerConfiguration();
        doReturn(false).when(appConfig).isFeedConfigured(CHANGE_IDENTIFIER);
        final int noOfEvents = 1;
        final int noOfFilesPerEvent = 1;

        Flux<FileReadyMessage> fileReadyMessages = fileReadyMessageFlux(noOfEvents, noOfFilesPerEvent, true);
        doReturn(fileReadyMessages).when(consumerMock).getMessageRouterResponse();

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(ScheduledTasks.class);
        testedObject.executeDatafileMainTask();

        await().untilAsserted(() -> assertEquals("currentNumberOfSubscriptions should have been 0", 0,
            testedObject.getCurrentNumberOfSubscriptions()));

        assertTrue("Error missing in log", logAppender.list.toString().contains(
            "[INFO] No feed is configured for: " + CHANGE_IDENTIFIER + ", file ignored: " + PM_FILE_NAME + "1"));
    }

    @Test
    public void createMainTask_consumeFail() {
        MDC.setContextMap(contextMap);
        doReturn(Flux.error(new Exception("Failed"))).when(consumerMock).getMessageRouterResponse();

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(ScheduledTasks.class);
        StepVerifier //
            .create(testedObject.createMainTask(contextMap)) //
            .expectSubscription() //
            .expectNextCount(0) //
            .expectComplete() //
            .verify(); //

        assertTrue("Error missing in log", logAppender.list.toString()
            .contains("[ERROR] Polling for file ready message failed, " + "exception: java.lang.Exception: Failed"));
    }

    @Test
    public void consume_successfulCase() throws DatafileTaskException {
        setUpConfiguration();

        final int noOfEvents = 200;
        final int noOfFilesPerEvent = 200;
        final int noOfFiles = noOfEvents * noOfFilesPerEvent;

        Flux<FileReadyMessage> fileReadyMessages = fileReadyMessageFlux(noOfEvents, noOfFilesPerEvent, true);
        doReturn(fileReadyMessages).when(consumerMock).getMessageRouterResponse();

        doReturn(false).when(publishedCheckerMock).isFilePublished(anyString(), anyString(), any());

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
        assertEquals(0, testedObject.getThreadPoolQueueSize());

        verify(consumerMock, times(1)).getMessageRouterResponse();
        verifyNoMoreInteractions(consumerMock);

        verify(fileCollectorMock, times(noOfFiles)).collectFile(notNull(), anyLong(), notNull(), notNull());
        verifyNoMoreInteractions(fileCollectorMock);

        verify(dataRouterMock, times(noOfFiles)).publishFile(notNull(), anyLong(), notNull());
        verifyNoMoreInteractions(dataRouterMock);

        assertEquals("totalReceivedEvents should have been 200", 200,
            testedObject.getCounters().getTotalReceivedEvents());
    }

    @Test
    public void consume_fetchFailedOnce() throws DatafileTaskException {
        setUpConfiguration();

        Flux<FileReadyMessage> fileReadyMessages = fileReadyMessageFlux(2, 2, true); // 4 files
        doReturn(fileReadyMessages).when(consumerMock).getMessageRouterResponse();

        doReturn(false).when(publishedCheckerMock).isFilePublished(anyString(), anyString(), any());

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
        verifyNoMoreInteractions(consumerMock);

        verify(fileCollectorMock, times(4)).collectFile(notNull(), anyLong(), notNull(), notNull());
        verifyNoMoreInteractions(fileCollectorMock);

        verify(dataRouterMock, times(3)).publishFile(notNull(), anyLong(), notNull());
        verifyNoMoreInteractions(dataRouterMock);

        assertEquals("totalReceivedEvents should have been 2", 2, testedObject.getCounters().getTotalReceivedEvents());
        assertEquals("failedFtp should have been 1", 1, testedObject.getCounters().getNoOfFailedFtp());
    }

    @Test
    public void consume_publishFailedOnce() throws DatafileTaskException {
        setUpConfiguration();

        Flux<FileReadyMessage> fileReadyMessages = fileReadyMessageFlux(2, 2, true); // 4 files
        doReturn(fileReadyMessages).when(consumerMock).getMessageRouterResponse();

        doReturn(false).when(publishedCheckerMock).isFilePublished(anyString(), anyString(), any());

        Mono<FilePublishInformation> collectedFile = Mono.just(filePublishInformation());
        doReturn(collectedFile).when(fileCollectorMock).collectFile(notNull(), anyLong(), notNull(), notNull());

        Mono<Object> error = Mono.error(new Exception("problem"));
        // One publish will fail, the rest will succeed
        doReturn(collectedFile, error, collectedFile, collectedFile) //
            .when(dataRouterMock) //
            .publishFile(notNull(), anyLong(), notNull());

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(ScheduledTasks.class);
        StepVerifier //
            .create(testedObject.createMainTask(contextMap)) //
            .expectSubscription() //
            .expectNextCount(3) // 3 completed files
            .expectComplete() //
            .verify(); //

        assertTrue("Error missing in log", logAppender.list.toString().contains("[ERROR] File publishing failed: "));

        assertEquals(0, testedObject.getCurrentNumberOfTasks());

        verify(consumerMock, times(1)).getMessageRouterResponse();
        verifyNoMoreInteractions(consumerMock);

        verify(fileCollectorMock, times(4)).collectFile(notNull(), anyLong(), notNull(), notNull());
        verifyNoMoreInteractions(fileCollectorMock);

        verify(dataRouterMock, times(4)).publishFile(notNull(), anyLong(), notNull());
        verifyNoMoreInteractions(dataRouterMock);

        assertEquals("totalReceivedEvents should have been 2", 2, testedObject.getCounters().getTotalReceivedEvents());
        assertEquals("noOfFailedPublish should have been 1", 1, testedObject.getCounters().getNoOfFailedPublish());
    }

    @Test
    public void consume_successfulCase_sameFileNames() throws DatafileTaskException {
        setUpConfiguration();

        final int noOfEvents = 1;
        final int noOfFilesPerEvent = 100;

        // 100 files with the same name
        Flux<FileReadyMessage> fileReadyMessages = fileReadyMessageFlux(noOfEvents, noOfFilesPerEvent, false);
        doReturn(fileReadyMessages).when(consumerMock).getMessageRouterResponse();

        doReturn(false).when(publishedCheckerMock).isFilePublished(anyString(), anyString(), any());

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
        verifyNoMoreInteractions(consumerMock);

        verify(fileCollectorMock, times(1)).collectFile(notNull(), anyLong(), notNull(), notNull());
        verifyNoMoreInteractions(fileCollectorMock);

        verify(dataRouterMock, times(1)).publishFile(notNull(), anyLong(), notNull());
        verifyNoMoreInteractions(dataRouterMock);

        verify(publishedCheckerMock, times(1)).isFilePublished(notNull(), anyString(), notNull());
        verifyNoMoreInteractions(publishedCheckerMock);

        assertEquals("totalReceivedEvents should have been 1", 1, testedObject.getCounters().getTotalReceivedEvents());
    }
}
