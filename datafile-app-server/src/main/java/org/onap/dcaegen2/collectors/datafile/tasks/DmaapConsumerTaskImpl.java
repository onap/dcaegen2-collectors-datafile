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


import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.configuration.Config;
import org.onap.dcaegen2.collectors.datafile.model.FileData;
import org.onap.dcaegen2.collectors.datafile.service.DmaapConsumerJsonParser;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapConsumerConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.service.consumer.DMaaPConsumerReactiveHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 3/23/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
@Component
public class DmaapConsumerTaskImpl extends DmaapConsumerTask {

    private static final Logger logger = LoggerFactory.getLogger(DmaapConsumerTaskImpl.class);

    private Config datafileAppConfig;
    private DmaapConsumerJsonParser dmaapConsumerJsonParser;
    private DMaaPConsumerReactiveHttpClient dmaaPConsumerReactiveHttpClient;

    @Autowired
    public DmaapConsumerTaskImpl(AppConfig datafileAppConfig) {
        this.datafileAppConfig = datafileAppConfig;
        this.dmaapConsumerJsonParser = new DmaapConsumerJsonParser();
    }

    protected DmaapConsumerTaskImpl(AppConfig datafileAppConfig,
                                    DMaaPConsumerReactiveHttpClient dmaaPConsumerReactiveHttpClient,
                                    DmaapConsumerJsonParser dmaapConsumerJsonParser) {
        this.datafileAppConfig = datafileAppConfig;
        this.dmaaPConsumerReactiveHttpClient = dmaaPConsumerReactiveHttpClient;
        this.dmaapConsumerJsonParser = dmaapConsumerJsonParser;
    }

    @Override
    Flux<FileData> consume(Mono<String> message) {
        logger.trace("consume called with arg {}", message);
        return dmaapConsumerJsonParser.getJsonObject(message);
    }

    @Override
    protected Flux<FileData> execute(String object) {
        dmaaPConsumerReactiveHttpClient = resolveClient();
        logger.trace("execute called with arg {}", object);
        return consume((dmaaPConsumerReactiveHttpClient.getDMaaPConsumerResponse()));
    }

    @Override
    void initConfigs() {
        datafileAppConfig.initFileStreamReader();
    }

    @Override
    protected DmaapConsumerConfiguration resolveConfiguration() {
        return datafileAppConfig.getDmaapConsumerConfiguration();
    }

    @Override
    protected DMaaPConsumerReactiveHttpClient resolveClient() {
        return new DMaaPConsumerReactiveHttpClient(resolveConfiguration(),buildWebClient());
    }
}
