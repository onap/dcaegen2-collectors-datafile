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

package org.onap.dcaegen2.collectors.datafile.service.consumer;

import java.net.URI;
import java.util.function.Consumer;

import org.onap.dcaegen2.collectors.datafile.config.DmaapConsumerConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 6/26/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class DmaapConsumerReactiveHttpClient {

    private WebClient webClient;
    private final String dmaapHostName;
    private final String dmaapProtocol;
    private final Integer dmaapPortNumber;
    private final String dmaapTopicName;
    private final String consumerGroup;
    private final String consumerId;
    private final String contentType;

    /**
     * Constructor of DmaapConsumerReactiveHttpClient.
     *
     * @param consumerConfiguration - DMaaP consumer configuration object
     */
    public DmaapConsumerReactiveHttpClient(DmaapConsumerConfiguration consumerConfiguration) {
        this.dmaapHostName = consumerConfiguration.dmaapHostName();
        this.dmaapProtocol = consumerConfiguration.dmaapProtocol();
        this.dmaapPortNumber = consumerConfiguration.dmaapPortNumber();
        this.dmaapTopicName = consumerConfiguration.dmaapTopicName();
        this.consumerGroup = consumerConfiguration.consumerGroup();
        this.consumerId = consumerConfiguration.consumerId();
        this.contentType = consumerConfiguration.dmaapContentType();
    }

    /**
     * Function for calling DMaaP HTTP consumer - consuming messages from Kafka/DMaaP from topic.
     *
     * @return reactive response from DMaaP in string format
     */
    public Mono<String> getDmaapConsumerResponse() {
        return webClient.get().uri(getUri()).headers(getHeaders()).retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(new Exception("HTTP 400")))
                .onStatus(HttpStatus::is5xxServerError, clientResponse -> Mono.error(new Exception("HTTP 500")))
                .bodyToMono(String.class);
    }

    private Consumer<HttpHeaders> getHeaders() {
        return httpHeaders -> httpHeaders.set(HttpHeaders.CONTENT_TYPE, contentType);
    }

    private String createRequestPath() {
        return dmaapTopicName + "/" + consumerGroup + "/" + consumerId;
    }

    public DmaapConsumerReactiveHttpClient createDmaapWebClient(WebClient webClient) {
        this.webClient = webClient;
        return this;
    }

    URI getUri() {
        return new DefaultUriBuilderFactory().builder().scheme(dmaapProtocol).host(dmaapHostName).port(dmaapPortNumber)
                .path(createRequestPath()).build();
    }
}
