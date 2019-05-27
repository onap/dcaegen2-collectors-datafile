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
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.http.configuration.EnvProperties;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.http.configuration.ImmutableEnvProperties;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.providers.CloudConfigurationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private Map<String, PublisherConfiguration> publishingConfiguration;
    private FtpesConfig ftpesConfiguration;
    private CloudConfigurationProvider cloudConfigurationProvider;
    @Value("#{systemEnvironment}")
    Properties systemEnvironment;
    private Disposable refreshConfigTask = null;

    @NotEmpty
    private String filepath;

    @Autowired
    public synchronized void setCloudConfigurationProvider(
            CloudConfigurationProvider reactiveCloudConfigurationProvider) {
        this.cloudConfigurationProvider = reactiveCloudConfigurationProvider;
    }

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

        refreshConfigTask = Flux.interval(Duration.ZERO, Duration.ofMinutes(5))
                .flatMap(count -> createRefreshConfigurationTask(count, context))
                .subscribe(e -> logger.info("Refreshed configuration data"),
                        throwable -> logger.error("Configuration refresh terminated due to exception", throwable),
                        () -> logger.error("Configuration refresh terminated"));
    }

    public void stop() {
        if (refreshConfigTask != null) {
            refreshConfigTask.dispose();
            refreshConfigTask = null;
        }
    }

    public synchronized ConsumerConfiguration getDmaapConsumerConfiguration() {
        return dmaapConsumerConfiguration;
    }

    public synchronized boolean isFeedConfigured(String changeIdentifier)
    {
       return publishingConfiguration.containsKey(changeIdentifier);
    }

    public synchronized PublisherConfiguration getPublisherConfiguration(String changeIdentifier)
            throws DatafileTaskException {

        if (publishingConfiguration == null) {
            throw new DatafileTaskException("No PublishingConfiguration loaded, changeIdentifier: " + changeIdentifier);
        }
        PublisherConfiguration cfg = publishingConfiguration.get(changeIdentifier);
        if (cfg == null) {
            throw new DatafileTaskException(
                    "Cannot find getPublishingConfiguration for changeIdentifier: " + changeIdentifier);
        }
        return cfg;
    }

    public synchronized FtpesConfig getFtpesConfiguration() {
        return ftpesConfiguration;
    }

    Flux<AppConfig> createRefreshConfigurationTask(Long counter, Map<String, String> context) {
        return Flux.just(counter) //
                .doOnNext(cnt -> logger.debug("Refresh config {}", cnt)) //
                .flatMap(cnt -> readEnvironmentVariables(systemEnvironment, context)) //
                .flatMap(this::fetchConfiguration);
    }

    Mono<EnvProperties> readEnvironmentVariables(Properties systemEnvironment, Map<String, String> context) {
        return EnvironmentProcessor.readEnvironmentVariables(systemEnvironment, context)
                .onErrorResume(this::onErrorResume);
    }

    private <R> Mono<R> onErrorResume(Throwable trowable) {
        logger.error("Could not refresh application configuration {}", trowable.toString());
        return Mono.empty();
    }

    private Mono<AppConfig> fetchConfiguration(EnvProperties env) {
        Mono<JsonObject> serviceCfg = cloudConfigurationProvider.callForServiceConfigurationReactive(env) //
                .onErrorResume(this::onErrorResume);

        // Note, have to use this callForServiceConfigurationReactive with EnvProperties, since the
        // other ones does not work
        EnvProperties dmaapEnv = ImmutableEnvProperties.builder() //
                .consulHost(env.consulHost()) //
                .consulPort(env.consulPort()) //
                .cbsName(env.cbsName()) //
                .appName(env.appName() + ":dmaap") //
                .build(); //
        Mono<JsonObject> dmaapCfg = cloudConfigurationProvider.callForServiceConfigurationReactive(dmaapEnv)
                .onErrorResume(t -> Mono.just(new JsonObject()));

        return serviceCfg.zipWith(dmaapCfg, this::parseCloudConfig) //
                .onErrorResume(this::onErrorResume);
    }

    /**
     * parse configuration
     *
     * @param serviceConfigRootObject
     * @param dmaapConfigRootObject if there is no dmaapConfigRootObject, the dmaap feeds are taken
     *        from the serviceConfigRootObject
     * @return this which is updated if successful
     */
    private AppConfig parseCloudConfig(JsonObject serviceConfigRootObject, JsonObject dmaapConfigRootObject) {
        try {
            CloudConfigParser parser = new CloudConfigParser(serviceConfigRootObject, dmaapConfigRootObject);
            setConfiguration(parser.getDmaapConsumerConfig(), parser.getDmaapPublisherConfig(),
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
            parseCloudConfig(rootObject, rootObject);
        } catch (JsonSyntaxException | IOException e) {
            logger.info("Local configuration file not loaded: {}", filepath, e);
        }
    }

    private synchronized void setConfiguration(ConsumerConfiguration consumerConfiguration,
            Map<String, PublisherConfiguration> publisherConfiguration, FtpesConfig ftpesConfig) {
        if (consumerConfiguration == null || publisherConfiguration == null || ftpesConfig == null) {
            logger.error(
                    "Problem with configuration consumerConfiguration: {}, publisherConfiguration: {}, ftpesConfig: {}",
                    consumerConfiguration, publisherConfiguration, ftpesConfig);
        } else {
            this.dmaapConsumerConfiguration = consumerConfiguration;
            this.publishingConfiguration = publisherConfiguration;
            this.ftpesConfiguration = ftpesConfig;
        }
    }

    JsonElement getJsonElement(JsonParser parser, InputStream inputStream) {
        return parser.parse(new InputStreamReader(inputStream));
    }

    InputStream createInputStream(@NotNull String filepath) throws IOException {
        return new BufferedInputStream(new FileInputStream(filepath));
    }

}
