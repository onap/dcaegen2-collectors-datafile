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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.configuration.DmaapBusControllerConfiguration;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.model.FeedData;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFeedData;
import org.onap.dcaegen2.collectors.datafile.service.HttpUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

import reactor.test.StepVerifier;


public class FeedCreatorTest {
    private static final String FEED_NAME = "bulk_pm_feed";
    private static final String HTTPS_SCHEME = "https";
    private static final String HOST = "54.45.33.2";
    private static final int PORT = 1234;
    private static final String WEBAPI_TOPIC = "webapis";
    private static final String FEEDS_PATH = "feeds";

    private static final String LOG_URL = "https://dmaap-dr-prov/feedlog/1";
    private static final String PUBLISH_URL = "https://dmaap-dr-prov/publish/1";
    private static final String USERNAME = "tmp_i63w8psh6ycnoqu";
    private static final String PASSWORD = "6jkc1uwywrc8q4w";

    private static final String CORRECT_RESPONSE = "{" //
            + "\"type\":\"feed\"," //
            + "\"lastMod\":\"2019-01-24T16:00:40.489\"," //
            + "\"status\":\"VALID\"," //
            + "\"asprClassification\":\"unclassified\"," //
            + "\"feedDescription\":\"Bulk PM file feed\"," //
            + "\"feedId\":\"1\"," //
            + "\"feedName\":\"" + FEED_NAME + "\"," //
            + "\"feedVersion\":\"csit\"," //
            + "\"logURL\":\"" + LOG_URL + "\"," //
            + "\"owner\":\"Datafile Collector\"," //
            + "\"publishURL\":\"" + PUBLISH_URL + "\"," //
            + "\"pubs\":[" //
            + "{" //
            + "\"lastMod\":\"2019-01-24T16:00:40.484\"," //
            + "\"status\":\"VALID\"," //
            + "\"dcaeLocationName\":\"san-francisco\"," //
            + "\"feedId\":\"3\"," //
            + "\"pubId\":\"3.4gh53\"," //
            + "\"username\":\"" + USERNAME + "\"," //
            + "\"userpwd\":\"" + PASSWORD + "\"" //
            + "}" //
            + "]," //
            + "\"subs\":[]," //
            + "\"subscribeURL\":\"https://dmaap-dr-prov/subscribe/1\"," //
            + "\"suspended\":false" //
            + "}";

    private static final FeedData FEED_DATA = ImmutableFeedData.builder() //
            .publishedCheckUrl(LOG_URL) //
            .publishUrl(PUBLISH_URL) //
            .username(USERNAME) //
            .password(PASSWORD) //
            .build();

    private static final Map<String, String> CONTEXT_MAP = new HashMap<>();

    private static DmaapBusControllerConfiguration busControllerConfigurationMock =
            mock(DmaapBusControllerConfiguration.class);
    private static AppConfig appConfigMock;
    private DmaapBusControllerHttpClient httpClientMock = mock(DmaapBusControllerHttpClient.class);

    private FeedCreator feedCreatorUnderTestSpy;

    /**
     * Sets up data for the tests.
     */
    @BeforeAll
    public static void setUp() {
        when(busControllerConfigurationMock.dmaapHostName()).thenReturn(HOST);
        when(busControllerConfigurationMock.dmaapProtocol()).thenReturn(HTTPS_SCHEME);
        when(busControllerConfigurationMock.dmaapPortNumber()).thenReturn(PORT);
        when(busControllerConfigurationMock.dmaapDrFeedName()).thenReturn(FEED_NAME);

        appConfigMock = mock(AppConfig.class);
        when(appConfigMock.getDmaapBusControllerConfiguration()).thenReturn(busControllerConfigurationMock);
    }

    @Test
    public void executeWhenFeedCreated_returnsFeedUrls() throws Exception {
        prepareMocksForTests(CORRECT_RESPONSE, null, HttpUtils.SC_OK);

        StepVerifier.create(feedCreatorUnderTestSpy.createFeed(1, Duration.ofSeconds(0), CONTEXT_MAP)) //
                .expectNext(FEED_DATA) //
                .verifyComplete();

        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClientMock).getBaseUri();
        verify(httpClientMock).addUserCredentialsToHead(any(HttpUriRequest.class));
        verify(httpClientMock).getDmaapBusControllerResponse(requestCaptor.capture(), anyInt());
        verifyNoMoreInteractions(httpClientMock);

        HttpUriRequest getRequest = requestCaptor.getValue();
        assertTrue(getRequest instanceof HttpPost);
        URI actualUri = getRequest.getURI();
        assertEquals(HTTPS_SCHEME, actualUri.getScheme());
        assertEquals(HOST, actualUri.getHost());
        assertEquals(PORT, actualUri.getPort());
        Path actualPath = Paths.get(actualUri.getPath());
        assertEquals(WEBAPI_TOPIC, actualPath.getName(0).toString());
        assertEquals(FEEDS_PATH, actualPath.getName(1).toString());
        String actualQuery = actualUri.getQuery();
        assertTrue(actualQuery.contains("useExisting=true"));
    }


    @Test
    void whenPassedObjectFits_firstFailsWithExceptionThenSucceeds() throws Exception {
        prepareMocksForTests(CORRECT_RESPONSE, new DatafileTaskException("Error"), HttpStatus.OK.value());

        StepVerifier.create(feedCreatorUnderTestSpy.createFeed(2, Duration.ofSeconds(0), CONTEXT_MAP)) //
                .expectNext(FEED_DATA) //
                .verifyComplete();
    }

    @Test
    public void whenPassedObjectFits_firstFailsThenSucceeds() throws Exception {
        prepareMocksForTests(CORRECT_RESPONSE, null, Integer.valueOf(HttpStatus.BAD_GATEWAY.value()),
                Integer.valueOf(HttpStatus.OK.value()));

        StepVerifier.create(feedCreatorUnderTestSpy.createFeed(1, Duration.ofSeconds(0), CONTEXT_MAP)) //
                .expectNext(FEED_DATA) //
                .verifyComplete();

        verify(httpClientMock, times(2)).getBaseUri();
        verify(httpClientMock, times(2)).addUserCredentialsToHead(any(HttpUriRequest.class));
        verify(httpClientMock, times(2)).getDmaapBusControllerResponse(any(HttpUriRequest.class), anyInt());
        verifyNoMoreInteractions(httpClientMock);
    }

    @Test
    public void whenPassedObjectFits_firstFailsThenFails() throws Exception {
        prepareMocksForTests("Malformed Json", null, Integer.valueOf(HttpStatus.BAD_GATEWAY.value()),
                Integer.valueOf((HttpStatus.OK.value())));

        StepVerifier.create(feedCreatorUnderTestSpy.createFeed(1, Duration.ofSeconds(0), CONTEXT_MAP)) //
                .expectErrorMessage("Retries exhausted: 1/1") //
                .verify();

        verify(httpClientMock, times(2)).getBaseUri();
        verify(httpClientMock, times(2)).addUserCredentialsToHead(any(HttpUriRequest.class));
        verify(httpClientMock, times(2)).getDmaapBusControllerResponse(any(HttpUriRequest.class), anyInt());
        verifyNoMoreInteractions(httpClientMock);
    }

    final void prepareMocksForTests(String content, Exception exception, Integer firstResponse,
            Integer... nextResponses) throws Exception {
        feedCreatorUnderTestSpy = spy(new FeedCreator(appConfigMock));

        doReturn(busControllerConfigurationMock).when(feedCreatorUnderTestSpy).resolveConfiguration();
        doReturn(httpClientMock).when(feedCreatorUnderTestSpy).resolveClient();

        UriBuilder uriBuilder = new DefaultUriBuilderFactory().builder().scheme(HTTPS_SCHEME).host(HOST).port(PORT);
        when(httpClientMock.getBaseUri()).thenReturn(uriBuilder);

        HttpResponse httpResponseMock = mock(HttpResponse.class);
        if (exception == null) {
            when(httpClientMock.getDmaapBusControllerResponse(any(HttpUriRequest.class), anyInt()))
                    .thenReturn(httpResponseMock);
        } else {
            when(httpClientMock.getDmaapBusControllerResponse(any(HttpUriRequest.class), anyInt())).thenThrow(exception)
                    .thenReturn(httpResponseMock);
        }
        HttpEntity httpEntityMock = mock(HttpEntity.class);
        StatusLine statusLineMock = mock(StatusLine.class);
        when(httpResponseMock.getStatusLine()).thenReturn(statusLineMock);
        when(statusLineMock.getStatusCode()).thenReturn(firstResponse, nextResponses);
        when(httpResponseMock.getEntity()).thenReturn(httpEntityMock);
        InputStream stream = new ByteArrayInputStream(content.getBytes());
        when(httpEntityMock.getContent()).thenReturn(stream);
    }
}
