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

package org.onap.dcaegen2.collectors.datafile.service;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.onap.dcaegen2.collectors.datafile.config.AaiClientConfiguration;
import org.onap.dcaegen2.collectors.datafile.service.AaiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AaiClientImpl implements AaiClient {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private AaiClientConfiguration aaiClientConfig;


    AaiClientImpl(AaiClientConfiguration aaiClientConfiguration) {
        this.aaiClientConfig = aaiClientConfiguration;
    }

    @Override
    public CloseableHttpClient getAaiHttpClient() {

        final HttpClientBuilder httpClientBuilder = HttpClients.custom().useSystemProperties();
        final boolean aaiIgnoreSslCertificateErrors = aaiClientConfig.aaiIgnoreSslCertificateErrors();

        TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;

        if (aaiIgnoreSslCertificateErrors) {
            try {
                logger.info("Setting SSL Context for AAI HTTP Client");
                httpClientBuilder.setSSLContext(new SSLContextBuilder()
                    .loadTrustMaterial(null, acceptingTrustStrategy)
                    .build());

            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                logger.error("Exception while setting SSL Context for AAI HTTP Client: {}", e);
            }

            httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        }

        final String aaiUserName = aaiClientConfig.aaiUserName();

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        if (aaiUserName != null) {
            final String aaiHost = aaiClientConfig.aaiHost();
            final Integer aaiHostPortNumber = aaiClientConfig.aaiPort();
            final String aaiUserPassword = aaiClientConfig.aaiUserPassword();
            final AuthScope aaiHostPortAuthScope = new AuthScope(aaiHost, aaiHostPortNumber);
            final Credentials aaiCredentials = new UsernamePasswordCredentials(aaiUserName, aaiUserPassword);
            credentialsProvider.setCredentials(aaiHostPortAuthScope, aaiCredentials);
        }

        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);

        return httpClientBuilder.build();
    }
}
