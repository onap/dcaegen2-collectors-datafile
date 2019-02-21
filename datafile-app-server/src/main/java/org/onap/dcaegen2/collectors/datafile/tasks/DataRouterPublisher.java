/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2019 Nordix Foundation. All rights reserved.
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
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.service.HttpUtils;
import org.onap.dcaegen2.collectors.datafile.service.producer.DmaapProducerReactiveHttpClient;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapPublisherConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import reactor.core.publisher.Flux;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 4/13/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class DataRouterPublisher {
    private static final String X_DMAAP_DR_META = "X-DMAAP-DR-META";
    private static final String CONTENT_TYPE = "application/octet-stream";
    private static final String NAME_JSON_TAG = "name";
    private static final String INTERNAL_LOCATION_JSON_TAG = "internalLocation";
    private static final String PUBLISH_TOPIC = "publish";
    private static final String DEFAULT_FEED_ID = "1";

    private static final Logger logger = LoggerFactory.getLogger(DataRouterPublisher.class);
    private final AppConfig datafileAppConfig;
    private DmaapProducerReactiveHttpClient dmaapProducerReactiveHttpClient;

    public DataRouterPublisher(AppConfig datafileAppConfig) {
        this.datafileAppConfig = datafileAppConfig;
    }


    /**
     * Publish one file
     *
     * @param consumerDmaapModel information about the file to publish
     * @param maxNumberOfRetries the maximal number of retries if the publishing fails
     * @param firstBackoffTimeout the time to delay the first retry
     * @return the HTTP response status as a string
     */
    public Flux<ConsumerDmaapModel> execute(ConsumerDmaapModel model, long numRetries, Duration firstBackoff) {
        logger.trace("Method called with arg {}", model);
        dmaapProducerReactiveHttpClient = resolveClient();

        return Flux.just(model) //
                .cache(1) //
                .flatMap(this::publishFile) //
                .flatMap(httpStatus -> handleHttpResponse(httpStatus, model)) //
                .retryBackoff(numRetries, firstBackoff);
    }

    private Flux<HttpStatus> publishFile(ConsumerDmaapModel consumerDmaapModel) {
        logger.trace("Entering publishFile with {}", consumerDmaapModel);
        try {
            HttpPut put = new HttpPut();
            prepareHead(consumerDmaapModel, put);
            prepareBody(consumerDmaapModel, put);
            dmaapProducerReactiveHttpClient.addUserCredentialsToHead(put);

            HttpResponse response = dmaapProducerReactiveHttpClient.getDmaapProducerResponse(put);
            logger.trace(response.toString());
            return Flux.just(HttpStatus.valueOf(response.getStatusLine().getStatusCode()));
        } catch (Exception e) {
            logger.error("Unable to send file to DataRouter. Data: {}", consumerDmaapModel.getInternalLocation(), e);
            return Flux.error(e);
        }
    }

    private void prepareHead(ConsumerDmaapModel model, HttpPut put) {
        put.addHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE);
        JsonElement metaData = new JsonParser().parse(CommonFunctions.createJsonBody(model));
        metaData.getAsJsonObject().remove(NAME_JSON_TAG).getAsString();
        metaData.getAsJsonObject().remove(INTERNAL_LOCATION_JSON_TAG);
        put.addHeader(X_DMAAP_DR_META, metaData.toString());
        put.setURI(getPublishUri(model.getInternalLocation().getFileName().toString()));
    }

    private void prepareBody(ConsumerDmaapModel model, HttpPut put) throws IOException {
        Path fileLocation = model.getInternalLocation();
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

    private Flux<ConsumerDmaapModel> handleHttpResponse(HttpStatus response, ConsumerDmaapModel model) {

        if (HttpUtils.isSuccessfulResponseCode(response.value())) {
            logger.trace("Publish to DR successful!");
            return Flux.just(model);
        } else {
            logger.warn("Publish to DR unsuccessful, response code: " + response);
            return Flux.error(new Exception("Publish to DR unsuccessful, response code: " + response));
        }
    }

    InputStream createInputStream(Path filePath) throws IOException {
        FileSystemResource realResource = new FileSystemResource(filePath);
        return realResource.getInputStream();
    }

    DmaapPublisherConfiguration resolveConfiguration() {
        return datafileAppConfig.getDmaapPublisherConfiguration();
    }

    DmaapProducerReactiveHttpClient resolveClient() {
        return new DmaapProducerReactiveHttpClient(resolveConfiguration());
    }
}
