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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.services.prh.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.services.prh.model.ConsumerDmaapModel;
import org.onap.dcaegen2.services.prh.model.ConsumerDmaapModelForUnitTest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 7/4/18
 */
class DMaaPProducerReactiveHttpClientTest {

    private DMaaPProducerReactiveHttpClient dmaapProducerReactiveHttpClient;

    private DmaapPublisherConfiguration dmaapPublisherConfigurationMock = mock(
        DmaapPublisherConfiguration.class);
    private ConsumerDmaapModel consumerDmaapModel = new ConsumerDmaapModelForUnitTest();
    private WebClient webClient = mock(WebClient.class);
    private RequestBodyUriSpec requestBodyUriSpec;
    private ResponseSpec responseSpec;


    @BeforeEach
    void setUp() {
        when(dmaapPublisherConfigurationMock.dmaapHostName()).thenReturn("54.45.33.2");
        when(dmaapPublisherConfigurationMock.dmaapProtocol()).thenReturn("https");
        when(dmaapPublisherConfigurationMock.dmaapPortNumber()).thenReturn(1234);
        when(dmaapPublisherConfigurationMock.dmaapUserName()).thenReturn("PRH");
        when(dmaapPublisherConfigurationMock.dmaapUserPassword()).thenReturn("PRH");
        when(dmaapPublisherConfigurationMock.dmaapContentType()).thenReturn("application/json");
        when(dmaapPublisherConfigurationMock.dmaapTopicName()).thenReturn("pnfReady");

        dmaapProducerReactiveHttpClient = new DMaaPProducerReactiveHttpClient(dmaapPublisherConfigurationMock);

        webClient = spy(WebClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, dmaapPublisherConfigurationMock.dmaapContentType())
            .filter(basicAuthentication(dmaapPublisherConfigurationMock.dmaapUserName(),
                dmaapPublisherConfigurationMock.dmaapUserPassword()))
            .build());
        requestBodyUriSpec = mock(RequestBodyUriSpec.class);
        responseSpec = mock(ResponseSpec.class);
    }

    @Test
    void getHttpResponse_Success() {
        //given
        Integer responseSuccess = 200;
        Mono<Integer> expectedResult = Mono.just(responseSuccess);

        //when
        mockWebClientDependantObject();
        doReturn(expectedResult).when(responseSpec).bodyToMono(String.class);
        dmaapProducerReactiveHttpClient.createDMaaPWebClient(webClient);
        Mono<String> response = dmaapProducerReactiveHttpClient.getDMaaPProducerResponse(Mono.just(consumerDmaapModel));

        //then
        Assertions.assertEquals(response.block(), expectedResult.block());
    }

    @Test
    void getHttpResponse_whenUriSyntaxExceptionHasBeenThrown() throws URISyntaxException {
        //given
        dmaapProducerReactiveHttpClient = spy(dmaapProducerReactiveHttpClient);
        //when
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        dmaapProducerReactiveHttpClient.createDMaaPWebClient(webClient);
        when(dmaapProducerReactiveHttpClient.getUri()).thenThrow(URISyntaxException.class);

        //then
        StepVerifier.create(dmaapProducerReactiveHttpClient.getDMaaPProducerResponse(any())).expectSubscription()
            .expectError(Exception.class).verify();
    }

    private void mockWebClientDependantObject() {
        RequestHeadersSpec requestHeadersSpec = mock(RequestHeadersSpec.class);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri((URI) any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any())).thenReturn(requestHeadersSpec);
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(responseSpec).when(responseSpec).onStatus(any(), any());
    }
}