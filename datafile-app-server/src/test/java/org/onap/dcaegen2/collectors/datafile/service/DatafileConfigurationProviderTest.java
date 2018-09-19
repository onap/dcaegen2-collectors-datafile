/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 NOKIA Intellectual Property. All rights reserved.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.dcaegen2.collectors.datafile.service;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.model.EnvProperties;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableEnvProperties;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class DatafileConfigurationProviderTest {
    private static final Gson gson = new Gson();
    private static final String configBindingService = "[{\"ID\":\"9c8dd674-34ce-7049-d318-e98d93a64303\",\"Node\""
        + ":\"dcae-bootstrap\",\"Address\":\"10.42.52.82\",\"Datacenter\":\"dc1\",\"TaggedAddresses\":"
        + "{\"lan\":\"10.42.52.82\",\"wan\":\"10.42.52.82\"},\"NodeMeta\":{\"consul-network-segment\":\"\"},"
        + "\"ServiceID\":\"dcae-cbs1\",\"ServiceName\":\"config-binding-service\",\"ServiceTags\":[],"
        + "\"ServiceAddress\":\"config-binding-service\",\"ServicePort\":10000,\"ServiceEnableTagOverride\":false,"
        + "\"CreateIndex\":14352,\"ModifyIndex\":14352},{\"ID\":\"35c6f540-a29c-1a92-23b0-1305bd8c81f5\",\"Node\":"
        + "\"dev-consul-server-1\",\"Address\":\"10.42.165.51\",\"Datacenter\":\"dc1\",\"TaggedAddresses\":"
        + "{\"lan\":\"10.42.165.51\",\"wan\":\"10.42.165.51\"},\"NodeMeta\":{\"consul-network-segment\":\"\"},"
        + "\"ServiceID\":\"dcae-cbs1\",\"ServiceName\":\"config-binding-service\",\"ServiceTags\":[],"
        + "\"ServiceAddress\":\"config-binding-service\",\"ServicePort\":10000,\"ServiceEnableTagOverride\":false,"
        + "\"CreateIndex\":803,\"ModifyIndex\":803}]";
    private static final JsonArray configBindingServiceJson = gson.fromJson(configBindingService, JsonArray.class);
    private static final JsonArray emptyConfigBindingServiceJson = gson.fromJson("[]", JsonArray.class);
    private static final String datafileCollectorMockConfiguration = "{\"test\":1}";
    private static final JsonObject datafileCollectorMockConfigurationJson = gson.fromJson(datafileCollectorMockConfiguration, JsonObject.class);

    private EnvProperties envProperties = ImmutableEnvProperties.builder()
        .appName("dcae-datafile-collector")
        .cbsName("config-binding-service")
        .consulHost("consul")
        .consulPort(8500)
        .build();

    @Test
    void shouldReturnDatafileCollectorConfiguration() {
        // given
        HttpGetClient webClient = mock(HttpGetClient.class);
        when(
            webClient.callHttpGet("http://consul:8500/v1/catalog/service/config-binding-service", JsonArray.class))
            .thenReturn(Mono.just(configBindingServiceJson));
        when(webClient.callHttpGet("http://config-binding-service:10000/service_component/dcae-datafile-collector", JsonObject.class))
            .thenReturn(Mono.just(datafileCollectorMockConfigurationJson));

        DatafileConfigurationProvider provider = new DatafileConfigurationProvider(webClient);

        //when/then
        StepVerifier.create(provider.callForDataFileCollectorConfiguration(envProperties)).expectSubscription()
            .expectNext(datafileCollectorMockConfigurationJson).verifyComplete();
    }

    @Test
    void shouldReturnMonoErrorWhenConsuleDoesntHaveConfigBindingServiceEntry() {
        // given
        HttpGetClient webClient = mock(HttpGetClient.class);
        when(
            webClient.callHttpGet("http://consul:8500/v1/catalog/service/config-binding-service", JsonArray.class))
            .thenReturn(Mono.just(emptyConfigBindingServiceJson));

        DatafileConfigurationProvider provider = new DatafileConfigurationProvider(webClient);

        //when/then
        StepVerifier.create(provider.callForDataFileCollectorConfiguration(envProperties)).expectSubscription()
            .expectError(IllegalStateException.class).verify();
    }
}