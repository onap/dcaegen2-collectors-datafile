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

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Properties;
import org.onap.dcaegen2.collectors.datafile.model.logging.MappedDiagnosticContext;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.providers.ReactiveCloudConfigurationProvider;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapConsumerConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapPublisherConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Gets the DFC configuration from the ConfigBindingService/Consul and parses it to the configurations needed in DFC.
 *
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 9/19/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
@Configuration
@ComponentScan("org.onap.dcaegen2.services.sdk.rest.services.cbs.client.providers")
@EnableConfigurationProperties
@EnableScheduling
@Primary
public class CloudConfiguration extends AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(CloudConfiguration.class);
    private ReactiveCloudConfigurationProvider reactiveCloudConfigurationProvider;
    private DmaapPublisherConfiguration dmaapPublisherCloudConfiguration;
    private DmaapConsumerConfiguration dmaapConsumerCloudConfiguration;
    private FtpesConfig ftpesCloudConfiguration;

    @Value("#{systemEnvironment}")
    private Properties systemEnvironment;

    @Autowired
    public synchronized void setThreadPoolTaskScheduler(
            ReactiveCloudConfigurationProvider reactiveCloudConfigurationProvider) {
        this.reactiveCloudConfigurationProvider = reactiveCloudConfigurationProvider;
    }

    /**
     * Reads the cloud configuration.
     */
    public void runTask() {
        Map<String,String> context = MappedDiagnosticContext.initializeTraceContext();
        EnvironmentProcessor.readEnvironmentVariables(systemEnvironment, context) //
                .subscribeOn(Schedulers.parallel()) //
                .flatMap(reactiveCloudConfigurationProvider::callForServiceConfigurationReactive) //
                .flatMap(this::parseCloudConfig) //
                .subscribe(null, this::onError, this::onComplete);
    }

    private void onComplete() {
        logger.trace("Configuration updated");
    }

    private void onError(Throwable throwable) {
        logger.warn("Exception during getting configuration from CONSUL/CONFIG_BINDING_SERVICE ", throwable);
    }

    private synchronized Mono<CloudConfiguration> parseCloudConfig(JsonObject jsonObject) {
        logger.info("Received application configuration: {}", jsonObject);
        CloudConfigParser cloudConfigParser = new CloudConfigParser(jsonObject);
        dmaapPublisherCloudConfiguration = cloudConfigParser.getDmaapPublisherConfig();
        dmaapConsumerCloudConfiguration = cloudConfigParser.getDmaapConsumerConfig();
        ftpesCloudConfiguration = cloudConfigParser.getFtpesConfig();
        return Mono.just(this);
    }

    @Override
    public synchronized DmaapPublisherConfiguration getDmaapPublisherConfiguration() {
        return dmaapPublisherCloudConfiguration != null ? dmaapPublisherCloudConfiguration
                : super.getDmaapPublisherConfiguration();
    }

    @Override
    public synchronized DmaapConsumerConfiguration getDmaapConsumerConfiguration() {
        return dmaapConsumerCloudConfiguration != null ? dmaapConsumerCloudConfiguration
                : super.getDmaapConsumerConfiguration();
    }

    @Override
    public synchronized FtpesConfig getFtpesConfiguration() {
        return ftpesCloudConfiguration != null ? ftpesCloudConfiguration : super.getFtpesConfiguration();
    }
}