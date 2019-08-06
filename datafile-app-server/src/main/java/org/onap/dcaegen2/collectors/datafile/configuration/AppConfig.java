/*-
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
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.model.logging.MappedDiagnosticContext;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClient;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClientFactory;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsRequests;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.CbsRequest;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.EnvProperties;
import org.onap.dcaegen2.services.sdk.rest.services.model.logging.RequestDiagnosticContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Holds all configuration for the DFC.
 *
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 3/23/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */

@Component
@ComponentScan("org.onap.dcaegen2.services.sdk.rest.services.cbs.client.providers")
@EnableConfigurationProperties
@ConfigurationProperties("app")
public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    private ConsumerConfiguration dmaapConsumerConfiguration;
    private Map<String, PublisherConfiguration> publishingConfigurations;
    private FtpesConfig ftpesConfiguration;
    @Value("#{systemEnvironment}")
    Properties systemEnvironment;
    private Disposable refreshConfigTask = null;

    @NotEmpty
    private String filepath;

    public synchronized void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    /**
     * Reads the cloud configuration.
     */
    public void initialize() {
        stop();
        Map<String, String> context = MappedDiagnosticContext.initializeTraceContext();
        loadConfigurationFromFile();

        refreshConfigTask = createRefreshTask(context) //
            .subscribe(e -> logger.info("Refreshed configuration data"),
                throwable -> logger.error("Configuration refresh terminated due to exception", throwable),
                () -> logger.error("Configuration refresh terminated"));
    }

    Flux<AppConfig> createRefreshTask(Map<String, String> context) {
        return getEnvironment(systemEnvironment, context).flatMap(this::createCbsClient)
            .flatMapMany(this::periodicConfigurationUpdates).map(this::parseCloudConfig)
            .onErrorResume(this::onErrorResume);
    }

    private Flux<JsonObject> periodicConfigurationUpdates(CbsClient cbsClient) {
        final Duration initialDelay = Duration.ZERO;
        final Duration refreshPeriod = Duration.ofMinutes(1);
        final CbsRequest getConfigRequest = CbsRequests.getAll(RequestDiagnosticContext.create());
        return cbsClient.updates(getConfigRequest, initialDelay, refreshPeriod);
    }

    /**
     * Stops the refreshing of the configuration.
     */
    public void stop() {
        if (refreshConfigTask != null) {
            refreshConfigTask.dispose();
            refreshConfigTask = null;
        }
    }

    public synchronized ConsumerConfiguration getDmaapConsumerConfiguration() {
        return dmaapConsumerConfiguration;
    }

    /**
     * Checks if there is a configuration for the given feed.
     *
     * @param changeIdentifier the change identifier the feed is configured to belong to.
     *
     * @return true if a feed is configured for the given change identifier, false if not.
     */
    public synchronized boolean isFeedConfigured(String changeIdentifier) {
        return publishingConfigurations.containsKey(changeIdentifier);
    }

    /**
     * Gets the feed configuration for the given change identifier.
     *
     * @param changeIdentifier the change identifier the feed is configured to belong to.
     * @return the <code>PublisherConfiguration</code> for the feed belonging to the given change identifier.
     *
     * @throws DatafileTaskException if no configuration has been loaded or the configuration is missing for the given
     *         change identifier.
     */
    public synchronized PublisherConfiguration getPublisherConfiguration(String changeIdentifier)
        throws DatafileTaskException {

        if (publishingConfigurations == null) {
            throw new DatafileTaskException("No PublishingConfiguration loaded, changeIdentifier: " + changeIdentifier);
        }
        PublisherConfiguration cfg = publishingConfigurations.get(changeIdentifier);
        if (cfg == null) {
            throw new DatafileTaskException(
                "Cannot find getPublishingConfiguration for changeIdentifier: " + changeIdentifier);
        }
        return cfg;
    }

    public synchronized FtpesConfig getFtpesConfiguration() {
        return ftpesConfiguration;
    }

    private <R> Mono<R> onErrorResume(Throwable trowable) {
        logger.error("Could not refresh application configuration {}", trowable.toString());
        return Mono.empty();
    }

    Mono<EnvProperties> getEnvironment(Properties systemEnvironment, Map<String, String> context) {
        return EnvironmentProcessor.readEnvironmentVariables(systemEnvironment, context);
    }

    Mono<CbsClient> createCbsClient(EnvProperties env) {
        return CbsClientFactory.createCbsClient(env);
    }

    /**
     * Parse configuration.
     *
     * @param jsonObject the DFC service's configuration
     * @return this which is updated if successful
     */
    private AppConfig parseCloudConfig(JsonObject jsonObject) {
        try {
            CloudConfigParser parser = new CloudConfigParser(jsonObject);
            setConfiguration(parser.getDmaapConsumerConfig(), parser.getDmaapPublisherConfigurations(),
                parser.getFtpesConfig());

        } catch (DatafileTaskException e) {
            logger.error("Could not parse configuration {}", e.toString(), e);
        }
        return this;
    }

    /**
     * Reads the configuration from file.
     */
    void loadConfigurationFromFile() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        ServiceLoader.load(TypeAdapterFactory.class).forEach(gsonBuilder::registerTypeAdapterFactory);

        try (InputStream inputStream = createInputStream(filepath)) {
            JsonParser parser = new JsonParser();
            JsonObject rootObject = getJsonElement(parser, inputStream).getAsJsonObject();
            if (rootObject == null) {
                throw new JsonSyntaxException("Root is not a json object");
            }
            parseCloudConfig(rootObject);
        } catch (JsonSyntaxException | IOException e) {
            logger.warn("Local configuration file not loaded: {}", filepath, e);
        }
    }

    private synchronized void setConfiguration(ConsumerConfiguration consumerConfiguration,
        Map<String, PublisherConfiguration> publisherConfigurations, FtpesConfig ftpesConfig) {
        this.dmaapConsumerConfiguration = consumerConfiguration;
        this.publishingConfigurations = publisherConfigurations;
        this.ftpesConfiguration = ftpesConfig;
    }

    JsonElement getJsonElement(JsonParser parser, InputStream inputStream) {
        return parser.parse(new InputStreamReader(inputStream));
    }

    InputStream createInputStream(@NotNull String filepath) throws IOException {
        return new BufferedInputStream(new FileInputStream(filepath));
    }

}
