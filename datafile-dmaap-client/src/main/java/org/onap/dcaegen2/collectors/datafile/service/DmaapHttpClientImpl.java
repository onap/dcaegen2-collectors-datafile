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
package org.onap.dcaegen2.collectors.datafile.service;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.onap.dcaegen2.collectors.datafile.config.DmaapCustomConfig;
import org.onap.dcaegen2.collectors.datafile.service.DmaapHttpClientImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DmaapHttpClientImpl {

    private static Logger logger = LoggerFactory.getLogger(DmaapHttpClientImpl.class);

    private final String dmaapHostName;
    private final Integer dmaapPortNumber;
    private final String dmaapUserName;
    private final String dmaapUserPassword;


    public DmaapHttpClientImpl(DmaapCustomConfig configuration) {
        this.dmaapHostName = configuration.dmaapHostName();
        this.dmaapPortNumber = configuration.dmaapPortNumber();
        this.dmaapUserName = configuration.dmaapUserName();
        this.dmaapUserPassword = configuration.dmaapUserPassword();
    }

    public CloseableHttpClient getHttpClient() {

        logger.info("Preparing closeable http client");

        HttpClientBuilder httpClientBuilder = HttpClients.custom().useSystemProperties();

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        if (dmaapUserName != null) {
            final AuthScope dmaapHostPortAuthScope = new AuthScope(dmaapHostName, dmaapPortNumber);
            final Credentials dmaapCredentials = new UsernamePasswordCredentials(dmaapUserName, dmaapUserPassword);
            credentialsProvider.setCredentials(dmaapHostPortAuthScope, dmaapCredentials);
        }

        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);

        return httpClientBuilder.build();
    }
}
