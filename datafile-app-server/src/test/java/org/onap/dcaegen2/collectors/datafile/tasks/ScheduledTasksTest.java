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

import java.nio.file.Paths;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.ftp.Scheme;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.FeedData;
import org.onap.dcaegen2.collectors.datafile.model.FileData;
import org.onap.dcaegen2.collectors.datafile.model.FileReadyMessage;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFeedData;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFileData;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFileReadyMessage;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableMessageMetaData;
import org.onap.dcaegen2.collectors.datafile.model.MessageMetaData;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class ScheduledTasksTest {

    private static final FeedData FEED_DATA = ImmutableFeedData.builder() //
            .publishedCheckUrl("LOG_URL") //
            .publishUrl("PUBLISH_URL") //
            .username("USERNAME") //
            .password("PASSWORD") //
            .build();

    private static final String PM_FILE_NAME = "A20161224.1030-1045.bin.gz";

    private AppConfig appConfig = mock(AppConfig.class);
    private DMaaPMessageConsumerTask consumerMock;
    private PublishedChecker publishedCheckerMock;
    private FileCollector fileCollectorMock;
    private DataRouterPublisher dataRouterMock;
    private ScheduledTasks testedObject;

    private int uniqueValue = 0;

    @BeforeEach
    private void setUp() {
        consumerMock = mock(DMaaPMessageConsumerTask.class);
        publishedCheckerMock = mock(PublishedChecker.class);
        fileCollectorMock = mock(FileCollector.class);
        dataRouterMock = mock(DataRouterPublisher.class);
        testedObject = spy(new ScheduledTasks(appConfig, FEED_DATA));

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
        MessageMetaData md = messageMetaData();
        return ImmutableFileReadyMessage.builder().pnfName(md.sourceName()).messageMetaData(md)
                .files(files(numberOfFiles, uniqueNames)).build();
    }

    private Flux<FileReadyMessage> fileReadyMessageFlux(int numberOfEvents, int filesPerEvent, boolean uniqueNames) {
        List<FileReadyMessage> list = new LinkedList<FileReadyMessage>();
        for (int i = 0; i < numberOfEvents; ++i) {
            list.add(createFileReadyMessage(filesPerEvent, uniqueNames));
        }
        return Flux.fromIterable(list);
    }

    private ConsumerDmaapModel consumerData() {
        return ImmutableConsumerDmaapModel //
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
                .build();
    }

    @Test
    public void nothingToConsume() {
        doReturn(consumerMock).when(testedObject).createConsumerTask();
        doReturn(Flux.empty()).when(consumerMock).execute();

        testedObject.scheduleMainDatafileEventTask(any());

        assertEquals(0, testedObject.getCurrentNumberOfTasks());
        verify(consumerMock, times(1)).execute();
        verifyNoMoreInteractions(consumerMock);
    }

    @Test
    public void consume_successfulCase() {
        final int noOfEvents = 200;
        final int noOfFilesPerEvent = 200;
        final int noOfFiles = noOfEvents * noOfFilesPerEvent;

        Flux<FileReadyMessage> fileReadyMessages = fileReadyMessageFlux(noOfEvents, noOfFilesPerEvent, true);
        doReturn(fileReadyMessages).when(consumerMock).execute();
        doReturn(true).when(publishedCheckerMock).isPublished(anyString(), any(FeedData.class), any());

        doReturn(false).when(publishedCheckerMock).isPublished(anyString(), any(FeedData.class), any());

        Mono<ConsumerDmaapModel> collectedFile = Mono.just(consumerData());
        doReturn(collectedFile).when(fileCollectorMock).collectFile(notNull(), notNull(), anyLong(), notNull(), any());
        doReturn(collectedFile).when(dataRouterMock).publishFile(notNull(), any(FeedData.class), anyLong(), notNull(),
                any());

        StepVerifier.create(testedObject.createMainTask(any())).expectSubscription() //
                .expectNextCount(noOfFiles) //
                .expectComplete() //
                .verify(); //

        assertEquals(0, testedObject.getCurrentNumberOfTasks());
        verify(consumerMock, times(1)).execute();
        verify(fileCollectorMock, times(noOfFiles)).collectFile(notNull(), notNull(), anyLong(), notNull(), any());
        verify(dataRouterMock, times(noOfFiles)).publishFile(notNull(), any(FeedData.class), anyLong(), notNull(),
                any());
        verifyNoMoreInteractions(dataRouterMock);
        verifyNoMoreInteractions(fileCollectorMock);
        verifyNoMoreInteractions(consumerMock);
    }

    @Test
    public void consume_fetchFailedOnce() {
        Flux<FileReadyMessage> fileReadyMessages = fileReadyMessageFlux(2, 2, true); // 4 files
        doReturn(fileReadyMessages).when(consumerMock).execute();
        doReturn(true).when(publishedCheckerMock).isPublished(anyString(), any(FeedData.class), any());

        doReturn(false).when(publishedCheckerMock).isPublished(anyString(), any(FeedData.class), any());

        Mono<ConsumerDmaapModel> collectedFile = Mono.just(consumerData());
        Mono<Object> error = Mono.error(new Exception("problem"));

        // First file collect will fail, 3 will succeed
        doReturn(error, collectedFile, collectedFile, collectedFile) //
                .when(fileCollectorMock) //
                .collectFile(any(FileData.class), any(MessageMetaData.class), anyLong(), any(Duration.class), any());

        doReturn(collectedFile).when(dataRouterMock).publishFile(notNull(), any(FeedData.class), anyLong(), notNull(),
                any());
        doReturn(collectedFile).when(dataRouterMock).publishFile(notNull(), any(FeedData.class), anyLong(), notNull(),
                any());

        StepVerifier.create(testedObject.createMainTask(any())).expectSubscription() //
                .expectNextCount(3) //
                .expectComplete() //
                .verify(); //

        assertEquals(0, testedObject.getCurrentNumberOfTasks());
        verify(consumerMock, times(1)).execute();
        verify(fileCollectorMock, times(4)).collectFile(notNull(), notNull(), anyLong(), notNull(), any());
        verify(dataRouterMock, times(3)).publishFile(notNull(), any(FeedData.class), anyLong(), notNull(), any());
        verifyNoMoreInteractions(dataRouterMock);
        verifyNoMoreInteractions(fileCollectorMock);
        verifyNoMoreInteractions(consumerMock);
    }

    @Test
    public void consume_publishFailedOnce() {

        Flux<FileReadyMessage> fileReadyMessages = fileReadyMessageFlux(2, 2, true); // 4 files
        doReturn(fileReadyMessages).when(consumerMock).execute();

        doReturn(false).when(publishedCheckerMock).isPublished(anyString(), any(FeedData.class), any());

        Mono<ConsumerDmaapModel> collectedFile = Mono.just(consumerData());
        doReturn(collectedFile).when(fileCollectorMock).collectFile(notNull(), notNull(), anyLong(), notNull(), any());

        Mono<Object> error = Mono.error(new Exception("problem"));
        // One publish will fail, the rest will succeed
        doReturn(collectedFile, error, collectedFile, collectedFile) //
                .when(dataRouterMock) //
                .publishFile(notNull(), any(FeedData.class), anyLong(), notNull(), any());

        StepVerifier.create(testedObject.createMainTask(any())).expectSubscription() //
                .expectNextCount(3) // 3 completed files
                .expectComplete() //
                .verify(); //

        assertEquals(0, testedObject.getCurrentNumberOfTasks());
        verify(consumerMock, times(1)).execute();
        verify(fileCollectorMock, times(4)).collectFile(notNull(), notNull(), anyLong(), notNull(), any());
        verify(dataRouterMock, times(4)).publishFile(notNull(), any(FeedData.class), anyLong(), notNull(), any());
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
        doReturn(fileReadyMessages).when(consumerMock).execute();

        doReturn(false).when(publishedCheckerMock).isPublished(anyString(), any(FeedData.class), any());

        Mono<ConsumerDmaapModel> collectedFile = Mono.just(consumerData());
        doReturn(collectedFile).when(fileCollectorMock).collectFile(notNull(), notNull(), anyLong(), notNull(), any());
        doReturn(collectedFile).when(dataRouterMock).publishFile(notNull(), any(FeedData.class), anyLong(), notNull(),
                any());

        StepVerifier.create(testedObject.createMainTask(any())).expectSubscription() //
                .expectNextCount(1) // 99 is skipped
                .expectComplete() //
                .verify(); //

        assertEquals(0, testedObject.getCurrentNumberOfTasks());
        verify(consumerMock, times(1)).execute();
        verify(fileCollectorMock, times(1)).collectFile(notNull(), notNull(), anyLong(), notNull(), any());
        verify(dataRouterMock, times(1)).publishFile(notNull(), any(FeedData.class), anyLong(), notNull(), any());
        verifyNoMoreInteractions(dataRouterMock);
        verifyNoMoreInteractions(fileCollectorMock);
        verifyNoMoreInteractions(consumerMock);
    }


}
