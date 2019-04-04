/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018-2019 Nordix Foundation. All rights reserved.
 * ===============================================================================================
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
 * ============LICENSE_END========================================================================
 */

package org.onap.dcaegen2.collectors.datafile.controllers;

import org.onap.dcaegen2.collectors.datafile.configuration.SchedulerConfig;
import org.onap.dcaegen2.collectors.datafile.model.logging.MappedDiagnosticContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 4/5/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */

@RestController
@Api(value = "ScheduleController")
public class ScheduleController {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleController.class);

    private final SchedulerConfig schedulerConfig;

    @Autowired
    public ScheduleController(SchedulerConfig schedulerConfig) {
        this.schedulerConfig = schedulerConfig;
    }

    @GetMapping("/start")
    @ApiOperation(value = "Start scheduling worker request")
    public Mono<ResponseEntity<String>> startTasks(@RequestHeader HttpHeaders headers) {
        MappedDiagnosticContents.initializeTraceContext(headers);
        logger.info(MappedDiagnosticContents.ENTRY, "Start request");
        Mono<ResponseEntity<String>> response = startTasks();
        logger.info(MappedDiagnosticContents.EXIT, "Start request");
        return response;
    }

    public Mono<ResponseEntity<String>> startTasks() {
        return Mono.fromSupplier(schedulerConfig::tryToStartTask) //
                .map(this::createStartTaskResponse);
    }

    @GetMapping("/stopDatafile")
    @ApiOperation(value = "Receiving stop scheduling worker request")
    public Mono<ResponseEntity<String>> stopTask(@RequestHeader HttpHeaders headers) {
        MappedDiagnosticContents.initializeTraceContext(headers);
        logger.info(MappedDiagnosticContents.ENTRY, "Stop request");
        Mono<ResponseEntity<String>> response =  schedulerConfig.getResponseFromCancellationOfTasks();
        logger.info(MappedDiagnosticContents.EXIT, "Stop request");
        return response;
    }

    @ApiOperation(value = "Sends success or error response on starting task execution")
    private ResponseEntity<String> createStartTaskResponse(boolean wasScheduled) {
        if (wasScheduled) {
            return new ResponseEntity<>("Datafile Service has been started!", HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>("Datafile Service is still running!", HttpStatus.NOT_ACCEPTABLE);
        }
    }
}
