/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018-2019 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;


import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapConsumerConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

class DmaapReactiveWebClientTest {

    @Mock
    private DmaapConsumerConfiguration dmaapConsumerConfiguration;

    @BeforeEach
    void setUp() {
        dmaapConsumerConfiguration = mock(DmaapConsumerConfiguration.class);
    }


    @Test
    void buildsDMaaPReactiveWebClientProperly() {
        when(dmaapConsumerConfiguration.dmaapContentType()).thenReturn("*/*");
        WebClient dmaapReactiveWebClient = new DmaapReactiveWebClient()
            .fromConfiguration(dmaapConsumerConfiguration)
            .build();

        verify(dmaapConsumerConfiguration, times(1)).dmaapContentType();
        assertNotNull(dmaapReactiveWebClient);
    }
}