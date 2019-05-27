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
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.ImmutableDmaapPublisherConfiguration;


@Value.Immutable
@Value.Style(redactedMask = "####")
@Gson.TypeAdapters
public interface PublisherConfiguration {

    String publishUrl();

    String logUrl();

    String userName();

    @Value.Redacted
    String passWord();

    String trustStorePath();

    String trustStorePasswordPath();

    String keyStorePath();

    String keyStorePasswordPath();

    Boolean enableDmaapCertAuth();

    String changeIdentifier();

    default DmaapPublisherConfiguration toDmaap() throws MalformedURLException {
        URL url = new URL(publishUrl());
        String urlPath = url.getPath();

        return new ImmutableDmaapPublisherConfiguration.Builder() //
                .dmaapContentType("application/octet-stream")
                .dmaapPortNumber(url.getPort())
                .dmaapHostName(url.getHost())
                .dmaapTopicName(urlPath)
                .dmaapProtocol(url.getProtocol())
                .dmaapUserName(this.userName())
                .dmaapUserPassword(this.passWord())
                .dmaapContentType("application/json")
                .trustStorePath(this.trustStorePath()) //
                .trustStorePasswordPath(this.trustStorePasswordPath()) //
                .keyStorePath(this.keyStorePath()) //
                .keyStorePasswordPath(this.keyStorePasswordPath()) //
                .enableDmaapCertAuth(this.enableDmaapCertAuth()) //
                .build();
    }

}
