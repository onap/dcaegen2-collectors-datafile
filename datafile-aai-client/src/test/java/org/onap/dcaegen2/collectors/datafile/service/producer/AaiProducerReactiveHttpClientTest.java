/*-
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

package org.onap.dcaegen2.collectors.datafile.service.producer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.config.AaiClientConfiguration;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModelForUnitTest;
import org.onap.dcaegen2.collectors.datafile.service.producer.AaiProducerReactiveHttpClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


class AaiProducerReactiveHttpClientTest {

    private static AaiProducerReactiveHttpClient aaiProducerReactiveHttpClient;

    private static final Integer SUCCESS_RESPONSE = 200;

    private static AaiClientConfiguration aaiConfigurationMock = mock(AaiClientConfiguration.class);
    private static WebClient webClient = mock(WebClient.class);

    private static ConsumerDmaapModel dmaapModel = new ConsumerDmaapModelForUnitTest();
    private static WebClient.RequestBodyUriSpec requestBodyUriSpec;
    private static ResponseSpec responseSpec;

    private static Map<String, String> aaiHeaders;

    @BeforeAll
    static void setUp() {
        setupHeaders();

        when(aaiConfigurationMock.aaiHost()).thenReturn("54.45.33.2");
        when(aaiConfigurationMock.aaiProtocol()).thenReturn("https");
        when(aaiConfigurationMock.aaiPort()).thenReturn(1234);
        when(aaiConfigurationMock.aaiUserName()).thenReturn("Datafile");
        when(aaiConfigurationMock.aaiUserPassword()).thenReturn("Datafile");
        when(aaiConfigurationMock.aaiBasePath()).thenReturn("/aai/v11");
        when(aaiConfigurationMock.aaiPnfPath()).thenReturn("/network/pnfs/pnf");
        when(aaiConfigurationMock.aaiHeaders()).thenReturn(aaiHeaders);

        aaiProducerReactiveHttpClient = new AaiProducerReactiveHttpClient(aaiConfigurationMock);

        webClient = spy(WebClient.builder()
            .defaultHeaders(httpHeaders -> httpHeaders.setAll(aaiHeaders))
            .filter(basicAuthentication(aaiConfigurationMock.aaiUserName(), aaiConfigurationMock.aaiUserPassword()))
            .build());

        requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        responseSpec = mock(ResponseSpec.class);
    }


    @Test
    void getAaiProducerResponse_shouldReturn200() {
        //given
        Mono<Integer> expectedResult = Mono.just(SUCCESS_RESPONSE);

        //when
        mockWebClientDependantObject();
        doReturn(expectedResult).when(responseSpec).bodyToMono(Integer.class);
        aaiProducerReactiveHttpClient.createAaiWebClient(webClient);
        Mono<Integer> response = aaiProducerReactiveHttpClient.getAaiProducerResponse(Mono.just(dmaapModel));

        //then
        StepVerifier.create(response).expectSubscription()
            .expectNextMatches(results -> {
                Assertions.assertEquals(results, expectedResult.block());
                return true;
            }).verifyComplete();
    }

    @Test
    void getHttpResponse_whenUriSyntaxExceptionHasBeenThrown() throws URISyntaxException {
        ///given
        aaiProducerReactiveHttpClient = spy(aaiProducerReactiveHttpClient);
        //when
        when(webClient.patch()).thenReturn(requestBodyUriSpec);
        aaiProducerReactiveHttpClient.createAaiWebClient(webClient);
        when(aaiProducerReactiveHttpClient.getUri("pnfName")).thenThrow(URISyntaxException.class);

        //then
        StepVerifier.create(
            aaiProducerReactiveHttpClient.getAaiProducerResponse(
                Mono.just(dmaapModel)
            )).expectSubscription().expectError(Exception.class).verify();
    }


    private void mockWebClientDependantObject() {
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        when(webClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri((URI) any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any())).thenReturn(requestHeadersSpec);
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(responseSpec).when(responseSpec).onStatus(any(), any());
    }

    private static void setupHeaders() {
        aaiHeaders = new HashMap<>();
        aaiHeaders.put("X-FromAppId", "Datafile");
        aaiHeaders.put("X-TransactionId", "vv-temp");
        aaiHeaders.put("Accept", "application/json");
        aaiHeaders.put("Real-Time", "true");
        aaiHeaders.put("Content-Type", "application/merge-patch+json");
    }

}

