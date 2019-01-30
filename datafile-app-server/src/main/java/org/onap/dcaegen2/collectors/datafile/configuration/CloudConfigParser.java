/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.configuration;

import com.google.gson.JsonObject;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapConsumerConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.ImmutableDmaapConsumerConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.ImmutableDmaapPublisherConfiguration;


/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 9/19/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class CloudConfigParser {

    private final JsonObject jsonObject;

    CloudConfigParser(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    DmaapPublisherConfiguration getDmaapPublisherConfig() {
        return new ImmutableDmaapPublisherConfiguration.Builder()
                .dmaapTopicName(jsonObject.get("dmaap.dmaapProducerConfiguration.dmaapTopicName").getAsString())
                .dmaapUserPassword(jsonObject.get("dmaap.dmaapProducerConfiguration.dmaapUserPassword").getAsString())
                .dmaapPortNumber(jsonObject.get("dmaap.dmaapProducerConfiguration.dmaapPortNumber").getAsInt())
                .dmaapProtocol(jsonObject.get("dmaap.dmaapProducerConfiguration.dmaapProtocol").getAsString())
                .dmaapContentType(jsonObject.get("dmaap.dmaapProducerConfiguration.dmaapContentType").getAsString())
                .dmaapHostName(jsonObject.get("dmaap.dmaapProducerConfiguration.dmaapHostName").getAsString())
                .dmaapUserName(jsonObject.get("dmaap.dmaapProducerConfiguration.dmaapUserName").getAsString())
                .trustStorePath(jsonObject.get("dmaap.security.trustStorePath").getAsString())
                .trustStorePasswordPath(jsonObject.get("dmaap.security.trustStorePasswordPath").getAsString())
                .keyStorePath(jsonObject.get("dmaap.security.keyStorePath").getAsString())
                .keyStorePasswordPath(jsonObject.get("dmaap.security.keyStorePasswordPath").getAsString())
                .enableDmaapCertAuth(jsonObject.get("dmaap.security.enableDmaapCertAuth").getAsBoolean())
                .build();
    }

    DmaapConsumerConfiguration getDmaapConsumerConfig() {
        return new ImmutableDmaapConsumerConfiguration.Builder()
                .timeoutMs(jsonObject.get("dmaap.dmaapConsumerConfiguration.timeoutMs").getAsInt())
                .dmaapHostName(jsonObject.get("dmaap.dmaapConsumerConfiguration.dmaapHostName").getAsString())
                .dmaapUserName(jsonObject.get("dmaap.dmaapConsumerConfiguration.dmaapUserName").getAsString())
                .dmaapUserPassword(jsonObject.get("dmaap.dmaapConsumerConfiguration.dmaapUserPassword").getAsString())
                .dmaapTopicName(jsonObject.get("dmaap.dmaapConsumerConfiguration.dmaapTopicName").getAsString())
                .dmaapPortNumber(jsonObject.get("dmaap.dmaapConsumerConfiguration.dmaapPortNumber").getAsInt())
                .dmaapContentType(jsonObject.get("dmaap.dmaapConsumerConfiguration.dmaapContentType").getAsString())
                .messageLimit(jsonObject.get("dmaap.dmaapConsumerConfiguration.messageLimit").getAsInt())
                .dmaapProtocol(jsonObject.get("dmaap.dmaapConsumerConfiguration.dmaapProtocol").getAsString())
                .consumerId(jsonObject.get("dmaap.dmaapConsumerConfiguration.consumerId").getAsString())
                .consumerGroup(jsonObject.get("dmaap.dmaapConsumerConfiguration.consumerGroup").getAsString())
                .trustStorePath(jsonObject.get("dmaap.security.trustStorePath").getAsString())
                .trustStorePasswordPath(jsonObject.get("dmaap.security.trustStorePasswordPath").getAsString())
                .keyStorePath(jsonObject.get("dmaap.security.keyStorePath").getAsString())
                .keyStorePasswordPath(jsonObject.get("dmaap.security.keyStorePasswordPath").getAsString())
                .enableDmaapCertAuth(jsonObject.get("dmaap.security.enableDmaapCertAuth").getAsBoolean())
                .build();
    }

    public FtpesConfig getFtpesConfig() {
        return new ImmutableFtpesConfig.Builder()
                .keyCert(jsonObject.get("dmaap.ftpesConfig.keyCert").getAsString())
                .keyPassword(jsonObject.get("dmaap.ftpesConfig.keyPassword").getAsString())
                .trustedCA(jsonObject.get("dmaap.ftpesConfig.trustedCA").getAsString())
                .trustedCAPassword(jsonObject.get("dmaap.ftpesConfig.trustedCAPassword").getAsString())
                .build();
    }
}