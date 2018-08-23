/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 Nordix Foundation. All rights reserved.
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
 * ============LICENSE_END=========================================================
 */
package org.onap.dcaegen2.collectors.datafile.service.producer;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.onap.dcaegen2.collectors.datafile.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.model.CommonFunctions;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.service.DmaapHttpClientImpl;
import org.onap.dcaegen2.collectors.datafile.service.producer.ExtendedDmaapProducerHttpClientImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class ExtendedDmaapProducerHttpClientImpl {

    private static Logger logger = LoggerFactory.getLogger(ExtendedDmaapProducerHttpClientImpl.class);

    private final CloseableHttpClient closeableHttpClient;
    private final String dmaapHostName;
    private final String dmaapProtocol;
    private final Integer dmaapPortNumber;
    private final String dmaapTopicName;
    private final String dmaapContentType;
    private ConsumerDmaapModel consumerDmaapModel;
    private static final String LOCATION="location";


    public ExtendedDmaapProducerHttpClientImpl(DmaapPublisherConfiguration configuration) {
        this.closeableHttpClient = new DmaapHttpClientImpl(configuration).getHttpClient();
        this.dmaapHostName = configuration.dmaapHostName();
        this.dmaapProtocol = configuration.dmaapProtocol();
        this.dmaapPortNumber = configuration.dmaapPortNumber();
        this.dmaapTopicName = configuration.dmaapTopicName();
        this.dmaapContentType = configuration.dmaapContentType();
    }

    public Optional<Integer> getHttpProducerResponse(ConsumerDmaapModel consumerDmaapModel) {
        this.consumerDmaapModel = consumerDmaapModel;
        try {
            return createRequest().flatMap(this::executeHttpClient);
        } catch (URISyntaxException e) {
            logger.warn("Exception while executing HTTP request: ", e);
        }
        return Optional.empty();
    }

    private Optional<Integer> executeHttpClient(HttpRequestBase httpRequestBase) {
        try {
            return closeableHttpClient.execute(httpRequestBase, CommonFunctions::handleResponse);
        } catch (IOException e) {
            logger.warn("Exception while executing HTTP request: ", e);
        }
        return Optional.empty();
    }

    private Optional<HttpRequestBase> createRequest() throws URISyntaxException {
        return "application/octet-stream".equals(dmaapContentType)
                ? createDmaapPublisherExtendedURI().map(this::createHttpPostRequest)
                : Optional.empty();
    }

    private Optional<URI> createDmaapPublisherExtendedURI() throws URISyntaxException {
        return Optional.ofNullable(new URIBuilder().setScheme(dmaapProtocol).setHost(dmaapHostName)
                .setPort(dmaapPortNumber).setPath(dmaapTopicName).build());
    }

    private HttpPost createHttpPostRequest(URI extendedURI) {
        HttpPost post = new HttpPost(extendedURI);
        post.addHeader("Content-type", dmaapContentType);
        JsonElement metaData = new JsonParser().parse(CommonFunctions.createJsonBody(consumerDmaapModel));
        metaData.getAsJsonObject().remove(LOCATION);
        post.addHeader("X-ATT-DR-META", metaData.toString());
        createStringEntity().ifPresent(post::setEntity);
        return post;
    }

    private Optional<HttpEntity> createStringEntity() {
    	String fileLocation=consumerDmaapModel.getLocation();
    	String fileName=FilenameUtils.getName(consumerDmaapModel.getLocation());
    	
        try {
            return Optional.of(MultipartEntityBuilder.create().addPart(fileName, new FileBody(new File(fileLocation))).build());
        } catch (IllegalArgumentException e) {
            logger.warn("Exception while parsing JSON: ", e);
        }
        return Optional.empty();
    }
}
