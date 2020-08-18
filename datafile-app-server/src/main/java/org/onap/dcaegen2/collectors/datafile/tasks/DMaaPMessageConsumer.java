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
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.configuration.ConsumerConfiguration;
import org.onap.dcaegen2.collectors.datafile.model.FileReadyMessage;
import org.onap.dcaegen2.collectors.datafile.service.JsonMessageParser;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.api.MessageRouterSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

/**
 * Component used to get messages from the MessageRouter.
 *
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class DMaaPMessageConsumer {
    private static final Logger logger = LoggerFactory.getLogger(DMaaPMessageConsumer.class);
    private final AppConfig datafileAppConfig;
    private final JsonMessageParser jsonMessageParser;

    public DMaaPMessageConsumer(AppConfig datafileAppConfig) {
        this(datafileAppConfig, new JsonMessageParser());
    }

    protected DMaaPMessageConsumer(AppConfig datafileAppConfig, JsonMessageParser jsonMessageParser) {
        this.datafileAppConfig = datafileAppConfig;
        this.jsonMessageParser = jsonMessageParser;
    }

    /**
     * Gets the response from the MessageRouter and turns it into a stream of fileReady messages.
     *
     * @return a stream of fileReady messages.
     */
    public Flux<FileReadyMessage> getMessageRouterResponse() {
        logger.trace("getMessageRouterResponse called");
        try {
            ConsumerConfiguration dmaapConsumerConfiguration = datafileAppConfig.getDmaapConsumerConfiguration();
            MessageRouterSubscriber messageRouterSubscriber =
                dmaapConsumerConfiguration.getMessageRouterSubscriber();
            Flux<JsonElement> responseElements =
                messageRouterSubscriber.getElements(dmaapConsumerConfiguration.getMessageRouterSubscribeRequest());
            return consume(responseElements);
        } catch (Exception e) {
            logger.warn("Unable to get response from message router", e);
            return Flux.empty();
        }
    }

    private Flux<FileReadyMessage> consume(Flux<JsonElement> messages) {
        return jsonMessageParser.getMessagesFromJson(messages);
    }

}
