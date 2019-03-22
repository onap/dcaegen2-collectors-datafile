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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapterFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ServiceLoader;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapConsumerConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapPublisherConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Holds all configuration for the DFC.
 *
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 3/23/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */

@Component
@EnableConfigurationProperties
@ConfigurationProperties("app")
public class AppConfig {

    private static final String CONFIG = "configs";
    private static final String DMAAP = "dmaap";
    private static final String DMAAP_PRODUCER = "dmaapProducerConfiguration";
    private static final String DMAAP_CONSUMER = "dmaapConsumerConfiguration";
    private static final String FTP = "ftp";
    private static final String FTPES_CONFIGURATION = "ftpesConfiguration";
    private static final String SECURITY = "security";
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    private DmaapConsumerConfiguration dmaapConsumerConfiguration;
    private DmaapPublisherConfiguration dmaapPublisherConfiguration;
    private FtpesConfig ftpesConfiguration;

    @NotEmpty
    private String filepath;

    public synchronized DmaapConsumerConfiguration getDmaapConsumerConfiguration() {
        return dmaapConsumerConfiguration;
    }

    public synchronized DmaapPublisherConfiguration getDmaapPublisherConfiguration() {
        return dmaapPublisherConfiguration;
    }

    public synchronized FtpesConfig getFtpesConfiguration() {
        return ftpesConfiguration;
    }

    /**
     * Reads the configuration from file.
     */
    public void loadConfigurationFromFile() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        ServiceLoader.load(TypeAdapterFactory.class).forEach(gsonBuilder::registerTypeAdapterFactory);
        JsonParser parser = new JsonParser();
        JsonObject jsonObject;
        try (InputStream inputStream = createInputStream(filepath)) {
            JsonElement rootElement = getJsonElement(parser, inputStream);
            if (rootElement.isJsonObject()) {
                jsonObject = rootElement.getAsJsonObject();
                FtpesConfig ftpesConfig = deserializeType(gsonBuilder,
                        jsonObject.getAsJsonObject(CONFIG).getAsJsonObject(FTP).getAsJsonObject(FTPES_CONFIGURATION),
                        FtpesConfig.class);
                DmaapConsumerConfiguration consumerConfiguration = deserializeType(gsonBuilder,
                        concatenateJsonObjects(
                                jsonObject.getAsJsonObject(CONFIG).getAsJsonObject(DMAAP)
                                        .getAsJsonObject(DMAAP_CONSUMER),
                                rootElement.getAsJsonObject().getAsJsonObject(CONFIG).getAsJsonObject(SECURITY)),
                        DmaapConsumerConfiguration.class);

                DmaapPublisherConfiguration publisherConfiguration = deserializeType(gsonBuilder,
                        concatenateJsonObjects(
                                jsonObject.getAsJsonObject(CONFIG).getAsJsonObject(DMAAP)
                                        .getAsJsonObject(DMAAP_PRODUCER),
                                rootElement.getAsJsonObject().getAsJsonObject(CONFIG).getAsJsonObject(SECURITY)),
                        DmaapPublisherConfiguration.class);

                setConfiguration(consumerConfiguration, publisherConfiguration, ftpesConfig);
            }
        } catch (JsonSyntaxException | IOException e) {
            logger.error("Problem with loading configuration, file: {}", filepath, e);
        }
    }

    synchronized void setConfiguration(DmaapConsumerConfiguration consumerConfiguration,
            DmaapPublisherConfiguration publisherConfiguration, FtpesConfig ftpesConfig) {
        this.dmaapConsumerConfiguration = consumerConfiguration;
        this.dmaapPublisherConfiguration = publisherConfiguration;
        this.ftpesConfiguration = ftpesConfig;
    }

    JsonElement getJsonElement(JsonParser parser, InputStream inputStream) {
        return parser.parse(new InputStreamReader(inputStream));
    }

    private <T> T deserializeType(@NotNull GsonBuilder gsonBuilder, @NotNull JsonObject jsonObject,
            @NotNull Class<T> type) {
        return gsonBuilder.create().fromJson(jsonObject, type);
    }

    InputStream createInputStream(@NotNull String filepath) throws IOException {
        return new BufferedInputStream(new FileInputStream(filepath));
    }

    synchronized String getFilepath() {
        return this.filepath;
    }

    public synchronized void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    private JsonObject concatenateJsonObjects(JsonObject target, JsonObject source) {
        source.entrySet().forEach(entry -> target.add(entry.getKey(), entry.getValue()));
        return target;
    }

}
