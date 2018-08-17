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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.config.AaiClientConfiguration;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.service.AaiConsumerClient;

class AaiConsumerClientTest {

    private static AaiConsumerClient testedObject;
    private static AaiClientConfiguration aaiHttpClientConfigurationMock = mock(AaiClientConfiguration.class);
    private static CloseableHttpClient closeableHttpClientMock = mock(CloseableHttpClient.class);
    private static final String JSON_MESSAGE = "{ \"pnf-id\": \"example-pnf-id-val-22343\", "
        + "\"regional-resource-zone\":null, \"ipaddress-v4-oam\": \"11.22.33.44\" }";
    private static ConsumerDmaapModel consumerDmaapModelMock = mock(ConsumerDmaapModel.class);
    private static final String PNF_NAME = "nokia-pnf-nhfsadhff";

    @BeforeAll
    static void setup() throws NoSuchFieldException, IllegalAccessException {

        Map<String, String> aaiHeaders = new HashMap<>();
        aaiHeaders.put("X-FromAppId", "datafile");
        aaiHeaders.put("X-TransactionId", "9999");
        aaiHeaders.put("Accept", "application/json");
        aaiHeaders.put("Authorization", "Basic QUFJOkFBSQ==");
        aaiHeaders.put("Real-Time", "true");
        aaiHeaders.put("Content-Type", "application/json");

        when(aaiHttpClientConfigurationMock.aaiHost()).thenReturn("54.45.33.2");
        when(aaiHttpClientConfigurationMock.aaiProtocol()).thenReturn("https");
        when(aaiHttpClientConfigurationMock.aaiPort()).thenReturn(1234);
        when(aaiHttpClientConfigurationMock.aaiUserName()).thenReturn("Datafile");
        when(aaiHttpClientConfigurationMock.aaiUserPassword()).thenReturn("Datafile");
        when(aaiHttpClientConfigurationMock.aaiBasePath()).thenReturn("/aai/v11");
        when(aaiHttpClientConfigurationMock.aaiPnfPath()).thenReturn("/network/pnfs/pnf");
        when(aaiHttpClientConfigurationMock.aaiHeaders()).thenReturn(aaiHeaders);

        when(consumerDmaapModelMock.getPnfName()).thenReturn(PNF_NAME);

        testedObject = new AaiConsumerClient(aaiHttpClientConfigurationMock);
        setField();
    }


    @Test
    void getExtendedDetails_returnsSuccess() throws IOException {

        when(closeableHttpClientMock.execute(any(HttpGet.class), any(ResponseHandler.class)))
            .thenReturn(Optional.of(JSON_MESSAGE));
        Optional<String> actualResult = testedObject.getHttpResponse(consumerDmaapModelMock);
        Assertions.assertEquals(Optional.of(JSON_MESSAGE), actualResult);
    }


    private static void setField() throws NoSuchFieldException, IllegalAccessException {
        Field field = testedObject.getClass().getDeclaredField("closeableHttpClient");
        field.setAccessible(true);
        field.set(testedObject, closeableHttpClientMock);
    }
}
