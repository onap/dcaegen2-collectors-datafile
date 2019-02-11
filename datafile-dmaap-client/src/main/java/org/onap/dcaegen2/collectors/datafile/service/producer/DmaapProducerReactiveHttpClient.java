/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018-2019 Nordix Foundation. All rights reserved.
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

import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.Future;

import javax.net.ssl.SSLContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.ssl.SSLContextBuilder;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.http.HttpAsyncClientBuilderWrapper;
import org.onap.dcaegen2.collectors.datafile.http.IHttpAsyncClientBuilder;
import org.onap.dcaegen2.collectors.datafile.model.logging.MdcVariables;
import org.onap.dcaegen2.collectors.datafile.web.PublishRedirectStrategy;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapPublisherConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 7/4/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class DmaapProducerReactiveHttpClient {

    private static final int NO_REQUEST_TIMEOUT = -1;
    private static final Marker INVOKE = MarkerFactory.getMarker("INVOKE");
    private static final Marker INVOKE_RETURN = MarkerFactory.getMarker("INVOKE_RETURN");

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String dmaapHostName;
    private final Integer dmaapPortNumber;
    private final String dmaapProtocol;
    private final String user;
    private final String pwd;

    /**
     * Constructor DmaapProducerReactiveHttpClient.
     *
     * @param dmaapPublisherConfiguration - DMaaP producer configuration object
     */
    public DmaapProducerReactiveHttpClient(DmaapPublisherConfiguration dmaapPublisherConfiguration) {
        this.dmaapHostName = dmaapPublisherConfiguration.dmaapHostName();
        this.dmaapPortNumber = dmaapPublisherConfiguration.dmaapPortNumber();
        this.dmaapProtocol = dmaapPublisherConfiguration.dmaapProtocol();
        this.user = dmaapPublisherConfiguration.dmaapUserName();
        this.pwd = dmaapPublisherConfiguration.dmaapUserPassword();
    }

    public HttpResponse getDmaapProducerResponseWithRedirect(HttpUriRequest request, Map<String, String> contextMap)
            throws DatafileTaskException {
        try (CloseableHttpAsyncClient webClient = createWebClient(true, NO_REQUEST_TIMEOUT)) {
            MdcVariables.setMdcContextMap(contextMap);
            webClient.start();

            logger.trace(INVOKE, "Starting to produce to DR {}", request);
            Future<HttpResponse> future = webClient.execute(request, null);
            HttpResponse response = future.get();
            logger.trace(INVOKE_RETURN, "Response from DR {}", response.toString());
            return response;
        } catch (Exception e) {
            throw new DatafileTaskException("Unable to create web client.", e);
        }
    }

    public HttpResponse getDmaapProducerResponseWithCustomTimeout(HttpUriRequest request, int requestTimeout,
            Map<String, String> contextMap) throws DatafileTaskException {
        try (CloseableHttpAsyncClient webClient = createWebClient(false, requestTimeout)) {
            MdcVariables.setMdcContextMap(contextMap);
            webClient.start();

            logger.trace(INVOKE, "Starting to produce to DR {}", request);
            Future<HttpResponse> future = webClient.execute(request, null);
            HttpResponse response = future.get();
            logger.trace(INVOKE_RETURN, "Response from DR {}", response.toString());
            return response;
        } catch (Exception e) {
            throw new DatafileTaskException("Unable to create web client.", e);
        }
    }

    public void addUserCredentialsToHead(HttpUriRequest request) {
        String plainCreds = user + ":" + pwd;
        byte[] plainCredsBytes = plainCreds.getBytes(StandardCharsets.ISO_8859_1);
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        String base64Creds = new String(base64CredsBytes);
        logger.trace("base64Creds...: {}", base64Creds);
        request.addHeader("Authorization", "Basic " + base64Creds);
    }

    public UriBuilder getBaseUri() {
        return new DefaultUriBuilderFactory().builder() //
                .scheme(dmaapProtocol) //
                .host(dmaapHostName) //
                .port(dmaapPortNumber);
    }

    private CloseableHttpAsyncClient createWebClient(boolean expectRedirect, int requestTimeout)
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        SSLContext sslContext =
                new SSLContextBuilder().loadTrustMaterial(null, (certificate, authType) -> true).build();

        IHttpAsyncClientBuilder clientBuilder = getHttpClientBuilder();
        clientBuilder.setSSLContext(sslContext) //
                .setSSLHostnameVerifier(new NoopHostnameVerifier());

        if (expectRedirect) {
            clientBuilder.setRedirectStrategy(PublishRedirectStrategy.INSTANCE);
        }

        if (requestTimeout > NO_REQUEST_TIMEOUT) {
            RequestConfig requestConfig = RequestConfig.custom() //
                    .setSocketTimeout(requestTimeout) //
                    .setConnectTimeout(requestTimeout) //
                    .setConnectionRequestTimeout(requestTimeout) //
                    .build();

            clientBuilder.setDefaultRequestConfig(requestConfig);
        }

        return clientBuilder.build();
    }

    IHttpAsyncClientBuilder getHttpClientBuilder() {
        return new HttpAsyncClientBuilderWrapper();
    }
}