/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018-2019 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.service;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.dcaegen2.collectors.datafile.utils.LoggingUtils;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapConsumerConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class DmaapWebClientTest {

    @Mock
    private DmaapConsumerConfiguration dmaapConsumerConfigurationMock;

    @Mock
    private ClientResponse clientResponseMock;

    @Mock
    private ClientRequest clientRequesteMock;

    @Test
    void buildsDMaaPReactiveWebClientProperly() {
        when(dmaapConsumerConfigurationMock.dmaapContentType()).thenReturn("*/*");
        WebClient dmaapWebClientUndetTest = new DmaapWebClient() //
            .fromConfiguration(dmaapConsumerConfigurationMock) //
            .build();

        verify(dmaapConsumerConfigurationMock, times(1)).dmaapContentType();
        assertNotNull(dmaapWebClientUndetTest);
    }

    @Test
    public void logResponseSuccess() {
        DmaapWebClient dmaapWebClientUndetTest = new DmaapWebClient();

        when(clientResponseMock.statusCode()).thenReturn(HttpStatus.OK);

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(DmaapWebClient.class, true);
        Mono<ClientResponse> logResponse = dmaapWebClientUndetTest.logResponse(clientResponseMock);

        assertEquals(clientResponseMock, logResponse.block());

        assertEquals(Level.TRACE, logAppender.list.get(0).getLevel());
        assertEquals("Response Status 200 OK", logAppender.list.get(0).getFormattedMessage());

        logAppender.stop();
    }

    @Test
    public void logRequestSuccess() throws URISyntaxException {
        when(clientRequesteMock.url()).thenReturn(new URI("http://test"));
        when(clientRequesteMock.method()).thenReturn(HttpMethod.GET);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("header", "value");
        when(clientRequesteMock.headers()).thenReturn(httpHeaders);

        DmaapWebClient dmaapWebClientUndetTest = new DmaapWebClient();

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(DmaapWebClient.class, true);
        Mono<ClientRequest> logRequest = dmaapWebClientUndetTest.logRequest(clientRequesteMock);

        assertEquals(clientRequesteMock, logRequest.block());

        assertEquals(Level.TRACE, logAppender.list.get(0).getLevel());
        assertEquals("Request: GET http://test", logAppender.list.get(0).getFormattedMessage());
        assertEquals(Level.TRACE, logAppender.list.get(1).getLevel());
        assertEquals("HTTP request headers: [header:\"value\"]", logAppender.list.get(1).getFormattedMessage());

        logAppender.stop();
    }
}
