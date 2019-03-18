/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018-2019 Nordix Foundation. All rights reserved.
 * ===============================================================================================
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * The DFC application configuration.
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
    private static final String DMAAP_CONSUMER = "dmaapConsumerConfiguration";
    private static final String DMAAP_BUSCONTROLLER = "dmaapBusControllerConfiguration";
    private static final String FTP = "ftp";
    private static final String FTPES_CONFIGURATION = "ftpesConfiguration";
    private static final String SECURITY = "security";
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    DmaapConsumerConfiguration dmaapConsumerConfiguration;

    DmaapBusControllerConfiguration dmaapBusControllerConfiguration;

    FtpesConfig ftpesConfig;

    @NotEmpty
    private String filepath;

    public DmaapConsumerConfiguration getDmaapConsumerConfiguration() {
        return dmaapConsumerConfiguration;
    }

    public DmaapBusControllerConfiguration getDmaapBusControllerConfiguration() {
        return dmaapBusControllerConfiguration;
    }

    public FtpesConfig getFtpesConfiguration() {
        return ftpesConfig;
    }

    /**
     * Reads the configuration from file.
     */
    public void initFileStreamReader() {

        GsonBuilder gsonBuilder = new GsonBuilder();
        ServiceLoader.load(TypeAdapterFactory.class).forEach(gsonBuilder::registerTypeAdapterFactory);
        JsonParser parser = new JsonParser();
        JsonObject jsonObject;
        try (InputStream inputStream = getInputStream(filepath)) {
            JsonElement rootElement = getJsonElement(parser, inputStream);
            if (rootElement.isJsonObject()) {
                jsonObject = rootElement.getAsJsonObject();
                ftpesConfig = deserializeType(gsonBuilder,
                        jsonObject.getAsJsonObject(CONFIG).getAsJsonObject(FTP).getAsJsonObject(FTPES_CONFIGURATION),
                        FtpesConfig.class);
                dmaapConsumerConfiguration = deserializeType(gsonBuilder, concatenateJsonObjects(
                        jsonObject.getAsJsonObject(CONFIG).getAsJsonObject(DMAAP).getAsJsonObject(DMAAP_CONSUMER),
                        rootElement.getAsJsonObject().getAsJsonObject(CONFIG).getAsJsonObject(SECURITY)),
                        DmaapConsumerConfiguration.class);

                dmaapBusControllerConfiguration = deserializeType(gsonBuilder, concatenateJsonObjects(
                        jsonObject.getAsJsonObject(CONFIG).getAsJsonObject(DMAAP).getAsJsonObject(DMAAP_BUSCONTROLLER),
                        rootElement.getAsJsonObject().getAsJsonObject(CONFIG).getAsJsonObject(SECURITY)),
                        DmaapBusControllerConfiguration.class);
            }
        } catch (IOException e) {
            logger.error("Problem with file loading, file: {}", filepath, e);
        } catch (JsonSyntaxException e) {
            logger.error("Problem with Json deserialization", e);
        }
    }

    JsonElement getJsonElement(JsonParser parser, InputStream inputStream) {
        return parser.parse(new InputStreamReader(inputStream));
    }

    private <T> T deserializeType(@NotNull GsonBuilder gsonBuilder, @NotNull JsonObject jsonObject,
                                  @NotNull Class<T> type) {
        return gsonBuilder.create().fromJson(jsonObject, type);
    }

    InputStream getInputStream(@NotNull String filepath) throws IOException {
        return new BufferedInputStream(new FileInputStream(filepath));
    }

    String getFilepath() {
        return this.filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    private JsonObject concatenateJsonObjects(JsonObject target, JsonObject source) {
        source.entrySet()
                .forEach(entry -> target.add(entry.getKey(), entry.getValue()));
        return target;
    }

}
