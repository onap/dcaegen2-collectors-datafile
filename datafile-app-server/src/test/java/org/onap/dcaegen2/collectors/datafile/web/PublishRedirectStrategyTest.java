/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.RequestLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PublishRedirectStrategyTest {

    private static final String URI = "sftp://localhost:80/";

    private static PublishRedirectStrategy publishRedirectStrategy;
    private static final Map<String, String> CONTEXT_MAP = new HashMap<>();

    @BeforeAll
    static void setUp() {
        publishRedirectStrategy = new PublishRedirectStrategy(CONTEXT_MAP);
    }

    @Test
    void isRedirectable_shouldReturnTrue() {
        Assertions.assertTrue(publishRedirectStrategy.isRedirectable("Put"));
    }

    @Test
    void isRedirectable_shouldReturnFalse() {
        Assertions.assertFalse(publishRedirectStrategy.isRedirectable("not valid method"));
    }

    @Test
    void getRedirect_shouldReturnCorrectUri() throws ProtocolException {
        HttpRequest requestMock = mock(HttpRequest.class);
        HttpResponse responseMock = mock(HttpResponse.class);
        HttpContext contextMock = mock(HttpContext.class);
        Header headerMock = mock(Header.class);
        when(responseMock.getFirstHeader("location")).thenReturn(headerMock);
        when(headerMock.getValue()).thenReturn(URI);
        RequestConfig requestConfigMock = mock(RequestConfig.class);
        when(contextMock.getAttribute(HttpClientContext.REQUEST_CONFIG)).thenReturn(requestConfigMock);
        RequestLine requestLineMock = mock(RequestLine.class);
        when(requestMock.getRequestLine()).thenReturn(requestLineMock);
        when(requestLineMock.getUri()).thenReturn(URI);

        HttpUriRequest actualRedirect = publishRedirectStrategy.getRedirect(requestMock, responseMock, contextMock);
        assertEquals(URI, actualRedirect.getURI().toString());
    }
}
