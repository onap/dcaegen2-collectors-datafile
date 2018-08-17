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

package org.onap.dcaegen2.services.prh.service.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.services.prh.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.services.prh.config.ImmutableDmaapPublisherConfiguration;

class DmaapPublisherConfigurationTest {


    @Test
    void builder_shouldBuildConfigurationObject() {

        // Given
        DmaapPublisherConfiguration configuration;
        String dmaapHostName = "localhost";
        Integer dmaapPortNumber = 2222;
        String dmaapTopicName = "temp";
        String dmaapProtocol = "http";
        String dmaapUserName = "admin";
        String dmaapUserPassword = "admin";
        String dmaapContentType = "application/json";

        // When
        configuration = new ImmutableDmaapPublisherConfiguration.Builder()
            .dmaapHostName(dmaapHostName)
            .dmaapPortNumber(dmaapPortNumber)
            .dmaapTopicName(dmaapTopicName)
            .dmaapProtocol(dmaapProtocol)
            .dmaapUserName(dmaapUserName)
            .dmaapUserPassword(dmaapUserPassword)
            .dmaapContentType(dmaapContentType)
            .build();

        // Then
        Assertions.assertNotNull(configuration);
        Assertions.assertEquals(dmaapHostName, configuration.dmaapHostName());
        Assertions.assertEquals(dmaapPortNumber, configuration.dmaapPortNumber());
        Assertions.assertEquals(dmaapTopicName, configuration.dmaapTopicName());
        Assertions.assertEquals(dmaapProtocol, configuration.dmaapProtocol());
        Assertions.assertEquals(dmaapUserName, configuration.dmaapUserName());
        Assertions.assertEquals(dmaapUserPassword, configuration.dmaapUserPassword());
    }
}
