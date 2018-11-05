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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.io.IFileSystemResource;
import org.onap.dcaegen2.collectors.datafile.model.CommonFunctions;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModelForUnitTest;
import org.onap.dcaegen2.collectors.datafile.web.IRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.DefaultUriBuilderFactory;

import reactor.test.StepVerifier;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 7/4/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
class DmaapProducerReactiveHttpClientTest {

    private static final String FILE_NAME = "A20161224.1030-1045.bin.gz";
    private static final String LOCATION_JSON_TAG = "location";
    private static final String NAME_JSON_TAG = "name";
    private static final String X_ATT_DR_META = "X-ATT-DR-META";

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
    private ConsumerDmaapModel consumerDmaapModel = new ConsumerDmaapModelForUnitTest();

    private IFileSystemResource fileSystemResourceMock = mock(IFileSystemResource.class);
    private IRestTemplate restTemplateMock = mock(IRestTemplate.class);
    private ResponseEntity<String> responseEntityMock = mock(ResponseEntity.class);
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

        dmaapProducerReactiveHttpClient = new DmaapProducerReactiveHttpClient(dmaapPublisherConfigurationMock);
        dmaapProducerReactiveHttpClient.setFileSystemResource(fileSystemResourceMock);
        dmaapProducerReactiveHttpClient.setRestTemplate(restTemplateMock);
    }

    @Test
    void getHttpResponse_Success() throws Exception {
        mockWebClientDependantObject();

        StepVerifier.create(dmaapProducerReactiveHttpClient.getDmaapProducerResponse(consumerDmaapModel))
                .expectNext(HttpStatus.OK.toString()).verifyComplete();

        URI expectedUri = new DefaultUriBuilderFactory().builder().scheme(HTTPS_SCHEME).host(HOST).port(PORT)
                .path(PUBLISH_TOPIC + URI_SEPARATOR + DEFAULT_FEED_ID + URI_SEPARATOR + FILE_NAME).build();

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.parseMediaType(APPLICATION_OCTET_STREAM_CONTENT_TYPE));

        JsonElement metaData = new JsonParser().parse(CommonFunctions.createJsonBody(consumerDmaapModel));
        metaData.getAsJsonObject().remove(NAME_JSON_TAG).getAsString();
        headers.set(X_ATT_DR_META, metaData.toString());

        String plainCreds = "dradmin" + ":" + "dradmin";
        byte[] plainCredsBytes = plainCreds.getBytes(StandardCharsets.ISO_8859_1);
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        String base64Creds = new String(base64CredsBytes);
        headers.add("Authorization", "Basic " + base64Creds);

        fileStream.reset();

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(IOUtils.toByteArray(fileStream), headers);
        verify(fileSystemResourceMock).setPath("target/" + FILE_NAME);
        verify(restTemplateMock).exchange(expectedUri, HttpMethod.PUT, requestEntity, String.class);
        verifyNoMoreInteractions(restTemplateMock);
    }

    private void mockWebClientDependantObject() throws IOException {
        fileStream = new ByteArrayInputStream(FILE_CONTENT.getBytes());
        when(fileSystemResourceMock.getInputStream()).thenReturn(fileStream);

        when(restTemplateMock.exchange(any(), any(), any(), any())).thenReturn(responseEntityMock);
        when(responseEntityMock.getStatusCode()).thenReturn(HttpStatus.OK);
    }
}
