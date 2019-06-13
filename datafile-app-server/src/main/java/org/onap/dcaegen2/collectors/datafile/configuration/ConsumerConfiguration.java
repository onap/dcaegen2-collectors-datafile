/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018,2019 Nordix Foundation. All rights reserved.
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

import java.net.MalformedURLException;
import java.net.URL;
import org.immutables.gson.Gson;
import org.immutables.value.Value;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapConsumerConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.ImmutableDmaapConsumerConfiguration;

@Value.Immutable
@Value.Style(redactedMask = "####")
@Gson.TypeAdapters
public abstract class ConsumerConfiguration {
    @Value.Redacted
    public abstract String topicUrl();

    public abstract String trustStorePath();

    public abstract String trustStorePasswordPath();

    public abstract String keyStorePath();

    public abstract String keyStorePasswordPath();

    public abstract Boolean enableDmaapCertAuth();

    public DmaapConsumerConfiguration toDmaap() throws DatafileTaskException {
        try {
            URL url = new URL(topicUrl());
            String passwd = "";
            String userName = "";
            if (url.getUserInfo() != null) {
                String[] userInfo = url.getUserInfo().split(":");
                userName = userInfo[0];
                passwd = userInfo[1];
            }
            String urlPath = url.getPath();
            DmaapConsumerUrlPath path = parseDmaapUrlPath(urlPath);

            return new ImmutableDmaapConsumerConfiguration.Builder() //
                    .endpointUrl(topicUrl())
                    .dmaapContentType("application/json") //
                    .dmaapPortNumber(url.getPort()) //
                    .dmaapHostName(url.getHost()) //
                    .dmaapTopicName(path.dmaapTopicName) //
                    .dmaapProtocol(url.getProtocol()) //
                    .dmaapUserName(userName) //
                    .dmaapUserPassword(passwd) //
                    .trustStorePath(this.trustStorePath()) //
                    .trustStorePasswordPath(this.trustStorePasswordPath()) //
                    .keyStorePath(this.keyStorePath()) //
                    .keyStorePasswordPath(this.keyStorePasswordPath()) //
                    .enableDmaapCertAuth(this.enableDmaapCertAuth()) //
                    .consumerId(path.consumerId) //
                    .consumerGroup(path.consumerGroup) //
                    .timeoutMs(-1) //
                    .messageLimit(-1) //
                    .build();
        } catch (MalformedURLException e) {
            throw new DatafileTaskException("Could not parse the URL", e);
        }
    }

    private class DmaapConsumerUrlPath {
        final String dmaapTopicName;
        final String consumerGroup;
        final String consumerId;

        DmaapConsumerUrlPath(String dmaapTopicName, String consumerGroup, String consumerId) {
            this.dmaapTopicName = dmaapTopicName;
            this.consumerGroup = consumerGroup;
            this.consumerId = consumerId;
        }
    }

    private DmaapConsumerUrlPath parseDmaapUrlPath(String urlPath) throws DatafileTaskException {
        String[] tokens = urlPath.split("/"); // UrlPath: /events/unauthenticated.VES_NOTIFICATION_OUTPUT/OpenDcae-c12/C12
        if (tokens.length != 5) {
            throw new DatafileTaskException("The path has incorrect syntax: " + urlPath);
        }

        final String dmaapTopicName =  tokens[1] + "/" + tokens[2]; // e.g. /events/unauthenticated.VES_NOTIFICATION_OUTPUT
        final String consumerGroup = tokens[3]; // ex. OpenDcae-c12
        final String consumerId = tokens[4]; // ex. C12
        return new DmaapConsumerUrlPath(dmaapTopicName, consumerGroup, consumerId);
    }

}
