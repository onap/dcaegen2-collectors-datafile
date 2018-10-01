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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.onap.dcaegen2.collectors.datafile.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.model.CommonFunctions;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.DefaultUriBuilderFactory;

import reactor.core.publisher.Flux;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 7/4/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class DmaapProducerReactiveHttpClient {

    private static final String X_ATT_DR_META = "X-ATT-DR-META";
    private static final String NAME_JSON_TAG = "name";
    private static final String LOCATION_JSON_TAG = "location";
    private static final String URI_SEPARATOR = "/";
    private static final String DEFAULT_FEED_ID = "1";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String dmaapHostName;
    private final Integer dmaapPortNumber;
    private final String dmaapTopicName;
    private final String dmaapProtocol;
    private final String dmaapContentType;
    private final String user;
    private final String pwd;

    private IFileSystemResource fileResource;
    private IRestTemplate restTemplate;

    /**
     * Constructor DmaapProducerReactiveHttpClient.
     *
     * @param dmaapPublisherConfiguration - DMaaP producer configuration object
     */
    public DmaapProducerReactiveHttpClient(DmaapPublisherConfiguration dmaapPublisherConfiguration) {

        this.dmaapHostName = dmaapPublisherConfiguration.dmaapHostName();
        this.dmaapPortNumber = dmaapPublisherConfiguration.dmaapPortNumber();
        this.dmaapTopicName = dmaapPublisherConfiguration.dmaapTopicName();
        this.dmaapProtocol = dmaapPublisherConfiguration.dmaapProtocol();
        this.dmaapContentType = dmaapPublisherConfiguration.dmaapContentType();
        this.user = dmaapPublisherConfiguration.dmaapUserName();
        this.pwd = dmaapPublisherConfiguration.dmaapUserPassword();
    }

    /**
     * Function for calling DMaaP HTTP producer - post request to DMaaP DataRouter.
     *
     * @param consumerDmaapModel - object which will be sent to DMaaP DataRouter
     * @return status code of operation
     */
    public Flux<String> getDmaapProducerResponse(ConsumerDmaapModel consumerDmaapModel) {
        logger.trace("Entering getDmaapProducerResponse with {}", consumerDmaapModel);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(dmaapContentType));
            addMetaDataToHead(consumerDmaapModel, headers);

            addUserCredentialsToHead(headers);

            HttpEntity<byte[]> request = addFileToRequest(consumerDmaapModel, headers);


            logger.trace("Starting to publish to DR");
            ResponseEntity<String> responseEntity = getRestTemplate().exchange(getUri(consumerDmaapModel.getName()),
                    HttpMethod.PUT, request, String.class);

            return Flux.just(responseEntity.getStatusCode().toString());
        } catch (Exception e) {
            logger.error("Unable to send file to DataRouter. Data: {}", consumerDmaapModel, e);
            return Flux.empty();
        }
    }

    private void addUserCredentialsToHead(HttpHeaders headers) {
        String plainCreds = user + ":" + pwd;
        byte[] plainCredsBytes = plainCreds.getBytes(StandardCharsets.ISO_8859_1);
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        String base64Creds = new String(base64CredsBytes);
        logger.trace("base64Creds...: {}", base64Creds);
        headers.add("Authorization", "Basic " + base64Creds);
    }

    private void addMetaDataToHead(ConsumerDmaapModel consumerDmaapModel, HttpHeaders headers) {
        JsonElement metaData = new JsonParser().parse(CommonFunctions.createJsonBody(consumerDmaapModel));
        metaData.getAsJsonObject().remove(NAME_JSON_TAG).getAsString();
        metaData.getAsJsonObject().remove(LOCATION_JSON_TAG);
        headers.set(X_ATT_DR_META, metaData.toString());
    }

    private HttpEntity<byte[]> addFileToRequest(ConsumerDmaapModel consumerDmaapModel, HttpHeaders headers)
            throws IOException {
        InputStream in = getInputStream(consumerDmaapModel.getLocation());
        return new HttpEntity<>(IOUtils.toByteArray(in), headers);
    }

    private InputStream getInputStream(String filePath) throws IOException {
        if (fileResource == null) {
            fileResource = new FileSystemResourceWrapper(filePath);
        }
        return fileResource.getInputStream();
    }

    private IRestTemplate getRestTemplate() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        if (restTemplate == null) {
            restTemplate = new RestTemplateWrapper();
        }
        return restTemplate;
    }

    private URI getUri(String fileName) {
        String path = dmaapTopicName + URI_SEPARATOR + DEFAULT_FEED_ID + URI_SEPARATOR + fileName;
        return new DefaultUriBuilderFactory().builder().scheme(dmaapProtocol).host(dmaapHostName).port(dmaapPortNumber)
                .path(path).build();
    }

    protected void setFileSystemResource(IFileSystemResource fileSystemResource) {
        fileResource = fileSystemResource;
    }

    protected void setRestTemplate(IRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
