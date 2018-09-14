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

import java.util.List;

import org.onap.dcaegen2.collectors.datafile.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.configuration.Config;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.exceptions.DmaapNotFoundException;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.service.producer.DmaapProducerReactiveHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 4/13/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
@Component
public class DmaapPublisherTaskImpl extends DmaapPublisherTask {

    private static final Logger logger = LoggerFactory.getLogger(DmaapPublisherTaskImpl.class);
    private final Config datafileAppConfig;
    private DmaapProducerReactiveHttpClient dmaapProducerReactiveHttpClient;

    @Autowired
    public DmaapPublisherTaskImpl(AppConfig datafileAppConfig) {
        this.datafileAppConfig = datafileAppConfig;
    }

    @Override
    public Mono<String> publish(Mono<List<ConsumerDmaapModel>> consumerDmaapModels)
            throws DatafileTaskException {
        logger.info("Publishing on DMaaP DataRouter {}", consumerDmaapModels);
        return dmaapProducerReactiveHttpClient.getDmaapProducerResponse(consumerDmaapModels);
    }

    @Override
    public Mono<String> execute(Mono<List<ConsumerDmaapModel>> consumerDmaapModels)
            throws DatafileTaskException {
        if (consumerDmaapModels == null) {
            throw new DmaapNotFoundException("Invoked null object to DMaaP task");
        }
        dmaapProducerReactiveHttpClient = resolveClient();
        logger.trace("Method called with arg {}", consumerDmaapModels);
        return publish(consumerDmaapModels);
    }

    @Override
    protected DmaapPublisherConfiguration resolveConfiguration() {
        return datafileAppConfig.getDmaapPublisherConfiguration();
    }

    @Override
    DmaapProducerReactiveHttpClient resolveClient() {
        return dmaapProducerReactiveHttpClient == null
                ? new DmaapProducerReactiveHttpClient(resolveConfiguration()).createDmaapWebClient(buildWebClient())
                : dmaapProducerReactiveHttpClient;
    }
}
