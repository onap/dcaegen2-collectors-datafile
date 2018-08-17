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

import org.onap.dcaegen2.collectors.datafile.config.DmaapConsumerConfiguration;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.configuration.Config;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.service.DmaapConsumerJsonParser;
import org.onap.dcaegen2.collectors.datafile.service.consumer.DMaaPConsumerReactiveHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 3/23/18
 */
@Component
public class DmaapConsumerTaskImpl extends DmaapConsumerTask {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Config datafileAppConfig;
    private DmaapConsumerJsonParser dmaapConsumerJsonParser;
    private DMaaPConsumerReactiveHttpClient dmaaPConsumerReactiveHttpClient;

    @Autowired
    public DmaapConsumerTaskImpl(AppConfig datafileAppConfig) {
        this.datafileAppConfig = datafileAppConfig;
        this.dmaapConsumerJsonParser = new DmaapConsumerJsonParser();
    }

    DmaapConsumerTaskImpl(AppConfig datafileAppConfig, DmaapConsumerJsonParser dmaapConsumerJsonParser) {
        this.datafileAppConfig = datafileAppConfig;
        this.dmaapConsumerJsonParser = dmaapConsumerJsonParser;
    }

    @Override
    Mono<ConsumerDmaapModel> consume(Mono<String> message) {
        logger.info("Consumed model from DMaaP: {}", message);
        return dmaapConsumerJsonParser.getJsonObject(message);
    }

    @Override
    public Mono<ConsumerDmaapModel> execute(String object) {
        dmaaPConsumerReactiveHttpClient = resolveClient();
        logger.trace("Method called with arg {}", object);
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
    DMaaPConsumerReactiveHttpClient resolveClient() {
        return dmaaPConsumerReactiveHttpClient == null
            ? new DMaaPConsumerReactiveHttpClient(resolveConfiguration()).createDMaaPWebClient(buildWebClient())
            : dmaaPConsumerReactiveHttpClient;
    }
}
