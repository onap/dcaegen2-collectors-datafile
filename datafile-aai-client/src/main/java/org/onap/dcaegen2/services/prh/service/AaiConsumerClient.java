/*
 * ============LICENSE_START=======================================================
 * PNF-REGISTRATION-HANDLER
 * ================================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property. All rights reserved.
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

package org.onap.dcaegen2.services.prh.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.onap.dcaegen2.services.prh.config.AaiClientConfiguration;
import org.onap.dcaegen2.services.prh.model.ConsumerDmaapModel;
import org.onap.dcaegen2.services.prh.model.utils.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AaiConsumerClient {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final CloseableHttpClient closeableHttpClient;
    private final String aaiHost;
    private final String aaiProtocol;
    private final Integer aaiHostPortNumber;
    private final String aaiPath;
    private final Map<String, String> aaiHeaders;

    /**
     * A{@literal &}AI client for consuming data from A{@literal &}AI.
     *
     * @param aaiClientConfiguration - A{@literal &}AI client config
     */
    public AaiConsumerClient(AaiClientConfiguration aaiClientConfiguration) {
        closeableHttpClient = new AaiClientImpl(aaiClientConfiguration).getAaiHttpClient();
        aaiHost = aaiClientConfiguration.aaiHost();
        aaiProtocol = aaiClientConfiguration.aaiProtocol();
        aaiHostPortNumber = aaiClientConfiguration.aaiPort();
        aaiPath = aaiClientConfiguration.aaiBasePath() + aaiClientConfiguration.aaiPnfPath();
        aaiHeaders = aaiClientConfiguration.aaiHeaders();
    }

    /**
     * Function which call http client for getting object from A{@literal &}AI.
     *
     * @param consumerDmaapModel - helper object for uri generation
     * @return - status code of operation
     * @throws IOException - Apache HTTP client exception
     */
    public Optional<String> getHttpResponse(ConsumerDmaapModel consumerDmaapModel) throws IOException {
        Optional<HttpRequestBase> request = createRequest(consumerDmaapModel);
        try {
            return closeableHttpClient.execute(request.get(), aaiResponseHandler());
        } catch (IOException e) {
            logger.warn("Exception while executing http client: ", e);
            throw new IOException();
        }
    }

    private URI createAaiExtendedUri(String pnfName) {

        URI extendedUri = null;

        final URIBuilder uriBuilder = new URIBuilder()
            .setScheme(aaiProtocol)
            .setHost(aaiHost)
            .setPort(aaiHostPortNumber)
            .setPath(aaiPath + "/" + pnfName);

        try {
            extendedUri = uriBuilder.build();
            logger.trace("Building extended URI: {}", extendedUri);
        } catch (URISyntaxException e) {
            logger.warn("Exception while building extended URI: {}", e);
        }

        return extendedUri;
    }

    private ResponseHandler<Optional<String>> aaiResponseHandler() {
        return httpResponse -> {
            final int responseCode = httpResponse.getStatusLine().getStatusCode();
            logger.info("Status code of operation: {}", responseCode);
            final HttpEntity responseEntity = httpResponse.getEntity();

            if (HttpUtils.isSuccessfulResponseCode(responseCode)) {
                logger.trace("HTTP response successful.");
                final String aaiResponse = EntityUtils.toString(responseEntity);
                return Optional.of(aaiResponse);
            } else {
                String aaiResponse = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
                logger.warn("HTTP response not successful : {}", aaiResponse);
                return Optional.of(String.valueOf(responseCode));
            }
        };
    }

    private HttpRequestBase createHttpRequest(URI extendedUri) {
        return isExtendedUriNotNull(extendedUri) ? new HttpGet(extendedUri) : null;
    }

    private Boolean isExtendedUriNotNull(URI extendedUri) {
        return extendedUri != null;
    }

    private Optional<HttpRequestBase> createRequest(ConsumerDmaapModel consumerDmaapModel) {
        final URI extendedUri = createAaiExtendedUri(consumerDmaapModel.getPnfName());
        HttpRequestBase request = createHttpRequest(extendedUri);
        aaiHeaders.forEach(Objects.requireNonNull(request)::addHeader);
        Objects.requireNonNull(request).addHeader("Content-Type", "application/json");
        return Optional.of(request);
    }
}
