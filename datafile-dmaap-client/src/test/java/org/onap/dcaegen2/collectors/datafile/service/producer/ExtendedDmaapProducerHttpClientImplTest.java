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
package org.onap.dcaegen2.collectors.datafile.service.producer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Optional;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModelForUnitTest;
import org.onap.dcaegen2.collectors.datafile.service.producer.ExtendedDmaapProducerHttpClientImpl;


public class ExtendedDmaapProducerHttpClientImplTest {

    private static ExtendedDmaapProducerHttpClientImpl objectUnderTest;
    private static DmaapPublisherConfiguration configurationMock = mock(DmaapPublisherConfiguration.class);
    private static CloseableHttpClient closeableHttpClientMock = mock(CloseableHttpClient.class);
    private static ConsumerDmaapModel consumerDmaapModel = new ConsumerDmaapModelForUnitTest();
    private static Integer expectedResult;
    private static final Integer RESPONSE_SUCCESS = 200;
    private static final Integer RESPONSE_FAILURE = 404;

    @BeforeAll
    public static void init() throws NoSuchFieldException, IllegalAccessException {
        when(configurationMock.dmaapHostName()).thenReturn("54.45.33.2");
        when(configurationMock.dmaapProtocol()).thenReturn("https");
        when(configurationMock.dmaapPortNumber()).thenReturn(1234);
        when(configurationMock.dmaapUserName()).thenReturn("DFC");
        when(configurationMock.dmaapUserPassword()).thenReturn("DFC");
        when(configurationMock.dmaapContentType()).thenReturn("application/octet-stream");
        when(configurationMock.dmaapTopicName()).thenReturn("fileReady");
        objectUnderTest = new ExtendedDmaapProducerHttpClientImpl(configurationMock);
        setField();
    }


    @Test
    public void getHttpResponsePost_success() throws IOException {
        expectedResult = RESPONSE_SUCCESS;
        when(closeableHttpClientMock.execute(any(HttpPost.class), any(ResponseHandler.class)))
                .thenReturn(Optional.of(expectedResult));
        Optional<Integer> actualResult = objectUnderTest.getHttpProducerResponse(consumerDmaapModel);
        Assertions.assertEquals(expectedResult, actualResult.get());
    }

    @Test
    public void getExtendedDetails_returnsFailure() throws IOException {
        expectedResult = RESPONSE_FAILURE;
        when(closeableHttpClientMock.execute(any(HttpPost.class), any(ResponseHandler.class)))
                .thenReturn(Optional.of(expectedResult));
        Optional<Integer> actualResult = objectUnderTest.getHttpProducerResponse(consumerDmaapModel);
        Assertions.assertEquals(expectedResult, actualResult.get());
    }

    private static void setField() throws NoSuchFieldException, IllegalAccessException {
        Field field = objectUnderTest.getClass().getDeclaredField("closeableHttpClient");
        field.setAccessible(true);
        field.set(objectUnderTest, closeableHttpClientMock);
    }
}
