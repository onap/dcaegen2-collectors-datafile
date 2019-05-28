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

package org.onap.dcaegen2.collectors.datafile.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.controllers.HeartbeatController;
import org.onap.dcaegen2.collectors.datafile.tasks.ScheduledTasks;
import org.onap.dcaegen2.collectors.datafile.utils.LoggingUtils;
import org.onap.dcaegen2.services.sdk.rest.services.model.logging.MdcVariables;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public class HeartbeatControllerTest {
    @Test
    public void heartbeat_success() {
        ScheduledTasks scheduledTasksMock = mock(ScheduledTasks.class);
        when(scheduledTasksMock.getCurrentNumberOfTasks()).thenReturn(10);
        when(scheduledTasksMock.publishedFilesCacheSize()).thenReturn(20);

        HttpHeaders httpHeaders = new HttpHeaders();

        HeartbeatController controllerUnderTest = new HeartbeatController(scheduledTasksMock);

        ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(HeartbeatController.class);
        Mono<ResponseEntity<String>> result = controllerUnderTest.heartbeat(httpHeaders);

        validateLogging(logAppender);

        String body = result.block().getBody();
        assertTrue(body.startsWith("I'm living! Status: "));
        assertTrue(body.contains("numberOfFileCollectionTasks=10"));
        assertTrue(body.contains("fileCacheSize=20"));

        assertFalse(StringUtils.isBlank(MDC.get(MdcVariables.REQUEST_ID)));
        assertFalse(StringUtils.isBlank(MDC.get(MdcVariables.INVOCATION_ID)));
    }

    private void validateLogging(ListAppender<ILoggingEvent> logAppender) {
        assertEquals(logAppender.list.get(0).getMarker().getName(), "ENTRY");
        assertNotNull(logAppender.list.get(0).getMDCPropertyMap().get("InvocationID"));
        assertNotNull(logAppender.list.get(0).getMDCPropertyMap().get("RequestID"));
        assertEquals("[INFO] Heartbeat request", logAppender.list.get(0).toString());
        assertEquals(logAppender.list.get(1).getMarker().getName(), "EXIT");
        assertEquals("[INFO] Heartbeat request", logAppender.list.get(1).toString());
        logAppender.stop();
    }
}