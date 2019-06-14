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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.configuration.PublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.model.logging.MappedDiagnosticContext;
import org.onap.dcaegen2.collectors.datafile.service.producer.DmaapProducerHttpClient;
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

    private static final Duration WEB_CLIENT_TIMEOUT = Duration.ofSeconds(4);
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final AppConfig appConfig;

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
     * @return <code>true</code> if the file has been published before, <code>false</code>
     *         otherwise.
     * @throws DatafileTaskException if the check fails
     */
    public boolean isFilePublished(String fileName, String changeIdentifier, Map<String, String> contextMap)
        throws DatafileTaskException {
        MDC.setContextMap(contextMap);
        PublisherConfiguration publisherConfig = resolveConfiguration(changeIdentifier);

        DmaapProducerHttpClient producerClient = resolveClient(publisherConfig);

        HttpGet getRequest = new HttpGet();
        MappedDiagnosticContext.appendTraceInfo(getRequest);

        try {
            getRequest.setURI(getPublishedQueryUri(fileName, publisherConfig));
            producerClient.addUserCredentialsToHead(getRequest);

            HttpResponse response =
                producerClient.getDmaapProducerResponseWithCustomTimeout(getRequest, WEB_CLIENT_TIMEOUT, contextMap);

            logger.trace("{}", response);
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            try (InputStream content = entity.getContent()) {
                String body = IOUtils.toString(content);
                return HttpStatus.SC_OK == status && !"[]".equals(body);
            }
        } catch (Exception e) {
            logger.warn("Unable to check if file has been published, file: {}", fileName, e);
            return false;
        }
    }

    private static URI getPublishedQueryUri(String fileName, PublisherConfiguration config) throws URISyntaxException {
        return new URIBuilder(config.logUrl()) //
            .addParameter("type", "pub") //
            .addParameter("filename", fileName) //
            .build();
    }

    protected PublisherConfiguration resolveConfiguration(String changeIdentifier) throws DatafileTaskException {
        return appConfig.getPublisherConfiguration(changeIdentifier);
    }

    protected DmaapProducerHttpClient resolveClient(PublisherConfiguration publisherConfig)
        throws DatafileTaskException {
        try {
            return new DmaapProducerHttpClient(publisherConfig.toDmaap());
        } catch (MalformedURLException e) {
            throw new DatafileTaskException("Cannot create published checker client", e);
        }
    }
}
