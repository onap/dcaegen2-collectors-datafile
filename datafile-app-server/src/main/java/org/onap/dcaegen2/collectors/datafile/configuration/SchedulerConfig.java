/*
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

package org.onap.dcaegen2.collectors.datafile.configuration;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;

import org.onap.dcaegen2.collectors.datafile.tasks.ScheduledTasks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.swagger.annotations.ApiOperation;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 6/13/18
 */
@Configuration
@EnableScheduling
public class SchedulerConfig extends DatafileAppConfig {


    private static final int SCHEDULING_DELAY_FOR_DATAFILE_COLLECTOR_TASKS = 10;
    private static final int SCHEDULING_REQUEST_FOR_CONFIGURATION_DELAY = 5;
    private static volatile List<ScheduledFuture> scheduledFutureList = new ArrayList<>();

    private final TaskScheduler taskScheduler;
    private final ScheduledTasks scheduledTask;
    private final CloudConfiguration cloudConfiguration;

    @Autowired
    public SchedulerConfig(TaskScheduler taskScheduler, ScheduledTasks scheduledTask,
        CloudConfiguration cloudConfiguration) {
        this.taskScheduler = taskScheduler;
        this.scheduledTask = scheduledTask;
        this.cloudConfiguration = cloudConfiguration;
    }

    /**
     * Function which have to stop tasks execution.
     *
     * @return response entity about status of cancellation operation
     */
    @ApiOperation(value = "Get response on stopping task execution")
    public synchronized Mono<ResponseEntity<String>> getResponseFromCancellationOfTasks() {
        scheduledFutureList.forEach(x -> x.cancel(false));
        scheduledFutureList.clear();
        return Mono.defer(() -> Mono
            .just(new ResponseEntity<>("Datafile Service has already been stopped!", HttpStatus.CREATED)));
    }

    /**
     * Function for starting scheduling Datafile workflow.
     *
     * @return status of operation execution: true - started, false - not started
     */
    @PostConstruct
    @ApiOperation(value = "Start task if possible")
    public synchronized boolean tryToStartTask() {
        if (scheduledFutureList.isEmpty()) {
            scheduledFutureList.add(taskScheduler
                .scheduleAtFixedRate(cloudConfiguration::runTask, Instant.now(),
                    Duration.ofMinutes(SCHEDULING_REQUEST_FOR_CONFIGURATION_DELAY)));
            scheduledFutureList.add(taskScheduler.scheduleWithFixedDelay(scheduledTask::scheduleMainDatafileEventTask,
                Duration.ofSeconds(SCHEDULING_DELAY_FOR_DATAFILE_COLLECTOR_TASKS)));
            return true;
        } else {
            return false;
        }

    }
}
