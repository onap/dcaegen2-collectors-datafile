/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018-2019 Nordix Foundation. All rights reserved.
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import org.onap.dcaegen2.collectors.datafile.integration.junit5.mockito.MockitoExtension;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 4/9/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
@ExtendWith({MockitoExtension.class})
class AppConfigTest {

    private static final String DATAFILE_ENDPOINTS = "datafile_endpoints.json";
    private static final boolean CORRECT_JSON = true;
    private static final boolean INCORRECT_JSON = false;

    private static AppConfig appConfigUnderTest;


    private static String filePath =
            Objects.requireNonNull(AppConfigTest.class.getClassLoader().getResource(DATAFILE_ENDPOINTS)).getFile();

    @BeforeEach
    public void setUp() {
        appConfigUnderTest = spy(AppConfig.class);
    }

    @Test
    public void whenApplicationWasStarted_FilePathIsSet() {
        // When
        appConfigUnderTest.setFilepath(filePath);

        // Then
        verify(appConfigUnderTest, times(1)).setFilepath(anyString());
        verify(appConfigUnderTest, times(0)).initFileStreamReader();
        Assertions.assertEquals(filePath, appConfigUnderTest.getFilepath());
    }

    @Test
    public void whenTheConfigurationFits_GetFtpsAndDmaapObjectRepresentationConfiguration() throws IOException {
        // Given
        InputStream inputStream =
                new ByteArrayInputStream((getJsonConfig(CORRECT_JSON).getBytes(StandardCharsets.UTF_8)));

        // When
        appConfigUnderTest.setFilepath(filePath);
        doReturn(inputStream).when(appConfigUnderTest).getInputStream(any());
        appConfigUnderTest.initFileStreamReader();
        appConfigUnderTest.dmaapConsumerConfiguration = appConfigUnderTest.getDmaapConsumerConfiguration();
        appConfigUnderTest.dmaapPublisherConfiguration = appConfigUnderTest.getDmaapPublisherConfiguration();
        appConfigUnderTest.ftpesConfig = appConfigUnderTest.getFtpesConfiguration();

        // Then
        verify(appConfigUnderTest, times(1)).setFilepath(anyString());
        verify(appConfigUnderTest, times(1)).initFileStreamReader();
        Assertions.assertNotNull(appConfigUnderTest.getDmaapConsumerConfiguration());
        Assertions.assertNotNull(appConfigUnderTest.getDmaapPublisherConfiguration());
        Assertions.assertEquals(appConfigUnderTest.getDmaapPublisherConfiguration(),
                appConfigUnderTest.getDmaapPublisherConfiguration());
        Assertions.assertEquals(appConfigUnderTest.getDmaapConsumerConfiguration(),
                appConfigUnderTest.getDmaapConsumerConfiguration());
        Assertions.assertEquals(appConfigUnderTest.getFtpesConfiguration(), appConfigUnderTest.getFtpesConfiguration());

    }

    @Test
    public void whenFileIsNotExist_ThrowIoException() {
        // Given
        filePath = "/temp.json";
        appConfigUnderTest.setFilepath(filePath);

        // When
        appConfigUnderTest.initFileStreamReader();

        // Then
        verify(appConfigUnderTest, times(1)).setFilepath(anyString());
        verify(appConfigUnderTest, times(1)).initFileStreamReader();
        Assertions.assertNull(appConfigUnderTest.getDmaapConsumerConfiguration());
        Assertions.assertNull(appConfigUnderTest.getDmaapPublisherConfiguration());
        Assertions.assertNull(appConfigUnderTest.getFtpesConfiguration());

    }

    @Test
    public void whenFileIsExistsButJsonIsIncorrect() throws IOException {
        // Given
        InputStream inputStream =
                new ByteArrayInputStream((getJsonConfig(INCORRECT_JSON).getBytes(StandardCharsets.UTF_8)));

        // When
        appConfigUnderTest.setFilepath(filePath);
        doReturn(inputStream).when(appConfigUnderTest).getInputStream(any());
        appConfigUnderTest.initFileStreamReader();

        // Then
        verify(appConfigUnderTest, times(1)).setFilepath(anyString());
        verify(appConfigUnderTest, times(1)).initFileStreamReader();
        Assertions.assertNotNull(appConfigUnderTest.getDmaapConsumerConfiguration());
        Assertions.assertNull(appConfigUnderTest.getDmaapPublisherConfiguration());
        Assertions.assertNotNull(appConfigUnderTest.getFtpesConfiguration());

    }


    @Test
    public void whenTheConfigurationFits_ButRootElementIsNotAJsonObject() throws IOException {
        // Given
        InputStream inputStream =
                new ByteArrayInputStream((getJsonConfig(CORRECT_JSON).getBytes(StandardCharsets.UTF_8)));
        // When
        appConfigUnderTest.setFilepath(filePath);
        doReturn(inputStream).when(appConfigUnderTest).getInputStream(any());
        JsonElement jsonElement = mock(JsonElement.class);
        when(jsonElement.isJsonObject()).thenReturn(false);
        doReturn(jsonElement).when(appConfigUnderTest).getJsonElement(any(JsonParser.class), any(InputStream.class));
        appConfigUnderTest.initFileStreamReader();
        appConfigUnderTest.dmaapConsumerConfiguration = appConfigUnderTest.getDmaapConsumerConfiguration();
        appConfigUnderTest.dmaapPublisherConfiguration = appConfigUnderTest.getDmaapPublisherConfiguration();
        appConfigUnderTest.ftpesConfig = appConfigUnderTest.getFtpesConfiguration();

        // Then
        verify(appConfigUnderTest, times(1)).setFilepath(anyString());
        verify(appConfigUnderTest, times(1)).initFileStreamReader();
        Assertions.assertNull(appConfigUnderTest.getDmaapConsumerConfiguration());
        Assertions.assertNull(appConfigUnderTest.getDmaapPublisherConfiguration());
        Assertions.assertNull(appConfigUnderTest.getFtpesConfiguration());
    }

    private String getJsonConfig(boolean correct) {
        JsonObject dmaapConsumerConfigData = new JsonObject();
        dmaapConsumerConfigData.addProperty("dmaapHostName", "localhost");
        dmaapConsumerConfigData.addProperty("dmaapPortNumber", 2222);
        dmaapConsumerConfigData.addProperty("dmaapTopicName", "/events/unauthenticated.VES_NOTIFICATION_OUTPUT");
        dmaapConsumerConfigData.addProperty("dmaapProtocol", "http");
        dmaapConsumerConfigData.addProperty("dmaapUserName", "admin");
        dmaapConsumerConfigData.addProperty("dmaapUserPassword", "admin");
        dmaapConsumerConfigData.addProperty("dmaapContentType", "application/json");
        dmaapConsumerConfigData.addProperty("consumerId", "C12");
        dmaapConsumerConfigData.addProperty("consumerGroup", "OpenDcae-c12");
        dmaapConsumerConfigData.addProperty("timeoutMs", -1);
        dmaapConsumerConfigData.addProperty("messageLimit", 1);

        JsonObject dmaapProducerConfigData = new JsonObject();
        dmaapProducerConfigData.addProperty("dmaapHostName", "localhost");
        dmaapProducerConfigData.addProperty("dmaapPortNumber", 3907);
        dmaapProducerConfigData.addProperty("dmaapTopicName", "publish");
        dmaapProducerConfigData.addProperty("dmaapProtocol", "https");
        if (correct) {
            dmaapProducerConfigData.addProperty("dmaapUserName", "dradmin");
            dmaapProducerConfigData.addProperty("dmaapUserPassword", "dradmin");
            dmaapProducerConfigData.addProperty("dmaapContentType", "application/octet-stream");
        }

        JsonObject dmaapConfigs = new JsonObject();
        dmaapConfigs.add("dmaapConsumerConfiguration", dmaapConsumerConfigData);
        dmaapConfigs.add("dmaapProducerConfiguration", dmaapProducerConfigData);

        JsonObject ftpesConfigData = new JsonObject();
        ftpesConfigData.addProperty("keyCert", "config/ftpKey.jks");
        ftpesConfigData.addProperty("keyPassword", "secret");
        ftpesConfigData.addProperty("trustedCA", "config/cacerts");
        ftpesConfigData.addProperty("trustedCAPassword", "secret");

        JsonObject security = new JsonObject();
        security.addProperty("trustStorePath", "trustStorePath");
        security.addProperty("trustStorePasswordPath", "trustStorePasswordPath");
        security.addProperty("keyStorePath", "keyStorePath");
        security.addProperty("keyStorePasswordPath", "keyStorePasswordPath");
        security.addProperty("enableDmaapCertAuth", "enableDmaapCertAuth");

        JsonObject ftpesConfiguration = new JsonObject();
        ftpesConfiguration.add("ftpesConfiguration", ftpesConfigData);

        JsonObject configs = new JsonObject();
        configs.add("dmaap", dmaapConfigs);
        configs.add("ftp", ftpesConfiguration);
        configs.add("security", security);

        JsonObject completeJson = new JsonObject();
        completeJson.add("configs", configs);

        return completeJson.toString();
    }
}
