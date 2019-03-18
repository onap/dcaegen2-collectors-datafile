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

package org.onap.dcaegen2.collectors.datafile.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.onap.dcaegen2.collectors.datafile.model.FeedData;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFeedData;
import org.onap.dcaegen2.collectors.datafile.tasks.FeedCreator;
import org.onap.dcaegen2.collectors.datafile.tasks.ScheduledTasks;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class SchedulerConfigTest {
    private static final FeedData FEED_DATA = ImmutableFeedData.builder() //
            .publishedCheckUrl("LOG_URL") //
            .publishUrl("PUBLISH_URL") //
            .username("USERNAME") //
            .password("PASSWORD") //
            .build();

    @Test
    public void getResponseFromCancellationOfTasks_success() {
        List<ScheduledFuture<?>> scheduledFutureList = new ArrayList<>();
        ScheduledFuture<?> scheduledFutureMock = mock(ScheduledFuture.class);
        scheduledFutureList.add(scheduledFutureMock);

        SchedulerConfig schedulerUnderTest = new SchedulerConfig(null, null, null);

        SchedulerConfig.setScheduledFutureList(scheduledFutureList);

        String msg = "Datafile Service has already been stopped!";
        StepVerifier.create(schedulerUnderTest.getResponseFromCancellationOfTasks())
                .expectNext(new ResponseEntity<String>(msg, HttpStatus.CREATED)) //
                .verifyComplete();

        verify(scheduledFutureMock).cancel(false);
        verifyNoMoreInteractions(scheduledFutureMock);

        assertEquals(0, scheduledFutureList.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void tryToStartTaskWhenNotStarted_success() {
        TaskScheduler taskSchedulerMock = mock(TaskScheduler.class);
        ScheduledTasks scheduledTasksMock = mock(ScheduledTasks.class);
        CloudConfiguration cloudConfigurationMock = mock(CloudConfiguration.class);
        FeedCreator feedCreatorMock = mock(FeedCreator.class);
        List<ScheduledFuture<?>> scheduledFutureList = new ArrayList<>();

        when(feedCreatorMock.execute(anyLong(), any(Duration.class))).thenReturn(Mono.just(FEED_DATA));

        SchedulerConfig schedulerUnderTestSpy =
                spy(new SchedulerConfig(taskSchedulerMock, cloudConfigurationMock, feedCreatorMock));

        SchedulerConfig.setScheduledFutureList(scheduledFutureList);
        doReturn(scheduledTasksMock).when(schedulerUnderTestSpy).createScheduledTasks(any(FeedData.class));

        HttpStatus actualHttpStatus = schedulerUnderTestSpy.tryToStartTask();

        assertEquals(HttpStatus.OK, actualHttpStatus);

        ArgumentCaptor<Runnable> runTaskRunnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskSchedulerMock).scheduleAtFixedRate(runTaskRunnableCaptor.capture(), any(Instant.class),
                eq(Duration.ofMinutes(5)));

        ArgumentCaptor<Runnable> scheduleMainDatafileEventTaskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskSchedulerMock).scheduleWithFixedDelay(scheduleMainDatafileEventTaskCaptor.capture(),
                eq(Duration.ofSeconds(15)));
        ArgumentCaptor<Runnable> purgeCachedInformationCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskSchedulerMock).scheduleWithFixedDelay(purgeCachedInformationCaptor.capture(),
                eq(Duration.ofHours(1)));
        verifyNoMoreInteractions(taskSchedulerMock);

        scheduleMainDatafileEventTaskCaptor.getValue().run();
        purgeCachedInformationCaptor.getValue().run();
        verify(scheduledTasksMock).purgeCachedInformation(any(Instant.class));
        ArgumentCaptor<Map<String, String>> contextMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(scheduledTasksMock).scheduleMainDatafileEventTask(contextMapCaptor.capture());
        Map<String, String> contextMap = contextMapCaptor.getValue();
        assertNotNull(contextMap.get("InvocationID"));
        assertNotNull(contextMap.get("RequestID"));
        verifyNoMoreInteractions(scheduledTasksMock);

        runTaskRunnableCaptor.getValue().run();
        verify(cloudConfigurationMock).runTask(any());
        verifyNoMoreInteractions(cloudConfigurationMock);

        verify(feedCreatorMock).execute(10, Duration.ofSeconds(5));
        verifyNoMoreInteractions(feedCreatorMock);

        assertEquals(3, scheduledFutureList.size());
    }

    @Test
    public void tryToStartTaskWhenAlreadyStarted_shouldReturnNotAcceptable() {
        List<ScheduledFuture<?>> scheduledFutureList = new ArrayList<>();
        ScheduledFuture<?> scheduledFutureMock = mock(ScheduledFuture.class);
        scheduledFutureList.add(scheduledFutureMock);

        SchedulerConfig schedulerUnderTest = new SchedulerConfig(null, null, null);

        SchedulerConfig.setScheduledFutureList(scheduledFutureList);

        HttpStatus actualHttpStatus = schedulerUnderTest.tryToStartTask();

        assertEquals(HttpStatus.NOT_ACCEPTABLE, actualHttpStatus);
    }

    @Test
    public void tryToStartTaskWhenFeedCreatorThrowsException_shouldReturnBadRequest() {
        FeedCreator feedCreatorMock = mock(FeedCreator.class);

        when(feedCreatorMock.execute(anyLong(), any(Duration.class))).thenThrow(new RuntimeException("Error"));

        SchedulerConfig schedulerUnderTest = new SchedulerConfig(null, null, feedCreatorMock);

        HttpStatus actualHttpStatus = schedulerUnderTest.tryToStartTask();

        assertEquals(HttpStatus.BAD_REQUEST, actualHttpStatus);
    }
}
