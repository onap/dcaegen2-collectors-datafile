/*
 * ============LICENSE_START=======================================================
 * PROJECT
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

package org.onap.dcaegen2.services.prh.tasks;

import org.onap.dcaegen2.services.prh.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.services.prh.configuration.AppConfig;
import org.onap.dcaegen2.services.prh.configuration.Config;
import org.onap.dcaegen2.services.prh.exceptions.DmaapNotFoundException;
import org.onap.dcaegen2.services.prh.model.ConsumerDmaapModel;
import org.onap.dcaegen2.services.prh.service.producer.DMaaPProducerReactiveHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 4/13/18
 */
@Component
public class DmaapPublisherTaskImpl extends DmaapPublisherTask {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Config prhAppConfig;
    private DMaaPProducerReactiveHttpClient dmaapProducerReactiveHttpClient;

    @Autowired
    public DmaapPublisherTaskImpl(AppConfig prhAppConfig) {
        this.prhAppConfig = prhAppConfig;
    }

    @Override
    Mono<String> publish(Mono<ConsumerDmaapModel> consumerDmaapModel) {
        logger.info("Publishing on DMaaP topic {} object {}", resolveConfiguration().dmaapTopicName(),
            consumerDmaapModel);
        return dmaapProducerReactiveHttpClient.getDMaaPProducerResponse(consumerDmaapModel);
    }

    @Override
    public Mono<String> execute(Mono<ConsumerDmaapModel> consumerDmaapModel) throws DmaapNotFoundException {
        if (consumerDmaapModel == null) {
            throw new DmaapNotFoundException("Invoked null object to DMaaP task");
        }
        dmaapProducerReactiveHttpClient = resolveClient();
        logger.trace("Method called with arg {}", consumerDmaapModel);
        return publish(consumerDmaapModel);
    }

    @Override
    protected DmaapPublisherConfiguration resolveConfiguration() {
        return prhAppConfig.getDmaapPublisherConfiguration();
    }

    @Override
    DMaaPProducerReactiveHttpClient resolveClient() {
        return dmaapProducerReactiveHttpClient == null
            ? new DMaaPProducerReactiveHttpClient(resolveConfiguration()).createDMaaPWebClient(buildWebClient())
            : dmaapProducerReactiveHttpClient;
    }
}