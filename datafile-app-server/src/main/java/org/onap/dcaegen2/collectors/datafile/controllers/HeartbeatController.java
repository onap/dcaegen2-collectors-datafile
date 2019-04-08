/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018 Nordix Foundation. All rights reserved.
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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.onap.dcaegen2.collectors.datafile.model.logging.MappedDiagnosticContext;
import org.onap.dcaegen2.collectors.datafile.tasks.ScheduledTasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controller to check the heartbeat of DFC.
 *
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 4/19/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
@RestController
@Api(value = "HeartbeatController")
public class HeartbeatController {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatController.class);

    private final ScheduledTasks scheduledTasks;

    @Autowired
    public HeartbeatController(ScheduledTasks scheduledTasks) {
        this.scheduledTasks = scheduledTasks;
    }

    /**
     * Checks the heartbeat of DFC.
     *
     * @return the heartbeat status of DFC.
     */
    @GetMapping("/heartbeat")
    @ApiOperation(value = "Returns liveness of DATAFILE service")
    @ApiResponses(value = { //
            @ApiResponse(code = 200, message = "DATAFILE service is living"),
            @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
            @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
            @ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
    public Mono<ResponseEntity<String>> heartbeat(@RequestHeader HttpHeaders headers) {
        MappedDiagnosticContext.initializeTraceContext(headers);
        logger.info(MappedDiagnosticContext.ENTRY, "Heartbeat request");

        StringBuilder statusString = new StringBuilder("I'm living! Status: ");
        statusString.append("noOfActiveEventTasks=").append(scheduledTasks.getCurrentNumberOfTasks()).append(",");
        statusString.append("fileCacheSize=").append(scheduledTasks.publishedFilesCacheSize()).append(',');
        statusString.append("noOfActiveThreads=").append(Thread.activeCount()).append(",");
        statusString.append("noOfExecutingThreads=").append(getNoExecutingThreads());

        Mono<ResponseEntity<String>> response = Mono.just(new ResponseEntity<>(statusString.toString(), HttpStatus.OK));
        logger.trace("Heartbeat");
        logger.info(MappedDiagnosticContext.EXIT, "Heartbeat request");
        return response;
    }

    private int getNoExecutingThreads() {
        int nbRunning = 0;
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getState() == Thread.State.RUNNABLE) {
                nbRunning++;
            }
        }
        return nbRunning;
    }
}
