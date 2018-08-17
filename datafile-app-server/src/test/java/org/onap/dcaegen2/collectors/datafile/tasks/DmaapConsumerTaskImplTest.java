/*
 * ============LICENSE_START=======================================================
 * Datafile Collector Service
 * ================================================================================
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

package org.onap.dcaegen2.collectors.datafile.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.dcaegen2.collectors.datafile.config.DmaapConsumerConfiguration;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.exceptions.DmaapEmptyResponseException;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.service.DmaapConsumerJsonParser;
import org.onap.dcaegen2.collectors.datafile.service.consumer.DMaaPConsumerReactiveHttpClient;
import org.onap.dcaegen2.collectors.datafile.tasks.DmaapConsumerTaskImpl;
import org.onap.dcaegen2.collectors.datafile.config.ImmutableDmaapConsumerConfiguration;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableConsumerDmaapModel;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 5/17/18
 */
class DmaapConsumerTaskImplTest {

    private static ConsumerDmaapModel consumerDmaapModel;
    private static DmaapConsumerTaskImpl dmaapConsumerTask;
    private static DMaaPConsumerReactiveHttpClient dMaaPConsumerReactiveHttpClient;
    private static AppConfig appConfig;
    private static DmaapConsumerConfiguration dmaapConsumerConfiguration;
    private static String message;
    private static String parsed;

    @BeforeAll
    static void setUp() {
        dmaapConsumerConfiguration = new ImmutableDmaapConsumerConfiguration.Builder().consumerGroup("OpenDCAE-c12")
            .consumerId("c12").dmaapContentType("application/json").dmaapHostName("54.45.33.2").dmaapPortNumber(1234)
            .dmaapProtocol("https").dmaapUserName("Datafile").dmaapUserPassword("Datafile")
            .dmaapTopicName("unauthenticated.SEC_OTHER_OUTPUT").timeoutMs(-1).messageLimit(-1).build();

        consumerDmaapModel = ImmutableConsumerDmaapModel.builder().ipv4("10.16.123.234")
            .ipv6("0:0:0:0:0:FFFF:0A10:7BEA")
            .pnfName("NOKQTFCOC540002E").build();
        appConfig = mock(AppConfig.class);
        message =
            "[{\"event\":{\"commonEventHeader\":{\"domain\":\"other\",\"eventId\":\"<<SerialNumber>>-reg\","
                + "\"eventName\":\"pnfRegistration_5GDU\",\"eventType\":\"pnfRegistration\",\"internalHeaderFields\""
                + ":{},\"lastEpochMicrosec\":1519837825682,\"nfNamingCode\":\"5GRAN\",\"nfcNamingCode\":\"5DU\","
                + "\"priority\":\"Normal\",\"reportingEntityName\":\"5GRAN_DU\",\"sequence\":0,\"sourceId\":"
                + "\"<<SerialNumber>>\",\"sourceName\":\"5GRAN_DU\",\"startEpochMicrosec\":1519837825682,\"version\""
                + ":3},\"otherFields\":{\"otherFieldsVersion\":1,\"pnfFamily\":\"BBU\",\"pnfLastServiceDate\":"
                + "1517206400,\"pnfManufactureDate\":1516406400,\"pnfModelNumber\":\"AJ02\",\"pnfOamIpv4Address\":"
                + "\"10.16.123.234\",\"pnfOamIpv6Address\":\"0:0:0:0:0:FFFF:0A10:7BEA\",\"pnfSerialNumber\":"
                + "\"QTFCOC540002E\",\"pnfSoftwareVersion\":\"v4.5.0.1\",\"pnfType\":\"AirScale\",\"pnfVendorName\":"
                + "\"Nokia\"}}}]";
        parsed =
            "{\"event\":{\"commonEventHeader\":{\"domain\":\"other\",\"eventId\":\"<<SerialNumber>>-reg\",\"eventName\""
                + ":\"pnfRegistration_5GDU\",\"eventType\":\"pnfRegistration\",\"internalHeaderFields\":{},"
                + "\"lastEpochMicrosec\":1519837825682,\"nfNamingCode\":\"5GRAN\",\"nfcNamingCode\":\"5DU\","
                + "\"priority\":\"Normal\",\"reportingEntityName\":\"5GRAN_DU\",\"sequence\":0,\"sourceId\":"
                + "\"<<SerialNumber>>\",\"sourceName\":\"5GRAN_DU\",\"startEpochMicrosec\":1519837825682,"
                + "\"version\":3},\"otherFields\":{\"otherFieldsVersion\":1,\"pnfFamily\":\"BBU\","
                + "\"pnfLastServiceDate\":1517206400,\"pnfManufactureDate\":1516406400,\"pnfModelNumber\":\"AJ02\","
                + "\"pnfOamIpv4Address\":\"10.16.123.234\",\"pnfOamIpv6Address\":\"0:0:0:0:0:FFFF:0A10:7BEA\","
                + "\"pnfSerialNumber\":\"QTFCOC540002E\",\"pnfSoftwareVersion\":\"v4.5.0.1\",\"pnfType\":\"AirScale\","
                + "\"pnfVendorName\":\"Nokia\"}}}";
    }

    @Test
    void whenPassedObjectDoesntFit_DoesNotThrowDatafileTaskException() {
        //given
        prepareMocksForDmaapConsumer(Optional.empty());

        //then
        StepVerifier.create(dmaapConsumerTask.execute("Sample input")).expectSubscription()
            .expectError(DmaapEmptyResponseException.class).verify();

        verify(dMaaPConsumerReactiveHttpClient, times(1)).getDMaaPConsumerResponse();
    }

    @Test
    void whenPassedObjectFits_ReturnsCorrectResponse() {
        //given
        prepareMocksForDmaapConsumer(Optional.of(message));
        //when
        Mono<ConsumerDmaapModel> response = dmaapConsumerTask.execute("Sample input");

        //then
        verify(dMaaPConsumerReactiveHttpClient, times(1)).getDMaaPConsumerResponse();
        assertEquals(consumerDmaapModel, response.block());


    }

    private void prepareMocksForDmaapConsumer(Optional<String> message) {
        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsed);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject()))
            .when(dmaapConsumerJsonParser).getJsonObjectFromAnArray(jsonElement);
        dMaaPConsumerReactiveHttpClient = mock(DMaaPConsumerReactiveHttpClient.class);
        when(dMaaPConsumerReactiveHttpClient.getDMaaPConsumerResponse()).thenReturn(Mono.just(message.orElse("")));
        when(appConfig.getDmaapConsumerConfiguration()).thenReturn(dmaapConsumerConfiguration);
        dmaapConsumerTask = spy(new DmaapConsumerTaskImpl(appConfig, dmaapConsumerJsonParser));
        when(dmaapConsumerTask.resolveConfiguration()).thenReturn(dmaapConsumerConfiguration);
        doReturn(dMaaPConsumerReactiveHttpClient).when(dmaapConsumerTask).resolveClient();
    }
}