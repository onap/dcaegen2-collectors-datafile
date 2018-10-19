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

package org.onap.dcaegen2.collectors.datafile.web;


import org.junit.jupiter.api.Test;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class RequestResponseLoggingInterceptorTest {

    @Test
    void intercept_shouldReturnObject() throws URISyntaxException, IOException {

        //given
        RequestResponseLoggingInterceptor requestResponseLoggingInterceptor = new RequestResponseLoggingInterceptor();

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        HttpRequest request = mock(HttpRequest.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);

        byte[] BODY = new byte[] { (byte)0xe0, 0x4f, (byte)0xd0, 0x20, (byte)0xa2 };
        URI uri = new URI("www.someuri.com");

        //when
        when(execution.execute(request, BODY)).thenReturn(response);

        //then
        assertNotNull(requestResponseLoggingInterceptor.intercept(request, BODY, execution));
    }
}
