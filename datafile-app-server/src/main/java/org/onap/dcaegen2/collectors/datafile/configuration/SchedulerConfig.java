/*-
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

package org.onap.dcaegen2.collectors.datafile.configuration;

import static org.onap.dcaegen2.services.sdk.rest.services.model.logging.MdcVariables.INVOCATION_ID;
import static org.onap.dcaegen2.services.sdk.rest.services.model.logging.MdcVariables.REQUEST_ID;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.onap.dcaegen2.collectors.datafile.model.FeedData;
import org.onap.dcaegen2.collectors.datafile.tasks.FeedCreator;
import org.onap.dcaegen2.collectors.datafile.tasks.ScheduledTasks;
import org.onap.dcaegen2.services.sdk.rest.services.model.logging.MdcVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.swagger.annotations.ApiOperation;
import reactor.core.publisher.Mono;

/**
 * Api for starting and stopping DFC.
 *
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 6/13/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final Duration SCHEDULING_DELAY_FOR_DATAFILE_COLLECTOR_TASKS = Duration.ofSeconds(15);
    private static final Duration SCHEDULING_REQUEST_FOR_CONFIGURATION_DELAY = Duration.ofMinutes(5);
    private static final Duration SCHEDULING_DELAY_FOR_DATAFILE_PURGE_CACHE = Duration.ofHours(1);
    private static final Marker ENTRY = MarkerFactory.getMarker("ENTRY");
    private static final Marker EXIT = MarkerFactory.getMarker("EXIT");
    private static volatile List<ScheduledFuture<?>> scheduledFutureList = new ArrayList<>();
    private Map<String, String> contextMap;

    private final TaskScheduler taskScheduler;
    private final CloudConfiguration cloudConfiguration;
    private final FeedCreator feedCreator;

    private ScheduledTasks scheduledTasks;

    /**
     * Constructor.
     *
     * @param taskScheduler scheduler.
     * @param cloudConfiguration the DFC cloud configuration.
     * @param feedCreator creates the feed in DataRouter.
     */
    @Autowired
    public SchedulerConfig(TaskScheduler taskScheduler, CloudConfiguration cloudConfiguration,
            FeedCreator feedCreator) {
        this.taskScheduler = taskScheduler;
        this.cloudConfiguration = cloudConfiguration;
        this.feedCreator = feedCreator;
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
        MdcVariables.setMdcContextMap(contextMap);
        logger.info(EXIT, "Stopped Datafile workflow");
        MDC.clear();
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
    public synchronized HttpStatus tryToStartTask() {
        String requestId = MDC.get(REQUEST_ID);
        if (StringUtils.isBlank(requestId)) {
            MDC.put(REQUEST_ID, UUID.randomUUID().toString());
        }
        String invocationId = MDC.get(INVOCATION_ID);
        if (StringUtils.isBlank(invocationId)) {
            MDC.put(INVOCATION_ID, UUID.randomUUID().toString());
        }
        contextMap = MDC.getCopyOfContextMap();
        logger.info(ENTRY, "Start scheduling Datafile workflow");
        if (scheduledFutureList.isEmpty()) {
            try {
                FeedData feedData = feedCreator.execute(10, Duration.ofSeconds(5)).block();
                scheduledTasks = createScheduledTasks(feedData);

                scheduledFutureList.add(taskScheduler.scheduleAtFixedRate(() -> cloudConfiguration.runTask(contextMap),
                        Instant.now(), SCHEDULING_REQUEST_FOR_CONFIGURATION_DELAY));
                scheduledFutureList.add(taskScheduler.scheduleWithFixedDelay(
                        () -> scheduledTasks.scheduleMainDatafileEventTask(contextMap),
                        SCHEDULING_DELAY_FOR_DATAFILE_COLLECTOR_TASKS));
                scheduledFutureList.add(
                        taskScheduler.scheduleWithFixedDelay(() -> scheduledTasks.purgeCachedInformation(Instant.now()),
                                SCHEDULING_DELAY_FOR_DATAFILE_PURGE_CACHE));

                return HttpStatus.OK;
            } catch (RuntimeException e) {
                logger.error("Unable to create feed.", e);
                return HttpStatus.BAD_REQUEST;
            }
        } else {
            return HttpStatus.NOT_ACCEPTABLE;
        }
    }

    static void setScheduledFutureList(List<ScheduledFuture<?>> scheduledFutureList) {
        SchedulerConfig.scheduledFutureList = scheduledFutureList;
    }

    ScheduledTasks createScheduledTasks(FeedData feedData) {
        return new ScheduledTasks(cloudConfiguration, feedData);
    }
}
