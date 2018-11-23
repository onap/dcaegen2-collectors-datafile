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
import java.util.concurrent.Future;

import javax.net.ssl.SSLContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.onap.dcaegen2.collectors.datafile.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.io.FileSystemResourceWrapper;
import org.onap.dcaegen2.collectors.datafile.io.IFileSystemResource;
import org.onap.dcaegen2.collectors.datafile.model.CommonFunctions;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.service.HttpUtils;
import org.onap.dcaegen2.collectors.datafile.web.PublishRedirectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
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
    private CloseableHttpAsyncClient webClient;

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
            logger.trace("Starting to publish to DR");

            webClient = getWebClient();
            webClient.start();

            HttpPut put = new HttpPut();
            prepareHead(consumerDmaapModel, put);
            prepareBody(consumerDmaapModel, put);
            addUserCredentialsToHead(put);

            Future<HttpResponse> future = webClient.execute(put, null);
            HttpResponse response = future.get();
            logger.trace(response.toString());
            webClient.close();
            handleHttpResponse(response);
            return Flux.just(response.toString());
        } catch (Exception e) {
            logger.error("Unable to send file to DataRouter. Data: {}", consumerDmaapModel, e);
            return Flux.empty();
        }
    }

    private void handleHttpResponse(HttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (HttpUtils.isSuccessfulResponseCode(statusCode)) {
            logger.trace("Publish to DR successful!");
        } else {
            logger.error("Publish to DR unsuccessful, response code: " + statusCode);
        }
    }

    private void addUserCredentialsToHead(HttpPut put) {
        String plainCreds = user + ":" + pwd;
        byte[] plainCredsBytes = plainCreds.getBytes(StandardCharsets.ISO_8859_1);
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        String base64Creds = new String(base64CredsBytes);
        logger.trace("base64Creds...: {}", base64Creds);
        put.addHeader("Authorization", "Basic " + base64Creds);
    }

    private void prepareHead(ConsumerDmaapModel model, HttpPut put) {
        put.addHeader(HttpHeaders.CONTENT_TYPE, dmaapContentType);
        JsonElement metaData = new JsonParser().parse(CommonFunctions.createJsonBody(model));
        String name = metaData.getAsJsonObject().remove(NAME_JSON_TAG).getAsString();
        metaData.getAsJsonObject().remove(LOCATION_JSON_TAG);
        put.addHeader(X_ATT_DR_META, metaData.toString());
        put.setURI(getUri(name));
    }

    private void prepareBody(ConsumerDmaapModel model, HttpPut put) {
        String fileLocation = model.getLocation();
        IFileSystemResource fileSystemResource = getFileSystemResource();
        fileSystemResource.setPath(fileLocation);
        InputStream fileInputStream = null;
        try {
            fileInputStream = fileSystemResource.getInputStream();
        } catch (IOException e) {
            logger.error("Unable to get stream from filesystem.", e);
        }
        try {
            put.setEntity(new ByteArrayEntity(IOUtils.toByteArray(fileInputStream)));
        } catch (IOException e) {
            logger.error("Unable to set put request body from ByteArray.", e);
        }
    }

    private URI getUri(String fileName) {
        String path = dmaapTopicName + URI_SEPARATOR + DEFAULT_FEED_ID + URI_SEPARATOR + fileName;
        return new DefaultUriBuilderFactory().builder().scheme(dmaapProtocol).host(dmaapHostName).port(dmaapPortNumber)
                .path(path).build();
    }

    private IFileSystemResource getFileSystemResource() {
        if (fileResource == null) {
            fileResource = new FileSystemResourceWrapper();
        }
        return fileResource;
    }

    protected void setFileSystemResource(IFileSystemResource fileSystemResource) {
        fileResource = fileSystemResource;
    }

    protected CloseableHttpAsyncClient getWebClient() {
        if (webClient != null) {
            return webClient;
        }
        SSLContext sslContext = null;
        try {
            sslContext = new SSLContextBuilder().loadTrustMaterial(null, (certificate, authType) -> true).build();
        } catch (Exception e) {
            logger.trace("Unable to get sslContext.", e);
        }
        //@formatter:off
        return HttpAsyncClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .setRedirectStrategy(PublishRedirectStrategy.INSTANCE)
                .build();
        //@formatter:on
    }

    protected void setWebClient(CloseableHttpAsyncClient client) {
        this.webClient = client;
    }
}
