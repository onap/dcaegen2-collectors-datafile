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

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.config.AaiClientConfiguration;
import org.onap.dcaegen2.collectors.datafile.service.AaiClientImpl;

class AaiHttpClientImplTest {

    private static AaiClientImpl testedObject;


    @BeforeAll
    public static void setup() {
        AaiClientConfiguration aaiHttpClientConfigurationMock = mock(AaiClientConfiguration.class);
        when(aaiHttpClientConfigurationMock.aaiHost()).thenReturn("54.45.33.2");
        when(aaiHttpClientConfigurationMock.aaiProtocol()).thenReturn("https");
        when(aaiHttpClientConfigurationMock.aaiPort()).thenReturn(1234);
        when(aaiHttpClientConfigurationMock.aaiUserName()).thenReturn("PNF");
        when(aaiHttpClientConfigurationMock.aaiUserPassword()).thenReturn("PNF");
        when(aaiHttpClientConfigurationMock.aaiIgnoreSslCertificateErrors()).thenReturn(true);

        testedObject  = new AaiClientImpl(aaiHttpClientConfigurationMock);
    }

    @Test
    public void getAaiHttpClientObject_shouldNotBeNull() {
        assertNotNull(testedObject.getAaiHttpClient());
    }
}

