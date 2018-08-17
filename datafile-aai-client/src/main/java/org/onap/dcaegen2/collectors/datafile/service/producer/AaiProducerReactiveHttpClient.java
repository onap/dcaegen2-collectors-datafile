/*-
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

package org.onap.dcaegen2.collectors.datafile.service.producer;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;
import org.onap.dcaegen2.collectors.datafile.config.AaiClientConfiguration;
import org.onap.dcaegen2.collectors.datafile.exceptions.AaiRequestException;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.service.producer.AaiProducerReactiveHttpClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


public class AaiProducerReactiveHttpClient {

    private WebClient webClient;
    private final String aaiHost;
    private final String aaiProtocol;
    private final Integer aaiHostPortNumber;
    private final String aaiBasePath;


    /**
     * Constructor of AaiProducerReactiveHttpClient.
     *
     * @param configuration - AAI producer configuration object
     */
    public AaiProducerReactiveHttpClient(AaiClientConfiguration configuration) {
        this.aaiHost = configuration.aaiHost();
        this.aaiProtocol = configuration.aaiProtocol();
        this.aaiHostPortNumber = configuration.aaiPort();
        this.aaiBasePath = configuration.aaiBasePath();
    }

    /**
     * Function for calling AAI Http producer - patch request to AAI database.
     *
     * @param consumerDmaapModelMono - object which will be sent to AAI database
     * @return status code of operation
     */
    public Mono<Integer> getAaiProducerResponse(Mono<ConsumerDmaapModel> consumerDmaapModelMono) {
        return consumerDmaapModelMono.flatMap(this::patchAaiRequest);
    }

    public AaiProducerReactiveHttpClient createAaiWebClient(WebClient webClient) {
        this.webClient = webClient;
        return this;
    }

    private Mono<Integer> patchAaiRequest(ConsumerDmaapModel dmaapModel) {
        try {
            return webClient.patch()
                .uri(getUri(dmaapModel.getPnfName()))
                .body(BodyInserters.fromObject(dmaapModel))
                .retrieve()
                .onStatus(
                    HttpStatus::is4xxClientError,
                    clientResponse -> Mono.error(new AaiRequestException("HTTP 400"))
                )
                .onStatus(HttpStatus::is5xxServerError,
                    clientResponse -> Mono.error(new AaiRequestException("HTTP 500")))
                .bodyToMono(Integer.class);
        } catch (URISyntaxException e) {
            return Mono.error(e);
        }
    }

    URI getUri(String pnfName) throws URISyntaxException {
        return new URIBuilder()
            .setScheme(aaiProtocol)
            .setHost(aaiHost)
            .setPort(aaiHostPortNumber)
            .setPath(aaiBasePath + "/" + pnfName)
            .build();
    }
}
