/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018, 2020 NOKIA Intellectual Property, 2018-2019 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.service.producer;

import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
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
import org.onap.dcaegen2.collectors.datafile.configuration.PublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.http.HttpAsyncClientBuilderWrapper;
import org.onap.dcaegen2.collectors.datafile.web.PublishRedirectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Client used to send requests to DataRouter.
 *
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 7/4/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class DmaapProducerHttpClient {

    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofMinutes(2);
    private static final Marker INVOKE = MarkerFactory.getMarker("INVOKE");
    private static final Marker INVOKE_RETURN = MarkerFactory.getMarker("INVOKE_RETURN");

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final PublisherConfiguration configuration;

    /**
     * Constructor DmaapProducerReactiveHttpClient.
     *
     * @param dmaapPublisherConfiguration - DMaaP producer configuration object
     */
    public DmaapProducerHttpClient(PublisherConfiguration dmaapPublisherConfiguration) {
        this.configuration = dmaapPublisherConfiguration;
    }

    /**
     * Executes the given request and handles redirects.
     *
     * @param request the request to execute.
     * @param contextMap context for logging.
     *
     * @return the response from the request.
     *
     * @throws DatafileTaskException if anything goes wrong.
     */
    public HttpResponse getDmaapProducerResponseWithRedirect(HttpUriRequest request, Map<String, String> contextMap)
        throws DatafileTaskException {
        MDC.setContextMap(contextMap);
        try (CloseableHttpAsyncClient webClient = createWebClient(true, DEFAULT_REQUEST_TIMEOUT, contextMap)) {
            webClient.start();

            logger.trace(INVOKE, "Starting to produce to DR {}", request);
            Future<HttpResponse> future = webClient.execute(request, null);
            HttpResponse response = future.get();
            logger.trace(INVOKE_RETURN, "Response from DR {}", response);
            return response;
        } catch (Exception e) {
            throw new DatafileTaskException("Unable to create web client.", e);
        }
    }

    /**
     * Executes the given request using the given timeout time.
     *
     * @param request the request to execute.
     * @param requestTimeout the timeout time for the request.
     * @param contextMap context for logging.
     *
     * @return the response from the request.
     *
     * @throws DatafileTaskException if anything goes wrong.
     */
    public HttpResponse getDmaapProducerResponseWithCustomTimeout(HttpUriRequest request, Duration requestTimeout,
        Map<String, String> contextMap) throws DatafileTaskException {
        MDC.setContextMap(contextMap);
        try (CloseableHttpAsyncClient webClient = createWebClient(false, requestTimeout, contextMap)) {
            webClient.start();

            logger.trace(INVOKE, "Starting to produce to DR {}", request);
            Future<HttpResponse> future = webClient.execute(request, null);
            HttpResponse response = future.get();
            logger.trace(INVOKE_RETURN, "Response from DR {}", response);
            return response;
        } catch (Exception e) {
            throw new DatafileTaskException("Unable to create web client.", e);
        }
    }

    /**
     * Adds the user credentials needed to talk to DataRouter to the provided request.
     *
     * @param request the request to add credentials to.
     */
    public void addUserCredentialsToHead(HttpUriRequest request) {
        String plainCreds = configuration.userName() + ":" + configuration.password();
        byte[] plainCredsBytes = plainCreds.getBytes(StandardCharsets.ISO_8859_1);
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        String base64Creds = new String(base64CredsBytes);
        logger.trace("base64Creds...: {}", base64Creds);
        request.addHeader("Authorization", "Basic " + base64Creds);
    }

    private CloseableHttpAsyncClient createWebClient(boolean expectRedirect, Duration requestTimeout,
        Map<String, String> contextMap) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        SSLContext sslContext =
            new SSLContextBuilder().loadTrustMaterial(null, (certificate, authType) -> true).build();

        HttpAsyncClientBuilderWrapper clientBuilder = getHttpClientBuilder();
        clientBuilder.setSslContext(sslContext) //
            .setSslHostnameVerifier(new NoopHostnameVerifier());

        if (expectRedirect) {
            clientBuilder.setRedirectStrategy(new PublishRedirectStrategy(contextMap));
        }

        if (requestTimeout.toMillis() > 0) {
            int millis = (int) requestTimeout.toMillis();
            RequestConfig requestConfig = RequestConfig.custom() //
                .setSocketTimeout(millis) //
                .setConnectTimeout(millis) //
                .setConnectionRequestTimeout(millis) //
                .build();

            clientBuilder.setDefaultRequestConfig(requestConfig);
        } else {
            logger.error("WEB client without timeout created {}", requestTimeout);
        }

        return clientBuilder.build();
    }

    HttpAsyncClientBuilderWrapper getHttpClientBuilder() {
        return new HttpAsyncClientBuilderWrapper();
    }
}
