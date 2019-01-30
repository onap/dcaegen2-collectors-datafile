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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.onap.dcaegen2.collectors.datafile.io.IFileSystemResource;
import org.onap.dcaegen2.collectors.datafile.model.CommonFunctions;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.service.HttpUtils;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapPublisherConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.DefaultUriBuilderFactory;

import reactor.test.StepVerifier;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 7/4/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
class DmaapProducerReactiveHttpClientTest {

    private static final String FILE_NAME = "A20161224.1030-1045.bin.gz";
    private static final String INTERNAL_LOCATION_JSON_TAG = "internalLocation";
    private static final String NAME_JSON_TAG = "name";
    private static final String X_ATT_DR_META = "X-DMAAP-DR-META";

    private static final String HOST = "54.45.33.2";
    private static final String HTTPS_SCHEME = "https";
    private static final int PORT = 1234;
    private static final String APPLICATION_OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";
    private static final String URI_SEPARATOR = "/";
    private static final String PUBLISH_TOPIC = "publish";
    private static final String DEFAULT_FEED_ID = "1";
    private static final String FILE_CONTENT = "Just a string.";

    private DmaapProducerReactiveHttpClient dmaapProducerReactiveHttpClient;

    private DmaapPublisherConfiguration dmaapPublisherConfigurationMock = mock(DmaapPublisherConfiguration.class);
    private ConsumerDmaapModel consumerDmaapModel;

    private IFileSystemResource fileSystemResourceMock = mock(IFileSystemResource.class);
    private CloseableHttpAsyncClient clientMock;
    private HttpResponse responseMock = mock(HttpResponse.class);
    private Future<HttpResponse> futureMock = mock(Future.class);
    private StatusLine statusLine = mock(StatusLine.class);
    private InputStream fileStream;

    @BeforeEach
    void setUp() {
        when(dmaapPublisherConfigurationMock.dmaapHostName()).thenReturn(HOST);
        when(dmaapPublisherConfigurationMock.dmaapProtocol()).thenReturn(HTTPS_SCHEME);
        when(dmaapPublisherConfigurationMock.dmaapPortNumber()).thenReturn(PORT);
        when(dmaapPublisherConfigurationMock.dmaapUserName()).thenReturn("dradmin");
        when(dmaapPublisherConfigurationMock.dmaapUserPassword()).thenReturn("dradmin");
        when(dmaapPublisherConfigurationMock.dmaapContentType()).thenReturn(APPLICATION_OCTET_STREAM_CONTENT_TYPE);
        when(dmaapPublisherConfigurationMock.dmaapTopicName()).thenReturn(PUBLISH_TOPIC);

        // @formatter:off
        consumerDmaapModel = ImmutableConsumerDmaapModel.builder()
                .productName("NrRadio")
                .vendorName("Ericsson")
                .lastEpochMicrosec("8745745764578")
                .sourceName("oteNB5309")
                .startEpochMicrosec("8745745764578")
                .timeZoneOffset("UTC+05:00")
                .name("A20161224.1030-1045.bin.gz")
                .location("ftpes://192.168.0.101:22/ftp/rop/A20161224.1030-1145.bin.gz")
                .internalLocation("target/A20161224.1030-1045.bin.gz")
                .compression("gzip")
                .fileFormatType("org.3GPP.32.435#measCollec")
                .fileFormatVersion("V10")
                .build();
        //formatter:on

        dmaapProducerReactiveHttpClient = new DmaapProducerReactiveHttpClient(dmaapPublisherConfigurationMock);
        dmaapProducerReactiveHttpClient.setFileSystemResource(fileSystemResourceMock);
        clientMock = mock(CloseableHttpAsyncClient.class);
        dmaapProducerReactiveHttpClient.setWebClient(clientMock);
    }

    @Test
    void getHttpResponse_Success() throws Exception {
        mockWebClientDependantObject(true);

        URI expectedUri = new DefaultUriBuilderFactory().builder().scheme(HTTPS_SCHEME).host(HOST).port(PORT)
                .path(PUBLISH_TOPIC + URI_SEPARATOR + DEFAULT_FEED_ID + URI_SEPARATOR + FILE_NAME).build();

        HttpPut httpPut = new HttpPut();
        httpPut.addHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_OCTET_STREAM_CONTENT_TYPE);

        JsonElement metaData = new JsonParser().parse(new CommonFunctions().createJsonBody(consumerDmaapModel));
        metaData.getAsJsonObject().remove(NAME_JSON_TAG).getAsString();
        metaData.getAsJsonObject().remove(INTERNAL_LOCATION_JSON_TAG);
        httpPut.addHeader(X_ATT_DR_META, metaData.toString());
        httpPut.setURI(expectedUri);

        String plainCreds = "dradmin" + ":" + "dradmin";
        byte[] plainCredsBytes = plainCreds.getBytes(StandardCharsets.ISO_8859_1);
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        String base64Creds = new String(base64CredsBytes);
        httpPut.addHeader("Authorization", "Basic " + base64Creds);

        fileStream.reset();
        StepVerifier.create(dmaapProducerReactiveHttpClient.getDmaapProducerResponse(consumerDmaapModel))
        .expectNext(responseMock.toString()).verifyComplete();

        verify(fileSystemResourceMock).setPath("target/" + FILE_NAME);
        InputStream fileInputStream = fileSystemResourceMock.getInputStream();
        httpPut.setEntity(new ByteArrayEntity(IOUtils.toByteArray(fileInputStream)));
    }

    @Test
    void getHttpResponse_Fail() throws Exception {
        mockWebClientDependantObject(false);
        StepVerifier.create(dmaapProducerReactiveHttpClient.getDmaapProducerResponse(consumerDmaapModel))
                .verifyComplete();
    }

    private void mockWebClientDependantObject(boolean success)
            throws IOException, InterruptedException, ExecutionException {
        fileStream = new ByteArrayInputStream(FILE_CONTENT.getBytes());
        when(fileSystemResourceMock.getInputStream()).thenReturn(fileStream);
        if (success) {
            when(clientMock.execute(any(HttpPut.class), any())).thenReturn(futureMock);
            when(futureMock.get()).thenReturn(responseMock);
            when(responseMock.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(HttpUtils.SC_OK);
        } else {
            when(clientMock.execute(any(HttpPut.class), any())).thenReturn(futureMock);
            when(futureMock.get()).thenThrow(new InterruptedException());
        }
    }
}
