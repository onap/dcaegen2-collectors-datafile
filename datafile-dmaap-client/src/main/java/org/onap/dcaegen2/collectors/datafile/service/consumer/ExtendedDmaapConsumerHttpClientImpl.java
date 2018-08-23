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
package org.onap.dcaegen2.collectors.datafile.service.consumer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.onap.dcaegen2.collectors.datafile.config.DmaapConsumerConfiguration;
import org.onap.dcaegen2.collectors.datafile.service.DmaapHttpClientImpl;
import org.onap.dcaegen2.collectors.datafile.service.HttpUtils;
import org.onap.dcaegen2.collectors.datafile.service.consumer.ExtendedDmaapConsumerHttpClientImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExtendedDmaapConsumerHttpClientImpl {

    private static Logger logger = LoggerFactory.getLogger(ExtendedDmaapConsumerHttpClientImpl.class);

    private final CloseableHttpClient closeableHttpClient;
    private final String dmaapHostName;
    private final String dmaapProtocol;
    private final Integer dmaapPortNumber;
    private final String dmaapTopicName;
    private final String consumerGroup;
    private final String consumerId;
    private final String dmaapContentType;


    public ExtendedDmaapConsumerHttpClientImpl(DmaapConsumerConfiguration configuration) {
        this.closeableHttpClient = new DmaapHttpClientImpl(configuration).getHttpClient();
        this.dmaapHostName = configuration.dmaapHostName();
        this.dmaapProtocol = configuration.dmaapProtocol();
        this.dmaapPortNumber = configuration.dmaapPortNumber();
        this.dmaapTopicName = configuration.dmaapTopicName();
        this.consumerGroup = configuration.consumerGroup();
        this.consumerId = configuration.consumerId();
        this.dmaapContentType = configuration.dmaapContentType();
    }

    public Optional<String> getHttpConsumerResponse() {

        try {
            return createRequest().flatMap(this::executeHttpClient);
        } catch (URISyntaxException e) {
            logger.warn("Exception while executing HTTP request: ", e);
        }
        return Optional.empty();
    }

    private Optional<String> executeHttpClient(HttpRequestBase httpRequestBase) {
        try {
            return closeableHttpClient.execute(httpRequestBase, getDmaapConsumerResponseHandler());
        } catch (IOException e) {
            logger.warn("Exception while executing HTTP request: ", e);
        }
        return Optional.empty();
    }

    private Optional<HttpRequestBase> createRequest() throws URISyntaxException {
        return "application/json".equals(dmaapContentType)
                ? createDmaapConsumerExtendedURI().map(this::createHttpRequest)
                : Optional.empty();
    }

    private HttpRequestBase createHttpRequest(URI extendedURI) {
        HttpRequestBase httpRequestBase = new HttpGet(extendedURI);
        httpRequestBase.addHeader("Content-type", dmaapContentType);
        return httpRequestBase;
    }


    private String createRequestPath() {
        return dmaapTopicName + "/" + consumerGroup + "/" + consumerId;
    }

    private Optional<URI> createDmaapConsumerExtendedURI() throws URISyntaxException {
        return Optional.ofNullable(new URIBuilder().setScheme(dmaapProtocol).setHost(dmaapHostName)
                .setPort(dmaapPortNumber).setPath(createRequestPath()).build());
    }

    private ResponseHandler<Optional<String>> getDmaapConsumerResponseHandler() {
        return httpResponse -> {
            final int responseCode = httpResponse.getStatusLine().getStatusCode();
            logger.info("Status code of operation: {}", responseCode);
            final HttpEntity responseEntity = httpResponse.getEntity();

            if (HttpUtils.isSuccessfulResponseCode(responseCode)) {
                logger.trace("HTTP response successful.");
                final String dmaapResponse = EntityUtils.toString(responseEntity);
                return Optional.of(dmaapResponse);
            } else {
                String dmaapResponse = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
                logger.trace("HTTP response not successful : {}", dmaapResponse);
                return Optional.of(String.valueOf(responseCode));
            }
        };
    }
}


