/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 Nordix Foundation. All rights reserved.
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
package org.onap.dcaegen2.collectors.datafile.service.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Optional;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.config.DmaapConsumerConfiguration;
import org.onap.dcaegen2.collectors.datafile.service.consumer.ExtendedDmaapConsumerHttpClientImpl;

public class ExtendedDmaapConsumerHttpClientImplTest {

    private static ExtendedDmaapConsumerHttpClientImpl objectUnderTest;

    private static DmaapConsumerConfiguration configurationMock = mock(DmaapConsumerConfiguration.class);
    private static CloseableHttpClient closeableHttpClientMock = mock(CloseableHttpClient.class);

    private static final String JSON_MESSAGE = "{ \"responseFromDmaap\": \"Success\" }";

    private static Optional<String> expectedResult = Optional.empty();

    @BeforeAll
    public static void init() throws NoSuchFieldException, IllegalAccessException {

        when(configurationMock.dmaapHostName()).thenReturn("54.45.33.2");
        when(configurationMock.dmaapProtocol()).thenReturn("https");
        when(configurationMock.dmaapPortNumber()).thenReturn(1234);
        when(configurationMock.dmaapUserName()).thenReturn("DFC");
        when(configurationMock.dmaapUserPassword()).thenReturn("DFC");
        when(configurationMock.dmaapContentType()).thenReturn("application/json");
        when(configurationMock.dmaapTopicName()).thenReturn("unauthenticated.SEC_OTHER_OUTPUT");
        when(configurationMock.consumerGroup()).thenReturn("OpenDCAE-c12");
        when(configurationMock.consumerId()).thenReturn("c12");

        objectUnderTest = new ExtendedDmaapConsumerHttpClientImpl(configurationMock);

        setField();
    }


    @Test
    public void getHttpResponseGet_success() throws IOException {
        expectedResult = Optional.of(JSON_MESSAGE);

        when(closeableHttpClientMock.execute(any(HttpGet.class), any(ResponseHandler.class)))
                .thenReturn(expectedResult);

        Optional<String> actualResult = objectUnderTest.getHttpConsumerResponse();
        System.out.println(actualResult.toString());
        Assertions.assertEquals(expectedResult.get(), actualResult.get());
    }

    @Test
    public void getExtendedDetails_returnsNull() throws IOException {
        when(closeableHttpClientMock.execute(any(HttpGet.class), any(ResponseHandler.class)))
                .thenReturn(Optional.empty());
        Optional<String> actualResult = objectUnderTest.getHttpConsumerResponse();
        Assertions.assertEquals(Optional.empty(), actualResult);
    }


    private static void setField() throws NoSuchFieldException, IllegalAccessException {
        Field field = objectUnderTest.getClass().getDeclaredField("closeableHttpClient");
        field.setAccessible(true);
        field.set(objectUnderTest, closeableHttpClientMock);
    }
}
