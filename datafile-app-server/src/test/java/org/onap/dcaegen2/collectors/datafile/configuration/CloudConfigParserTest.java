/*
 * ============LICENSE_START=======================================================
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

package org.onap.dcaegen2.collectors.datafile.configuration;


import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.config.DmaapConsumerConfiguration;
import org.onap.dcaegen2.collectors.datafile.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.config.ImmutableDmaapConsumerConfiguration;
import org.onap.dcaegen2.collectors.datafile.config.ImmutableDmaapPublisherConfiguration;

class CloudConfigParserTest {

    private static final String CORRECT_JSON =
        "{\"dmaap.dmaapProducerConfiguration.dmaapTopicName\": \"/events/unauthenticated.VES_NOTIFICATION_OUTPUT\", "
            + "\"dmaap.dmaapConsumerConfiguration.timeoutMS\": -1,"
            + " \"dmaap.dmaapConsumerConfiguration.dmaapHostName\": \"message-router.onap.svc.cluster.local\","
            + "\"dmaap.dmaapConsumerConfiguration.dmaapUserName\": \"admin\", "
            + "\"dmaap.dmaapProducerConfiguration.dmaapPortNumber\": 3904, "
            + "\"dmaap.dmaapConsumerConfiguration.dmaapUserPassword\": \"admin\", "
            + "\"dmaap.dmaapProducerConfiguration.dmaapProtocol\": \"http\", "
            + "\"dmaap.dmaapProducerConfiguration.dmaapContentType\": \"application/json\", "
            + "\"dmaap.dmaapConsumerConfiguration.dmaapTopicName\": \"/events/unauthenticated.VES_NOTIFICATION_OUTPUT\", "
            + "\"dmaap.dmaapConsumerConfiguration.dmaapPortNumber\": 3904, "
            + "\"dmaap.dmaapConsumerConfiguration.dmaapContentType\": \"application/json\", "
            + "\"dmaap.dmaapConsumerConfiguration.messageLimit\": -1, "
            + "\"dmaap.dmaapConsumerConfiguration.dmaapProtocol\": \"http\", "
            + "\"dmaap.dmaapConsumerConfiguration.consumerId\": \"c12\","
            + "\"dmaap.dmaapProducerConfiguration.dmaapHostName\": \"message-router.onap.svc.cluster.local\", "
            + "\"dmaap.dmaapConsumerConfiguration.consumerGroup\": \"OpenDCAE-c12\", "
            + "\"dmaap.dmaapProducerConfiguration.dmaapUserName\": \"admin\", "
            + "\"dmaap.dmaapProducerConfiguration.dmaapUserPassword\": \"admin\"}";

    private static final ImmutableDmaapConsumerConfiguration correctDmaapConsumerConfig =
        new ImmutableDmaapConsumerConfiguration.Builder()
            .timeoutMS(-1)
            .dmaapHostName("message-router.onap.svc.cluster.local")
            .dmaapUserName("admin")
            .dmaapUserPassword("admin")
            .dmaapTopicName("/events/unauthenticated.VES_NOTIFICATION_OUTPUT")
            .dmaapPortNumber(3904)
            .dmaapContentType("application/json")
            .messageLimit(-1)
            .dmaapProtocol("http")
            .consumerId("c12")
            .consumerGroup("OpenDCAE-c12")
            .build();

    private static final ImmutableDmaapPublisherConfiguration correctDmaapPublisherConfig =
        new ImmutableDmaapPublisherConfiguration.Builder()
            .dmaapTopicName("/events/unauthenticated.VES_NOTIFICATION_OUTPUT")
            .dmaapUserPassword("admin")
            .dmaapPortNumber(3904)
            .dmaapProtocol("http")
            .dmaapContentType("application/json")
            .dmaapHostName("message-router.onap.svc.cluster.local")
            .dmaapUserName("admin")
            .build();

    private CloudConfigParser cloudConfigParser = new CloudConfigParser(
        new Gson().fromJson(CORRECT_JSON, JsonObject.class));


    @Test
    public void shouldCreateDmaapConsumerConfigurationCorrectly() {
        // when
        DmaapConsumerConfiguration dmaapConsumerConfig = cloudConfigParser.getDmaapConsumerConfig();

        // then
        assertThat(dmaapConsumerConfig).isNotNull();
        assertThat(dmaapConsumerConfig).isEqualToComparingFieldByField(correctDmaapConsumerConfig);
    }


    @Test
    public void shouldCreateDmaapPublisherConfigurationCorrectly() {
        // when
        DmaapPublisherConfiguration dmaapPublisherConfig = cloudConfigParser.getDmaapPublisherConfig();

        // then
        assertThat(dmaapPublisherConfig).isNotNull();
        assertThat(dmaapPublisherConfig).isEqualToComparingFieldByField(correctDmaapPublisherConfig);
    }
}