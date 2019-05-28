/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018 Nordix Foundation. All rights reserved.
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

    private final JsonObject serviceConfigurationRoot;
    private final JsonObject dmaapConfigurationRoot;

    public CloudConfigParser(JsonObject serviceConfigurationRoot, JsonObject dmaapConfigurationRoot) {
        this.serviceConfigurationRoot = serviceConfigurationRoot;
        this.dmaapConfigurationRoot = dmaapConfigurationRoot;
    }

    public Map<String, PublisherConfiguration> getDmaapPublisherConfig() throws DatafileTaskException {
        Iterator<JsonElement> producerCfgs =
                toArray(serviceConfigurationRoot.get("dmaap.dmaapProducerConfiguration")).iterator();

        Map<String, PublisherConfiguration> result = new HashMap<>();

        while (producerCfgs.hasNext()) {
            JsonObject producerCfg = producerCfgs.next().getAsJsonObject();
            String feeedName = getAsString(producerCfg, "feeedName");
            JsonObject feedConfig = getFeedConfig(feeedName);

            PublisherConfiguration cfg = ImmutablePublisherConfiguration.builder() //
                    .publishUrl(getAsString(feedConfig, "publish_url")) //
                    .passWord(getAsString(feedConfig, "password")) //
                    .userName(getAsString(feedConfig, "username")) //
                    .trustStorePath(getAsString(serviceConfigurationRoot, DMAAP_SECURITY_TRUST_STORE_PATH)) //
                    .trustStorePasswordPath(getAsString(serviceConfigurationRoot, DMAAP_SECURITY_TRUST_STORE_PASS_PATH)) //
                    .keyStorePath(getAsString(serviceConfigurationRoot, DMAAP_SECURITY_KEY_STORE_PATH)) //
                    .keyStorePasswordPath(getAsString(serviceConfigurationRoot, DMAAP_SECURITY_KEY_STORE_PASS_PATH)) //
                    .enableDmaapCertAuth(
                            get(serviceConfigurationRoot, DMAAP_SECURITY_ENABLE_DMAAP_CERT_AUTH).getAsBoolean()) //
                    .changeIdentifier(getAsString(producerCfg, "changeIdentifier")) //
                    .logUrl(getAsString(feedConfig, "log_url")) //
                    .build();

            result.put(cfg.changeIdentifier(), cfg);
        }
        return result;
    }

    public ConsumerConfiguration getDmaapConsumerConfig() throws DatafileTaskException {
        JsonObject consumerCfg = serviceConfigurationRoot.get("streams_subscribes").getAsJsonObject();
        Set<Entry<String, JsonElement>> topics = consumerCfg.entrySet();
        if (topics.size() != 1) {
            throw new DatafileTaskException("Invalid configuration, number oftopic must be one, config: " + topics);
        }
        JsonObject topic = topics.iterator().next().getValue().getAsJsonObject();
        JsonObject dmaapInfo = get(topic, "dmmap_info").getAsJsonObject();
        String topicUrl = getAsString(dmaapInfo, "topic_url");

        return ImmutableConsumerConfiguration.builder().topicUrl(topicUrl)
                .trustStorePath(getAsString(serviceConfigurationRoot, DMAAP_SECURITY_TRUST_STORE_PATH))
                .trustStorePasswordPath(getAsString(serviceConfigurationRoot, DMAAP_SECURITY_TRUST_STORE_PASS_PATH))
                .keyStorePath(getAsString(serviceConfigurationRoot, DMAAP_SECURITY_KEY_STORE_PATH))
                .keyStorePasswordPath(getAsString(serviceConfigurationRoot, DMAAP_SECURITY_KEY_STORE_PASS_PATH))
                .enableDmaapCertAuth(
                        get(serviceConfigurationRoot, DMAAP_SECURITY_ENABLE_DMAAP_CERT_AUTH).getAsBoolean()) //
                .build();
    }

    public FtpesConfig getFtpesConfig() throws DatafileTaskException {
        return new ImmutableFtpesConfig.Builder() //
                .keyCert(getAsString(serviceConfigurationRoot, "dmaap.ftpesConfig.keyCert"))
                .keyPassword(getAsString(serviceConfigurationRoot, "dmaap.ftpesConfig.keyPassword"))
                .trustedCa(getAsString(serviceConfigurationRoot, "dmaap.ftpesConfig.trustedCa"))
                .trustedCaPassword(getAsString(serviceConfigurationRoot, "dmaap.ftpesConfig.trustedCaPassword")) //
                .build();
    }

    private static JsonElement get(JsonObject obj, String memberName) throws DatafileTaskException {
        JsonElement elem = obj.get(memberName);
        if (elem == null) {
            throw new DatafileTaskException("Could not find member: " + memberName + " in: " + obj);
        }
        return elem;
    }

    private static String getAsString(JsonObject obj, String memberName) throws DatafileTaskException {
        return get(obj, memberName).getAsString();
    }

    private JsonObject getFeedConfig(String feedName) throws DatafileTaskException {
        JsonElement elem = dmaapConfigurationRoot.get(feedName);
        if (elem == null) {
            elem = get(serviceConfigurationRoot, feedName); // Fallback, try to find it under
                                                            // serviceConfigurationRoot
        }
        return elem.getAsJsonObject();
    }

    private static JsonArray toArray(JsonElement obj) {
        if (obj.isJsonArray()) {
            return obj.getAsJsonArray();
        }
        JsonArray arr = new JsonArray();
        arr.add(obj);
        return arr;
    }
}
