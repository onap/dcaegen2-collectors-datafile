/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018-2020 NOKIA Intellectual Property, 2018-2019 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.configuration.PublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.model.Counters;
import org.onap.dcaegen2.collectors.datafile.model.FilePublishInformation;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFilePublishInformation;
import org.onap.dcaegen2.collectors.datafile.service.producer.DmaapProducerHttpClient;
import org.onap.dcaegen2.collectors.datafile.utils.LoggingUtils;
import org.springframework.http.HttpStatus;
import reactor.test.StepVerifier;

/**
 * Tests the DataRouter publisher.
 *
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 5/17/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
class DataRouterPublisherTest {

    private static final String PRODUCT_NAME = "NrRadio";
    private static final String VENDOR_NAME = "Ericsson";
    private static final String LAST_EPOCH_MICROSEC = "8745745764578";
    private static final String SOURCE_NAME = "oteNB5309";
    private static final String START_EPOCH_MICROSEC = "8745745764578";
    private static final String TIME_ZONE_OFFSET = "UTC+05:00";
    private static final String PM_FILE_NAME = "A20161224.1030-1045.bin.gz";
    private static final String FTPES_ADDRESS = "ftpes://192.168.0.101:22/ftp/rop/" + PM_FILE_NAME;
    private static final String CHANGE_IDENTIFIER = "PM_MEAS_FILES";

    private static final String COMPRESSION = "gzip";
    private static final String FILE_FORMAT_TYPE = "org.3GPP.32.435#measCollec";
    private static final String FILE_FORMAT_VERSION = "V10";
    private static final String X_DMAAP_DR_META = "X-DMAAP-DR-META";

    private static final String HOST = "54.45.33.2";
    private static final String HTTPS_SCHEME = "https";
    private static final int PORT = 1234;
    private static final String APPLICATION_OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";
    private static final String PUBLISH_TOPIC = "publish";
    private static final String FEED_ID = "1";

    // "https://54.45.333.2:1234/publish/1";
    private static final String PUBLISH_URL =
        HTTPS_SCHEME + "://" + HOST + ":" + PORT + "/" + PUBLISH_TOPIC + "/" + FEED_ID;

    private static FilePublishInformation filePublishInformation;
    private static DmaapProducerHttpClient httpClientMock;
    private static AppConfig appConfig;
    private static PublisherConfiguration publisherConfigurationMock = mock(PublisherConfiguration.class);
    private static Map<String, String> context = new HashMap<>();
    private static DataRouterPublisher publisherTaskUnderTestSpy;
    private Counters counters;

    @BeforeAll
    public static void setUp() {
        when(publisherConfigurationMock.publishUrl()).thenReturn(PUBLISH_URL);

        filePublishInformation = ImmutableFilePublishInformation.builder() //
            .productName(PRODUCT_NAME) //
            .vendorName(VENDOR_NAME) //
            .lastEpochMicrosec(LAST_EPOCH_MICROSEC) //
            .sourceName(SOURCE_NAME) //
            .startEpochMicrosec(START_EPOCH_MICROSEC) //
            .timeZoneOffset(TIME_ZONE_OFFSET) //
            .name(PM_FILE_NAME) //
            .location(FTPES_ADDRESS) //
            .internalLocation(Paths.get("target/" + PM_FILE_NAME)) //
            .compression("gzip") //
            .fileFormatType(FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .context(context) //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .build(); //
        appConfig = mock(AppConfig.class);
    }

    @BeforeEach
    void setUpTest() {
        counters = new Counters();
        publisherTaskUnderTestSpy = spy(new DataRouterPublisher(appConfig, counters));
    }

    @Test
    public void whenPassedObjectFits_ReturnsCorrectStatus() throws Exception {
        prepareMocksForTests(null, Integer.valueOf(HttpStatus.OK.value()));
        StepVerifier //
            .create(publisherTaskUnderTestSpy.publishFile(filePublishInformation, 1, Duration.ofSeconds(0)))
            .expectNext(filePublishInformation) //
            .verifyComplete();

        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClientMock).addUserCredentialsToHead(any(HttpUriRequest.class));
        verify(httpClientMock).getDmaapProducerResponseWithRedirect(requestCaptor.capture(), any());
        verifyNoMoreInteractions(httpClientMock);

        HttpPut actualPut = (HttpPut) requestCaptor.getValue();
        URI actualUri = actualPut.getURI();
        assertEquals(HTTPS_SCHEME, actualUri.getScheme());
        assertEquals(HOST, actualUri.getHost());
        assertEquals(PORT, actualUri.getPort());

        Path actualPath = Paths.get(actualUri.getPath());
        assertTrue(PUBLISH_TOPIC.equals(actualPath.getName(0).toString()));
        assertTrue(FEED_ID.equals(actualPath.getName(1).toString()));
        assertTrue(PM_FILE_NAME.equals(actualPath.getName(2).toString()));

        Header[] contentHeaders = actualPut.getHeaders("content-type");
        assertEquals(APPLICATION_OCTET_STREAM_CONTENT_TYPE, contentHeaders[0].getValue());

        Header[] metaHeaders = actualPut.getHeaders(X_DMAAP_DR_META);
        Map<String, String> metaHash = getMetaDataAsMap(metaHeaders);

        assertEquals(PRODUCT_NAME, metaHash.get("productName"));
        assertEquals(VENDOR_NAME, metaHash.get("vendorName"));
        assertEquals(LAST_EPOCH_MICROSEC, metaHash.get("lastEpochMicrosec"));
        assertEquals(SOURCE_NAME, metaHash.get("sourceName"));
        assertEquals(START_EPOCH_MICROSEC, metaHash.get("startEpochMicrosec"));
        assertEquals(TIME_ZONE_OFFSET, metaHash.get("timeZoneOffset"));
        assertEquals(COMPRESSION, metaHash.get("compression"));
        assertEquals(FTPES_ADDRESS, metaHash.get("location"));
        assertEquals(FILE_FORMAT_TYPE, metaHash.get("fileFormatType"));
        assertEquals(FILE_FORMAT_VERSION, metaHash.get("fileFormatVersion"));

        // Note that the following line checks the number of properties that are sent to the data
        // router.
        // This should be 10 unless the API is updated (which is the fields checked above)
        assertEquals(10, metaHash.size());

        assertEquals(1, counters.getTotalPublishedFiles(),"totalPublishedFiles should have been 1");
        assertEquals(0, counters.getNoOfFailedPublishAttempts(),"noOfFailedPublishAttempts should have been 0");
    }

    @Test
    void whenPassedObjectFits_firstFailsWithExceptionThenSucceeds() throws Exception {
        prepareMocksForTests(new DatafileTaskException("Error"), HttpStatus.OK.value());

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(DataRouterPublisher.class);
        StepVerifier.create(publisherTaskUnderTestSpy.publishFile(filePublishInformation, 2, Duration.ofSeconds(0)))
            .expectNext(filePublishInformation) //
            .verifyComplete();

        assertTrue(logAppender.list.toString().contains("[WARN] Publishing file " + PM_FILE_NAME + " to DR unsuccessful.")
            ,"Warning missing in log");

        assertEquals(1, counters.getTotalPublishedFiles(),"totalPublishedFiles should have been 1");
        assertEquals(1, counters.getNoOfFailedPublishAttempts(),"noOfFailedPublishAttempts should have been 1");
    }

    @Test
    public void whenPassedObjectFits_firstFailsThenSucceeds() throws Exception {
        prepareMocksForTests(null, Integer.valueOf(HttpStatus.BAD_GATEWAY.value()),
            Integer.valueOf(HttpStatus.OK.value()));

        StepVerifier //
            .create(publisherTaskUnderTestSpy.publishFile(filePublishInformation, 1, Duration.ofSeconds(0)))
            .expectNext(filePublishInformation) //
            .verifyComplete();

        verify(httpClientMock, times(2)).addUserCredentialsToHead(any(HttpUriRequest.class));
        verify(httpClientMock, times(2)).getDmaapProducerResponseWithRedirect(any(HttpUriRequest.class), any());
        verifyNoMoreInteractions(httpClientMock);

        assertEquals(1, counters.getTotalPublishedFiles(),"totalPublishedFiles should have been 1");
        assertEquals(1, counters.getNoOfFailedPublishAttempts(),"noOfFailedPublishAttempts should have been 1");
    }

    @Test
    public void whenPassedObjectFits_firstFailsThenFails() throws Exception {
        prepareMocksForTests(null, Integer.valueOf(HttpStatus.BAD_GATEWAY.value()),
            Integer.valueOf((HttpStatus.BAD_GATEWAY.value())));

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(DataRouterPublisher.class);
        StepVerifier.create(publisherTaskUnderTestSpy.publishFile(filePublishInformation, 1, Duration.ofSeconds(0)))
            .expectErrorMessage("Retries exhausted: 1/1") //
            .verify();

        assertTrue(logAppender.list.toString().contains("[WARN] Publishing file "
            + PM_FILE_NAME + " to DR unsuccessful. Response code: " + HttpStatus.BAD_GATEWAY),"Warning missing in log");

        verify(httpClientMock, times(2)).addUserCredentialsToHead(any(HttpUriRequest.class));
        verify(httpClientMock, times(2)).getDmaapProducerResponseWithRedirect(any(HttpUriRequest.class), any());
        verifyNoMoreInteractions(httpClientMock);

        assertEquals(0, counters.getTotalPublishedFiles(),"totalPublishedFiles should have been 0");
        assertEquals(2, counters.getNoOfFailedPublishAttempts(),"noOfFailedPublishAttempts should have been 2");
    }

    @SafeVarargs
    final void prepareMocksForTests(Exception exception, Integer firstResponse, Integer... nextHttpResponses)
        throws Exception {
        httpClientMock = mock(DmaapProducerHttpClient.class);
        when(appConfig.getPublisherConfiguration(CHANGE_IDENTIFIER)).thenReturn(publisherConfigurationMock);
        doReturn(publisherConfigurationMock).when(publisherTaskUnderTestSpy).resolveConfiguration(CHANGE_IDENTIFIER);
        doReturn(httpClientMock).when(publisherTaskUnderTestSpy).resolveClient(CHANGE_IDENTIFIER);

        HttpResponse httpResponseMock = mock(HttpResponse.class);
        if (exception == null) {
            when(httpClientMock.getDmaapProducerResponseWithRedirect(any(HttpUriRequest.class), any()))
                .thenReturn(httpResponseMock);
        } else {
            when(httpClientMock.getDmaapProducerResponseWithRedirect(any(HttpUriRequest.class), any()))
                .thenThrow(exception).thenReturn(httpResponseMock);
        }
        StatusLine statusLineMock = mock(StatusLine.class);
        when(httpResponseMock.getStatusLine()).thenReturn(statusLineMock);
        when(statusLineMock.getStatusCode()).thenReturn(firstResponse, nextHttpResponses);

        File file = File.createTempFile("DFC", "tmp");
        doReturn(file).when(publisherTaskUnderTestSpy).createInputFile(Paths.get("target", PM_FILE_NAME));
    }

    private Map<String, String> getMetaDataAsMap(Header[] metaHeaders) {
        Map<String, String> metaHash = new HashMap<>();
        String actualMetaData = metaHeaders[0].getValue();
        actualMetaData = actualMetaData.substring(1, actualMetaData.length() - 1);
        actualMetaData = actualMetaData.replace("\"", "");
        String[] commaSplitedMetaData = actualMetaData.split(",");
        for (int i = 0; i < commaSplitedMetaData.length; i++) {
            String[] keyValuePair = commaSplitedMetaData[i].split(":");
            if (keyValuePair.length > 2) {
                List<String> arrayKeyValuePair = new ArrayList<>(keyValuePair.length);
                for (int j = 1; j < keyValuePair.length; j++) {
                    arrayKeyValuePair.add(keyValuePair[j]);
                }
                keyValuePair[1] = String.join(":", arrayKeyValuePair);
            }
            metaHash.put(keyValuePair[0], keyValuePair[1]);
        }
        return metaHash;
    }
}
