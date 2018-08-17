/*
 * ============LICENSE_START=======================================================
 * PNF-REGISTRATION-HANDLER
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

package org.onap.dcaegen2.services.prh.service;

import static org.mockito.Mockito.spy;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.dcaegen2.services.prh.exceptions.DmaapNotFoundException;
import org.onap.dcaegen2.services.prh.model.ConsumerDmaapModel;
import org.onap.dcaegen2.services.prh.model.ImmutableConsumerDmaapModel;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 5/8/18
 */
class DmaapConsumerJsonParserTest {

    @Test
    void whenPassingCorrectJson_validationNotThrowingAnException() {
        //given
        String message =
            "[{\"event\":{\"commonEventHeader\":{\"domain\":\"other\",\"eventId\":\"<<SerialNumber>>-reg\","
                + "\"eventName\":\"pnfRegistration_5GDU\",\"eventType\":\"pnfRegistration\",\"internalHeaderFields\""
                + ":{},\"lastEpochMicrosec\":1519837825682,\"nfNamingCode\":\"5GRAN\",\"nfcNamingCode\":\"5DU\","
                + "\"priority\":\"Normal\",\"reportingEntityName\":\"5GRAN_DU\",\"sequence\":0,\"sourceId\":"
                + "\"<<SerialNumber>>\",\"sourceName\":\"5GRAN_DU\",\"startEpochMicrosec\":1519837825682,\"version\":"
                + "3},\"otherFields\":{\"otherFieldsVersion\":1,\"pnfFamily\":\"BBU\",\"pnfLastServiceDate\":1517206400"
                + ",\"pnfManufactureDate\":1516406400,\"pnfModelNumber\":\"AJ02\",\"pnfOamIpv4Address\":"
                + "\"10.16.123.234\",\"pnfOamIpv6Address\":\"0:0:0:0:0:FFFF:0A10:7BEA\",\"pnfSerialNumber\":"
                + "\"QTFCOC540002E\",\"pnfSoftwareVersion\":\"v4.5.0.1\",\"pnfType\":\"AirScale\",\"pnfVendorName\":"
                + "\"Nokia\"}}}]";

        String parsed =
            "{\"event\":{\"commonEventHeader\":{\"domain\":\"other\",\"eventId\":\"<<SerialNumber>>-reg\",\"eventName\""
                + ":\"pnfRegistration_5GDU\",\"eventType\":\"pnfRegistration\",\"internalHeaderFields\":{},"
                + "\"lastEpochMicrosec\":1519837825682,\"nfNamingCode\":\"5GRAN\",\"nfcNamingCode\":\"5DU\","
                + "\"priority\":\"Normal\",\"reportingEntityName\":\"5GRAN_DU\",\"sequence\":0,\"sourceId\":"
                + "\"<<SerialNumber>>\",\"sourceName\":\"5GRAN_DU\",\"startEpochMicrosec\":1519837825682,\"version\":"
                + "3},\"otherFields\":{\"otherFieldsVersion\":1,\"pnfFamily\":\"BBU\",\"pnfLastServiceDate\":"
                + "1517206400,\"pnfManufactureDate\":1516406400,\"pnfModelNumber\":\"AJ02\",\"pnfOamIpv4Address\":"
                + "\"10.16.123.234\",\"pnfOamIpv6Address\":\"0:0:0:0:0:FFFF:0A10:7BEA\",\"pnfSerialNumber\":"
                + "\"QTFCOC540002E\",\"pnfSoftwareVersion\":\"v4.5.0.1\",\"pnfType\":\"AirScale\",\"pnfVendorName\":"
                + "\"Nokia\"}}}";
        ConsumerDmaapModel expectedObject = ImmutableConsumerDmaapModel.builder().ipv4("10.16.123.234")
            .ipv6("0:0:0:0:0:FFFF:0A10:7BEA")
            .pnfName("NOKQTFCOC540002E").build();
        //when
        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsed);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject()))
            .when(dmaapConsumerJsonParser).getJsonObjectFromAnArray(jsonElement);
        ConsumerDmaapModel consumerDmaapModel = dmaapConsumerJsonParser
            .getJsonObject(Mono.just((message))).block();
        //then
        Assertions.assertNotNull(consumerDmaapModel);
        Assertions.assertEquals(expectedObject, consumerDmaapModel);
    }

    @Test
    void whenPassingCorrectJsonWithoutIpv4_validationNotThrowingAnException() {
        //given
        String message =
            "[{\"event\":{\"commonEventHeader\":{\"domain\":\"other\",\"eventId\":\"<<SerialNumber>>-reg\","
                + "\"eventName\":\"pnfRegistration_5GDU\",\"eventType\":\"pnfRegistration\",\"internalHeaderFields\":"
                + "{},\"lastEpochMicrosec\":1519837825682,\"nfNamingCode\":\"5GRAN\",\"nfcNamingCode\":\"5DU\","
                + "\"priority\":\"Normal\",\"reportingEntityName\":\"5GRAN_DU\",\"sequence\":0,\"sourceId\":"
                + "\"<<SerialNumber>>\",\"sourceName\":\"5GRAN_DU\",\"startEpochMicrosec\":1519837825682,\"version\":3}"
                + ",\"otherFields\":{\"otherFieldsVersion\":1,\"pnfFamily\":\"BBU\",\"pnfLastServiceDate\":1517206400,"
                + "\"pnfManufactureDate\":1516406400,\"pnfModelNumber\":\"AJ02\",\"pnfOamIpv6Address\":"
                + "\"0:0:0:0:0:FFFF:0A10:7BEA\",\"pnfSerialNumber\":\"QTFCOC540002E\",\"pnfSoftwareVersion\""
                + ":\"v4.5.0.1\",\"pnfType\":\"AirScale\",\"pnfVendorName\":\"Nokia\"}}}]";

        String parsed =
            "{\"event\":{\"commonEventHeader\":{\"domain\":\"other\",\"eventId\":\"<<SerialNumber>>-reg\",\"eventName\""
                + ":\"pnfRegistration_5GDU\",\"eventType\":\"pnfRegistration\",\"internalHeaderFields\":{},"
                + "\"lastEpochMicrosec\":1519837825682,\"nfNamingCode\":\"5GRAN\",\"nfcNamingCode\":\"5DU\","
                + "\"priority\":\"Normal\",\"reportingEntityName\":\"5GRAN_DU\",\"sequence\":0,\"sourceId\":"
                + "\"<<SerialNumber>>\",\"sourceName\":\"5GRAN_DU\",\"startEpochMicrosec\":1519837825682,\"version\":3}"
                + ",\"otherFields\":{\"otherFieldsVersion\":1,\"pnfFamily\":\"BBU\",\"pnfLastServiceDate\":1517206400,"
                + "\"pnfManufactureDate\":1516406400,\"pnfModelNumber\":\"AJ02\",\"pnfOamIpv6Address\":"
                + "\"0:0:0:0:0:FFFF:0A10:7BEA\",\"pnfSerialNumber\":\"QTFCOC540002E\",\"pnfSoftwareVersion\""
                + ":\"v4.5.0.1\",\"pnfType\":\"AirScale\",\"pnfVendorName\":\"Nokia\"}}}";

        //when
        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsed);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject()))
            .when(dmaapConsumerJsonParser).getJsonObjectFromAnArray(jsonElement);
        dmaapConsumerJsonParser.getJsonObject(Mono.just((message)));
        ConsumerDmaapModel consumerDmaapModel = dmaapConsumerJsonParser.getJsonObject(Mono.just((message)))
            .block();
        //then
        ConsumerDmaapModel expectedObject = ImmutableConsumerDmaapModel.builder().ipv4("")
            .ipv6("0:0:0:0:0:FFFF:0A10:7BEA")
            .pnfName("NOKQTFCOC540002E").build();
        Assertions.assertNotNull(consumerDmaapModel);
        Assertions.assertEquals(expectedObject, consumerDmaapModel);
    }

    @Test
    void whenPassingCorrectJsonWihoutIpv6_validationNotThrowingAnException() {
        //given
        String message =
            "[{\"event\":{\"commonEventHeader\":{\"domain\":\"other\",\"eventId\":\"<<SerialNumber>>-reg\","
                + "\"eventName\":\"pnfRegistration_5GDU\",\"eventType\":\"pnfRegistration\",\"internalHeaderFields\":"
                + "{},\"lastEpochMicrosec\":1519837825682,\"nfNamingCode\":\"5GRAN\",\"nfcNamingCode\":\"5DU\","
                + "\"priority\":\"Normal\",\"reportingEntityName\":\"5GRAN_DU\",\"sequence\":0,\"sourceId\":"
                + "\"<<SerialNumber>>\",\"sourceName\":\"5GRAN_DU\",\"startEpochMicrosec\":1519837825682,"
                + "\"version\":3},\"otherFields\":{\"otherFieldsVersion\":1,\"pnfFamily\":\"BBU\",\"pnfLastServiceDate"
                + "\":1517206400,\"pnfManufactureDate\":1516406400,\"pnfModelNumber\":\"AJ02\",\"pnfOamIpv4Address"
                + "\":\"10.16.123.234\",\"pnfSerialNumber\":\"QTFCOC540002E\",\"pnfSoftwareVersion\":\"v4.5.0.1\","
                + "\"pnfType\":\"AirScale\",\"pnfVendorName\":\"Nokia\"}}}]";
        String parsed =
            "{\"event\":{\"commonEventHeader\":{\"domain\":\"other\",\"eventId\":\"<<SerialNumber>>-reg\",\"eventName\""
                + ":\"pnfRegistration_5GDU\",\"eventType\":\"pnfRegistration\",\"internalHeaderFields\":{},"
                + "\"lastEpochMicrosec\":1519837825682,\"nfNamingCode\":\"5GRAN\",\"nfcNamingCode\":\"5DU\","
                + "\"priority\":\"Normal\",\"reportingEntityName\":\"5GRAN_DU\",\"sequence\":0,\"sourceId\":"
                + "\"<<SerialNumber>>\",\"sourceName\":\"5GRAN_DU\",\"startEpochMicrosec\":1519837825682,"
                + "\"version\":3},\"otherFields\":{\"otherFieldsVersion\":1,\"pnfFamily\":\"BBU\",\"pnfLastServiceDate"
                + "\":1517206400,\"pnfManufactureDate\":1516406400,\"pnfModelNumber\":\"AJ02\",\"pnfOamIpv4Address"
                + "\":\"10.16.123.234\",\"pnfSerialNumber\":\"QTFCOC540002E\",\"pnfSoftwareVersion\":\"v4.5.0.1\","
                + "\"pnfType\":\"AirScale\",\"pnfVendorName\":\"Nokia\"}}}";

        ConsumerDmaapModel expectedObject = ImmutableConsumerDmaapModel.builder().ipv4("10.16.123.234").ipv6("")
            .pnfName("NOKQTFCOC540002E").build();
        //when
        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsed);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject()))
            .when(dmaapConsumerJsonParser).getJsonObjectFromAnArray(jsonElement);
        ConsumerDmaapModel consumerDmaapModel = dmaapConsumerJsonParser.getJsonObject(Mono.just((message)))
            .block();
        //then
        Assertions.assertNotNull(consumerDmaapModel);
        Assertions.assertEquals(expectedObject, consumerDmaapModel);
    }

    @Test
    void whenPassingCorrectJsonWihoutIpv4andIpv6_validationThrowingAnException() {
        String message =
            "[{\"event\":{\"commonEventHeader\":{\"domain\":\"other\",\"eventId\":\"<<SerialNumber>>-reg\","
                + "\"eventName\":\"pnfRegistration_5GDU\",\"eventType\":\"pnfRegistration\",\"internalHeaderFields\":"
                + "{},\"lastEpochMicrosec\":1519837825682,\"nfNamingCode\":\"5GRAN\",\"nfcNamingCode\":\"5DU\","
                + "\"priority\":\"Normal\",\"reportingEntityName\":\"5GRAN_DU\",\"sequence\":0,\"sourceId\":"
                + "\"<<SerialNumber>>\",\"sourceName\":\"5GRAN_DU\",\"startEpochMicrosec\":1519837825682,\"version\""
                + ":3},\"otherFields\":{\"otherFieldsVersion\":1,\"pnfFamily\":\"BBU\",\"pnfLastServiceDate\""
                + ":1517206400,\"pnfManufactureDate\":1516406400,\"pnfModelNumber\":\"AJ02\",\"pnfSoftwareVersion\":"
                + "\"v4.5.0.1\",\"pnfType\":\"AirScale\",\"pnfVendorName\":\"Nokia\"}}}]";
        String parsed =
            "{\"event\":{\"commonEventHeader\":{\"domain\":\"other\",\"eventId\":\"<<SerialNumber>>-reg\",\"eventName\""
                + ":\"pnfRegistration_5GDU\",\"eventType\":\"pnfRegistration\",\"internalHeaderFields\":{},"
                + "\"lastEpochMicrosec\":1519837825682,\"nfNamingCode\":\"5GRAN\",\"nfcNamingCode\":\"5DU\","
                + "\"priority\":\"Normal\",\"reportingEntityName\":\"5GRAN_DU\",\"sequence\":0,\"sourceId\":"
                + "\"<<SerialNumber>>\",\"sourceName\":\"5GRAN_DU\",\"startEpochMicrosec\":1519837825682,\"version\""
                + ":3},\"otherFields\":{\"otherFieldsVersion\":1,\"pnfFamily\":\"BBU\",\"pnfLastServiceDate\""
                + ":1517206400,\"pnfManufactureDate\":1516406400,\"pnfModelNumber\":\"AJ02\",\"pnfSoftwareVersion\":"
                + "\"v4.5.0.1\",\"pnfType\":\"AirScale\",\"pnfVendorName\":\"Nokia\"}}}";
        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsed);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject()))
            .when(dmaapConsumerJsonParser).getJsonObjectFromAnArray(jsonElement);
        StepVerifier.create(dmaapConsumerJsonParser.getJsonObject(Mono.just(message)))
            .expectSubscription().expectError(DmaapNotFoundException.class).verify();

    }

    @Test
    void whenPassingJsonWithoutMandatoryHeaderInformation_validationThrowingAnException() {
        String parsed = "{\"event\":{\"commonEventHeader\":{\"domain\":\"other\",\"eventId\":\"<<SerialNumber>>-reg\""
            + ",\"eventName\":\"pnfRegistration_5GDU\",\"eventType\":\"pnfRegistration\",\"internalHeaderFields\":{},"
            + "\"lastEpochMicrosec\":1519837825682,\"nfNamingCode\":\"5GRAN\",\"nfcNamingCode\":\"5DU\",\"priority\""
            + ":\"Normal\",\"reportingEntityName\":\"5GRAN_DU\",\"sequence\":0,\"sourceId\":\"<<SerialNumber>>\","
            + "\"sourceName\":\"5GRAN_DU\",\"startEpochMicrosec\":1519837825682,\"version\":3}}}";
        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsed);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject()))
            .when(dmaapConsumerJsonParser).getJsonObjectFromAnArray(jsonElement);
        String incorrectMessage =
            "[{\"event\":{\"commonEventHeader\":{\"domain\":\"other\",\"eventId\":\"<<SerialNumber>>-reg\""
                + ",\"eventName\":\"pnfRegistration_5GDU\",\"eventType\":\"pnfRegistration\",\"internalHeaderFields\":"
                + "{},\"lastEpochMicrosec\":1519837825682,\"nfNamingCode\":\"5GRAN\",\"nfcNamingCode\":\"5DU\","
                + "\"priority\":\"Normal\",\"reportingEntityName\":\"5GRAN_DU\",\"sequence\":0,\"sourceId\":"
                + "\"<<SerialNumber>>\",\"sourceName\":\"5GRAN_DU\",\"startEpochMicrosec\":1519837825682,\"version\":3"
                + "}}}]";
        StepVerifier.create(dmaapConsumerJsonParser.getJsonObject(Mono.just(incorrectMessage)))
            .expectSubscription().expectError(DmaapNotFoundException.class).verify();
    }

    @Test
    void whenPassingJsonWithoutPnfSerialNumberOrPnfVendorName_validationThrowingAnException() {
        String parsed = "{\"event\":{\"commonEventHeader\":{\"domain\":\"other\",\"eventId\":"
            + "\"<<SerialNumber>>-reg\",\"eventName\":\"pnfRegistration_5GDU\",\"eventType\":\"pnfRegistration\",\""
            + "internalHeaderFields\":{},\"lastEpochMicrosec\":1519837825682,\"nfNamingCode\":\"5GRAN\","
            + "\"nfcNamingCode\":\"5DU\",\"priority\":\"Normal\",reportingEntityName\":\"5GRAN_DU\",\"sequence\":0,"
            + "\"sourceId\":\"<<SerialNumber>>\",\"sourceName\":\"5GRAN_DU\",startEpochMicrosec\":1519837825682,\""
            + "version\":3},\"otherFields\":{\"otherFieldsVersion\":1,\"pnfFamily\":\"BBU\",\"pnfLastServiceDate\""
            + ":1517206400,\"pnfManufactureDate\":1516406400,\"pnfModelNumber\":\"AJ02\",\"pnfOamIpv4Address\":"
            + "\"10.16.123.234\",\"pnfOamIpv6Address\":\"0:0:0:0:0:FFFF:0A10:7BEA\",\"pnfSoftwareVersion\":"
            + "\"v4.5.0.1\",\"pnfType\":\"AirScale\"}}}";
        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsed);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject()))
            .when(dmaapConsumerJsonParser).getJsonObjectFromAnArray(jsonElement);
        String jsonWithoutPnfVendorAndSerialNumber =
            "[{\"event\":{\"commonEventHeader\":{\"domain\":\"other\",\"eventId\":"
                + "\"<<SerialNumber>>-reg\",\"eventName\":\"pnfRegistration_5GDU\",\"eventType\":\"pnfRegistration\",\""
                + "internalHeaderFields\":{},\"lastEpochMicrosec\":1519837825682,\"nfNamingCode\":\"5GRAN\","
                + "\"nfcNamingCode\":\"5DU\",\"priority\":\"Normal\",reportingEntityName\":\"5GRAN_DU\",\"sequence\":0,"
                + "\"sourceId\":\"<<SerialNumber>>\",\"sourceName\":\"5GRAN_DU\",startEpochMicrosec\":1519837825682,"
                + "\"version\":3},\"otherFields\":{\"otherFieldsVersion\":1,\"pnfFamily\":\"BBU\","
                + "\"pnfLastServiceDate\":1517206400,\"pnfManufactureDate\":1516406400,\"pnfModelNumber\":\"AJ02\","
                + "\"pnfOamIpv4Address\":\"10.16.123.234\",\"pnfOamIpv6Address\":\"0:0:0:0:0:FFFF:0A10:7BEA\","
                + "\"pnfSoftwareVersion\":\"v4.5.0.1\",\"pnfType\":\"AirScale\"}}}]";
        StepVerifier
            .create(dmaapConsumerJsonParser.getJsonObject(Mono.just(jsonWithoutPnfVendorAndSerialNumber)))
            .expectSubscription().expectError(DmaapNotFoundException.class).verify();
    }

    @Test
    void whenPassingJsonWithoutIpInformation_validationThrowingAnException() {
        String parsed =
            "{\"event\":{\"commonEventHeader\":{\"domain\":\"other\",\"eventId\":\"<<SerialNumber>>-reg\","
                + "\"eventName\":\"pnfRegistration_5GDU\",\"eventType\":\"pnfRegistration\",\"internalHeaderFields\""
                + ":{},\"lastEpochMicrosec\":1519837825682,\"nfNamingCode\":\"5GRAN\",\"nfcNamingCode\":\"5DU\","
                + "\"priority\":\"Normal\",\"reportingEntityName\":\"5GRAN_DU\",\"sequence\":0,\"sourceId\":"
                + "\"<<SerialNumber>>\",\"sourceName\":\"5GRAN_DU\",\"startEpochMicrosec\":1519837825682,\"version\""
                + ":3},\"otherFields\":{\"otherFieldsVersion\":1,\"pnfFamily\":\"BBU\",\"pnfLastServiceDate\":"
                + "1517206400,\"pnfManufactureDate\":1516406400,\"pnfModelNumber\":"
                + "\"AJ02\",\"pnfSerialNumber\":\"QTFCOC540002E\",\"pnfSoftwareVersion\":\"v4.5.0.1\",\"pnfType\":"
                + "\"AirScale\"," + "\"pnfVendorName\":\"Nokia\"}}}";
        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsed);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject()))
            .when(dmaapConsumerJsonParser).getJsonObjectFromAnArray(jsonElement);
        String jsonWithoutIpInformation =
            "[{\"event\":{\"commonEventHeader\":{\"domain\":\"other\",\"eventId\":\"<<SerialNumber>>-reg\","
                + "\"eventName\":\"pnfRegistration_5GDU\",\"eventType\":\"pnfRegistration\",\"internalHeaderFields\""
                + ":{},\"lastEpochMicrosec\":1519837825682,\"nfNamingCode\":\"5GRAN\",\"nfcNamingCode\":\"5DU\","
                + "\"priority\":\"Normal\",\"reportingEntityName\":\"5GRAN_DU\",\"sequence\":0,\"sourceId\":"
                + "\"<<SerialNumber>>\",\"sourceName\":\"5GRAN_DU\",\"startEpochMicrosec\":1519837825682,\"version\""
                + ":3},\"otherFields\":{\"otherFieldsVersion\":1,\"pnfFamily\":\"BBU\",\"pnfLastServiceDate\""
                + ":1517206400,\"pnfManufactureDate\":1516406400,\"pnfModelNumber\":\"AJ02\",\"pnfSerialNumber\""
                + ":\"QTFCOC540002E\",\"pnfSoftwareVersion\":\"v4.5.0.1\",\"pnfType\":\"AirScale\","
                + "\"pnfVendorName\":\"Nokia\"}}}]";
        StepVerifier.create(dmaapConsumerJsonParser.getJsonObject(Mono.just(jsonWithoutIpInformation)))
            .expectSubscription().expectError(DmaapNotFoundException.class).verify();
    }
}
