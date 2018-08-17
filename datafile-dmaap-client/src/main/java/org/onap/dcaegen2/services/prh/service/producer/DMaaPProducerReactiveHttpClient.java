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

package org.onap.dcaegen2.services.prh.service.producer;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.client.utils.URIBuilder;
import org.onap.dcaegen2.services.prh.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.services.prh.model.ConsumerDmaapModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 7/4/18
 */
public class DMaaPProducerReactiveHttpClient {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private WebClient webClient;
    private final String dmaapHostName;
    private final Integer dmaapPortNumber;
    private final String dmaapProtocol;
    private final String dmaapTopicName;

    /**
     * Constructor DMaaPProducerReactiveHttpClient.
     *
     * @param dmaapPublisherConfiguration - DMaaP producer configuration object
     */
    public DMaaPProducerReactiveHttpClient(DmaapPublisherConfiguration dmaapPublisherConfiguration) {
        this.dmaapHostName = dmaapPublisherConfiguration.dmaapHostName();
        this.dmaapProtocol = dmaapPublisherConfiguration.dmaapProtocol();
        this.dmaapPortNumber = dmaapPublisherConfiguration.dmaapPortNumber();
        this.dmaapTopicName = dmaapPublisherConfiguration.dmaapTopicName();
    }

    /**
     * Function for calling DMaaP HTTP producer - post request to DMaaP.
     *
     * @param consumerDmaapModelMono - object which will be sent to DMaaP
     * @return status code of operation
     */
    public Mono<String> getDMaaPProducerResponse(Mono<ConsumerDmaapModel> consumerDmaapModelMono) {
        try {
            return webClient
                .post()
                .uri(getUri())
                .body(BodyInserters.fromObject(consumerDmaapModelMono))
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse ->
                    Mono.error(new Exception("HTTP 400"))
                )
                .onStatus(HttpStatus::is5xxServerError, clientResponse ->
                    Mono.error(new Exception("HTTP 500")))
                .bodyToMono(String.class);
        } catch (URISyntaxException e) {
            logger.warn("Exception while evaluating URI");
            return Mono.error(e);
        }
    }

    public DMaaPProducerReactiveHttpClient createDMaaPWebClient(WebClient webClient) {
        this.webClient = webClient;
        return this;
    }

    URI getUri() throws URISyntaxException {
        return new URIBuilder().setScheme(dmaapProtocol).setHost(dmaapHostName).setPort(dmaapPortNumber)
            .setPath(dmaapTopicName).build();
    }

}
