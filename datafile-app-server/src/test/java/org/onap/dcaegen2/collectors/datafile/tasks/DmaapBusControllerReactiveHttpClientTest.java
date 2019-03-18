/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.dcaegen2.collectors.datafile.tasks;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;

import javax.net.ssl.SSLContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.onap.dcaegen2.collectors.datafile.configuration.DmaapBusControllerConfiguration;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.http.IHttpAsyncClientBuilder;

/**
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class DmaapBusControllerReactiveHttpClientTest {

    private static final String HOST = "54.45.33.2";
    private static final String HTTPS_SCHEME = "https";
    private static final int PORT = 1234;
    private static final String USER_NAME = "dradmin";

    private DmaapBusControllerReactiveHttpClient controllerClientUnderTestSpy;

    private DmaapBusControllerConfiguration publisherConfigurationMock = mock(DmaapBusControllerConfiguration.class);

    private IHttpAsyncClientBuilder clientBuilderMock;
    private CloseableHttpAsyncClient clientMock;
    @SuppressWarnings("unchecked")
    private Future<HttpResponse> futureMock = mock(Future.class);

    @BeforeEach
    void setUp() {
        when(publisherConfigurationMock.dmaapHostName()).thenReturn(HOST);
        when(publisherConfigurationMock.dmaapProtocol()).thenReturn(HTTPS_SCHEME);
        when(publisherConfigurationMock.dmaapPortNumber()).thenReturn(PORT);
        when(publisherConfigurationMock.dmaapUserName()).thenReturn(USER_NAME);
        when(publisherConfigurationMock.dmaapUserPassword()).thenReturn(USER_NAME);

        controllerClientUnderTestSpy = spy(new DmaapBusControllerReactiveHttpClient(publisherConfigurationMock));

        clientBuilderMock = mock(IHttpAsyncClientBuilder.class);
        clientMock = mock(CloseableHttpAsyncClient.class);
    }

    @Test
    void getHttpResponseWithRederict_Success() throws Exception {
        doReturn(clientBuilderMock).when(controllerClientUnderTestSpy).getHttpClientBuilder();
        when(clientBuilderMock.setSSLContext(any(SSLContext.class))).thenReturn(clientBuilderMock);
        when(clientBuilderMock.setSSLHostnameVerifier(any(NoopHostnameVerifier.class))).thenReturn(clientBuilderMock);
        when(clientBuilderMock.build()).thenReturn(clientMock);
        when(clientMock.execute(any(HttpUriRequest.class), any())).thenReturn(futureMock);
        HttpResponse responseMock = mock(HttpResponse.class);
        when(futureMock.get()).thenReturn(responseMock);

        HttpGet request = new HttpGet();
        controllerClientUnderTestSpy.getDmaapBusControllerResponse(request, 1000);

        ArgumentCaptor<RequestConfig> requestConfigCaptor = ArgumentCaptor.forClass(RequestConfig.class);
        verify(clientBuilderMock).setDefaultRequestConfig(requestConfigCaptor.capture());
        RequestConfig requestConfig = requestConfigCaptor.getValue();
        assertEquals(1000, requestConfig.getSocketTimeout());
        assertEquals(1000, requestConfig.getConnectTimeout());
        assertEquals(1000, requestConfig.getConnectionRequestTimeout());
        verify(clientBuilderMock).build();
        verifyNoMoreInteractions(clientBuilderMock);

        verify(clientMock).start();
        verify(clientMock).close();

        verify(futureMock).get();
        verifyNoMoreInteractions(futureMock);
    }

    @Test
    public void getResponseWithException_throwsException() throws Exception {
        doReturn(clientBuilderMock).when(controllerClientUnderTestSpy).getHttpClientBuilder();
        when(clientBuilderMock.setDefaultRequestConfig(any(RequestConfig.class))).thenReturn(clientBuilderMock);
        when(clientBuilderMock.build()).thenReturn(clientMock);
        HttpPut request = new HttpPut();
        when(clientMock.execute(any(HttpPut.class), any())).thenReturn(futureMock);

        try {
            when(futureMock.get()).thenThrow(new InterruptedException("Interrupted"));

            controllerClientUnderTestSpy.getDmaapBusControllerResponse(request, 1000);

            fail("Should have got an exception.");
        } catch (DatafileTaskException e) {
            assertTrue(e.getCause() instanceof InterruptedException);
            assertEquals("Interrupted", e.getCause().getMessage());
        } catch (Exception e) {
            fail("Wrong exception");
        }

        verify(clientMock).start();
        verify(clientMock).close();
    }

    @Test
    public void addCredentialsToHead_success() {
        HttpPut request = new HttpPut();

        controllerClientUnderTestSpy.addUserCredentialsToHead(request);

        String plainCreds = USER_NAME + ":" + USER_NAME;
        byte[] plainCredsBytes = plainCreds.getBytes(StandardCharsets.ISO_8859_1);
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        String base64Creds = "Basic " + new String(base64CredsBytes);
        Header[] authorizationHeaders = request.getHeaders("Authorization");
        assertEquals(base64Creds, authorizationHeaders[0].getValue());
    }

    @Test
    public void getBaseUri_success() {
        URI uri = controllerClientUnderTestSpy.getBaseUri().build();
        assertEquals(HTTPS_SCHEME + "://" + HOST + ":" + PORT, uri.toString());
    }
}
