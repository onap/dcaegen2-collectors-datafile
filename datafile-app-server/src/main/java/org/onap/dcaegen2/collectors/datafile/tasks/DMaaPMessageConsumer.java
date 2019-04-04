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
import org.onap.dcaegen2.collectors.datafile.service.DmaapWebClient;
import org.onap.dcaegen2.collectors.datafile.service.JsonMessageParser;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapConsumerConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.service.consumer.DMaaPConsumerReactiveHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Component used to get messages from the MessageRouter.
 *
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class DMaaPMessageConsumer {
    private static final Logger logger = LoggerFactory.getLogger(DMaaPMessageConsumer.class);

    private final JsonMessageParser jsonMessageParser;
    private final DMaaPConsumerReactiveHttpClient dmaaPConsumerReactiveHttpClient;

    public DMaaPMessageConsumer(AppConfig datafileAppConfig) {
        this.jsonMessageParser = new JsonMessageParser();
        this.dmaaPConsumerReactiveHttpClient = createHttpClient(datafileAppConfig);
    }

    protected DMaaPMessageConsumer(DMaaPConsumerReactiveHttpClient dmaaPConsumerReactiveHttpClient,
            JsonMessageParser messageParser) {
        this.dmaaPConsumerReactiveHttpClient = dmaaPConsumerReactiveHttpClient;
        this.jsonMessageParser = messageParser;
    }

    /**
     * Gets the response from the MessageRouter and turns it into a stream of fileReady messages.
     *
     * @return a stream of fileReady messages.
     */
    public Flux<FileReadyMessage> getMessageRouterResponse() {
        logger.trace("getMessageRouterResponse called");
        return consume((dmaaPConsumerReactiveHttpClient.getDMaaPConsumerResponse()));
    }

    private Flux<FileReadyMessage> consume(Mono<String> message) {
        logger.trace("consume called with arg {}", message);
        return jsonMessageParser.getMessagesFromJson(message);
    }

    private static DMaaPConsumerReactiveHttpClient createHttpClient(AppConfig datafileAppConfig) {
        DmaapConsumerConfiguration config = datafileAppConfig.getDmaapConsumerConfiguration();
        WebClient client = new DmaapWebClient().fromConfiguration(config).build();
        return new DMaaPConsumerReactiveHttpClient(config, client);
    }

}
