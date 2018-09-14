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

import java.util.Optional;

import org.onap.dcaegen2.collectors.datafile.config.DmaapConsumerConfiguration;
import org.onap.dcaegen2.collectors.datafile.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.config.ImmutableDmaapConsumerConfiguration;
import org.onap.dcaegen2.collectors.datafile.config.ImmutableDmaapPublisherConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 3/23/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */

@Component
@Configuration
public class AppConfig extends DatafileAppConfig {

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
            .timeoutMS(Optional.ofNullable(consumerTimeoutMS).filter(p -> !p.toString().isEmpty())
                .orElse(dmaapConsumerConfiguration.timeoutMS()))
            .consumerGroup(Optional.ofNullable(consumerGroup).filter(p -> !p.isEmpty())
                .orElse(dmaapConsumerConfiguration.consumerGroup()))
            .consumerId(Optional.ofNullable(consumerId).filter(p -> !p.isEmpty())
                .orElse(dmaapConsumerConfiguration.consumerId()))
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
            .build();
    }

}
