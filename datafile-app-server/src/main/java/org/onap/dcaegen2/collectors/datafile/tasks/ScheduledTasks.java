/*
 * ============LICENSE_START=======================================================
 * PNF-REGISTRATION-HANDLER
 * ================================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.tasks;

import java.util.concurrent.Callable;

import org.onap.dcaegen2.collectors.datafile.exceptions.DmaapEmptyResponseException;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 3/23/18
 */
@Component
public class ScheduledTasks {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DmaapConsumerTask dmaapConsumerTask;
    private final DmaapPublisherTask dmaapProducerTask;
    private final AaiProducerTask aaiProducerTask;

    /**
     * Constructor for tasks registration in DatafileWorkflow.
     *
     * @param dmaapConsumerTask - fist task
     * @param dmaapPublisherTask - third task
     * @param aaiPublisherTask - second task
     */
    @Autowired
    public ScheduledTasks(DmaapConsumerTask dmaapConsumerTask, DmaapPublisherTask dmaapPublisherTask,
        AaiProducerTask aaiPublisherTask) {
        this.dmaapConsumerTask = dmaapConsumerTask;
        this.dmaapProducerTask = dmaapPublisherTask;
        this.aaiProducerTask = aaiPublisherTask;
    }

    /**
     * Main function for scheduling datafileWorkflow.
     */
    public void scheduleMainDatafileEventTask() {
        logger.trace("Execution of tasks was registered");

        Mono<String> dmaapProducerResponse = Mono.fromCallable(consumeFromDMaaPMessage())
            .doOnError(DmaapEmptyResponseException.class, error -> logger.warn("Nothing to consume from DMaaP"))
            .map(this::publishToAaiConfiguration)
            .flatMap(this::publishToDmaapConfiguration)
            .subscribeOn(Schedulers.elastic());

        dmaapProducerResponse.subscribe(this::onSuccess, this::onError, this::onComplete);
    }

    private void onComplete() {
        logger.info("Datafile tasks have been completed");
    }

    private void onSuccess(String responseCode) {
        logger.info("Datafile consumed tasks. HTTP Response code {}", responseCode);
    }

    private void onError(Throwable throwable) {
        if (!(throwable instanceof DmaapEmptyResponseException)) {
            logger.warn("Chain of tasks have been aborted due to errors in Datafile workflow", throwable);
        }
    }

    private Callable<Mono<ConsumerDmaapModel>> consumeFromDMaaPMessage() {
        return () -> {
            dmaapConsumerTask.initConfigs();
            return dmaapConsumerTask.execute("");
        };
    }

    private Mono<ConsumerDmaapModel> publishToAaiConfiguration(Mono<ConsumerDmaapModel> monoDMaaPModel) {
        try {
            return aaiProducerTask.execute(monoDMaaPModel);
        } catch (DatafileTaskException e) {
            return Mono.error(e);
        }
    }

    private Mono<String> publishToDmaapConfiguration(Mono<ConsumerDmaapModel> monoAaiModel) {
        try {
            return dmaapProducerTask.execute(monoAaiModel);
        } catch (DatafileTaskException e) {
            return Mono.error(e);
        }
    }
}
