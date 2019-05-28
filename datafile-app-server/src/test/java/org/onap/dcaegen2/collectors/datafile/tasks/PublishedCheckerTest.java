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
import org.onap.dcaegen2.collectors.datafile.configuration.PublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.service.HttpUtils;
import org.onap.dcaegen2.collectors.datafile.service.producer.DmaapProducerHttpClient;

public class PublishedCheckerTest {
    private static final String PUBLISH_URL = "https://54.45.33.2:1234/";
    private static final String EMPTY_CONTENT = "[]";
    private static final String SOURCE_NAME = "oteNB5309";
    private static final String FILE_NAME = "A20161224.1030-1045.bin.gz";
    private static final String LOCAL_FILE_NAME = SOURCE_NAME + "_" + FILE_NAME;
    private static final String CHANGE_IDENTIFIER = "PM_MEAS_FILES";
    private static final String LOG_URI = "https://localhost:3907/feedlog/1";

    private static final Map<String, String> CONTEXT_MAP = new HashMap<>();

    private static PublisherConfiguration publisherConfigurationMock = mock(PublisherConfiguration.class);
    private static AppConfig appConfigMock;
    private DmaapProducerHttpClient httpClientMock = mock(DmaapProducerHttpClient.class);

    private PublishedChecker publishedCheckerUnderTestSpy;


    @BeforeAll
    public static void setUp() throws DatafileTaskException {
        when(publisherConfigurationMock.publishUrl()).thenReturn(PUBLISH_URL);

        appConfigMock = mock(AppConfig.class);
        when(appConfigMock.getPublisherConfiguration(CHANGE_IDENTIFIER)).thenReturn(publisherConfigurationMock);
    }

    @Test
    public void executeWhenNotPublished_returnsFalse() throws Exception {
        prepareMocksForTests(HttpUtils.SC_OK, EMPTY_CONTENT, null);

        boolean isPublished =
                publishedCheckerUnderTestSpy.isFilePublished(LOCAL_FILE_NAME, CHANGE_IDENTIFIER, CONTEXT_MAP);

        assertFalse(isPublished);

        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClientMock).addUserCredentialsToHead(any(HttpUriRequest.class));
        verify(httpClientMock).getDmaapProducerResponseWithCustomTimeout(requestCaptor.capture(), any(), any());
        verifyNoMoreInteractions(httpClientMock);

        HttpUriRequest getRequest = requestCaptor.getValue();
        assertTrue(getRequest instanceof HttpGet);
        URI actualUri = getRequest.getURI();
        // https://localhost:3907/feedlog/1?type=pub&filename=oteNB5309_A20161224.1030-1045.bin.gz
        String expUri = LOG_URI + "?type=pub&filename=" + LOCAL_FILE_NAME;
        assertEquals(expUri, actualUri.toString());
    }

    @Test
    public void executeWhenDataRouterReturnsNok_returnsFalse() throws Exception {
        prepareMocksForTests(HttpUtils.SC_BAD_REQUEST, EMPTY_CONTENT, null);

        boolean isPublished =
                publishedCheckerUnderTestSpy.isFilePublished(LOCAL_FILE_NAME, CHANGE_IDENTIFIER, CONTEXT_MAP);

        assertFalse(isPublished);
    }

    @Test
    public void executeWhenPublished_returnsTrue() throws Exception {
        prepareMocksForTests(HttpUtils.SC_OK, "[" + LOCAL_FILE_NAME + "]", null);

        boolean isPublished =
                publishedCheckerUnderTestSpy.isFilePublished(LOCAL_FILE_NAME, CHANGE_IDENTIFIER, CONTEXT_MAP);

        assertTrue(isPublished);
    }

    @Test
    public void executeWhenErrorInDataRouter_returnsFalse() throws Exception {
        prepareMocksForTests(HttpUtils.SC_OK, EMPTY_CONTENT, new DatafileTaskException(""));

        boolean isPublished =
                publishedCheckerUnderTestSpy.isFilePublished(LOCAL_FILE_NAME, CHANGE_IDENTIFIER, CONTEXT_MAP);

        assertFalse(isPublished);
    }

    final void prepareMocksForTests(int responseCode, String content, Exception exception) throws Exception {
        publishedCheckerUnderTestSpy = spy(new PublishedChecker(appConfigMock));

        doReturn(publisherConfigurationMock).when(publishedCheckerUnderTestSpy).resolveConfiguration(CHANGE_IDENTIFIER);
        doReturn(LOG_URI).when(publisherConfigurationMock).logUrl();
        doReturn(httpClientMock).when(publishedCheckerUnderTestSpy).resolveClient(publisherConfigurationMock);

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
