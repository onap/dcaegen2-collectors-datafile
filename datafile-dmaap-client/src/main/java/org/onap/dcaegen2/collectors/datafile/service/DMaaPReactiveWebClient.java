/*
 * ============LICENSE_START=======================================================
 * Datafile Collector Service
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

package org.onap.dcaegen2.collectors.datafile.service;

import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

import org.onap.dcaegen2.collectors.datafile.config.DmaapCustomConfig;
import org.onap.dcaegen2.collectors.datafile.service.DMaaPReactiveWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 7/4/18
 */
public class DMaaPReactiveWebClient {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String dmaaPContentType;
    private String dmaaPUserName;
    private String dmaaPUserPassword;

    /**
     * Creating DMaaPReactiveWebClient passing to them basic DMaaPConfig.
     *
     * @param dmaapCustomConfig - configuration object
     * @return DMaaPReactiveWebClient
     */
    public DMaaPReactiveWebClient fromConfiguration(DmaapCustomConfig dmaapCustomConfig) {
        this.dmaaPUserName = dmaapCustomConfig.dmaapUserName();
        this.dmaaPUserPassword = dmaapCustomConfig.dmaapUserPassword();
        this.dmaaPContentType = dmaapCustomConfig.dmaapContentType();
        return this;
    }

    /**
     * Construct Reactive WebClient with appropriate settings.
     *
     * @return WebClient
     */
    public WebClient build() {
        return WebClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, dmaaPContentType)
            .filter(basicAuthentication(dmaaPUserName, dmaaPUserPassword))
            .filter(logRequest())
            .filter(logResponse())
            .build();
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            logger.info("Response Status {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            logger.info("Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers()
                .forEach((name, values) -> values.forEach(value -> logger.info("{}={}", name, value)));
            return Mono.just(clientRequest);
        });
    }

}
