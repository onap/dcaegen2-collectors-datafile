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
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.HttpHeaders;
import org.apache.http.client.utils.URIBuilder;
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

import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 7/4/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class DmaapProducerReactiveHttpClient {

    private static final String X_ATT_DR_META = "X-ATT-DR-META";
    private static final String LOCATION = "location";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private WebClient webClient;
    private final String dmaapHostName;
    private final Integer dmaapPortNumber;
    private final String dmaapProtocol;
    private final String dmaapTopicName;
    private final String dmaapContentType;

    /**
     * Constructor DmaapProducerReactiveHttpClient.
     *
     * @param dmaapPublisherConfiguration - DMaaP producer configuration object
     */
    public DmaapProducerReactiveHttpClient(DmaapPublisherConfiguration dmaapPublisherConfiguration) {

        this.dmaapHostName = dmaapPublisherConfiguration.dmaapHostName();
        this.dmaapProtocol = dmaapPublisherConfiguration.dmaapProtocol();
        this.dmaapPortNumber = dmaapPublisherConfiguration.dmaapPortNumber();
        this.dmaapTopicName = dmaapPublisherConfiguration.dmaapTopicName();
        this.dmaapContentType = dmaapPublisherConfiguration.dmaapContentType();
    }

    /**
     * Function for calling DMaaP HTTP producer - post request to DMaaP.
     *
     * @param consumerDmaapModelMono - object which will be sent to DMaaP
     * @return status code of operation
     */
    public Mono<String> getDmaapProducerResponse(Mono<List<ConsumerDmaapModel>> consumerDmaapModelMono) {
        consumerDmaapModelMono.subscribe(models -> postFilesAndData(models));
        return Mono.just(HttpStatus.OK.toString());
    }

    public DmaapProducerReactiveHttpClient createDmaapWebClient(WebClient webClient) {
        this.webClient = webClient;
        return this;
    }

    private void postFilesAndData(List<ConsumerDmaapModel> models) {
        for (ConsumerDmaapModel consumerDmaapModel : models) {
            postFileAndData(consumerDmaapModel);
        }
    }

    private void postFileAndData(ConsumerDmaapModel model) {
        RequestBodyUriSpec post = webClient.post();

        boolean headPrepared = prepareHead(model, post);

        if (headPrepared) {
            prepareBody(model, post);

            ResponseSpec responseSpec = post.retrieve();
            responseSpec.onStatus(HttpStatus::is4xxClientError,
                    clientResponse -> handlePostErrors(model, clientResponse));
            responseSpec.onStatus(HttpStatus::is5xxServerError,
                    clientResponse -> handlePostErrors(model, clientResponse));
            String bodyToMono = responseSpec.bodyToMono(String.class).block();
        }
    }

    private boolean prepareHead(ConsumerDmaapModel model, RequestBodyUriSpec post) {
        boolean result = true;
        try {
            post.header(HttpHeaders.CONTENT_TYPE, dmaapContentType);

            JsonElement metaData = new JsonParser().parse(CommonFunctions.createJsonBody(model));
            metaData.getAsJsonObject().remove(LOCATION);
            post.header(X_ATT_DR_META, metaData.toString());

            post.uri(getUri());
        } catch (Exception e) {
            logger.error("Unable to post file to Data Router. " + model, e);
            result = false;
        }

        return result;
    }

    private void prepareBody(ConsumerDmaapModel model, RequestBodyUriSpec post) {
        String fileLocation = model.getLocation();
        File fileResource = new File(fileLocation);
        FileSystemResource httpResource = new FileSystemResource(fileResource);
        post.body(BodyInserters.fromResource(httpResource));
    }

    private URI getUri() throws URISyntaxException {
        return new URIBuilder().setScheme(dmaapProtocol).setHost(dmaapHostName).setPort(dmaapPortNumber)
                .setPath(dmaapTopicName).build();
    }

    private Mono<Exception> handlePostErrors(ConsumerDmaapModel model, ClientResponse clientResponse) {
        String errorMessage = "Unable to post file to Data Router. " + model + "Reason: " + clientResponse.toString();
        logger.error(errorMessage);

        return Mono.error(new Exception(errorMessage));
    }
}
