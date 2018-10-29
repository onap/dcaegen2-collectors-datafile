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

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.dcaegen2.collectors.datafile.exceptions.DmaapNotFoundException;
import org.onap.dcaegen2.collectors.datafile.model.FileData;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFileData;
import org.onap.dcaegen2.collectors.datafile.utils.JsonMessage;
import org.onap.dcaegen2.collectors.datafile.utils.JsonMessage.AdditionalField;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 5/8/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
class DmaapConsumerJsonParserTest {
    private static final String PM_FILE_NAME = "A20161224.1030-1045.bin.gz";
    private static final String LOCATION = "ftpes://192.168.0.101:22/ftp/rop/" + PM_FILE_NAME;
    private static final String GZIP_COMPRESSION = "gzip";
    private static final String FILE_FORMAT_TYPE = "org.3GPP.32.435#measCollec";
    private static final String FILE_FORMAT_VERSION = "V10";
    private static final String CHANGE_IDENTIFIER = "PM_MEAS_FILES";
    private static final String INCORRECT_CHANGE_IDENTIFIER = "INCORRECT_PM_MEAS_FILES";
    private static final String CHANGE_TYPE = "FileReady";
    private static final String INCORRECT_CHANGE_TYPE = "IncorrectFileReady";
    private static final String NOTIFICATION_FIELDS_VERSION = "1.0";

    @Test
    void whenPassingCorrectJson_validationNotThrowingAnException() throws DmaapNotFoundException {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder().name(PM_FILE_NAME).location(LOCATION)
                .compression(GZIP_COMPRESSION).fileFormatType(FILE_FORMAT_TYPE).fileFormatVersion(FILE_FORMAT_VERSION)
                .build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder().changeIdentifier(CHANGE_IDENTIFIER)
                .changeType(CHANGE_TYPE).notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION)
                .addAdditionalField(additionalField).build();

        FileData expectedFileData = ImmutableFileData.builder().changeIdentifier(CHANGE_IDENTIFIER)
                .changeType(CHANGE_TYPE).name(PM_FILE_NAME).location(LOCATION).compression(GZIP_COMPRESSION)
                .fileFormatType(FILE_FORMAT_TYPE).fileFormatVersion(FILE_FORMAT_VERSION).build();

        String messageString = message.toString();
        String parsedString = message.getParsed();
        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(dmaapConsumerJsonParser)
                .getJsonObjectFromAnArray(jsonElement);

        StepVerifier.create(dmaapConsumerJsonParser.getJsonObject(Mono.just(messageString))).expectSubscription()
                .expectNext(expectedFileData).verifyComplete();
    }

    @Test
    void whenPassingCorrectJsonWithoutName_noFileData() {
        AdditionalField additionalField =
                new JsonMessage.AdditionalFieldBuilder().location(LOCATION).compression(GZIP_COMPRESSION)
                        .fileFormatType(FILE_FORMAT_TYPE).fileFormatVersion(FILE_FORMAT_VERSION).build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder().changeIdentifier(CHANGE_IDENTIFIER)
                .changeType(CHANGE_TYPE).notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION)
                .addAdditionalField(additionalField).build();

        String messageString = message.toString();
        String parsedString = message.getParsed();
        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(dmaapConsumerJsonParser)
                .getJsonObjectFromAnArray(jsonElement);

        StepVerifier.create(dmaapConsumerJsonParser.getJsonObject(Mono.just(messageString))).expectSubscription()
                .expectNextCount(0).verifyComplete();
    }

    @Test
    void whenPassingCorrectJsonWithoutLocation_noFileData() {
        AdditionalField additionalField =
                new JsonMessage.AdditionalFieldBuilder().name(PM_FILE_NAME).compression(GZIP_COMPRESSION)
                        .fileFormatType(FILE_FORMAT_TYPE).fileFormatVersion(FILE_FORMAT_VERSION).build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder().changeIdentifier(CHANGE_IDENTIFIER)
                .changeType(CHANGE_TYPE).notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION)
                .addAdditionalField(additionalField).build();

        String messageString = message.toString();
        String parsedString = message.getParsed();
        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(dmaapConsumerJsonParser)
                .getJsonObjectFromAnArray(jsonElement);

        StepVerifier.create(dmaapConsumerJsonParser.getJsonObject(Mono.just(messageString))).expectSubscription()
                .expectNextCount(0).verifyComplete();
    }

    @Test
    void whenPassingCorrectJsonWithoutCompression_noFileData() {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder().name(PM_FILE_NAME).location(LOCATION)
                .fileFormatType(FILE_FORMAT_TYPE).fileFormatVersion(FILE_FORMAT_VERSION).build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder().changeIdentifier(CHANGE_IDENTIFIER)
                .changeType(CHANGE_TYPE).notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION)
                .addAdditionalField(additionalField).build();

        String messageString = message.toString();
        String parsedString = message.getParsed();
        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(dmaapConsumerJsonParser)
                .getJsonObjectFromAnArray(jsonElement);

        StepVerifier.create(dmaapConsumerJsonParser.getJsonObject(Mono.just(messageString))).expectSubscription()
                .expectNextCount(0).verifyComplete();
    }

    @Test
    void whenPassingCorrectJsonWithoutFileFormatType_noFileData() {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder().name(PM_FILE_NAME).location(LOCATION)
                .compression(GZIP_COMPRESSION).fileFormatVersion(FILE_FORMAT_VERSION).build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder().changeIdentifier(CHANGE_IDENTIFIER)
                .changeType(CHANGE_TYPE).notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION)
                .addAdditionalField(additionalField).build();

        String messageString = message.toString();
        String parsedString = message.getParsed();
        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(dmaapConsumerJsonParser)
                .getJsonObjectFromAnArray(jsonElement);

        StepVerifier.create(dmaapConsumerJsonParser.getJsonObject(Mono.just(messageString))).expectSubscription()
                .expectNextCount(0).verifyComplete();
    }

    @Test
    void whenPassingOneCorrectJsonWithoutFileFormatVersionAndOneCorrect_oneFileData() {
        AdditionalField additionalFaultyField = new JsonMessage.AdditionalFieldBuilder().name(PM_FILE_NAME)
                .location(LOCATION).compression(GZIP_COMPRESSION).fileFormatType(FILE_FORMAT_TYPE).build();
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder().name(PM_FILE_NAME).location(LOCATION)
                .compression(GZIP_COMPRESSION).fileFormatType(FILE_FORMAT_TYPE).fileFormatVersion(FILE_FORMAT_VERSION)
                .build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder().changeIdentifier(CHANGE_IDENTIFIER)
                .changeType(CHANGE_TYPE).notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION)
                .addAdditionalField(additionalFaultyField).addAdditionalField(additionalField).build();

        FileData expectedFileData = ImmutableFileData.builder().changeIdentifier(CHANGE_IDENTIFIER)
                .changeType(CHANGE_TYPE).name(PM_FILE_NAME).location(LOCATION).compression(GZIP_COMPRESSION)
                .fileFormatType(FILE_FORMAT_TYPE).fileFormatVersion(FILE_FORMAT_VERSION).build();

        String messageString = message.toString();
        String parsedString = message.getParsed();
        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(dmaapConsumerJsonParser)
                .getJsonObjectFromAnArray(jsonElement);

        StepVerifier.create(dmaapConsumerJsonParser.getJsonObject(Mono.just(messageString))).expectSubscription()
                .expectNext(expectedFileData).verifyComplete();
    }

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
        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);

        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(dmaapConsumerJsonParser)
                .getJsonObjectFromAnArray(jsonElement);

        StepVerifier.create(dmaapConsumerJsonParser.getJsonObject(Mono.just(incorrectMessageString)))
                .expectSubscription().expectError(DmaapNotFoundException.class).verify();
    }

    @Test
    void whenPassingCorrectJsonWithIncorrectChangeType_validationThrowingAnException() {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder().name(PM_FILE_NAME).location(LOCATION)
                .compression(GZIP_COMPRESSION).fileFormatVersion(FILE_FORMAT_VERSION).build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder().changeIdentifier(CHANGE_IDENTIFIER)
                .changeType(INCORRECT_CHANGE_TYPE).notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION)
                .addAdditionalField(additionalField).build();

        String messageString = message.toString();
        String parsedString = message.getParsed();
        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(dmaapConsumerJsonParser)
                .getJsonObjectFromAnArray(jsonElement);

        StepVerifier.create(dmaapConsumerJsonParser.getJsonObject(Mono.just(messageString))).expectSubscription()
                .expectNextCount(0).expectError(DmaapNotFoundException.class).verify();
    }

    @Test
    void whenPassingCorrectJsonWithIncorrectChangeIdentifier_validationThrowingAnException() {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder().name(PM_FILE_NAME).location(LOCATION)
                .compression(GZIP_COMPRESSION).fileFormatVersion(FILE_FORMAT_VERSION).build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder().changeIdentifier(INCORRECT_CHANGE_IDENTIFIER)
                .changeType(CHANGE_TYPE).notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION)
                .addAdditionalField(additionalField).build();

        String messageString = message.toString();
        String parsedString = message.getParsed();
        DmaapConsumerJsonParser dmaapConsumerJsonParser = spy(new DmaapConsumerJsonParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(dmaapConsumerJsonParser)
                .getJsonObjectFromAnArray(jsonElement);

        StepVerifier.create(dmaapConsumerJsonParser.getJsonObject(Mono.just(messageString))).expectSubscription()
                .expectNextCount(0).expectError(DmaapNotFoundException.class).verify();
    }
}
