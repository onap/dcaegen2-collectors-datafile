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

package org.onap.dcaegen2.collectors.datafile.tasks;

import org.onap.dcaegen2.collectors.datafile.exceptions.DmaapEmptyResponseException;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.FileData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 3/23/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
@Component
public class ScheduledTasks {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    private final DmaapConsumerTask dmaapConsumerTask;
    private final XnfCollectorTask xnfCollectorTask;
    private final DmaapPublisherTask dmaapProducerTask;

    /**
     * Constructor for task registration in Datafile Workflow.
     *
     * @param dmaapConsumerTask - fist task
     * @param xnfCollectorTask - second task
     * @param dmaapPublisherTask - third task
     */
    @Autowired
    public ScheduledTasks(DmaapConsumerTask dmaapConsumerTask, XnfCollectorTask xnfCollectorTask,
            DmaapPublisherTask dmaapPublisherTask) {
        this.dmaapConsumerTask = dmaapConsumerTask;
        this.xnfCollectorTask = xnfCollectorTask;
        this.dmaapProducerTask = dmaapPublisherTask;
    }

    /**
     * Main function for scheduling Datafile Workflow.
     */
    public void scheduleMainDatafileEventTask() {
        logger.trace("Execution of tasks was registered");
        //@formatter:off
        consumeFromDmaapMessage()
                .publishOn(Schedulers.parallel())
                .cache()
                .doOnError(DmaapEmptyResponseException.class, error -> logger.info("Nothing to consume from DMaaP"))
                .flatMap(this::collectFilesFromXnf)
                .retry(3)
                .cache()
                .flatMap(this::publishToDmaapConfiguration)
                .retry(3)
                .subscribe(this::onSuccess, this::onError, this::onComplete);
        //@formatter:on
    }

    private void onComplete() {
        logger.info("Datafile tasks have been completed");
    }

    private void onSuccess(String responseCode) {
        logger.info("Datafile consumed tasks. HTTP Response code {}", responseCode);
    }

    private void onError(Throwable throwable) {
        if (!(throwable instanceof DmaapEmptyResponseException)) {
            logger.error("Chain of tasks have been aborted due to errors in Datafile workflow", throwable);
        }
    }

    private Flux<FileData> consumeFromDmaapMessage() {
        dmaapConsumerTask.initConfigs();
        return dmaapConsumerTask.execute("");
    }

    private Flux<ConsumerDmaapModel> collectFilesFromXnf(FileData fileData) {
        return xnfCollectorTask.execute(fileData);
    }

    private Flux<String> publishToDmaapConfiguration(ConsumerDmaapModel monoModel) {
        return dmaapProducerTask.execute(monoModel);
    }
}
