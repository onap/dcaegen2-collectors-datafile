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
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.configuration.DmaapBusControllerConfiguration;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.model.FeedData;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFeedData;
import org.onap.dcaegen2.collectors.datafile.service.HttpUtils;
import org.onap.dcaegen2.services.sdk.rest.services.model.logging.MdcVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/**
 * Creates the feed used to publish files to DataRouter.
 *
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
@Component
public class FeedCreator {
    private static final Logger logger = LoggerFactory.getLogger(DataRouterPublisher.class);
    private final AppConfig datafileAppConfig;

    @Autowired
    public FeedCreator(AppConfig datafileAppConfig) {
        this.datafileAppConfig = datafileAppConfig;
    }

    /**
     * Create the feed to publish on.
     *
     * @param numRetries the number of retries if the publishing fails
     * @param firstBackoff time to delay the first retry
     * @param contextMap context for logging.
     * @return data about the created feed.
     */
    public Mono<FeedData> createFeed(long numRetries, Duration firstBackoff, Map<String, String> contextMap) {
        logger.trace("Executing");
        MdcVariables.setMdcContextMap(contextMap);
        DmaapBusControllerHttpClient busControllerHttpClient = resolveClient();

        return Mono.just(datafileAppConfig.getDmaapBusControllerConfiguration().dmaapDrFeedName()) //
                .flatMap(feedName -> postCreateFeedRequest(feedName, busControllerHttpClient)) //
                .flatMap(this::handleHttpResponse) //
                .retryBackoff(numRetries, firstBackoff);
    }

    private Mono<HttpResponse> postCreateFeedRequest(String feedName,
            DmaapBusControllerHttpClient busControllerHttpClient) {
        logger.trace("Entering publishFile with {}", feedName);
        try {
            HttpPost post = new HttpPost();
            prepareHead(post, busControllerHttpClient);
            prepareBody(post, feedName);
            busControllerHttpClient.addUserCredentialsToHead(post);

            HttpResponse response = busControllerHttpClient.getDmaapBusControllerResponse(post, 1000);
            logger.trace(response.toString());
            return Mono.just(response);
        } catch (Exception e) {
            logger.error("Feed creation unsuccessful. Data: {}", feedName, e);
            return Mono.error(e);
        }
    }

    private void prepareHead(HttpPost post, DmaapBusControllerHttpClient busControllerHttpClient) {
        post.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        URI uri = busControllerHttpClient.getBaseUri() //
                .pathSegment("webapis") //
                .pathSegment("feeds") //
                .queryParam("useExisting", "true") //
                .build();
        post.setURI(uri);
    }

    private void prepareBody(HttpPost post, String feedName) throws IOException {
        JsonObject paramsAsJson = new JsonObject();
        paramsAsJson.addProperty("feedName", feedName);
        paramsAsJson.addProperty("feedVersion", "1");
        paramsAsJson.addProperty("feedDescription", "Bulk PM file feed");
        paramsAsJson.addProperty("owner", "Datafile Collector");
        paramsAsJson.addProperty("asprClassification", "unclassified");
        StringEntity params = new StringEntity(paramsAsJson.toString());
        post.setEntity(params);
    }

    private Mono<FeedData> handleHttpResponse(HttpResponse response) {

        try {
            if (HttpUtils.isSuccessfulResponseCode(response.getStatusLine().getStatusCode())) {
                logger.trace("Feed creation successful!");
                FeedData feedData = getFeedDataFromJson(response);

                return Mono.just(feedData);
            } else {
                logger.warn("Feed creation unsuccessful, response code: " + response);
                return Mono.error(new DatafileTaskException("Feed creation unsuccessful, response code: " + response));
            }
        } catch (JsonIOException | JsonSyntaxException | UnsupportedOperationException | IOException e) {
            logger.error("Feed creation unsuccesful.", e);
            return Mono.error(e);
        }
    }

    private FeedData getFeedDataFromJson(HttpResponse response) throws IOException {
        JsonElement element = new JsonParser().parse(new InputStreamReader(response.getEntity().getContent()));

        JsonObject feedInfo = element.getAsJsonObject();
        JsonObject pubData = (JsonObject) feedInfo.get("pubs").getAsJsonArray().get(0);
        return ImmutableFeedData.builder() //
                .publishedCheckUrl(feedInfo.get("logURL").getAsString()) //
                .publishUrl(feedInfo.get("publishURL").getAsString()) //
                .username(pubData.get("username").getAsString()) //
                .password(pubData.get("userpwd").getAsString()) //
                .build();
    }

    DmaapBusControllerConfiguration resolveConfiguration() {
        return datafileAppConfig.getDmaapBusControllerConfiguration();
    }

    DmaapBusControllerHttpClient resolveClient() {
        return new DmaapBusControllerHttpClient(resolveConfiguration());
    }
}
