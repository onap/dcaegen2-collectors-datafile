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
import static org.mockito.Mockito.doReturn;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.dcaegen2.collectors.datafile.model.Counters;
import org.onap.dcaegen2.collectors.datafile.tasks.ScheduledTasks;
import org.onap.dcaegen2.collectors.datafile.utils.LoggingUtils;
import org.onap.dcaegen2.services.sdk.rest.services.model.logging.MdcVariables;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class StatusControllerTest {
    @Mock
    ScheduledTasks scheduledTasksMock;

    StatusController controllerUnderTest;

    @BeforeEach
    public void setup() {
        controllerUnderTest = new StatusController(scheduledTasksMock);
    }

    @Test
    public void heartbeat_success() {
        HttpHeaders httpHeaders = new HttpHeaders();

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(StatusController.class);
        Mono<ResponseEntity<String>> result = controllerUnderTest.heartbeat(httpHeaders);

        validateLogging(logAppender);

        String body = result.block().getBody();
        assertTrue(body.startsWith("I'm living!"));

        assertFalse(StringUtils.isBlank(MDC.get(MdcVariables.REQUEST_ID)));
        assertFalse(StringUtils.isBlank(MDC.get(MdcVariables.INVOCATION_ID)));
    }

    @Test
    public void status() {
        Counters counters = new Counters();
        doReturn(counters).when(scheduledTasksMock).getCounters();

        HttpHeaders httpHeaders = new HttpHeaders();

        Mono<ResponseEntity<String>> result = controllerUnderTest.status(httpHeaders);

        String body = result.block().getBody();
        System.out.println(body);

        assertFalse(StringUtils.isBlank(MDC.get(MdcVariables.REQUEST_ID)));
        assertFalse(StringUtils.isBlank(MDC.get(MdcVariables.INVOCATION_ID)));
    }

    private void validateLogging(ListAppender<ILoggingEvent> logAppender) {
        assertEquals("ENTRY", logAppender.list.get(0).getMarker().getName());
        assertNotNull(logAppender.list.get(0).getMDCPropertyMap().get("InvocationID"));
        assertNotNull(logAppender.list.get(0).getMDCPropertyMap().get("RequestID"));
        assertTrue("Info missing in log", logAppender.list.toString().contains("[INFO] Heartbeat request"));
        assertEquals("EXIT", logAppender.list.get(1).getMarker().getName());
        logAppender.stop();
    }
}
