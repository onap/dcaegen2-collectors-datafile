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

package org.onap.dcaegen2.services.prh.configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.dcaegen2.services.prh.integration.junit5.mockito.MockitoExtension;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 4/9/18
 */
@ExtendWith({MockitoExtension.class})
class PrhAppConfigTest {

    private static final String PRH_ENDPOINTS = "prh_endpoints.json";
    private static final String jsonString = "{\"configs\":{\"aai\":{\"aaiClientConfiguration\":{\"aaiHost\":"
        + "\"localhost\",\"aaiPort\":8080,\"aaiIgnoreSslCertificateErrors\":true,\"aaiProtocol\":"
        + "\"https\",\"aaiUserName\":\"admin\",\"aaiUserPassword\":\"admin\",\"aaiBasePath\":\"/aai/v11\","
        + "\"aaiPnfPath\":\"/network/pnfs/pnf\",\"aaiHeaders\":{\"X-FromAppId\":\"prh\",\"X-TransactionId\":\"9999\","
        + "\"Accept\":\"application/json\",\"Real-Time\":\"true\",\"Content-Type\":\"application/merge-patch+json\","
        + "\"Authorization\":\"Basic QUFJOkFBSQ==\"}}},"
        + "\"dmaap\":{\"dmaapConsumerConfiguration\":{\"consumerGroup\":\"other\",\"consumerId\":\"1\","
        + "\"dmaapContentType\":\"application/json\",\"dmaapHostName\":\"localhost\",\"dmaapPortNumber\":2222,"
        + "\"dmaapProtocol\":\"http\",\"dmaapTopicName\":\"temp\",\"dmaapUserName\":\"admin\",\"dmaapUserPassword\""
        + ":\"admin\",\"messageLimit\":1000,\"timeoutMs\":1000},\"dmaapProducerConfiguration\":{\"dmaapContentType\":"
        + "\"application/json\",\"dmaapHostName\":\"localhost\",\"dmaapPortNumber\":2223,\"dmaapProtocol\":\"http\","
        + "\"dmaapTopicName\":\"temp\",\"dmaapUserName\":\"admin\",\"dmaapUserPassword\":\"admin\"}}}}";

    private static final String incorrectJsonString = "{\"configs\":{\"aai\":{\"aaiClientConfiguration\":{\"aaiHost\":"
        + "\"localhost\",\"aaiPort\":8080,\"aaiIgnoreSslCertificateErrors\":true,\"aaiProtocol\":\"https\","
        + "\"aaiUserName\":\"admin\",\"aaiUserPassword\":\"admin\",\"aaiBasePath\":\"/aai/v11\",\"aaiPnfPath\":"
        + "\"/network/pnfs/pnf\",\"aaiHeaders\":{\"X-FromAppId\":\"prh\",\"X-TransactionId\":\"9999\",\"Accept\":"
        + "\"application/json\",\"Real-Time\":\"true\",\"Content-Type\":\"application/merge-patch+json\","
        + "\"Authorization\":\"Basic QUFJOkFBSQ==\"}}},\"dmaap\""
        + ":{\"dmaapConsumerConfiguration\":{\"consumerGroup\":\"other\",\"consumerId\":\"1\",\"dmaapContentType\""
        + ":\"application/json\",\"dmaapHostName\":\"localhost\",\"dmaapPortNumber\":2222,\"dmaapProtocol\":\"http\""
        + ",\"dmaapTopicName\":\"temp\",\"dmaapUserName\":\"admin\",\"dmaapUserPassword\":\"admin\",\"messageLimit\""
        + ":1000,\"timeoutMs\":1000},\"dmaapProducerConfiguration\":{\"dmaapContentType\":\"application/json\","
        + "\"dmaapHostName\":\"localhost\",\"dmaapPortNumber\":2223,\"dmaapProtocol\":\"http\",\"dmaaptopicName\""
        + ":\"temp\",\"dmaapuserName\":\"admin\",\"dmaapuserPassword\":\"admin\"}}}}";

    private static PrhAppConfig prhAppConfig;
    private static AppConfig appConfig;

    private static String filePath = Objects
        .requireNonNull(PrhAppConfigTest.class.getClassLoader().getResource(PRH_ENDPOINTS)).getFile();

    @BeforeEach
    void setUp() {
        prhAppConfig = spy(PrhAppConfig.class);
        appConfig = spy(new AppConfig());
    }

    @Test
    void whenApplicationWasStarted_FilePathIsSet() {
        //
        // When
        //
        prhAppConfig.setFilepath(filePath);
        //
        // Then
        //
        verify(prhAppConfig, times(1)).setFilepath(anyString());
        verify(prhAppConfig, times(0)).initFileStreamReader();
        Assertions.assertEquals(filePath, prhAppConfig.getFilepath());
    }

    @Test
    void whenTheConfigurationFits_GetAaiAndDmaapObjectRepresentationConfiguration()
        throws IOException {
        //
        // Given
        //
        InputStream inputStream = new ByteArrayInputStream((jsonString.getBytes(
            StandardCharsets.UTF_8)));
        //
        // When
        //
        prhAppConfig.setFilepath(filePath);
        doReturn(inputStream).when(prhAppConfig).getInputStream(any());
        prhAppConfig.initFileStreamReader();
        appConfig.dmaapConsumerConfiguration = prhAppConfig.getDmaapConsumerConfiguration();
        appConfig.dmaapPublisherConfiguration = prhAppConfig.getDmaapPublisherConfiguration();
        appConfig.aaiClientConfiguration = prhAppConfig.getAaiClientConfiguration();
        //
        // Then
        //
        verify(prhAppConfig, times(1)).setFilepath(anyString());
        verify(prhAppConfig, times(1)).initFileStreamReader();
        Assertions.assertNotNull(prhAppConfig.getAaiClientConfiguration());
        Assertions.assertNotNull(prhAppConfig.getDmaapConsumerConfiguration());
        Assertions.assertNotNull(prhAppConfig.getDmaapPublisherConfiguration());
        Assertions
            .assertEquals(appConfig.getDmaapPublisherConfiguration(), prhAppConfig.getDmaapPublisherConfiguration());
        Assertions
            .assertEquals(appConfig.getDmaapConsumerConfiguration(), prhAppConfig.getDmaapConsumerConfiguration());
        Assertions
            .assertEquals(appConfig.getAaiClientConfiguration(), prhAppConfig.getAaiClientConfiguration());

    }

    @Test
    void whenFileIsNotExist_ThrowIoException() {
        //
        // Given
        //
        filePath = "/temp.json";
        prhAppConfig.setFilepath(filePath);
        //
        // When
        //
        prhAppConfig.initFileStreamReader();
        //
        // Then
        //
        verify(prhAppConfig, times(1)).setFilepath(anyString());
        verify(prhAppConfig, times(1)).initFileStreamReader();
        Assertions.assertNull(prhAppConfig.getAaiClientConfiguration());
        Assertions.assertNull(prhAppConfig.getDmaapConsumerConfiguration());
        Assertions.assertNull(prhAppConfig.getDmaapPublisherConfiguration());

    }

    @Test
    void whenFileIsExistsButJsonIsIncorrect() throws IOException {
        //
        // Given
        //
        InputStream inputStream = new ByteArrayInputStream((incorrectJsonString.getBytes(
            StandardCharsets.UTF_8)));
        //
        // When
        //
        prhAppConfig.setFilepath(filePath);
        doReturn(inputStream).when(prhAppConfig).getInputStream(any());
        prhAppConfig.initFileStreamReader();

        //
        // Then
        //
        verify(prhAppConfig, times(1)).setFilepath(anyString());
        verify(prhAppConfig, times(1)).initFileStreamReader();
        Assertions.assertNotNull(prhAppConfig.getAaiClientConfiguration());
        Assertions.assertNotNull(prhAppConfig.getDmaapConsumerConfiguration());
        Assertions.assertNull(prhAppConfig.getDmaapPublisherConfiguration());

    }


    @Test
    void whenTheConfigurationFits_ButRootElementIsNotAJsonObject()
        throws IOException {
        // Given
        InputStream inputStream = new ByteArrayInputStream((jsonString.getBytes(
            StandardCharsets.UTF_8)));
        // When
        prhAppConfig.setFilepath(filePath);
        doReturn(inputStream).when(prhAppConfig).getInputStream(any());
        JsonElement jsonElement = mock(JsonElement.class);
        when(jsonElement.isJsonObject()).thenReturn(false);
        doReturn(jsonElement).when(prhAppConfig).getJsonElement(any(JsonParser.class), any(InputStream.class));
        prhAppConfig.initFileStreamReader();
        appConfig.dmaapConsumerConfiguration = prhAppConfig.getDmaapConsumerConfiguration();
        appConfig.dmaapPublisherConfiguration = prhAppConfig.getDmaapPublisherConfiguration();
        appConfig.aaiClientConfiguration = prhAppConfig.getAaiClientConfiguration();

        // Then
        verify(prhAppConfig, times(1)).setFilepath(anyString());
        verify(prhAppConfig, times(1)).initFileStreamReader();
        Assertions.assertNull(prhAppConfig.getAaiClientConfiguration());
        Assertions.assertNull(prhAppConfig.getDmaapConsumerConfiguration());
        Assertions.assertNull(prhAppConfig.getDmaapPublisherConfiguration());
    }
}