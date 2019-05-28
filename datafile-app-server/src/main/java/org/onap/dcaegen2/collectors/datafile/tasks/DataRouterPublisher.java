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
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.configuration.PublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.model.FilePublishInformation;
import org.onap.dcaegen2.collectors.datafile.model.JsonSerializer;
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
import org.springframework.web.util.DefaultUriBuilderFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(DataRouterPublisher.class);
    private final AppConfig datafileAppConfig;

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
            DmaapProducerHttpClient dmaapProducerHttpClient = resolveClient(publishInfo.getChangeIdentifier());
            HttpPut put = new HttpPut();
            prepareHead(publishInfo, put);
            prepareBody(publishInfo, put);
            dmaapProducerHttpClient.addUserCredentialsToHead(put);

            HttpResponse response =
                    dmaapProducerHttpClient.getDmaapProducerResponseWithRedirect(put, publishInfo.getContext());
            logger.trace("{}", response);
            return Mono.just(HttpStatus.valueOf(response.getStatusLine().getStatusCode()));
        } catch (Exception e) {
            logger.warn("Unable to send file to DataRouter. Data: {}", publishInfo.getInternalLocation(), e);
            return Mono.error(e);
        }
    }

    private void prepareHead(FilePublishInformation publishInfo, HttpPut put) throws DatafileTaskException {

        put.addHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE);
        JsonElement metaData = new JsonParser().parse(JsonSerializer.createJsonBodyForDataRouter(publishInfo));
        put.addHeader(X_DMAAP_DR_META, metaData.toString());
        URI uri = new DefaultUriBuilderFactory(
                datafileAppConfig.getPublisherConfiguration(publishInfo.getChangeIdentifier()).publishUrl()) //
                .builder() //
                .pathSegment(publishInfo.getName()) //
                .build();
        put.setURI(uri);

        MappedDiagnosticContext.appendTraceInfo(put);
    }

    private void prepareBody(FilePublishInformation publishInfo, HttpPut put) throws IOException {
        Path fileLocation = publishInfo.getInternalLocation();
        try (InputStream fileInputStream = createInputStream(fileLocation)) {
            put.setEntity(new ByteArrayEntity(IOUtils.toByteArray(fileInputStream)));
        }
    }

    private static Mono<FilePublishInformation> handleHttpResponse(HttpStatus response, FilePublishInformation publishInfo) {
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

    PublisherConfiguration resolveConfiguration(String changeIdentifer) throws DatafileTaskException {
        return datafileAppConfig.getPublisherConfiguration(changeIdentifer);
    }

    DmaapProducerHttpClient resolveClient(String changeIdentifier) throws DatafileTaskException {
        try {
            DmaapPublisherConfiguration cfg = resolveConfiguration(changeIdentifier).toDmaap();
            return new DmaapProducerHttpClient(cfg);
        } catch (MalformedURLException e) {
            throw new DatafileTaskException("Cannot resolve producer client", e);
        }

    }
}
