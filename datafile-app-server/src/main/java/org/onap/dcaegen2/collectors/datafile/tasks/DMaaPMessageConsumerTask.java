/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.dcaegen2.collectors.datafile.tasks;


import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.model.FileReadyMessage;
import org.onap.dcaegen2.collectors.datafile.service.DmaapReactiveWebClient;
import org.onap.dcaegen2.collectors.datafile.service.JsonMessageParser;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapConsumerConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.service.consumer.DMaaPConsumerReactiveHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class DMaaPMessageConsumerTask {
    private static final Logger logger = LoggerFactory.getLogger(DMaaPMessageConsumerTask.class);

    private AppConfig datafileAppConfig;
    private JsonMessageParser jsonMessageParser;
    private DMaaPConsumerReactiveHttpClient dmaaPConsumerReactiveHttpClient;

    public DMaaPMessageConsumerTask(AppConfig datafileAppConfig) {
        this.datafileAppConfig = datafileAppConfig;
        this.jsonMessageParser = new JsonMessageParser();
    }

    protected DMaaPMessageConsumerTask(AppConfig datafileAppConfig,
                                    DMaaPConsumerReactiveHttpClient dmaaPConsumerReactiveHttpClient,
            JsonMessageParser messageParser) {
        this.datafileAppConfig = datafileAppConfig;
        this.dmaaPConsumerReactiveHttpClient = dmaaPConsumerReactiveHttpClient;
        this.jsonMessageParser = messageParser;
    }

    public Flux<FileReadyMessage> execute() {
        dmaaPConsumerReactiveHttpClient = resolveClient();
        logger.trace("execute called");
        return consume((dmaaPConsumerReactiveHttpClient.getDMaaPConsumerResponse()));
    }

    private Flux<FileReadyMessage> consume(Mono<String> message) {
        logger.trace("consume called with arg {}", message);
        return jsonMessageParser.getMessagesFromJson(message);
    }

    protected DmaapConsumerConfiguration resolveConfiguration() {
        return datafileAppConfig.getDmaapConsumerConfiguration();
    }

    protected DMaaPConsumerReactiveHttpClient resolveClient() {
        return new DMaaPConsumerReactiveHttpClient(resolveConfiguration(), buildWebClient());
    }

    protected WebClient buildWebClient() {
        return new DmaapReactiveWebClient().fromConfiguration(resolveConfiguration()).build();
    }
}
