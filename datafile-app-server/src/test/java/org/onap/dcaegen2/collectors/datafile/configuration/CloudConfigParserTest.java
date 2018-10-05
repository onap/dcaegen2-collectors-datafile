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

package org.onap.dcaegen2.collectors.datafile.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.config.DmaapConsumerConfiguration;
import org.onap.dcaegen2.collectors.datafile.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.config.ImmutableDmaapConsumerConfiguration;
import org.onap.dcaegen2.collectors.datafile.config.ImmutableDmaapPublisherConfiguration;

class CloudConfigParserTest {

    private static final ImmutableDmaapConsumerConfiguration CORRECT_DMAAP_CONSUMER_CONFIG =
            new ImmutableDmaapConsumerConfiguration.Builder().timeoutMS(-1)
                    .dmaapHostName("message-router.onap.svc.cluster.local").dmaapUserName("admin")
                    .dmaapUserPassword("admin").dmaapTopicName("/events/unauthenticated.VES_NOTIFICATION_OUTPUT")
                    .dmaapPortNumber(2222).dmaapContentType("application/json").messageLimit(-1).dmaapProtocol("http")
                    .consumerId("C12").consumerGroup("OpenDCAE-c12").build();

    private static final ImmutableDmaapPublisherConfiguration CORRECT_DMAAP_PUBLISHER_CONFIG =
            new ImmutableDmaapPublisherConfiguration.Builder().dmaapTopicName("publish").dmaapUserPassword("dradmin")
                    .dmaapPortNumber(3907).dmaapProtocol("https").dmaapContentType("application/json")
                    .dmaapHostName("message-router.onap.svc.cluster.local").dmaapUserName("dradmin").build();

    private static final ImmutableFtpesConfig CORRECT_FTPES_CONFIGURATION =
            new ImmutableFtpesConfig.Builder().keyCert("/config/ftpKey.jks").keyPassword("secret")
                    .trustedCA("config/cacerts").trustedCAPassword("secret").build();

    private CloudConfigParser cloudConfigParser = new CloudConfigParser(getCloudConfigJsonObject());

    @Test
    public void shouldCreateDmaapConsumerConfigurationCorrectly() {
        DmaapConsumerConfiguration dmaapConsumerConfig = cloudConfigParser.getDmaapConsumerConfig();

        assertThat(dmaapConsumerConfig).isNotNull();
        assertThat(dmaapConsumerConfig).isEqualToComparingFieldByField(CORRECT_DMAAP_CONSUMER_CONFIG);
    }

    @Test
    public void shouldCreateDmaapPublisherConfigurationCorrectly() {
        DmaapPublisherConfiguration dmaapPublisherConfig = cloudConfigParser.getDmaapPublisherConfig();

        assertThat(dmaapPublisherConfig).isNotNull();
        assertThat(dmaapPublisherConfig).isEqualToComparingFieldByField(CORRECT_DMAAP_PUBLISHER_CONFIG);
    }

    @Test
    public void shouldCreateFtpesConfigurationCorrectly() {
        FtpesConfig ftpesConfig = cloudConfigParser.getFtpesConfig();

        assertThat(ftpesConfig).isNotNull();
        assertThat(ftpesConfig).isEqualToComparingFieldByField(CORRECT_FTPES_CONFIGURATION);
    }

    public JsonObject getCloudConfigJsonObject() {
        JsonObject config = new JsonObject();
        config.addProperty("dmaap.dmaapConsumerConfiguration.timeoutMS", -1);
        config.addProperty("dmaap.dmaapConsumerConfiguration.dmaapHostName", "message-router.onap.svc.cluster.local");
        config.addProperty("dmaap.dmaapConsumerConfiguration.dmaapUserName", "admin");
        config.addProperty("dmaap.dmaapConsumerConfiguration.dmaapUserPassword", "admin");
        config.addProperty("dmaap.dmaapConsumerConfiguration.dmaapTopicName",
                "/events/unauthenticated.VES_NOTIFICATION_OUTPUT");
        config.addProperty("dmaap.dmaapConsumerConfiguration.dmaapPortNumber", 2222);
        config.addProperty("dmaap.dmaapConsumerConfiguration.dmaapContentType", "application/json");
        config.addProperty("dmaap.dmaapConsumerConfiguration.messageLimit", -1);
        config.addProperty("dmaap.dmaapConsumerConfiguration.dmaapProtocol", "http");
        config.addProperty("dmaap.dmaapConsumerConfiguration.consumerId", "C12");
        config.addProperty("dmaap.dmaapConsumerConfiguration.consumerGroup", "OpenDCAE-c12");
        config.addProperty("dmaap.dmaapProducerConfiguration.dmaapTopicName", "publish");
        config.addProperty("dmaap.dmaapProducerConfiguration.dmaapProtocol", "https");
        config.addProperty("dmaap.dmaapProducerConfiguration.dmaapContentType", "application/json");
        config.addProperty("dmaap.dmaapProducerConfiguration.dmaapHostName", "message-router.onap.svc.cluster.local");
        config.addProperty("dmaap.dmaapProducerConfiguration.dmaapPortNumber", 3907);
        config.addProperty("dmaap.dmaapProducerConfiguration.dmaapUserName", "dradmin");
        config.addProperty("dmaap.dmaapProducerConfiguration.dmaapUserPassword", "dradmin");
        config.addProperty("dmaap.ftpesConfig.keyCert", "/config/ftpKey.jks");
        config.addProperty("dmaap.ftpesConfig.keyPassword", "secret");
        config.addProperty("dmaap.ftpesConfig.trustedCA", "config/cacerts");
        config.addProperty("dmaap.ftpesConfig.trustedCAPassword", "secret");

        return config;
    }
}
