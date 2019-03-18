/*
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

package org.onap.dcaegen2.collectors.datafile.controllers;

import static org.onap.dcaegen2.services.sdk.rest.services.model.logging.MdcVariables.INVOCATION_ID;
import static org.onap.dcaegen2.services.sdk.rest.services.model.logging.MdcVariables.REQUEST_ID;
import static org.onap.dcaegen2.services.sdk.rest.services.model.logging.MdcVariables.X_INVOCATION_ID;
import static org.onap.dcaegen2.services.sdk.rest.services.model.logging.MdcVariables.X_ONAP_REQUEST_ID;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.onap.dcaegen2.collectors.datafile.configuration.SchedulerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import reactor.core.publisher.Mono;

/**
 * The HTTP api to start and stop DFC.
 *
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 4/5/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */

@RestController
@Api(value = "ScheduleController", description = "Schedule Controller")
public class ScheduleController {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleController.class);

    private final SchedulerConfig schedulerConfig;

    @Autowired
    public ScheduleController(SchedulerConfig schedulerConfig) {
        this.schedulerConfig = schedulerConfig;
    }

    /**
     * Start the DFC.
     *
     * @param headers the request headers.
     * @return the response.
     */
    @RequestMapping(value = "start", method = RequestMethod.GET)
    @ApiOperation(value = "Start scheduling worker request")
    public Mono<ResponseEntity<String>> startTasks(@RequestHeader HttpHeaders headers) {
        String requestId = headers.getFirst(X_ONAP_REQUEST_ID);
        if (StringUtils.isBlank(requestId)) {
            requestId = UUID.randomUUID().toString();
        }
        String invocationId = headers.getFirst(X_INVOCATION_ID);
        if (StringUtils.isBlank(invocationId)) {
            invocationId = UUID.randomUUID().toString();
        }
        MDC.put(REQUEST_ID, requestId);
        MDC.put(INVOCATION_ID, invocationId);
        logger.trace("Receiving start scheduling worker request");
        return Mono.fromSupplier(schedulerConfig::tryToStartTask).map(this::createStartTaskResponse);
    }

    /**
     * Stop the DFC.
     *
     * @return the response.
     */
    @RequestMapping(value = "stopDatafile", method = RequestMethod.GET)
    @ApiOperation(value = "Receiving stop scheduling worker request")
    public Mono<ResponseEntity<String>> stopTask() {
        logger.trace("Receiving stop scheduling worker request");
        return schedulerConfig.getResponseFromCancellationOfTasks();
    }

    @ApiOperation(value = "Sends success or error response on starting task execution")
    private ResponseEntity<String> createStartTaskResponse(HttpStatus startResult) {
        if (HttpStatus.CREATED.equals(startResult)) {
            return new ResponseEntity<>("Datafile Service has been started!", startResult);
        } else if (HttpStatus.NOT_ACCEPTABLE.equals(startResult)) {
            return new ResponseEntity<>("Datafile Service is still running!", startResult);
        } else {
            return new ResponseEntity<>("Datafile Service not started! " //
                    + "Unable to create the DataRouter feed, see log for details.", startResult);
        }
    }
}
