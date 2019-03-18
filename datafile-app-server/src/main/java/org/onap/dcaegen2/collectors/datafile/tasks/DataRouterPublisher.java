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

import static org.onap.dcaegen2.services.sdk.rest.services.model.logging.MdcVariables.REQUEST_ID;
import static org.onap.dcaegen2.services.sdk.rest.services.model.logging.MdcVariables.X_INVOCATION_ID;
import static org.onap.dcaegen2.services.sdk.rest.services.model.logging.MdcVariables.X_ONAP_REQUEST_ID;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.onap.dcaegen2.collectors.datafile.model.CommonFunctions;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.FeedData;
import org.onap.dcaegen2.collectors.datafile.service.HttpUtils;
import org.onap.dcaegen2.collectors.datafile.service.producer.DmaapProducerReactiveHttpClient;
import org.onap.dcaegen2.services.sdk.rest.services.model.logging.MdcVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.DefaultUriBuilderFactory;

import reactor.core.publisher.Mono;

/**
 * Stateless component used to publish a file to the DataRouter.
 *
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 4/13/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class DataRouterPublisher {
    private static final String X_DMAAP_DR_META = "X-DMAAP-DR-META";
    private static final String CONTENT_TYPE = "application/octet-stream";
    private static final String NAME_JSON_TAG = "name";
    private static final String INTERNAL_LOCATION_JSON_TAG = "internalLocation";

    private static final Logger logger = LoggerFactory.getLogger(DataRouterPublisher.class);

    /**
     * Publish one file.
     *
     * @param model information about the file to publish
     * @param feedData data about the DataRouter feed.
     * @param numRetries the number of retries if the publishing fails
     * @param firstBackoff the time to delay the first retry
     *
     * @return a <code>Mono</code> containing the <code>ConsumerDmaapModel</code> for the file.
     */
    public Mono<ConsumerDmaapModel> execute(ConsumerDmaapModel model, FeedData feedData, long numRetries,
            Duration firstBackoff, Map<String, String> contextMap) {
        MdcVariables.setMdcContextMap(contextMap);
        logger.trace("Method called with arg {}", model);
        DmaapProducerReactiveHttpClient datarouterHttpClient = createClient();

        return Mono.just(model).cache()
                .flatMap(consumerModel -> publishFile(consumerModel, feedData, datarouterHttpClient, contextMap)) //
                .flatMap(httpStatus -> handleHttpResponse(httpStatus, model, contextMap)) //
                .retryBackoff(numRetries, firstBackoff);
    }

    private Mono<HttpStatus> publishFile(ConsumerDmaapModel consumerDmaapModel, FeedData feedData,
            DmaapProducerReactiveHttpClient datarouterHttpClient, Map<String, String> contextMap) {
        logger.trace("Entering publishFile with {}", consumerDmaapModel);
        try {
            HttpPut put = new HttpPut();
            String requestId = MDC.get(REQUEST_ID);
            put.addHeader(X_ONAP_REQUEST_ID, requestId);
            String invocationId = UUID.randomUUID().toString();
            put.addHeader(X_INVOCATION_ID, invocationId);

            prepareHead(consumerDmaapModel, put);

            put.setURI(getCompletePublishUri(feedData.publishUrl(),
                    consumerDmaapModel.getInternalLocation().getFileName().toString()));
            prepareBody(consumerDmaapModel, put);

            feedData.addUserCredentialsToHead(put);

            HttpResponse response = datarouterHttpClient.getDmaapProducerResponseWithRedirect(put, contextMap);
            logger.trace(response.toString());
            return Mono.just(HttpStatus.valueOf(response.getStatusLine().getStatusCode()));
        } catch (Exception e) {
            logger.error("Unable to send file to DataRouter. Data: {}", consumerDmaapModel.getInternalLocation(), e);
            return Mono.error(e);
        }
    }

    private void prepareHead(ConsumerDmaapModel model, HttpPut put) {
        put.addHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE);
        JsonElement metaData = new JsonParser().parse(CommonFunctions.createJsonBody(model));
        metaData.getAsJsonObject().remove(NAME_JSON_TAG).getAsString();
        metaData.getAsJsonObject().remove(INTERNAL_LOCATION_JSON_TAG);
        put.addHeader(X_DMAAP_DR_META, metaData.toString());
    }

    private void prepareBody(ConsumerDmaapModel model, HttpPut put) throws IOException {
        Path fileLocation = model.getInternalLocation();
        try (InputStream fileInputStream = createInputStream(fileLocation)) {
            put.setEntity(new ByteArrayEntity(IOUtils.toByteArray(fileInputStream)));
        }
    }

    private URI getCompletePublishUri(String publishUri, String fileName) {
        return new DefaultUriBuilderFactory() //
                .uriString(publishUri) //
                .pathSegment(fileName) //
                .build();
    }

    private Mono<ConsumerDmaapModel> handleHttpResponse(HttpStatus response, ConsumerDmaapModel model,
            Map<String, String> contextMap) {
        MdcVariables.setMdcContextMap(contextMap);
        if (HttpUtils.isSuccessfulResponseCode(response.value())) {
            logger.trace("Publish to DR successful!");
            return Mono.just(model);
        } else {
            logger.warn("Publish to DR unsuccessful, response code: {}", response);
            return Mono.error(new Exception("Publish to DR unsuccessful, response code: " + response));
        }
    }

    InputStream createInputStream(Path filePath) throws IOException {
        FileSystemResource realResource = new FileSystemResource(filePath);
        return realResource.getInputStream();
    }

    DmaapProducerReactiveHttpClient createClient() {
        return new DmaapProducerReactiveHttpClient();
    }
}
