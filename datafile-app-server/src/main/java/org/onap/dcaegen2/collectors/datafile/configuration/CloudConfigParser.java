/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018-2019 Nordix Foundation. All rights reserved.
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import javax.validation.constraints.NotNull;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;

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

    Properties systemEnvironment;

    private final JsonObject jsonObject;

    public CloudConfigParser(JsonObject jsonObject, Properties properties) {
        this.jsonObject = jsonObject.getAsJsonObject(CONFIG);
        this.systemEnvironment = properties;
    }

    /**
     * Get the publisher configurations.
     *
     * @return a map with change identifier as key and the connected publisher configuration as value.
     * @throws DatafileTaskException if a member of the configuration is missing.
     */
    public @NotNull Map<String, PublisherConfiguration> getDmaapPublisherConfigurations()
        throws DatafileTaskException {
        JsonObject producerCfgs = jsonObject.get("streams_publishes").getAsJsonObject();
        Iterator<String> changeIdentifierList = producerCfgs.keySet().iterator();
        Map<String, PublisherConfiguration> result = new HashMap<>();

        while (changeIdentifierList.hasNext()) {
            String changeIdentifier = changeIdentifierList.next();
            JsonObject producerCfg = getAsJson(producerCfgs, changeIdentifier);
            JsonObject feedConfig = get(producerCfg, "dmaap_info").getAsJsonObject();

            PublisherConfiguration cfg = ImmutablePublisherConfiguration.builder() //
                .publishUrl(getAsString(feedConfig, "publish_url")) //
                .passWord(getAsString(feedConfig, "password")) //
                .userName(getAsString(feedConfig, "username")) //
                .trustStorePath(getAsString(jsonObject, DMAAP_SECURITY_TRUST_STORE_PATH)) //
                .trustStorePasswordPath(getAsString(jsonObject, DMAAP_SECURITY_TRUST_STORE_PASS_PATH)) //
                .keyStorePath(getAsString(jsonObject, DMAAP_SECURITY_KEY_STORE_PATH)) //
                .keyStorePasswordPath(getAsString(jsonObject, DMAAP_SECURITY_KEY_STORE_PASS_PATH)) //
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
     * @throws DatafileTaskException if a member of the configuration is missing.
     */
    public @NotNull ConsumerConfiguration getDmaapConsumerConfig() throws DatafileTaskException {
        JsonObject consumerCfg = jsonObject.get("streams_subscribes").getAsJsonObject();
        Set<Entry<String, JsonElement>> topics = consumerCfg.entrySet();
        if (topics.size() != 1) {
            throw new DatafileTaskException(
                "Invalid configuration, number of topic must be one, config: " + topics);
        }
        JsonObject topic = topics.iterator().next().getValue().getAsJsonObject();
        JsonObject dmaapInfo = get(topic, "dmaap_info").getAsJsonObject();
        String topicUrl = getAsString(dmaapInfo, "topic_url");

        return ImmutableConsumerConfiguration.builder().topicUrl(topicUrl)
            .trustStorePath(getAsString(jsonObject, DMAAP_SECURITY_TRUST_STORE_PATH))
            .trustStorePasswordPath(getAsString(jsonObject, DMAAP_SECURITY_TRUST_STORE_PASS_PATH))
            .keyStorePath(getAsString(jsonObject, DMAAP_SECURITY_KEY_STORE_PATH))
            .keyStorePasswordPath(getAsString(jsonObject, DMAAP_SECURITY_KEY_STORE_PASS_PATH))
            .enableDmaapCertAuth(get(jsonObject, DMAAP_SECURITY_ENABLE_DMAAP_CERT_AUTH).getAsBoolean()) //
            .build();
    }

    /**
     * Get the consumer configuration.
     *
     * @return the consumer configuration.
     * @throws DatafileTaskException if a member of the configuration is missing.
     */
    public @NotNull SfptConfig getSftpConfig() throws DatafileTaskException {
        String filePath = "";
        if (systemEnvironment != null
            && systemEnvironment.getProperty("KNOWN_HOSTS_FILE_PATH") != null) {
            filePath = systemEnvironment.getProperty("KNOWN_HOSTS_FILE_PATH");
        }
        return new ImmutableSfptConfig.Builder() //
            .strictHostKeyChecking(getAsBoolean(jsonObject, "sftp.security.strictHostKeyChecking"))
            .knownHostsFilePath(filePath)
            .build();
    }

    /**
     * Get the security configuration for communication with the xNF.
     *
     * @return the xNF communication security configuration.
     * @throws DatafileTaskException if a member of the configuration is missing.
     */
    public @NotNull FtpesConfig getFtpesConfig() throws DatafileTaskException {
        return new ImmutableFtpesConfig.Builder() //
            .keyCert(getAsString(jsonObject, "dmaap.ftpesConfig.keyCert"))
            .keyPasswordPath(getAsString(jsonObject, "dmaap.ftpesConfig.keyPasswordPath"))
            .trustedCa(getAsString(jsonObject, "dmaap.ftpesConfig.trustedCa"))
            .trustedCaPasswordPath(
                getAsString(jsonObject, "dmaap.ftpesConfig.trustedCaPasswordPath")) //
            .build();
    }

    private static @NotNull JsonElement get(JsonObject obj, String memberName)
        throws DatafileTaskException {
        JsonElement elem = obj.get(memberName);
        if (elem == null) {
            throw new DatafileTaskException("Could not find member: " + memberName + " in: " + obj);
        }
        return elem;
    }

    private static @NotNull String getAsString(JsonObject obj, String memberName)
        throws DatafileTaskException {
        return get(obj, memberName).getAsString();
    }

    private static @NotNull Boolean getAsBoolean(JsonObject obj, String memberName)
        throws DatafileTaskException {
        return get(obj, memberName).getAsBoolean();
    }

    private static @NotNull JsonObject getAsJson(JsonObject obj, String memberName)
        throws DatafileTaskException {
        return get(obj, memberName).getAsJsonObject();
    }

}
