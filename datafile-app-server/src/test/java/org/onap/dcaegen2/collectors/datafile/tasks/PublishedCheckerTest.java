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

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.service.HttpUtils;
import org.onap.dcaegen2.collectors.datafile.service.producer.DmaapProducerReactiveHttpClient;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapPublisherConfiguration;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

/**
 * @author <a href="mailto:maxime.bonneau@est.tech">Maxime Bonneau</a>
 *
 */
public class PublishedCheckerTest {
    private static final String EMPTY_CONTENT = "[]";
    private static final String FEEDLOG_TOPIC = "feedlog";
    private static final String FEED_ID = "1";
    private static final String HTTPS_SCHEME = "https";
    private static final String HOST = "54.45.33.2";
    private static final int PORT = 1234;
    private static final String SOURCE_NAME = "oteNB5309";
    private static final String FILE_NAME = "A20161224.1030-1045.bin.gz";
    private static final String LOCAL_FILE_NAME = SOURCE_NAME + "_" + FILE_NAME;

    private static final Map<String, String> CONTEXT_MAP = new HashMap<>();

    private static DmaapPublisherConfiguration publisherConfigurationMock = mock(DmaapPublisherConfiguration.class);
    private static AppConfig appConfigMock;
    private DmaapProducerReactiveHttpClient httpClientMock = mock(DmaapProducerReactiveHttpClient.class);

    private PublishedChecker publishedCheckerUnderTestSpy;

    /**
     * Sets up data for the tests.
     */
    @BeforeAll
    public static void setUp() {
        when(publisherConfigurationMock.dmaapHostName()).thenReturn(HOST);
        when(publisherConfigurationMock.dmaapProtocol()).thenReturn(HTTPS_SCHEME);
        when(publisherConfigurationMock.dmaapPortNumber()).thenReturn(PORT);

        appConfigMock = mock(AppConfig.class);
        when(appConfigMock.getDmaapPublisherConfiguration()).thenReturn(publisherConfigurationMock);
    }

    @Test
    public void executeWhenNotPublished_returnsFalse() throws Exception {
        prepareMocksForTests(HttpUtils.SC_OK, EMPTY_CONTENT, null);

        boolean isPublished = publishedCheckerUnderTestSpy.execute(LOCAL_FILE_NAME, CONTEXT_MAP);

        assertFalse(isPublished);

        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClientMock).getBaseUri();
        verify(httpClientMock).addUserCredentialsToHead(any(HttpUriRequest.class));
        verify(httpClientMock).getDmaapProducerResponseWithCustomTimeout(requestCaptor.capture(), any(), any());
        verifyNoMoreInteractions(httpClientMock);

        HttpUriRequest getRequest = requestCaptor.getValue();
        assertTrue(getRequest instanceof HttpGet);
        URI actualUri = getRequest.getURI();
        assertEquals(HTTPS_SCHEME, actualUri.getScheme());
        assertEquals(HOST, actualUri.getHost());
        assertEquals(PORT, actualUri.getPort());
        Path actualPath = Paths.get(actualUri.getPath());
        assertTrue(FEEDLOG_TOPIC.equals(actualPath.getName(0).toString()));
        assertTrue(FEED_ID.equals(actualPath.getName(1).toString()));
        String actualQuery = actualUri.getQuery();
        assertTrue(actualQuery.contains("type=pub"));
        assertTrue(actualQuery.contains("filename=" + LOCAL_FILE_NAME));
    }

    @Test
    public void executeWhenDataRouterReturnsNok_returnsFalse() throws Exception {
        prepareMocksForTests(HttpUtils.SC_BAD_REQUEST, EMPTY_CONTENT, null);

        boolean isPublished = publishedCheckerUnderTestSpy.execute(LOCAL_FILE_NAME, CONTEXT_MAP);

        assertFalse(isPublished);
    }

    @Test
    public void executeWhenPublished_returnsTrue() throws Exception {
        prepareMocksForTests(HttpUtils.SC_OK, "[" + LOCAL_FILE_NAME + "]", null);

        boolean isPublished = publishedCheckerUnderTestSpy.execute(LOCAL_FILE_NAME, CONTEXT_MAP);

        assertTrue(isPublished);
    }

    @Test
    public void executeWhenErrorInDataRouter_returnsFalse() throws Exception {
        prepareMocksForTests(HttpUtils.SC_OK, EMPTY_CONTENT, new DatafileTaskException(""));

        boolean isPublished = publishedCheckerUnderTestSpy.execute(LOCAL_FILE_NAME, CONTEXT_MAP);

        assertFalse(isPublished);
    }

    final void prepareMocksForTests(int responseCode, String content, Exception exception) throws Exception {
        publishedCheckerUnderTestSpy = spy(new PublishedChecker(appConfigMock));

        doReturn(publisherConfigurationMock).when(publishedCheckerUnderTestSpy).resolveConfiguration();
        doReturn(httpClientMock).when(publishedCheckerUnderTestSpy).resolveClient();

        UriBuilder uriBuilder = new DefaultUriBuilderFactory().builder().scheme(HTTPS_SCHEME).host(HOST).port(PORT);
        when(httpClientMock.getBaseUri()).thenReturn(uriBuilder);

        HttpResponse httpResponseMock = mock(HttpResponse.class);
        if (exception == null) {
            when(httpClientMock.getDmaapProducerResponseWithCustomTimeout(any(HttpUriRequest.class), any(), any()))
                    .thenReturn(httpResponseMock);
        } else {
            when(httpClientMock.getDmaapProducerResponseWithCustomTimeout(any(HttpUriRequest.class), any(), any()))
                    .thenThrow(exception);
        }
        HttpEntity httpEntityMock = mock(HttpEntity.class);
        StatusLine statusLineMock = mock(StatusLine.class);
        when(httpResponseMock.getStatusLine()).thenReturn(statusLineMock);
        when(statusLineMock.getStatusCode()).thenReturn(responseCode);
        when(httpResponseMock.getEntity()).thenReturn(httpEntityMock);
        InputStream stream = new ByteArrayInputStream(content.getBytes());
        when(httpEntityMock.getContent()).thenReturn(stream);
    }
}
