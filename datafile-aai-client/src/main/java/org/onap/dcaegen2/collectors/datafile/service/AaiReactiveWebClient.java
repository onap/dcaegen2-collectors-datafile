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

package org.onap.dcaegen2.collectors.datafile.service;

import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

import java.util.Map;

import org.onap.dcaegen2.collectors.datafile.config.AaiClientConfiguration;
import org.onap.dcaegen2.collectors.datafile.service.AaiReactiveWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


public class AaiReactiveWebClient {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private String aaiUserName;
    private String aaiUserPassword;
    private Map<String, String> aaiHeaders;

    /**
     * Creating AaiReactiveWebClient.
     * @param configuration - configuration object
     * @return AaiReactiveWebClient
     */
    public AaiReactiveWebClient fromConfiguration(AaiClientConfiguration configuration) {
        this.aaiUserName = configuration.aaiUserName();
        this.aaiUserPassword = configuration.aaiUserPassword();
        this.aaiHeaders = configuration.aaiHeaders();
        return this;
    }

    /**
     * Construct Reactive WebClient with appropriate settings.
     *
     * @return WebClient
     */
    public WebClient build() {
        return WebClient.builder()
                .defaultHeaders(httpHeaders -> httpHeaders.setAll(aaiHeaders))
                .filter(basicAuthentication(aaiUserName, aaiUserPassword))
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            logger.info("Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers()
                    .forEach((name, values) -> values.forEach(value -> logger.info("{}={}",name, value)));
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            logger.info("Response Status {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}
