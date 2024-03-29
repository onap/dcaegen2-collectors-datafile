/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018, 2020-2022 Nokia. All rights reserved.
 * Copyright (C) 2018-2019 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.configuration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.validation.constraints.NotNull;

import io.vavr.collection.Stream;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.services.sdk.model.streams.RawDataStream;
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.MessageRouterSource;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.streams.DataStreams;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.streams.StreamFromGsonParsers;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.api.DmaapClientFactory;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.api.MessageRouterSubscriber;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.ImmutableMessageRouterSubscribeRequest;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.MessageRouterSubscribeRequest;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.config.ImmutableMessageRouterSubscriberConfig;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.config.MessageRouterSubscriberConfig;
import org.onap.dcaegen2.services.sdk.security.ssl.ImmutableSecurityKeys;
import org.onap.dcaegen2.services.sdk.security.ssl.ImmutableSecurityKeysStore;
import org.onap.dcaegen2.services.sdk.security.ssl.Passwords;
import org.onap.dcaegen2.services.sdk.security.ssl.SecurityKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses the cloud configuration.
 *
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 9/19/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class CloudConfigParser {

    private static final String DMAAP_SECURITY_TRUST_STORE_PATH = "dmaap.security.trustStorePath";
    private static final String DMAAP_SECURITY_TRUST_STORE_PASS_PATH = "dmaap.security.trustStorePasswordPath";
    private static final String DMAAP_SECURITY_KEY_STORE_PATH = "dmaap.security.keyStorePath";
    private static final String DMAAP_SECURITY_KEY_STORE_PASS_PATH = "dmaap.security.keyStorePasswordPath";
    private static final String DMAAP_SECURITY_ENABLE_DMAAP_CERT_AUTH = "dmaap.security.enableDmaapCertAuth";
    private static final String CONFIG = "config";

    private static final String KNOWN_HOSTS_FILE_PATH_ENV_PROPERTY = "KNOWN_HOSTS_FILE_PATH";
    private static final String CBS_PROPERTY_SFTP_SECURITY_STRICT_HOST_KEY_CHECKING =
        "sftp.security.strictHostKeyChecking";

    private static final String DMAAP_CONSUMER_CONFIGURATION_CONSUMER_GROUP = "dmaap.dmaapConsumerConfiguration.consumerGroup";
    private static final String DMAAP_CONSUMER_CONFIGURATION_CONSUMER_ID = "dmaap.dmaapConsumerConfiguration.consumerId";
    private static final String DMAAP_CONSUMER_CONFIGURATION_TIMEOUT_MS = "dmaap.dmaapConsumerConfiguration.timeoutMs";
    private static final int EXPECTED_NUMBER_OF_SOURCE_TOPICS = 1;
    private static final int FIRST_SOURCE_INDEX = 0;

    private static final Logger logger = LoggerFactory.getLogger(CloudConfigParser.class);

    private final Properties systemEnvironment;

    private final JsonObject jsonObject;

    public CloudConfigParser(JsonObject jsonObject, Properties systemEnvironment) {
        this.jsonObject = jsonObject.getAsJsonObject(CONFIG);
        this.systemEnvironment = systemEnvironment;
    }

    /**
     * Get the publisher configurations.
     *
     * @return a map with change identifier as key and the connected publisher configuration as value.
     * @throws DatafileTaskException if a member of the configuration is missing.
     */
    public @NotNull Map<String, PublisherConfiguration> getDmaapPublisherConfigurations() throws DatafileTaskException {
        JsonObject producerCfgs = jsonObject.get("streams_publishes").getAsJsonObject();
        Iterator<String> changeIdentifierList = producerCfgs.keySet().iterator();
        Map<String, PublisherConfiguration> result = new HashMap<>();

        while (changeIdentifierList.hasNext()) {
            String changeIdentifier = changeIdentifierList.next();
            JsonObject producerCfg = getAsJson(producerCfgs, changeIdentifier);
            JsonObject feedConfig = get(producerCfg, "dmaap_info").getAsJsonObject();

            PublisherConfiguration cfg = ImmutablePublisherConfiguration.builder() //
                .publishUrl(getAsString(feedConfig, "publish_url")) //
                .password(getAsString(feedConfig, "password")) //
                .userName(getAsString(feedConfig, "username")) //
                .trustStorePath(getAsOptionalStringOrDefault(jsonObject, DMAAP_SECURITY_TRUST_STORE_PATH,"")) //
                .trustStorePasswordPath(getAsOptionalStringOrDefault(jsonObject, DMAAP_SECURITY_TRUST_STORE_PASS_PATH, "")) //
                .keyStorePath(getAsOptionalStringOrDefault(jsonObject, DMAAP_SECURITY_KEY_STORE_PATH,"")) //
                .keyStorePasswordPath(getAsOptionalStringOrDefault(jsonObject, DMAAP_SECURITY_KEY_STORE_PASS_PATH,"")) //
                .enableDmaapCertAuth(get(jsonObject, DMAAP_SECURITY_ENABLE_DMAAP_CERT_AUTH).getAsBoolean()) //
                .changeIdentifier(changeIdentifier) //
                .logUrl(getAsString(feedConfig, "log_url")) //
                .build();

            result.put(cfg.changeIdentifier(), cfg);
        }
        return result;
    }

    /**
     * Get the consumer configuration.
     *
     * @return the consumer configuration.
     * @throws DatafileTaskException if the configuration is invalid.
     */
    public @NotNull ConsumerConfiguration getConsumerConfiguration() throws DatafileTaskException {
        try {
            MessageRouterSubscriberConfig messageRouterSubscriberConfig = getMessageRouterSubscriberConfig();
            MessageRouterSubscribeRequest messageRouterSubscribeRequest = getMessageRouterSubscribeRequest();
            MessageRouterSubscriber messageRouterSubscriber = DmaapClientFactory.createMessageRouterSubscriber(messageRouterSubscriberConfig);
            return new ConsumerConfiguration(messageRouterSubscriberConfig, messageRouterSubscriber, messageRouterSubscribeRequest);
        } catch (Exception e) {
            throw new DatafileTaskException("Could not parse message router consumer configuration", e);
        }
    }

    private MessageRouterSubscriberConfig getMessageRouterSubscriberConfig() throws DatafileTaskException {
        return ImmutableMessageRouterSubscriberConfig.builder()
            .securityKeys(isDmaapCertAuthEnabled(jsonObject) ? createSecurityKeys() : null)
            .build();
    }

    private SecurityKeys createSecurityKeys() throws DatafileTaskException {
        return ImmutableSecurityKeys.builder()
            .keyStore(ImmutableSecurityKeysStore.of(getAsPath(jsonObject, DMAAP_SECURITY_KEY_STORE_PATH)))
            .keyStorePassword(Passwords.fromPath(getAsPath(jsonObject, DMAAP_SECURITY_KEY_STORE_PASS_PATH)))
            .trustStore(ImmutableSecurityKeysStore.of(getAsPath(jsonObject, DMAAP_SECURITY_TRUST_STORE_PATH)))
            .trustStorePassword(Passwords.fromPath(getAsPath(jsonObject, DMAAP_SECURITY_TRUST_STORE_PASS_PATH)))
            .build();
    }

    private boolean isDmaapCertAuthEnabled(JsonObject config) {
        return config.get(DMAAP_SECURITY_ENABLE_DMAAP_CERT_AUTH).getAsBoolean();
    }

    private MessageRouterSubscribeRequest getMessageRouterSubscribeRequest() throws DatafileTaskException {
        Stream<RawDataStream<JsonObject>> sources = DataStreams.namedSources(jsonObject);
        if (sources.size() != EXPECTED_NUMBER_OF_SOURCE_TOPICS) {
            throw new DatafileTaskException("Invalid configuration, number of topic must be one, config: " + sources);
        }
        RawDataStream<JsonObject> source = sources.get(FIRST_SOURCE_INDEX);
        MessageRouterSource parsedSource = StreamFromGsonParsers.messageRouterSourceParser().unsafeParse(source);

        return ImmutableMessageRouterSubscribeRequest.builder()
            .consumerGroup(getAsString(jsonObject, DMAAP_CONSUMER_CONFIGURATION_CONSUMER_GROUP))
            .sourceDefinition(parsedSource)
            .consumerId(getAsString(jsonObject, DMAAP_CONSUMER_CONFIGURATION_CONSUMER_ID))
            .timeout(Duration.ofMillis(get(jsonObject, DMAAP_CONSUMER_CONFIGURATION_TIMEOUT_MS).getAsLong()))
            .build();
    }

    /**
     * Get the sFTP configuration.
     *
     * @return the sFTP configuration.
     * @throws DatafileTaskException if a member of the configuration is missing.
     */
    public @NotNull SftpConfig getSftpConfig() throws DatafileTaskException {
        String filePath = determineKnownHostsFilePath();
        return new ImmutableSftpConfig.Builder() //
            .strictHostKeyChecking(getAsBoolean(jsonObject, CBS_PROPERTY_SFTP_SECURITY_STRICT_HOST_KEY_CHECKING))
            .knownHostsFilePath(filePath).build();
    }

    /**
     * Get the security configuration for communication with the xNF.
     *
     * @return the xNF communication security configuration.
     * @throws DatafileTaskException if a member of the configuration is missing.
     */
    public @NotNull CertificateConfig getCertificateConfig() throws DatafileTaskException {
        boolean enableCertAuth = getAsBooleanOrDefault(jsonObject, "dmaap.certificateConfig.enableCertAuth",
            Boolean.TRUE);

        String keyCert = "";
        String keyPasswordPath = "";
        String trustedCa = "";
        String trustedCaPasswordPath = "";
        boolean httpsHostnameVerify = true;

        if (enableCertAuth) {
            logger.debug("TlS enabled, attempt to read certificates property");
            try {
                keyCert = getAsString(jsonObject, "dmaap.certificateConfig.keyCert");
                keyPasswordPath = getAsString(jsonObject, "dmaap.certificateConfig.keyPasswordPath");
                trustedCa = getAsString(jsonObject, "dmaap.certificateConfig.trustedCa");
                trustedCaPasswordPath = getAsString(jsonObject, "dmaap.certificateConfig.trustedCaPasswordPath");
                httpsHostnameVerify = getAsBooleanOrDefault(jsonObject, "dmaap.certificateConfig.httpsHostnameVerify",
                    Boolean.TRUE);
            } catch (DatafileTaskException e) {
                throw new DatafileTaskException(
                    "Wrong configuration. External certificate enabled but configs are missing: "
                        + e.getMessage());
            }
        }

        return new ImmutableCertificateConfig.Builder() //
            .keyCert(keyCert)
            .keyPasswordPath(keyPasswordPath)
            .trustedCa(trustedCa)
            .trustedCaPasswordPath(trustedCaPasswordPath) //
            .httpsHostnameVerify(httpsHostnameVerify)
            .enableCertAuth(enableCertAuth)
            .build();
    }

    private String determineKnownHostsFilePath() {
        String filePath = "";
        if (systemEnvironment != null) {
            filePath =
                systemEnvironment.getProperty(KNOWN_HOSTS_FILE_PATH_ENV_PROPERTY, "/home/datafile/.ssh/known_hosts");
        }
        return filePath;
    }

    private static @NotNull JsonElement get(JsonObject obj, String memberName) throws DatafileTaskException {
        JsonElement elem = obj.get(memberName);
        if (elem == null) {
            throw new DatafileTaskException("Could not find member: " + memberName + " in: " + obj);
        }
        return elem;
    }

    private static @NotNull String getAsString(JsonObject obj, String memberName) throws DatafileTaskException {
        return get(obj, memberName).getAsString();
    }

    private static String getAsOptionalStringOrDefault(JsonObject obj, String memberName, String def) {
        try {
            return get(obj, memberName).getAsString();
        } catch (DatafileTaskException e) {
            return def;
        }
    }


    private static @NotNull Boolean getAsBoolean(JsonObject obj, String memberName) throws DatafileTaskException {
        return get(obj, memberName).getAsBoolean();
    }

    private static @NotNull Boolean getAsBooleanOrDefault(JsonObject obj, String memberName, Boolean def) {
        try {
            return get(obj, memberName).getAsBoolean();
        } catch (DatafileTaskException e) {
            return def;
        }
    }

    private static @NotNull JsonObject getAsJson(JsonObject obj, String memberName) throws DatafileTaskException {
        return get(obj, memberName).getAsJsonObject();
    }

    private static @NotNull Path getAsPath(JsonObject obj, String dmaapSecurityKeyStorePath) throws DatafileTaskException {
        return Paths.get(getAsString(obj, dmaapSecurityKeyStorePath));
    }

}
