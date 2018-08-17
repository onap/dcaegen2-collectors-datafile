/*
 * ============LICENSE_START=======================================================
 * Datafile Collector Service
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.config.DmaapConsumerConfiguration;
import org.onap.dcaegen2.collectors.datafile.service.DMaaPReactiveWebClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 7/5/18
 */
class DMaaPReactiveWebClientTest {


    @Test
    void builder_shouldBuildDMaaPReactiveWebClient() {
        //given
        DmaapConsumerConfiguration dmaapConsumerConfiguration = mock(DmaapConsumerConfiguration.class);
        String dmaaPContentType = "*/*";
        String dmaaPUserName = "DMaaP";
        String dmaaPUserPassword = "DMaaP";

        //when
        when(dmaapConsumerConfiguration.dmaapContentType()).thenReturn(dmaaPContentType);
        when(dmaapConsumerConfiguration.dmaapUserName()).thenReturn(dmaaPUserName);
        when(dmaapConsumerConfiguration.dmaapUserPassword()).thenReturn(dmaaPUserPassword);
        WebClient dmaapreactiveWebClient = new DMaaPReactiveWebClient()
            .fromConfiguration(dmaapConsumerConfiguration)
            .build();

        //then
        Assertions.assertNotNull(dmaapreactiveWebClient);

    }
}