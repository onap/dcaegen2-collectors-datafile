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

import java.time.Duration;

import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.configuration.Config;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.service.HttpUtils;
import org.onap.dcaegen2.collectors.datafile.service.producer.DmaapProducerReactiveHttpClient;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapPublisherConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import reactor.core.publisher.Flux;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 4/13/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class DmaapPublisherTaskImpl {

    private static final Logger logger = LoggerFactory.getLogger(DmaapPublisherTaskImpl.class);
    private final Config datafileAppConfig;

    public DmaapPublisherTaskImpl(AppConfig datafileAppConfig) {
        this.datafileAppConfig = datafileAppConfig;
    }

    /**
     * Publish one file
     * @param consumerDmaapModel information about the file to publish
     * @param maxNumberOfRetries the maximal number of retries if the publishing fails
     * @param firstBackoffTimeout the time to delay the first retry
     * @return the HTTP response status as a string
     */
    public Flux<String> execute(ConsumerDmaapModel consumerDmaapModel, long numRetries, Duration firstBackoff) {
        logger.trace("Method called with arg {}", consumerDmaapModel);
        DmaapProducerReactiveHttpClient dmaapProducerReactiveHttpClient = resolveClient();

    //@formatter:off
    return Flux.just(consumerDmaapModel)
        .cache(1)
        .flatMap(dmaapProducerReactiveHttpClient::getDmaapProducerResponse)
        .flatMap(this::handleHttpResponse)
        .retryBackoff(numRetries, firstBackoff);
    //@formatter:on
    }

    private Flux<String> handleHttpResponse(HttpStatus response) {

        if (HttpUtils.isSuccessfulResponseCode(response.value())) {
            logger.trace("Publish to DR successful!");
            return Flux.just(response.toString());
        } else {
            logger.warn("Publish to DR unsuccessful, response code: " + response);
            return Flux.error(new Exception("Publish to DR unsuccessful, response code: " + response));
        }
    }

    DmaapPublisherConfiguration resolveConfiguration() {
        return datafileAppConfig.getDmaapPublisherConfiguration();
    }

    DmaapProducerReactiveHttpClient resolveClient() {
        return new DmaapProducerReactiveHttpClient(resolveConfiguration());
    }
}
