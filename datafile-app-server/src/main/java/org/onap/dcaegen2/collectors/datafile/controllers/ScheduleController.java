/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018 Nordix Foundation. All rights reserved.
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
 * ============LICENSE_END=========================================================
 */
package org.onap.dcaegen2.collectors.datafile.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.onap.dcaegen2.collectors.datafile.tasks.ScheduledTasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 4/5/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */

@RestController
@Api(value = "ScheduleController", description = "Schedule Controller")
public class ScheduleController {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleController.class);
    private static final int SCHEDULING_DELAY = 5000;
    private static volatile List<ScheduledFuture> scheduledFutureList = new ArrayList<>();

    private final TaskScheduler taskScheduler;
    private final ScheduledTasks scheduledTask;


    @Autowired
    public ScheduleController(TaskScheduler taskScheduler, ScheduledTasks scheduledTask) {
        this.taskScheduler = taskScheduler;
        this.scheduledTask = scheduledTask;
    }

    @RequestMapping(value = "start", method = RequestMethod.GET)
    @ApiOperation(value = "Start scheduling worker request")
    public Mono<ResponseEntity<String>> startTasks() {
        logger.trace("Receiving start scheduling worker request");
        return Mono.fromSupplier(this::tryToStartTask).map(this::createStartTaskResponse);
    }

    @RequestMapping(value = "stopDatafile", method = RequestMethod.GET)
    @ApiOperation(value = "Receiving stop scheduling worker request")
    public Mono<ResponseEntity<String>> stopTask() {
        logger.trace("Receiving stop scheduling worker request");
        return getResponseFromCancellationOfTasks();
    }

    @ApiOperation(value = "Get response on stopping task execution")
    private synchronized Mono<ResponseEntity<String>> getResponseFromCancellationOfTasks() {
        scheduledFutureList.forEach(x -> x.cancel(false));
        scheduledFutureList.clear();
        return Mono.defer(() ->
            Mono.just(new ResponseEntity<>("DATAFILE Service has already been stopped!", HttpStatus.CREATED))
        );
    }

    @ApiOperation(value = "Start task if possible")
    private synchronized boolean tryToStartTask() {
        if (scheduledFutureList.isEmpty()) {
            scheduledFutureList.add(taskScheduler
                .scheduleWithFixedDelay(scheduledTask::scheduleMainDatafileEventTask, SCHEDULING_DELAY));
            return true;
        } else {
            return false;
        }

    }

    @ApiOperation(value = "Sends success or error response on starting task execution")
    private ResponseEntity<String> createStartTaskResponse(boolean wasScheduled) {
        if (wasScheduled) {
            return new ResponseEntity<>("DATAFILE Service has been started!", HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>("DATAFILE Service is still running!", HttpStatus.NOT_ACCEPTABLE);
        }
    }
}
