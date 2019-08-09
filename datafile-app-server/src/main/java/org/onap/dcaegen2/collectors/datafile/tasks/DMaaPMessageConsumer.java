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

import com.google.gson.JsonElement;

import java.util.Optional;

import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.model.FileReadyMessage;
import org.onap.dcaegen2.collectors.datafile.service.JsonMessageParser;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.service.consumer.ConsumerReactiveHttpClientFactory;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.service.consumer.DMaaPConsumerReactiveHttpClient;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.service.consumer.DMaaPReactiveWebClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Component used to get messages from the MessageRouter.
 *
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class DMaaPMessageConsumer {
    private static final Logger logger = LoggerFactory.getLogger(DMaaPMessageConsumer.class);
    private final AppConfig datafileAppConfig;
    private final JsonMessageParser jsonMessageParser;
    private final ConsumerReactiveHttpClientFactory httpClientFactory;

    public DMaaPMessageConsumer(AppConfig datafileAppConfig) {
        this(datafileAppConfig, new JsonMessageParser(),
            new ConsumerReactiveHttpClientFactory(new DMaaPReactiveWebClientFactory()));
    }

    protected DMaaPMessageConsumer(AppConfig datafileAppConfig, JsonMessageParser jsonMessageParser,
        ConsumerReactiveHttpClientFactory httpClientFactory) {
        this.datafileAppConfig = datafileAppConfig;
        this.jsonMessageParser = jsonMessageParser;
        this.httpClientFactory = httpClientFactory;
    }

    /**
     * Gets the response from the MessageRouter and turns it into a stream of fileReady messages.
     *
     * @return a stream of fileReady messages.
     */
    public Flux<FileReadyMessage> getMessageRouterResponse() {
        logger.trace("getMessageRouterResponse called");
        try {
            DMaaPConsumerReactiveHttpClient client = createHttpClient();
            return consume((client.getDMaaPConsumerResponse(Optional.empty())));
        } catch (DatafileTaskException e) {
            logger.warn("Unable to get response from message router", e);
            return Flux.empty();
        }
    }

    private Flux<FileReadyMessage> consume(Mono<JsonElement> message) {
        logger.trace("consume called with arg {}", message);
        return jsonMessageParser.getMessagesFromJson(message);
    }

    public DMaaPConsumerReactiveHttpClient createHttpClient() throws DatafileTaskException {
        return httpClientFactory.create(datafileAppConfig.getDmaapConsumerConfiguration().toDmaap());
    }

}
