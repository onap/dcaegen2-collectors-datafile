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

package org.onap.dcaegen2.collectors.datafile.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.dcaegen2.collectors.datafile.configuration.SchedulerConfig;
import org.onap.dcaegen2.collectors.datafile.utils.LoggingUtils;
import org.onap.dcaegen2.services.sdk.rest.services.model.logging.MdcVariables;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class ScheduleControllerTest {
    @Mock
    private SchedulerConfig schedulerConfigMock;

    private ScheduleController scheduleControllerUnderTest;

    @BeforeEach
    public void setup() {
        scheduleControllerUnderTest = new ScheduleController(schedulerConfigMock);
    }

    @Test
    public void startTasksSuccess() {
        when(schedulerConfigMock.tryToStartTask()).thenReturn(Boolean.TRUE);

        HttpHeaders httpHeaders = new HttpHeaders();

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(ScheduleController.class);
        Mono<ResponseEntity<String>> response = scheduleControllerUnderTest.startTasks(httpHeaders);

        validateLogging(logAppender, "Start request");

        String body = response.block().getBody();
        assertTrue(body.startsWith("Datafile Service has been started!"));

        assertFalse(StringUtils.isBlank(MDC.get(MdcVariables.REQUEST_ID)));
        assertFalse(StringUtils.isBlank(MDC.get(MdcVariables.INVOCATION_ID)));
    }

    @Test
    public void startTasksFail() {
        when(schedulerConfigMock.tryToStartTask()).thenReturn(Boolean.FALSE);

        HttpHeaders httpHeaders = new HttpHeaders();
        // The following headers are set to create branch coverage in MappedDiagnosticContext:initializeTraceContext().
        httpHeaders.set(MdcVariables.httpHeader(MdcVariables.REQUEST_ID), "Onap request ID");
        httpHeaders.set(MdcVariables.httpHeader(MdcVariables.INVOCATION_ID), "Invocation ID");

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(ScheduleController.class);
        Mono<ResponseEntity<String>> response = scheduleControllerUnderTest.startTasks(httpHeaders);

        validateLogging(logAppender, "Start request");

        String body = response.block().getBody();
        assertTrue(body.startsWith("Datafile Service is still running!"));

        assertFalse(StringUtils.isBlank(MDC.get(MdcVariables.REQUEST_ID)));
        assertFalse(StringUtils.isBlank(MDC.get(MdcVariables.INVOCATION_ID)));
    }

    @Test
    public void stopTaskSuccess() {
        when(schedulerConfigMock.getResponseFromCancellationOfTasks()).thenReturn(
            Mono.just(new ResponseEntity<>("Datafile Service has already been stopped!", HttpStatus.CREATED)));

        HttpHeaders httpHeaders = new HttpHeaders();

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(ScheduleController.class);
        Mono<ResponseEntity<String>> actualResponse = scheduleControllerUnderTest.stopTask(httpHeaders);

        validateLogging(logAppender, "Stop request");

        String body = actualResponse.block().getBody();
        assertTrue(body.startsWith("Datafile Service has already been stopped!"));

        assertFalse(StringUtils.isBlank(MDC.get(MdcVariables.REQUEST_ID)));
        assertFalse(StringUtils.isBlank(MDC.get(MdcVariables.INVOCATION_ID)));
    }

    private void validateLogging(ListAppender<ILoggingEvent> logAppender, String infoMessage) {
        assertEquals("ENTRY", logAppender.list.get(0).getMarker().getName());
        assertNotNull(logAppender.list.get(0).getMDCPropertyMap().get("InvocationID"));
        assertNotNull(logAppender.list.get(0).getMDCPropertyMap().get("RequestID"));
        assertTrue("Info missing in log", logAppender.list.toString().contains("[INFO] " + infoMessage));
        assertEquals("EXIT", logAppender.list.get(1).getMarker().getName());
        logAppender.stop();
    }
}
