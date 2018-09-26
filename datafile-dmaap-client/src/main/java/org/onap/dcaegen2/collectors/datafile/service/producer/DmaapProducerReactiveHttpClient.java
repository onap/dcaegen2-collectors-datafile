/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.service.producer;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.File;
import java.net.URI;

import org.apache.http.HttpHeaders;
import org.onap.dcaegen2.collectors.datafile.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.model.CommonFunctions;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.util.DefaultUriBuilderFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 7/4/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class DmaapProducerReactiveHttpClient {

    private static final String X_ATT_DR_META = "X-ATT-DR-META";
    private static final String NAME_JSON_TAG = "name";
    private static final String LOCATION_JSON_TAG = "location";
    private static final String DEFAULT_FEED_ID = "1";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private WebClient webClient;
    private final String dmaapHostName;
    private final Integer dmaapPortNumber;
    private final String dmaapTopicName;
    private final String dmaapProtocol;
    private final String dmaapContentType;

    /**
     * Constructor DmaapProducerReactiveHttpClient.
     *
     * @param dmaapPublisherConfiguration - DMaaP producer configuration object
     */
    public DmaapProducerReactiveHttpClient(DmaapPublisherConfiguration dmaapPublisherConfiguration) {

        this.dmaapHostName = dmaapPublisherConfiguration.dmaapHostName();
        this.dmaapPortNumber = dmaapPublisherConfiguration.dmaapPortNumber();
        this.dmaapTopicName = dmaapPublisherConfiguration.dmaapTopicName();
        this.dmaapProtocol = dmaapPublisherConfiguration.dmaapProtocol();
        this.dmaapContentType = dmaapPublisherConfiguration.dmaapContentType();
    }

    /**
     * Function for calling DMaaP HTTP producer - post request to DMaaP.
     *
     * @param consumerDmaapModel - object which will be sent to DMaaP
     * @return status code of operation
     */
    public Flux<String> getDmaapProducerResponse(ConsumerDmaapModel consumerDmaapModel) {
        logger.trace("Entering getDmaapProducerResponse with {}", consumerDmaapModel);

        RequestBodyUriSpec post = webClient.post();

        prepareHead(consumerDmaapModel, post);

        prepareBody(consumerDmaapModel, post);

        ResponseSpec responseSpec = post.retrieve();
        responseSpec.onStatus(HttpStatus::is4xxClientError, clientResponse -> handlePostErrors(consumerDmaapModel, clientResponse));
        responseSpec.onStatus(HttpStatus::is5xxServerError, clientResponse -> handlePostErrors(consumerDmaapModel, clientResponse));
        Flux<String> response = responseSpec.bodyToFlux(String.class);

        logger.trace("Exiting getDmaapProducerResponse with {}", response);
        return response;
    }

    public DmaapProducerReactiveHttpClient createDmaapWebClient(WebClient webClient) {
        this.webClient = webClient;
        return this;
    }

    private void prepareHead(ConsumerDmaapModel model, RequestBodyUriSpec post) {
        post.header(HttpHeaders.CONTENT_TYPE, dmaapContentType);

        JsonElement metaData = new JsonParser().parse(CommonFunctions.createJsonBody(model));
        String name = metaData.getAsJsonObject().remove(NAME_JSON_TAG).getAsString();
        metaData.getAsJsonObject().remove(LOCATION_JSON_TAG);
        post.header(X_ATT_DR_META, metaData.toString());

        post.uri(getUri(name));
    }

    private void prepareBody(ConsumerDmaapModel model, RequestBodyUriSpec post) {
        String fileLocation = model.getLocation();
        File fileResource = new File(fileLocation);
        FileSystemResource httpResource = new FileSystemResource(fileResource);
        post.body(BodyInserters.fromResource(httpResource));
    }

    private URI getUri(String fileName) {
        String path = dmaapTopicName + "/" + DEFAULT_FEED_ID + "/" + fileName;
        return new DefaultUriBuilderFactory().builder().scheme(dmaapProtocol).host(dmaapHostName).port(dmaapPortNumber)
                .path(path).build();
    }

    private Mono<Exception> handlePostErrors(ConsumerDmaapModel model, ClientResponse clientResponse) {
        String errorMessage = "Unable to post file to Data Router. " + model + "Reason: " + clientResponse.toString();
        logger.error(errorMessage);

        return Mono.error(new Exception(errorMessage));
    }
}
