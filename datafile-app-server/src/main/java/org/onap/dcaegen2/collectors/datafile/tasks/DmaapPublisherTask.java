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

import java.util.ArrayList;

import org.onap.dcaegen2.collectors.datafile.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.service.DmaapReactiveWebClient;
import org.onap.dcaegen2.collectors.datafile.service.producer.DmaapProducerReactiveHttpClient;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 3/23/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
abstract class DmaapPublisherTask {

    abstract Mono<String> publish(Mono<ArrayList<ConsumerDmaapModel>> consumerDmaapModel) throws DatafileTaskException;

    abstract DmaapProducerReactiveHttpClient resolveClient();

    protected abstract DmaapPublisherConfiguration resolveConfiguration();

    protected abstract Mono<String> execute(Mono<ArrayList<ConsumerDmaapModel>> consumerDmaapModel)
            throws DatafileTaskException;

    WebClient buildWebClient() {
        return new DmaapReactiveWebClient().fromConfiguration(resolveConfiguration()).build();
    }
}
