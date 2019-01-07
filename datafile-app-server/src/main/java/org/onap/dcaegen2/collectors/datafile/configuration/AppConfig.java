/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018 Nordix Foundation. All rights reserved.
 * ===============================================================================================
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
 * ============LICENSE_END========================================================================
 */

package org.onap.dcaegen2.collectors.datafile.configuration;

import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapConsumerConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.ImmutableDmaapConsumerConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.ImmutableDmaapPublisherConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 3/23/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */

@Component
@Configuration
public class AppConfig extends DatafileAppConfig {

    private static Predicate<String> isEmpty = String::isEmpty;
    @Value("${dmaap.dmaapConsumerConfiguration.dmaapHostName:}")
    public String consumerDmaapHostName;

    @Value("${dmaap.dmaapConsumerConfiguration.dmaapPortNumber:}")
    public Integer consumerDmaapPortNumber;

    @Value("${dmaap.dmaapConsumerConfiguration.dmaapTopicName:}")
    public String consumerDmaapTopicName;

    @Value("${dmaap.dmaapConsumerConfiguration.dmaapProtocol:}")
    public String consumerDmaapProtocol;

    @Value("${dmaap.dmaapConsumerConfiguration.dmaapUserName:}")
    public String consumerDmaapUserName;

    @Value("${dmaap.dmaapConsumerConfiguration.dmaapUserPassword:}")
    public String consumerDmaapUserPassword;

    @Value("${dmaap.dmaapConsumerConfiguration.dmaapContentType:}")
    public String consumerDmaapContentType;

    @Value("${dmaap.dmaapConsumerConfiguration.consumerId:}")
    public String consumerId;

    @Value("${dmaap.dmaapConsumerConfiguration.consumerGroup:}")
    public String consumerGroup;

    @Value("${dmaap.dmaapConsumerConfiguration.timeoutMS:}")
    public Integer consumerTimeoutMS;

    @Value("${dmaap.dmaapConsumerConfiguration.message-limit:}")
    public Integer consumerMessageLimit;

    @Value("${dmaap.dmaapProducerConfiguration.dmaapHostName:}")
    public String producerDmaapHostName;

    @Value("${dmaap.dmaapProducerConfiguration.dmaapPortNumber:}")
    public Integer producerDmaapPortNumber;

    @Value("${dmaap.dmaapProducerConfiguration.dmaapTopicName:}")
    public String producerDmaapTopicName;

    @Value("${dmaap.dmaapProducerConfiguration.dmaapProtocol:}")
    public String producerDmaapProtocol;

    @Value("${dmaap.dmaapProducerConfiguration.dmaapUserName:}")
    public String producerDmaapUserName;

    @Value("${dmaap.dmaapProducerConfiguration.dmaapUserPassword:}")
    public String producerDmaapUserPassword;

    @Value("${dmaap.dmaapProducerConfiguration.dmaapContentType:}")
    public String producerDmaapContentType;

    @Value("${ftp.ftpesConfiguration.keyCert:}")
    public String keyCert;

    @Value("${ftp.ftpesConfiguration.keyPassword:}")
    public String keyPassword;

    @Value("${ftp.ftpesConfiguration.trustedCA:}")
    public String trustedCA;

    @Value("${ftp.ftpesConfiguration.trustedCAPassword:}")
    public String trustedCAPassword;

    @Value("${security.trustStorePath:}")
    public String trustStorePath;

    @Value("${security.trustStorePasswordPath:}")
    public String trustStorePasswordPath;

    @Value("${security.keyStorePath:}")
    public String keyStorePath;

    @Value("${security.keyStorePasswordPath:}")
    public String keyStorePasswordPath;

    @Value("${security.enableAaiCertAuth:}")
    public Boolean enableAaiCertAuth;

    @Value("${security.enableDmaapCertAuth:}")
    public Boolean enableDmaapCertAuth;

    @Override
    public DmaapConsumerConfiguration getDmaapConsumerConfiguration() {
        return new ImmutableDmaapConsumerConfiguration.Builder()
                .dmaapUserPassword(
                        Optional.ofNullable(consumerDmaapUserPassword).filter(p -> !p.isEmpty())
                                .orElse(dmaapConsumerConfiguration.dmaapUserPassword()))
                .dmaapUserName(
                        Optional.ofNullable(consumerDmaapUserName).filter(p -> !p.isEmpty())
                                .orElse(dmaapConsumerConfiguration.dmaapUserName()))
                .dmaapHostName(
                        Optional.ofNullable(consumerDmaapHostName).filter(p -> !p.isEmpty())
                                .orElse(dmaapConsumerConfiguration.dmaapHostName()))
                .dmaapPortNumber(
                        Optional.ofNullable(consumerDmaapPortNumber).filter(p -> !p.toString().isEmpty())
                                .orElse(dmaapConsumerConfiguration.dmaapPortNumber()))
                .dmaapProtocol(
                        Optional.ofNullable(consumerDmaapProtocol).filter(p -> !p.isEmpty())
                                .orElse(dmaapConsumerConfiguration.dmaapProtocol()))
                .dmaapContentType(
                        Optional.ofNullable(consumerDmaapContentType).filter(p -> !p.isEmpty())
                                .orElse(dmaapConsumerConfiguration.dmaapContentType()))
                .dmaapTopicName(
                        Optional.ofNullable(consumerDmaapTopicName).filter(p -> !p.isEmpty())
                                .orElse(dmaapConsumerConfiguration.dmaapTopicName()))
                .messageLimit(
                        Optional.ofNullable(consumerMessageLimit).filter(p -> !p.toString().isEmpty())
                                .orElse(dmaapConsumerConfiguration.messageLimit()))
                .timeoutMs(Optional.ofNullable(consumerTimeoutMS).filter(p -> !p.toString().isEmpty())
                        .orElse(dmaapConsumerConfiguration.timeoutMs()))
                .consumerGroup(Optional.ofNullable(consumerGroup).filter(p -> !p.isEmpty())
                        .orElse(dmaapConsumerConfiguration.consumerGroup()))
                .consumerId(Optional.ofNullable(consumerId).filter(p -> !p.isEmpty())
                        .orElse(dmaapConsumerConfiguration.consumerId()))
                .trustStorePath(
                        Optional.ofNullable(trustStorePath).filter(isEmpty.negate())
                                .orElse(dmaapConsumerConfiguration.trustStorePath()))
                .trustStorePasswordPath(
                        Optional.ofNullable(trustStorePasswordPath).filter(isEmpty.negate())
                                .orElse(dmaapConsumerConfiguration.trustStorePasswordPath()))
                .keyStorePath(
                        Optional.ofNullable(keyStorePath).filter(isEmpty.negate())
                                .orElse(dmaapConsumerConfiguration.keyStorePath()))
                .keyStorePasswordPath(
                        Optional.ofNullable(keyStorePasswordPath).filter(isEmpty.negate())
                                .orElse(dmaapConsumerConfiguration.keyStorePasswordPath()))
                .enableDmaapCertAuth(
                        Optional.ofNullable(enableDmaapCertAuth).filter(p -> !p.toString().isEmpty())
                                .orElse(dmaapConsumerConfiguration.enableDmaapCertAuth()))
                .build();
    }

    @Override
    public DmaapPublisherConfiguration getDmaapPublisherConfiguration() {
        return new ImmutableDmaapPublisherConfiguration.Builder()
                .dmaapContentType(
                        Optional.ofNullable(producerDmaapContentType).filter(p -> !p.isEmpty())
                                .orElse(dmaapPublisherConfiguration.dmaapContentType()))
                .dmaapHostName(
                        Optional.ofNullable(producerDmaapHostName).filter(p -> !p.isEmpty())
                                .orElse(dmaapPublisherConfiguration.dmaapHostName()))
                .dmaapPortNumber(
                        Optional.ofNullable(producerDmaapPortNumber).filter(p -> !p.toString().isEmpty())
                                .orElse(dmaapPublisherConfiguration.dmaapPortNumber()))
                .dmaapProtocol(
                        Optional.ofNullable(producerDmaapProtocol).filter(p -> !p.isEmpty())
                                .orElse(dmaapPublisherConfiguration.dmaapProtocol()))
                .dmaapTopicName(
                        Optional.ofNullable(producerDmaapTopicName).filter(p -> !p.isEmpty())
                                .orElse(dmaapPublisherConfiguration.dmaapTopicName()))
                .dmaapUserName(
                        Optional.ofNullable(producerDmaapUserName).filter(p -> !p.isEmpty())
                                .orElse(dmaapPublisherConfiguration.dmaapUserName()))
                .dmaapUserPassword(
                        Optional.ofNullable(producerDmaapUserPassword).filter(p -> !p.isEmpty())
                                .orElse(dmaapPublisherConfiguration.dmaapUserPassword()))
                .trustStorePath(
                        Optional.ofNullable(trustStorePath).filter(isEmpty.negate())
                                .orElse(dmaapPublisherConfiguration.trustStorePath()))
                .trustStorePasswordPath(
                        Optional.ofNullable(trustStorePasswordPath).filter(isEmpty.negate())
                                .orElse(dmaapPublisherConfiguration.trustStorePasswordPath()))
                .keyStorePath(
                        Optional.ofNullable(keyStorePath).filter(isEmpty.negate())
                                .orElse(dmaapPublisherConfiguration.keyStorePath()))
                .keyStorePasswordPath(
                        Optional.ofNullable(keyStorePasswordPath).filter(isEmpty.negate())
                                .orElse(dmaapPublisherConfiguration.keyStorePasswordPath()))
                .enableDmaapCertAuth(
                        Optional.ofNullable(enableAaiCertAuth).filter(p -> !p.toString().isEmpty())
                                .orElse(dmaapPublisherConfiguration.enableDmaapCertAuth()))
                .build();
    }

    @Override
    public FtpesConfig getFtpesConfiguration() {
        return new ImmutableFtpesConfig.Builder()
                .keyCert(
                        Optional.ofNullable(keyCert).filter(p -> !p.isEmpty())
                                .orElse(ftpesConfig.keyCert()))
                .keyPassword(
                        Optional.ofNullable(keyPassword).filter(p -> !p.isEmpty())
                                .orElse(ftpesConfig.keyPassword()))
                .trustedCA(
                        Optional.ofNullable(trustedCA).filter(p -> !p.isEmpty())
                                .orElse(ftpesConfig.trustedCA()))
                .trustedCAPassword(
                        Optional.ofNullable(trustedCAPassword).filter(p -> !p.isEmpty())
                                .orElse(ftpesConfig.trustedCAPassword()))
                .build();
    }
}
