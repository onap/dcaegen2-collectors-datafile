/*-
* ============LICENSE_START=======================================================
*  Copyright (C) 2019 Nordix Foundation.
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
*
* SPDX-License-Identifier: Apache-2.0
* ============LICENSE_END=========================================================
*/

package org.onap.dcaegen2.collectors.datafile.tasks;

import static org.onap.dcaegen2.collectors.datafile.model.logging.MdcVariables.REQUEST_ID;
import static org.onap.dcaegen2.collectors.datafile.model.logging.MdcVariables.X_INVOCATION_ID;
import static org.onap.dcaegen2.collectors.datafile.model.logging.MdcVariables.X_ONAP_REQUEST_ID;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.model.logging.MdcVariables;
import org.onap.dcaegen2.collectors.datafile.service.producer.DmaapProducerReactiveHttpClient;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapPublisherConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Bean used to check with DataRouter if a file has been published.
 *
 * @author <a href="mailto:maxime.bonneau@est.tech">Maxime Bonneau</a>
 *
 */
public class PublishedChecker {
    private static final String FEEDLOG_TOPIC = "feedlog";
    private static final String DEFAULT_FEED_ID = "1";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private AppConfig appConfig;

    /**
     * Constructor.
     *
     * @param appConfig The DFC configuration.
     */
    public PublishedChecker(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * Checks with DataRouter if the given file has been published already.
     *
     * @param fileName the name of the file used when it is published.
     *
     * @return <code>true</code> if the file has been published before, <code>false</code> otherwise.
     */
    public boolean execute(String fileName, Map<String, String> contextMap) {
        MdcVariables.setMdcContextMap(contextMap);
        DmaapProducerReactiveHttpClient producerClient = resolveClient();

        HttpGet getRequest = new HttpGet();
        String requestId = MDC.get(REQUEST_ID);
        getRequest.addHeader(X_ONAP_REQUEST_ID, requestId);
        String invocationId = UUID.randomUUID().toString();
        getRequest.addHeader(X_INVOCATION_ID, invocationId);
        getRequest.setURI(getPublishedQueryUri(fileName, producerClient));
        producerClient.addUserCredentialsToHead(getRequest);

        try {
            HttpResponse response =
                    producerClient.getDmaapProducerResponseWithCustomTimeout(getRequest, 2000, contextMap);

            logger.trace(response.toString());
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            InputStream content = entity.getContent();
            String body = IOUtils.toString(content);
            return HttpStatus.SC_OK == status && !"[]".equals(body);
        } catch (Exception e) {
            logger.warn("Unable to check if file has been published.", e);
            return false;
        }
    }

    private URI getPublishedQueryUri(String fileName, DmaapProducerReactiveHttpClient producerClient) {
        return producerClient.getBaseUri() //
                .pathSegment(FEEDLOG_TOPIC) //
                .pathSegment(DEFAULT_FEED_ID) //
                .queryParam("type", "pub") //
                .queryParam("filename", fileName) //
                .build();
    }

    protected DmaapPublisherConfiguration resolveConfiguration() {
        return appConfig.getDmaapPublisherConfiguration();
    }

    protected DmaapProducerReactiveHttpClient resolveClient() {
        return new DmaapProducerReactiveHttpClient(resolveConfiguration());
    }
}
