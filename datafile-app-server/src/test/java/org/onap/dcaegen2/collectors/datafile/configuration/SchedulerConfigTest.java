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

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.onap.dcaegen2.collectors.datafile.tasks.ScheduledTasks;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import reactor.test.StepVerifier;

public class SchedulerConfigTest {

    @Test
    public void getResponseFromCancellationOfTasks_success() {
        List<ScheduledFuture<?>> scheduledFutureList = new ArrayList<>();
        ScheduledFuture<?> scheduledFutureMock = mock(ScheduledFuture.class);
        scheduledFutureList.add(scheduledFutureMock);

        SchedulerConfig.setScheduledFutureList(scheduledFutureList);

        SchedulerConfig schedulerUnderTest = new SchedulerConfig(null, null, null);

        String msg = "Datafile Service has already been stopped!";
        StepVerifier.create(schedulerUnderTest.getResponseFromCancellationOfTasks())
                .expectNext(new ResponseEntity<String>(msg, HttpStatus.CREATED)) //
                .verifyComplete();

        verify(scheduledFutureMock).cancel(false);
        verifyNoMoreInteractions(scheduledFutureMock);

        assertEquals(0, scheduledFutureList.size());
    }

    @Test
    public void tryToStartTaskWhenNotStarted_success() {
        TaskScheduler taskSchedulerMock = mock(TaskScheduler.class);
        ScheduledTasks scheduledTasksMock = mock(ScheduledTasks.class);
        CloudConfiguration cloudConfigurationMock = mock(CloudConfiguration.class);
        List<ScheduledFuture<?>> scheduledFutureList = new ArrayList<>();

        SchedulerConfig.setScheduledFutureList(scheduledFutureList);

        SchedulerConfig schedulerUnderTestSpy =
                spy(new SchedulerConfig(taskSchedulerMock, scheduledTasksMock, cloudConfigurationMock));

        boolean actualResult = schedulerUnderTestSpy.tryToStartTask();

        assertTrue(actualResult);

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
        verify(scheduledTasksMock).executeDatafileMainTask();
        verifyNoMoreInteractions(scheduledTasksMock);

        runTaskRunnableCaptor.getValue().run();
        verify(cloudConfigurationMock).runTask();
        verifyNoMoreInteractions(cloudConfigurationMock);

        assertEquals(3, scheduledFutureList.size());
    }

    @Test
    public void tryToStartTaskWhenAlreadyStarted_shouldReturnFalse() {
        List<ScheduledFuture<?>> scheduledFutureList = new ArrayList<>();
        ScheduledFuture<?> scheduledFutureMock = mock(ScheduledFuture.class);
        scheduledFutureList.add(scheduledFutureMock);

        SchedulerConfig.setScheduledFutureList(scheduledFutureList);

        SchedulerConfig schedulerUnderTest = new SchedulerConfig(null, null, null);

        boolean actualResult = schedulerUnderTest.tryToStartTask();

        assertFalse(actualResult);
    }
}
