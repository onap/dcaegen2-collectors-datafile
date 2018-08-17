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

import org.onap.dcaegen2.services.prh.config.DmaapConsumerConfiguration;
import org.onap.dcaegen2.services.prh.exceptions.PrhTaskException;
import org.onap.dcaegen2.services.prh.model.ConsumerDmaapModel;
import org.onap.dcaegen2.services.prh.service.DMaaPReactiveWebClient;
import org.onap.dcaegen2.services.prh.service.consumer.DMaaPConsumerReactiveHttpClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 4/13/18
 */
abstract class DmaapConsumerTask {

    abstract Mono<ConsumerDmaapModel> consume(Mono<String> message) throws PrhTaskException;

    abstract DMaaPConsumerReactiveHttpClient resolveClient();

    abstract void initConfigs();

    protected abstract DmaapConsumerConfiguration resolveConfiguration();

    protected abstract Mono<ConsumerDmaapModel> execute(String object) throws PrhTaskException;

    WebClient buildWebClient() {
        return new DMaaPReactiveWebClient().fromConfiguration(resolveConfiguration()).build();
    }
}
