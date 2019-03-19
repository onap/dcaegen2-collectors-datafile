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

package org.onap.dcaegen2.collectors.datafile.tasks;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.onap.dcaegen2.collectors.datafile.configuration.DmaapBusControllerConfiguration;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.http.HttpAsyncClientBuilderWrapper;
import org.onap.dcaegen2.collectors.datafile.http.IHttpAsyncClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

/**
 * Client to communicate with the BusController.
 *
 * @author <a href="mailto:martin.c.yann@est.tech">Martin Yan</a>
 */
public class DmaapBusControllerHttpClient {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String dmaapHostName;
    private final Integer dmaapPortNumber;
    private final String dmaapProtocol;
    private final String user;
    private final String pwd;

    /**
     * Constructor.
     *
     * @param dmaapBusControllerConfiguration - DMaaP bus controller configuration object
     */
    public DmaapBusControllerHttpClient(DmaapBusControllerConfiguration dmaapBusControllerConfiguration) {
        this.dmaapHostName = dmaapBusControllerConfiguration.dmaapHostName();
        this.dmaapPortNumber = dmaapBusControllerConfiguration.dmaapPortNumber();
        this.dmaapProtocol = dmaapBusControllerConfiguration.dmaapProtocol();
        this.user = dmaapBusControllerConfiguration.dmaapUserName();
        this.pwd = dmaapBusControllerConfiguration.dmaapUserPassword();
    }

    /**
     * Function for calling DMaaP HTTP Bus Controller - post request to DMaaP BusController.
     *
     * @param request the request to use
     * @param requestTimeout the timeout of the request
     *
     * @return status code of operation
     * @throws DatafileTaskException when there is an error in the communication.
     */
    public HttpResponse getDmaapBusControllerResponse(HttpUriRequest request, int requestTimeout)
            throws DatafileTaskException {
        logger.trace("Entering getDmaapBusControllerResponse");
        try (CloseableHttpAsyncClient webClient = createWebClient(requestTimeout)) {
            logger.trace("Creating feed via bus controller to DR");

            webClient.start();

            Future<HttpResponse> future = webClient.execute(request, null);
            HttpResponse response = future.get();
            logger.trace(response.toString());
            return response;
        } catch (Exception e) {
            logger.error("Unable to send request to Bus Controller.", e);
            throw new DatafileTaskException("Unable to send request to Bus Controller.", e);
        }
    }

    /**
     * Adds the user credentials to use for the BusController to the head of the request.
     *
     * @param request the request to add the credentials to.
     */
    public void addUserCredentialsToHead(HttpUriRequest request) {
        String plainCreds = user + ":" + pwd;
        byte[] plainCredsBytes = plainCreds.getBytes(StandardCharsets.ISO_8859_1);
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        String base64Creds = new String(base64CredsBytes);
        logger.trace("base64Creds...: {}", base64Creds);
        request.addHeader("Authorization", "Basic " + base64Creds);
    }

    /**
     * Get the base URI to use for the BusController.
     *
     * @return A <code>UriBuilder</code> htan can be used to add paths and other things to the URI.
     */
    public UriBuilder getBaseUri() {
        return new DefaultUriBuilderFactory().builder() //
                .scheme(dmaapProtocol) //
                .host(dmaapHostName) //
                .port(dmaapPortNumber);
    }

    private CloseableHttpAsyncClient createWebClient(int requestTimeout) {
        IHttpAsyncClientBuilder clientBuilder = getHttpClientBuilder();

        RequestConfig requestConfig = RequestConfig.custom() //
                .setSocketTimeout(requestTimeout) //
                .setConnectTimeout(requestTimeout) //
                .setConnectionRequestTimeout(requestTimeout) //
                .build();

        clientBuilder.setDefaultRequestConfig(requestConfig);

        return clientBuilder.build();
    }

    IHttpAsyncClientBuilder getHttpClientBuilder() {
        return new HttpAsyncClientBuilderWrapper();
    }
}
