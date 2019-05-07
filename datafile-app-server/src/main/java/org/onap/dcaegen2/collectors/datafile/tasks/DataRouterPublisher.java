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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.model.CommonFunctions;
import org.onap.dcaegen2.collectors.datafile.model.FilePublishInformation;
import org.onap.dcaegen2.collectors.datafile.model.logging.MappedDiagnosticContext;
import org.onap.dcaegen2.collectors.datafile.service.HttpUtils;
import org.onap.dcaegen2.collectors.datafile.service.producer.DmaapProducerHttpClient;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapPublisherConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import reactor.core.publisher.Mono;

/**
 * Publishes a file to the DataRouter.
 *
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 4/13/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class DataRouterPublisher {
    private static final String X_DMAAP_DR_META = "X-DMAAP-DR-META";
    private static final String CONTENT_TYPE = "application/octet-stream";
    private static final String NAME_JSON_TAG = "name";
    private static final String INTERNAL_LOCATION_JSON_TAG = "internalLocation";
    private static final String CONTEXT_JSON_TAG = "context";
    private static final String PUBLISH_TOPIC = "publish";
    private static final String DEFAULT_FEED_ID = "1";

    private static final Logger logger = LoggerFactory.getLogger(DataRouterPublisher.class);
    private final AppConfig datafileAppConfig;
    private DmaapProducerHttpClient dmaapProducerReactiveHttpClient;

    public DataRouterPublisher(AppConfig datafileAppConfig) {
        this.datafileAppConfig = datafileAppConfig;
    }

    /**
     * Publish one file.
     *
     * @param publishInfo information about the file to publish
     * @param numRetries the maximal number of retries if the publishing fails
     * @param firstBackoff the time to delay the first retry
     * @return the (same) filePublishInformation
     */
    public Mono<FilePublishInformation> publishFile(FilePublishInformation publishInfo, long numRetries,
            Duration firstBackoff) {
        MDC.setContextMap(publishInfo.getContext());
        dmaapProducerReactiveHttpClient = resolveClient();
        return Mono.just(publishInfo) //
                .cache() //
                .flatMap(this::publishFile) //
                .flatMap(httpStatus -> handleHttpResponse(httpStatus, publishInfo)) //
                .retryBackoff(numRetries, firstBackoff);
    }

    private Mono<HttpStatus> publishFile(FilePublishInformation publishInfo) {
        MDC.setContextMap(publishInfo.getContext());
        logger.trace("Entering publishFile with {}", publishInfo);
        try {
            HttpPut put = new HttpPut();
            prepareHead(publishInfo, put);
            prepareBody(publishInfo, put);
            dmaapProducerReactiveHttpClient.addUserCredentialsToHead(put);

            HttpResponse response =
                    dmaapProducerReactiveHttpClient.getDmaapProducerResponseWithRedirect(put, publishInfo.getContext());
            logger.trace("{}", response);
            return Mono.just(HttpStatus.valueOf(response.getStatusLine().getStatusCode()));
        } catch (Exception e) {
            logger.warn("Unable to send file to DataRouter. Data: {}", publishInfo.getInternalLocation(), e);
            return Mono.error(e);
        }
    }

    private void prepareHead(FilePublishInformation publishInfo, HttpPut put) {
        put.addHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE);
        JsonElement metaData = new JsonParser().parse(CommonFunctions.createJsonBody(publishInfo));
        metaData.getAsJsonObject().remove(NAME_JSON_TAG).getAsString();
        metaData.getAsJsonObject().remove(INTERNAL_LOCATION_JSON_TAG);
        metaData.getAsJsonObject().remove(CONTEXT_JSON_TAG);

        put.addHeader(X_DMAAP_DR_META, metaData.toString());
        put.setURI(getPublishUri(publishInfo.getName()));
        MappedDiagnosticContext.appendTraceInfo(put);
    }

    private void prepareBody(FilePublishInformation publishInfo, HttpPut put) throws IOException {
        Path fileLocation = publishInfo.getInternalLocation();
        try (InputStream fileInputStream = createInputStream(fileLocation)) {
            put.setEntity(new ByteArrayEntity(IOUtils.toByteArray(fileInputStream)));
        }
    }

    private URI getPublishUri(String fileName) {
        return dmaapProducerReactiveHttpClient.getBaseUri() //
                .pathSegment(PUBLISH_TOPIC) //
                .pathSegment(DEFAULT_FEED_ID) //
                .pathSegment(fileName).build();
    }

    private Mono<FilePublishInformation> handleHttpResponse(HttpStatus response, FilePublishInformation publishInfo) {
        MDC.setContextMap(publishInfo.getContext());
        if (HttpUtils.isSuccessfulResponseCode(response.value())) {
            logger.trace("Publish to DR successful!");
            return Mono.just(publishInfo);
        } else {
            logger.warn("Publish to DR unsuccessful, response code: {}", response);
            return Mono.error(new Exception("Publish to DR unsuccessful, response code: " + response));
        }
    }

    InputStream createInputStream(Path filePath) throws IOException {
        FileSystemResource realResource = new FileSystemResource(filePath);
        return realResource.getInputStream();
    }

    DmaapPublisherConfiguration resolveConfiguration() {
        return datafileAppConfig.getDmaapPublisherConfiguration();
    }

    DmaapProducerHttpClient resolveClient() {
        return new DmaapProducerHttpClient(resolveConfiguration());
    }
}
