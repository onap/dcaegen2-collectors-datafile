/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018-2019 Nordix Foundation. All rights reserved.
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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.dcaegen2.collectors.datafile.ftp.Scheme;
import org.onap.dcaegen2.collectors.datafile.model.FileData;
import org.onap.dcaegen2.collectors.datafile.model.FileReadyMessage;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFileData;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFileReadyMessage;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableMessageMetaData;
import org.onap.dcaegen2.collectors.datafile.model.MessageMetaData;
import org.onap.dcaegen2.collectors.datafile.utils.JsonMessage;
import org.onap.dcaegen2.collectors.datafile.utils.JsonMessage.AdditionalField;
import org.onap.dcaegen2.collectors.datafile.utils.LoggingUtils;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Tests the JsonMessageParser.
 *
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 5/8/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
class JsonMessageParserTest {
    private static final String ERROR_LOG_TAG = "[ERROR] ";

    private static final String NR_RADIO_ERICSSON_EVENT_NAME = "Noti_NrRadio-Ericsson_FileReady";
    private static final String PRODUCT_NAME = "NrRadio";
    private static final String VENDOR_NAME = "Ericsson";
    private static final String LAST_EPOCH_MICROSEC = "1519837825682";
    private static final String SOURCE_NAME = "5GRAN_DU";
    private static final String START_EPOCH_MICROSEC = "1519837825682";
    private static final String TIME_ZONE_OFFSET = "UTC+05:00";
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
    void whenPassingCorrectJson_oneFileReadyMessage() throws URISyntaxException {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder() //
            .name(PM_FILE_NAME) //
            .location(LOCATION) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatType(FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder() //
            .eventName(NR_RADIO_ERICSSON_EVENT_NAME) //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .changeType(CHANGE_TYPE) //
            .notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION) //
            .addAdditionalField(additionalField) //
            .build();

        MessageMetaData messageMetaData = ImmutableMessageMetaData.builder() //
            .productName(PRODUCT_NAME) //
            .vendorName(VENDOR_NAME) //
            .lastEpochMicrosec(LAST_EPOCH_MICROSEC) //
            .sourceName(SOURCE_NAME) //
            .startEpochMicrosec(START_EPOCH_MICROSEC) //
            .timeZoneOffset(TIME_ZONE_OFFSET) //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .changeType(CHANGE_TYPE) //
            .build();
        FileData expectedFileData = ImmutableFileData.builder() //
            .name(PM_FILE_NAME) //
            .location(LOCATION) //
            .scheme(Scheme.FTPS) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatType(FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .messageMetaData(messageMetaData) //
            .build();
        List<FileData> files = new ArrayList<>();
        files.add(expectedFileData);
        FileReadyMessage expectedMessage = ImmutableFileReadyMessage.builder() //
            .files(files) //
            .build();

        String parsedString = message.getParsed();
        JsonMessageParser jsonMessageParserUnderTest = spy(new JsonMessageParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(jsonMessageParserUnderTest)
            .getJsonObjectFromAnArray(jsonElement);

        StepVerifier.create(jsonMessageParserUnderTest.getMessagesFromJson(Mono.just(jsonElement))).expectSubscription()
            .expectNext(expectedMessage).verifyComplete();
    }

    @Test
    void whenPassingCorrectJsonWithTwoEvents_twoMessages() {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder() //
            .name(PM_FILE_NAME) //
            .location(LOCATION) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatType(FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder() //
            .eventName(NR_RADIO_ERICSSON_EVENT_NAME) //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .changeType(CHANGE_TYPE) //
            .notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION) //
            .addAdditionalField(additionalField) //
            .build();

        MessageMetaData messageMetaData = ImmutableMessageMetaData.builder() //
            .productName(PRODUCT_NAME) //
            .vendorName(VENDOR_NAME) //
            .lastEpochMicrosec(LAST_EPOCH_MICROSEC) //
            .sourceName(SOURCE_NAME) //
            .startEpochMicrosec(START_EPOCH_MICROSEC) //
            .timeZoneOffset(TIME_ZONE_OFFSET) //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .changeType(CHANGE_TYPE) //
            .build();
        FileData expectedFileData = ImmutableFileData.builder() //
            .name(PM_FILE_NAME) //
            .location(LOCATION) //
            .scheme(Scheme.FTPS) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatType(FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .messageMetaData(messageMetaData) //
            .build();
        List<FileData> files = new ArrayList<>();
        files.add(expectedFileData);
        FileReadyMessage expectedMessage = ImmutableFileReadyMessage.builder() //
            .files(files) //
            .build();

        String parsedString = message.getParsed();
        String messageString = "[" + parsedString + "," + parsedString + "]";
        JsonMessageParser jsonMessageParserUnderTest = spy(new JsonMessageParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        JsonElement jsonElement1 = new JsonParser().parse(messageString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(jsonMessageParserUnderTest)
            .getJsonObjectFromAnArray(jsonElement);

        StepVerifier.create(jsonMessageParserUnderTest.getMessagesFromJson(Mono.just(jsonElement1)))
            .expectSubscription().expectNext(expectedMessage).expectNext(expectedMessage).verifyComplete();
    }

    @Test
    void whenPassingCorrectJsonWithoutLocation_noMessage() {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder() //
            .name(PM_FILE_NAME) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatType(FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder() //
            .eventName(NR_RADIO_ERICSSON_EVENT_NAME) //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .changeType(CHANGE_TYPE) //
            .notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION) //
            .addAdditionalField(additionalField) //
            .build();

        String parsedString = message.getParsed();
        JsonMessageParser jsonMessageParserUnderTest = spy(new JsonMessageParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(jsonMessageParserUnderTest)
            .getJsonObjectFromAnArray(jsonElement);

        ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(JsonMessageParser.class);
        StepVerifier.create(jsonMessageParserUnderTest.getMessagesFromJson(Mono.just(jsonElement))).expectSubscription()
            .expectNextCount(0).verifyComplete();

        assertTrue(logAppender.list.toString()
            .contains("[ERROR] VES event parsing. File information wrong. " + "Missing location."));
        assertTrue(logAppender.list.get(0).toString().contains("sourceName=5GRAN_DU"));
    }

    @Test
    void whenPassingCorrectJsonWrongScheme_noMessage() {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder() //
            .name(PM_FILE_NAME) //
            .location("http://location.xml") //
            .compression(GZIP_COMPRESSION) //
            .fileFormatType(FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder() //
            .eventName(NR_RADIO_ERICSSON_EVENT_NAME) //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .changeType(CHANGE_TYPE) //
            .notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION) //
            .addAdditionalField(additionalField) //
            .build();

        String parsedString = message.getParsed();
        JsonMessageParser jsonMessageParserUnderTest = spy(new JsonMessageParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(jsonMessageParserUnderTest)
            .getJsonObjectFromAnArray(jsonElement);

        ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(JsonMessageParser.class);
        StepVerifier.create(jsonMessageParserUnderTest.getMessagesFromJson(Mono.just(jsonElement))).expectSubscription()
            .expectNextCount(0).verifyComplete();

        assertTrue("Error missing in log",
            logAppender.list.toString()
                .contains(ERROR_LOG_TAG + JsonMessageParser.ERROR_MSG_VES_EVENT_PARSING
                    + Scheme.DFC_DOES_NOT_SUPPORT_PROTOCOL_ERROR_MSG + "http" + Scheme.SUPPORTED_PROTOCOLS_ERROR_MESSAGE
                    + ". Location: http://location.xml"));
        assertTrue("Missing sourceName in log", logAppender.list.toString().contains("sourceName=5GRAN_DU"));
    }

    @Test
    void whenPassingCorrectJsonWithTwoEventsFirstNoHeader_oneFileDatan() {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder() //
            .name(PM_FILE_NAME) //
            .location(LOCATION) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatType(FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder() //
            .eventName(NR_RADIO_ERICSSON_EVENT_NAME) //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .changeType(CHANGE_TYPE) //
            .notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION) //
            .addAdditionalField(additionalField) //
            .build();

        MessageMetaData messageMetaData = ImmutableMessageMetaData.builder() //
            .productName(PRODUCT_NAME) //
            .vendorName(VENDOR_NAME) //
            .lastEpochMicrosec(LAST_EPOCH_MICROSEC) //
            .sourceName(SOURCE_NAME) //
            .startEpochMicrosec(START_EPOCH_MICROSEC) //
            .timeZoneOffset(TIME_ZONE_OFFSET) //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .changeType(CHANGE_TYPE) //
            .build();
        FileData expectedFileData = ImmutableFileData.builder() //
            .name(PM_FILE_NAME) //
            .location(LOCATION) //
            .scheme(Scheme.FTPS) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatType(FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .messageMetaData(messageMetaData) //
            .build();
        List<FileData> files = new ArrayList<>();
        files.add(expectedFileData);
        FileReadyMessage expectedMessage = ImmutableFileReadyMessage.builder() //
            .files(files) //
            .build();

        String parsedString = message.getParsed();
        String messageString = "[{\"event\":{}}," + parsedString + "]";
        JsonMessageParser jsonMessageParserUnderTest = new JsonMessageParser();
        JsonElement jsonElement = new JsonParser().parse(messageString);

        StepVerifier.create(jsonMessageParserUnderTest.getMessagesFromJson(Mono.just(jsonElement))).expectSubscription()
            .expectNext(expectedMessage).verifyComplete();
    }

    @Test
    void whenPassingCorrectJsonWithFaultyEventName_noFileData() {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder() //
            .location(LOCATION) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatType(FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder() //
            .eventName("Faulty event name") //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .changeType(CHANGE_TYPE) //
            .notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION) //
            .addAdditionalField(additionalField) //
            .build();

        String parsedString = message.getParsed();
        JsonMessageParser jsonMessageParserUnderTest = spy(new JsonMessageParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(jsonMessageParserUnderTest)
            .getJsonObjectFromAnArray(jsonElement);

        ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(JsonMessageParser.class);
        StepVerifier.create(jsonMessageParserUnderTest.getMessagesFromJson(Mono.just(jsonElement))).expectSubscription()
            .expectComplete().verify();

        assertTrue("Error missing in log",
            logAppender.list.toString().contains(ERROR_LOG_TAG + JsonMessageParser.ERROR_MSG_VES_EVENT_PARSING
                + "Can not get PRODUCT_NAME from eventName, eventName is not in correct format: Faulty event name"));
    }

    @Test
    void whenPassingCorrectJsonWithoutName_noFileData() {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder() //
            .location(LOCATION) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatType(FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder() //
            .eventName(NR_RADIO_ERICSSON_EVENT_NAME) //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .changeType(CHANGE_TYPE) //
            .notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION) //
            .addAdditionalField(additionalField) //
            .build();

        String parsedString = message.getParsed();
        JsonMessageParser jsonMessageParserUnderTest = spy(new JsonMessageParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(jsonMessageParserUnderTest)
            .getJsonObjectFromAnArray(jsonElement);

        ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(JsonMessageParser.class);
        StepVerifier.create(jsonMessageParserUnderTest.getMessagesFromJson(Mono.just(jsonElement))).expectSubscription()
            .expectNextCount(0).verifyComplete();

        assertTrue("Error missing in log",
            logAppender.list.toString()
                .contains(ERROR_LOG_TAG + JsonMessageParser.ERROR_MSG_VES_EVENT_PARSING
                    + "File information wrong. Missing data: [name] Data: "
                    + message.getAdditionalFields().get(0).toString()));
    }

    @Test
    void whenPassingCorrectJsonWithoutAdditionalFields_noFileData() {
        JsonMessage message = new JsonMessage.JsonMessageBuilder() //
            .eventName(NR_RADIO_ERICSSON_EVENT_NAME) //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .changeType(CHANGE_TYPE) //
            .notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION) //
            .build();

        String parsedString = message.getParsed();
        JsonMessageParser jsonMessageParserUnderTest = spy(new JsonMessageParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(jsonMessageParserUnderTest)
            .getJsonObjectFromAnArray(jsonElement);

        ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(JsonMessageParser.class);
        StepVerifier.create(jsonMessageParserUnderTest.getMessagesFromJson(Mono.just(jsonElement))).expectSubscription()
            .expectNextCount(0).verifyComplete();

        assertTrue("Error missing in log",
            logAppender.list.toString().contains(ERROR_LOG_TAG + JsonMessageParser.ERROR_MSG_VES_EVENT_PARSING
                + "Missing arrayOfNamedHashMap in message. " + message.getParsed()));
    }

    @Test
    void whenPassingCorrectJsonWithoutCompression_noFileData() {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder() //
            .name(PM_FILE_NAME) //
            .location(LOCATION) //
            .fileFormatType(FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder() //
            .eventName(NR_RADIO_ERICSSON_EVENT_NAME) //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .changeType(CHANGE_TYPE) //
            .notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION) //
            .addAdditionalField(additionalField) //
            .build();

        String parsedString = message.getParsed();
        JsonMessageParser jsonMessageParserUnderTest = spy(new JsonMessageParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(jsonMessageParserUnderTest)
            .getJsonObjectFromAnArray(jsonElement);

        ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(JsonMessageParser.class);
        StepVerifier.create(jsonMessageParserUnderTest.getMessagesFromJson(Mono.just(jsonElement))).expectSubscription()
            .expectNextCount(0).verifyComplete();

        assertTrue("Error missing in log",
            logAppender.list.toString()
                .contains(ERROR_LOG_TAG + JsonMessageParser.ERROR_MSG_VES_EVENT_PARSING
                    + "File information wrong. Missing data: [compression] Data: "
                    + message.getAdditionalFields().get(0).toString()));
    }

    @Test
    void whenPassingCorrectJsonWithoutFileFormatType_noFileData() {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder() //
            .name(PM_FILE_NAME) //
            .location(LOCATION) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder() //
            .eventName(NR_RADIO_ERICSSON_EVENT_NAME) //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .changeType(CHANGE_TYPE) //
            .notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION) //
            .addAdditionalField(additionalField) //
            .build();

        String parsedString = message.getParsed();
        JsonMessageParser jsonMessageParserUnderTest = spy(new JsonMessageParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(jsonMessageParserUnderTest)
            .getJsonObjectFromAnArray(jsonElement);

        ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(JsonMessageParser.class);
        StepVerifier.create(jsonMessageParserUnderTest.getMessagesFromJson(Mono.just(jsonElement))).expectSubscription()
            .expectNextCount(0).verifyComplete();

        assertTrue("Error missing in log",
            logAppender.list.toString()
                .contains(ERROR_LOG_TAG + JsonMessageParser.ERROR_MSG_VES_EVENT_PARSING
                    + "File information wrong. Missing data: [fileFormatType] Data: "
                    + message.getAdditionalFields().get(0).toString()));
    }

    @Test
    void whenPassingOneCorrectJsonWithoutFileFormatVersionAndOneCorrect_oneFileData() {
        AdditionalField additionalFaultyField = new JsonMessage.AdditionalFieldBuilder() //
            .name(PM_FILE_NAME) //
            .location(LOCATION) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatType(FILE_FORMAT_TYPE) //
            .build();
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder() //
            .name(PM_FILE_NAME) //
            .location(LOCATION) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatType(FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder() //
            .eventName(NR_RADIO_ERICSSON_EVENT_NAME) //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .changeType(CHANGE_TYPE) //
            .notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION) //
            .addAdditionalField(additionalFaultyField) //
            .addAdditionalField(additionalField) //
            .build();

        MessageMetaData messageMetaData = ImmutableMessageMetaData.builder() //
            .productName(PRODUCT_NAME) //
            .vendorName(VENDOR_NAME) //
            .lastEpochMicrosec(LAST_EPOCH_MICROSEC) //
            .sourceName(SOURCE_NAME) //
            .startEpochMicrosec(START_EPOCH_MICROSEC) //
            .timeZoneOffset(TIME_ZONE_OFFSET) //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .changeType(CHANGE_TYPE) //
            .build();
        FileData expectedFileData = ImmutableFileData.builder() //
            .name(PM_FILE_NAME) //
            .location(LOCATION) //
            .scheme(Scheme.FTPS) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatType(FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .messageMetaData(messageMetaData) //
            .build();
        List<FileData> files = new ArrayList<>();
        files.add(expectedFileData);
        FileReadyMessage expectedMessage = ImmutableFileReadyMessage.builder() //
            .files(files) //
            .build();

        String parsedString = message.getParsed();
        JsonMessageParser jsonMessageParserUnderTest = spy(new JsonMessageParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(jsonMessageParserUnderTest)
            .getJsonObjectFromAnArray(jsonElement);

        StepVerifier.create(jsonMessageParserUnderTest.getMessagesFromJson(Mono.just(jsonElement))).expectSubscription()
            .expectNext(expectedMessage).verifyComplete();
    }

    @Test
    void whenPassingJsonWithoutMandatoryHeaderInformation_noFileData() {
        JsonMessage message = new JsonMessage.JsonMessageBuilder() //
            .eventName(NR_RADIO_ERICSSON_EVENT_NAME) //
            .build();

        String parsedString = message.getParsed();
        JsonMessageParser jsonMessageParserUnderTest = spy(new JsonMessageParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(jsonMessageParserUnderTest)
            .getJsonObjectFromAnArray(jsonElement);

        ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(JsonMessageParser.class);
        StepVerifier.create(jsonMessageParserUnderTest.getMessagesFromJson(Mono.just(jsonElement))).expectSubscription()
            .expectComplete().verify();

        assertTrue("Error missing in log",
            logAppender.list.toString()
                .contains(ERROR_LOG_TAG + JsonMessageParser.ERROR_MSG_VES_EVENT_PARSING
                    + "Missing data: [changeIdentifier, changeType, notificationFieldsVersion]. "
                    + "Change type is wrong:  Expected: FileReady Message: " + message.getParsed()));
    }

    @Test
    void whenPassingJsonWithNullJsonElement_noFileData() {
        JsonMessageParser jsonMessageParserUnderTest = spy(new JsonMessageParser());
        JsonElement jsonElement = new JsonParser().parse("{}");

        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(jsonMessageParserUnderTest)
            .getJsonObjectFromAnArray(jsonElement);

        ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(JsonMessageParser.class);
        StepVerifier.create(jsonMessageParserUnderTest.getMessagesFromJson(Mono.just(jsonElement))).expectSubscription()
            .expectComplete().verify();

        assertTrue("Error missing in log",
            logAppender.list.toString().contains(ERROR_LOG_TAG + "Incorrect JsonObject - missing header. "));
    }

    @Test
    void whenPassingCorrectJsonWithIncorrectChangeType_noFileData() {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder() //
            .name(PM_FILE_NAME) //
            .location(LOCATION) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder() //
            .eventName(NR_RADIO_ERICSSON_EVENT_NAME) //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .changeType(INCORRECT_CHANGE_TYPE) //
            .notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION) //
            .addAdditionalField(additionalField) //
            .build();

        String parsedString = message.getParsed();
        JsonMessageParser jsonMessageParserUnderTest = spy(new JsonMessageParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(jsonMessageParserUnderTest)
            .getJsonObjectFromAnArray(jsonElement);

        ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(JsonMessageParser.class);
        StepVerifier.create(jsonMessageParserUnderTest.getMessagesFromJson(Mono.just(jsonElement))).expectSubscription()
            .expectNextCount(0).expectComplete().verify();

        assertTrue("Error missing in log",
            logAppender.list.toString()
                .contains(ERROR_LOG_TAG + JsonMessageParser.ERROR_MSG_VES_EVENT_PARSING + " Change type is wrong: "
                    + INCORRECT_CHANGE_TYPE + " Expected: FileReady Message: " + message.getParsed()));
    }

    @Test
    void whenPassingCorrectJsonWithIncorrectChangeIdentifier_noFileData() {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder() //
            .name(PM_FILE_NAME) //
            .location(LOCATION) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder() //
            .eventName(NR_RADIO_ERICSSON_EVENT_NAME) //
            .changeIdentifier(INCORRECT_CHANGE_IDENTIFIER) //
            .changeType(CHANGE_TYPE) //
            .notificationFieldsVersion(NOTIFICATION_FIELDS_VERSION) //
            .addAdditionalField(additionalField) //
            .build();

        String parsedString = message.getParsed();
        JsonMessageParser jsonMessageParserUnderTest = spy(new JsonMessageParser());
        JsonElement jsonElement = new JsonParser().parse(parsedString);
        Mockito.doReturn(Optional.of(jsonElement.getAsJsonObject())).when(jsonMessageParserUnderTest)
            .getJsonObjectFromAnArray(jsonElement);

        StepVerifier.create(jsonMessageParserUnderTest.getMessagesFromJson(Mono.just(jsonElement))).expectSubscription()
            .expectComplete().verify();
    }
}
