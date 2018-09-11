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

package org.onap.dcaegen2.collectors.datafile.service;

import static org.mockito.Mockito.spy;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.dcaegen2.collectors.datafile.exceptions.DmaapNotFoundException;
import org.onap.dcaegen2.collectors.datafile.utils.JsonMessage;
import org.onap.dcaegen2.collectors.datafile.utils.JsonMessage.AdditionalField;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 5/8/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
class DmaapConsumerJsonParserTest {

    @Test
    void whenPassingCorrectJson_validationNotThrowingAnException() throws DmaapNotFoundException {
        // given
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder()
                .location("ftpes://192.168.0.101:22/ftp/rop/A20161224.1030-1045.bin.gz").compression("gzip")
                .fileFormatType("org.3GPP.32.435#measCollec").fileFormatVersion("V10").build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder().changeIdentifier("PM_MEAS_FILES")
                .changeType("FileReady").notificationFieldsVersion("1.0").addAdditionalField(additionalField).build();

        String messageString = message.toString();

        String parsedString = message.getParsed();

        FileData expectedFileData = ImmutableFileData.builder().changeIdentifier("PM_MEAS_FILES")
                .changeType("FileReady").location("ftpes://192.168.0.101:22/ftp/rop/A20161224.1030-1045.bin.gz")
                .compression("gzip").fileFormatType("org.3GPP.32.435#measCollec").fileFormatVersion("V10").build();
        // when
        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(dmaapConsumerJsonParser)
                .getJsonObjectFromAnArray(jsonElement);
        ArrayList<FileData> listOfFileData = dmaapConsumerJsonParser.getJsonObject(Mono.just((messageString))).block();
        // then
        Assertions.assertNotNull(listOfFileData);
        Assertions.assertEquals(expectedFileData, listOfFileData.get(0));
    }

    @Test
    void whenPassingCorrectJsonWihoutLocation_validationThrowingAnException() {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder().compression("gzip")
                .fileFormatType("org.3GPP.32.435#measCollec").fileFormatVersion("V10").build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder().changeIdentifier("PM_MEAS_FILES")
                .changeType("FileReady").notificationFieldsVersion("1.0").addAdditionalField(additionalField).build();

        String messageString = message.toString();

        String parsedString = message.getParsed();

        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(dmaapConsumerJsonParser)
                .getJsonObjectFromAnArray(jsonElement);
        StepVerifier.create(dmaapConsumerJsonParser.getJsonObject(Mono.just(messageString))).expectSubscription()
                .expectError(DmaapNotFoundException.class).verify();
    }

    @Test
    void whenPassingCorrectJsonWihoutCompression_validationThrowingAnException() {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder()
                .location("ftpes://192.168.0.101:22/ftp/rop/A20161224.1030-1045.bin.gz")
                .fileFormatType("org.3GPP.32.435#measCollec").fileFormatVersion("V10").build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder().changeIdentifier("PM_MEAS_FILES")
                .changeType("FileReady").notificationFieldsVersion("1.0").addAdditionalField(additionalField).build();

        String messageString = message.toString();

        String parsedString = message.getParsed();

        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(dmaapConsumerJsonParser)
                .getJsonObjectFromAnArray(jsonElement);
        StepVerifier.create(dmaapConsumerJsonParser.getJsonObject(Mono.just(messageString))).expectSubscription()
                .expectError(DmaapNotFoundException.class).verify();
    }

    @Test
    void whenPassingCorrectJsonWihoutFileFormatType_validationThrowingAnException() {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder()
                .location("ftpes://192.168.0.101:22/ftp/rop/A20161224.1030-1045.bin.gz").compression("gzip")
                .fileFormatVersion("V10").build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder().changeIdentifier("PM_MEAS_FILES")
                .changeType("FileReady").notificationFieldsVersion("1.0").addAdditionalField(additionalField).build();

        String messageString = message.toString();

        String parsedString = message.getParsed();

        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(dmaapConsumerJsonParser)
                .getJsonObjectFromAnArray(jsonElement);
        StepVerifier.create(dmaapConsumerJsonParser.getJsonObject(Mono.just(messageString))).expectSubscription()
                .expectError(DmaapNotFoundException.class).verify();
    }

    @Test
    void whenPassingCorrectJsonWihoutFileFormatVersion_validationThrowingAnException() {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder()
                .location("ftpes://192.168.0.101:22/ftp/rop/A20161224.1030-1045.bin.gz").compression("gzip")
                .fileFormatType("org.3GPP.32.435#measCollec").build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder().changeIdentifier("PM_MEAS_FILES")
                .changeType("FileReady").notificationFieldsVersion("1.0").addAdditionalField(additionalField).build();

        String messageString = message.toString();

        String parsedString = message.getParsed();

        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(dmaapConsumerJsonParser)
                .getJsonObjectFromAnArray(jsonElement);
        StepVerifier.create(dmaapConsumerJsonParser.getJsonObject(Mono.just(messageString))).expectSubscription()
                .expectError(DmaapNotFoundException.class).verify();
    }

    // Fixed temprarily
    @Test
    void whenPassingJsonWithoutMandatoryHeaderInformation_validationThrowingAnException() {
        JsonMessage message = new JsonMessage.JsonMessageBuilder().changeIdentifier("PM_MEAS_FILES_INVALID")
                .changeType("FileReady_INVALID").notificationFieldsVersion("1.0_INVALID").build();
        String incorrectMessageString = message.toString();

        String parsedString = message.getParsed();
        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(dmaapConsumerJsonParser)
                .getJsonObjectFromAnArray(jsonElement);
        StepVerifier.create(dmaapConsumerJsonParser.getJsonObject(Mono.just(incorrectMessageString)))
                .expectSubscription().expectError(DmaapNotFoundException.class).verify();
    }

    @Test
    void whenPassingJsonWithNullJsonElement_validationThrowingAnException() {
        JsonMessage message = new JsonMessage.JsonMessageBuilder().build();
        String incorrectMessageString = message.toString();

        String parsedString = message.getParsed();
        // System.out.println(parsedString);
        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        // System.out.println(jsonElement);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(dmaapConsumerJsonParser)
                .getJsonObjectFromAnArray(jsonElement);
        StepVerifier.create(dmaapConsumerJsonParser.getJsonObject(Mono.just(incorrectMessageString)))
                .expectSubscription().expectError(DmaapNotFoundException.class).verify();
    }
}
