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

package org.onap.dcaegen2.collectors.datafile.service.producer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.model.CommonFunctions;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModelForUnitTest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.util.DefaultUriBuilderFactory;

import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 7/4/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
class DmaapProducerReactiveHttpClientTest {

    private static final String FILE_NAME = "A20161224.1030-1045.bin.gz";
    private static final String LOCATION_JSON_TAG = "location";
    private static final String X_ATT_DR_META = "X-ATT-DR-META";

    private static final String HOST = "54.45.33.2";
    private static final String HTTP_SCHEME = "http";
    private static final int PORT = 1234;
    private static final String APPLICATION_OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";
    private static final String PUBLISH_TOPIC = "publish";
    private static final String DEFAULT_FEED_ID = "1";

    private DmaapProducerReactiveHttpClient dmaapProducerReactiveHttpClient;

    private DmaapPublisherConfiguration dmaapPublisherConfigurationMock = mock(DmaapPublisherConfiguration.class);
    private ConsumerDmaapModel consumerDmaapModel = new ConsumerDmaapModelForUnitTest();
    private WebClient webClientMock = mock(WebClient.class);
    private RequestBodyUriSpec requestBodyUriSpecMock;
    private ResponseSpec responseSpecMock;


    @BeforeEach
    void setUp() {
        when(dmaapPublisherConfigurationMock.dmaapHostName()).thenReturn(HOST);
        when(dmaapPublisherConfigurationMock.dmaapProtocol()).thenReturn(HTTP_SCHEME);
        when(dmaapPublisherConfigurationMock.dmaapPortNumber()).thenReturn(PORT);
        when(dmaapPublisherConfigurationMock.dmaapUserName()).thenReturn("DATAFILE");
        when(dmaapPublisherConfigurationMock.dmaapUserPassword()).thenReturn("DATAFILE");
        when(dmaapPublisherConfigurationMock.dmaapContentType()).thenReturn(APPLICATION_OCTET_STREAM_CONTENT_TYPE);
        when(dmaapPublisherConfigurationMock.dmaapTopicName()).thenReturn(PUBLISH_TOPIC);

        dmaapProducerReactiveHttpClient = new DmaapProducerReactiveHttpClient(dmaapPublisherConfigurationMock);

        webClientMock = spy(WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, dmaapPublisherConfigurationMock.dmaapContentType())
                .filter(basicAuthentication(dmaapPublisherConfigurationMock.dmaapUserName(),
                        dmaapPublisherConfigurationMock.dmaapUserPassword()))
                .build());
        requestBodyUriSpecMock = mock(RequestBodyUriSpec.class);
        responseSpecMock = mock(ResponseSpec.class);
    }

    @Test
    void getHttpResponse_Success() {
        mockWebClientDependantObject();
        dmaapProducerReactiveHttpClient.createDmaapWebClient(webClientMock);
        List<ConsumerDmaapModel> consumerDmaapModelList = new ArrayList<ConsumerDmaapModel>();
        consumerDmaapModelList.add(consumerDmaapModel);

        dmaapProducerReactiveHttpClient.getDmaapProducerResponse(Mono.just(consumerDmaapModelList));

        verify(requestBodyUriSpecMock).header(HttpHeaders.CONTENT_TYPE, APPLICATION_OCTET_STREAM_CONTENT_TYPE);
        JsonElement metaData = new JsonParser().parse(CommonFunctions.createJsonBody(consumerDmaapModel));
        metaData.getAsJsonObject().remove(LOCATION_JSON_TAG);
        verify(requestBodyUriSpecMock).header(X_ATT_DR_META, metaData.toString());
        URI expectedUri = new DefaultUriBuilderFactory().builder().scheme(HTTP_SCHEME).host(HOST).port(PORT)
                .path(PUBLISH_TOPIC + "/" + DEFAULT_FEED_ID + "/" + FILE_NAME).build();
        verify(requestBodyUriSpecMock).uri(expectedUri);
        verify(requestBodyUriSpecMock).body(any());
    }

    private void mockWebClientDependantObject() {
        when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
        when(requestBodyUriSpecMock.uri((URI) any())).thenReturn(requestBodyUriSpecMock);

        when(requestBodyUriSpecMock.retrieve()).thenReturn(responseSpecMock);
        when(responseSpecMock.onStatus(any(), any())).thenReturn(responseSpecMock);
        Mono<String> expectedResult = Mono.just("200");
        when(responseSpecMock.bodyToMono(String.class)).thenReturn(expectedResult);
    }
}
