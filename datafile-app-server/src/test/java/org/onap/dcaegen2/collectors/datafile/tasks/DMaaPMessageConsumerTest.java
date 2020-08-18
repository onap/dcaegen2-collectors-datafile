/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018-2019 Nordix Foundation.
 * Copyright (C) 2020 Nokia. All rights reserved.
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
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.configuration.ConsumerConfiguration;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.ftp.Scheme;
import org.onap.dcaegen2.collectors.datafile.model.FileData;
import org.onap.dcaegen2.collectors.datafile.model.FilePublishInformation;
import org.onap.dcaegen2.collectors.datafile.model.FileReadyMessage;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFileData;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFilePublishInformation;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFileReadyMessage;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableMessageMetaData;
import org.onap.dcaegen2.collectors.datafile.model.MessageMetaData;
import org.onap.dcaegen2.collectors.datafile.service.JsonMessageParser;
import org.onap.dcaegen2.collectors.datafile.utils.JsonMessage;
import org.onap.dcaegen2.collectors.datafile.utils.JsonMessage.AdditionalField;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.api.MessageRouterSubscriber;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.MessageRouterSubscribeRequest;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.config.MessageRouterSubscriberConfig;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DMaaPMessageConsumerTest {
    private static final String NR_RADIO_ERICSSON_EVENT_NAME = "Noti_NrRadio-Ericsson_FileReady";
    private static final String PRODUCT_NAME = "NrRadio";
    private static final String VENDOR_NAME = "Ericsson";
    private static final String LAST_EPOCH_MICROSEC = "8745745764578";
    private static final String SOURCE_NAME = "oteNB5309";
    private static final String START_EPOCH_MICROSEC = "8745745764578";
    private static final String TIME_ZONE_OFFSET = "UTC+05:00";
    private static final String PM_MEAS_CHANGE_IDENTIFIER = "PM_MEAS_FILES";
    private static final String FILE_READY_CHANGE_TYPE = "FileReady";
    private static final String FTPES_SCHEME = "ftpes://";
    private static final String SFTP_SCHEME = "sftp://";
    private static final String SERVER_ADDRESS = "192.168.0.101";
    private static final String PORT_22 = "22";
    private static final String PM_FILE_NAME = "A20161224.1030-1045.bin.gz";
    private static final String REMOTE_FILE_LOCATION = "/ftp/rop/" + PM_FILE_NAME;
    private static final Path LOCAL_FILE_LOCATION = Paths.get("target/" + PM_FILE_NAME);
    private static final String FTPES_LOCATION = FTPES_SCHEME + SERVER_ADDRESS + ":" + PORT_22 + REMOTE_FILE_LOCATION;
    private static final String SFTP_LOCATION = SFTP_SCHEME + SERVER_ADDRESS + ":" + PORT_22 + REMOTE_FILE_LOCATION;
    private static final String GZIP_COMPRESSION = "gzip";
    private static final String MEAS_COLLECT_FILE_FORMAT_TYPE = "org.3GPP.32.435#measCollec";
    private static final String FILE_FORMAT_VERSION = "V10";
    private static List<FilePublishInformation> listOfFilePublishInformation = new ArrayList<>();
    private static final String CHANGE_IDENTIFIER = "PM_MEAS_FILES";

    private DMaaPMessageConsumer messageConsumer;
    private static String ftpesMessageString;
    private static JsonElement ftpesMessageJson;
    private static FileData ftpesFileData;
    private static FileReadyMessage expectedFtpesMessage;

    private static String sftpMessageString;
    private static JsonElement sftpMessageJson;
    private static FileData sftpFileData;
    private static FileReadyMessage expectedSftpMessage;

    private static AppConfig appConfig;
    private static ConsumerConfiguration dmaapConsumerConfiguration;
    private static MessageRouterSubscriber messageRouterSubscriber;

    /**
     * Sets up data for the test.
     */
    @BeforeAll
    public static void setUp() {

        appConfig = mock(AppConfig.class);

        messageRouterSubscriber = mock(MessageRouterSubscriber.class);
        dmaapConsumerConfiguration = new ConsumerConfiguration(mock(MessageRouterSubscriberConfig.class),
                messageRouterSubscriber, mock(MessageRouterSubscribeRequest.class));

        JsonParser jsonParser = new JsonParser();

        AdditionalField ftpesAdditionalField = new JsonMessage.AdditionalFieldBuilder() //
            .location(FTPES_LOCATION) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatType(MEAS_COLLECT_FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .build();

        JsonMessage ftpesJsonMessage = new JsonMessage.JsonMessageBuilder() //
            .eventName(NR_RADIO_ERICSSON_EVENT_NAME) //
            .changeIdentifier(PM_MEAS_CHANGE_IDENTIFIER) //
            .changeType(FILE_READY_CHANGE_TYPE) //
            .notificationFieldsVersion("1.0") //
            .addAdditionalField(ftpesAdditionalField) //
            .build();

        ftpesMessageString = ftpesJsonMessage.toString();
        ftpesMessageJson = jsonParser.parse(ftpesMessageString);

        MessageMetaData messageMetaData = ImmutableMessageMetaData.builder() //
            .productName(PRODUCT_NAME) //
            .vendorName(VENDOR_NAME) //
            .lastEpochMicrosec(LAST_EPOCH_MICROSEC) //
            .sourceName(SOURCE_NAME) //
            .startEpochMicrosec(START_EPOCH_MICROSEC) //
            .timeZoneOffset(TIME_ZONE_OFFSET) //
            .changeIdentifier(PM_MEAS_CHANGE_IDENTIFIER) //
            .changeType(FILE_READY_CHANGE_TYPE) //
            .build();
        ftpesFileData = ImmutableFileData.builder() //
            .name(PM_FILE_NAME) //
            .location(FTPES_LOCATION) //
            .scheme(Scheme.FTPES) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatType(MEAS_COLLECT_FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .messageMetaData(messageMetaData) //
            .build();

        List<FileData> files = new ArrayList<>();
        files.add(ftpesFileData);
        expectedFtpesMessage = ImmutableFileReadyMessage.builder() //
            .files(files) //
            .build();

        AdditionalField sftpAdditionalField = new JsonMessage.AdditionalFieldBuilder() //
            .location(SFTP_LOCATION) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatType(MEAS_COLLECT_FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .build();
        JsonMessage sftpJsonMessage = new JsonMessage.JsonMessageBuilder() //
            .eventName(NR_RADIO_ERICSSON_EVENT_NAME) //
            .changeIdentifier(PM_MEAS_CHANGE_IDENTIFIER) //
            .changeType(FILE_READY_CHANGE_TYPE) //
            .notificationFieldsVersion("1.0") //
            .addAdditionalField(sftpAdditionalField) //
            .build();
        sftpMessageString = sftpJsonMessage.toString();
        sftpMessageJson = jsonParser.parse(sftpMessageString);
        sftpFileData = ImmutableFileData.builder() //
            .name(PM_FILE_NAME) //
            .location(SFTP_LOCATION) //
            .scheme(Scheme.FTPES) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatType(MEAS_COLLECT_FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .messageMetaData(messageMetaData) //
            .build();

        ImmutableFilePublishInformation filePublishInformation = ImmutableFilePublishInformation.builder() //
            .productName(PRODUCT_NAME) //
            .vendorName(VENDOR_NAME) //
            .lastEpochMicrosec(LAST_EPOCH_MICROSEC) //
            .sourceName(SOURCE_NAME) //
            .startEpochMicrosec(START_EPOCH_MICROSEC) //
            .timeZoneOffset(TIME_ZONE_OFFSET) //
            .name(PM_FILE_NAME) //
            .location(FTPES_LOCATION) //
            .internalLocation(LOCAL_FILE_LOCATION) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatType(MEAS_COLLECT_FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .context(new HashMap<String, String>()) //
            .build();
        listOfFilePublishInformation.add(filePublishInformation);

        files = new ArrayList<>();
        files.add(sftpFileData);
        expectedSftpMessage = ImmutableFileReadyMessage.builder() //
            .files(files) //
            .build();
    }

    @Test
    public void whenPassedObjectDoesntFit_ThrowsDatafileTaskException() {
        prepareMocksForDmaapConsumer(Optional.empty(), null);

        StepVerifier.create(messageConsumer.getMessageRouterResponse()) //
            .expectSubscription() //
            .expectError(DatafileTaskException.class) //
            .verify();

        verify(messageRouterSubscriber, times(1))
                .getElements(dmaapConsumerConfiguration.getMessageRouterSubscribeRequest());    }

    @Test
    public void whenFtpes_ReturnsCorrectResponse() {
        prepareMocksForDmaapConsumer(Optional.of(ftpesMessageJson), expectedFtpesMessage);

        StepVerifier.create(messageConsumer.getMessageRouterResponse()) //
            .expectNext(expectedFtpesMessage) //
            .verifyComplete();

        verify(messageRouterSubscriber, times(1))
                .getElements(dmaapConsumerConfiguration.getMessageRouterSubscribeRequest());
        verifyNoMoreInteractions(messageRouterSubscriber);
    }

    @Test
    public void whenSftp_ReturnsCorrectResponse() {
        prepareMocksForDmaapConsumer(Optional.of(sftpMessageJson), expectedSftpMessage);

        StepVerifier.create(messageConsumer.getMessageRouterResponse()) //
            .expectNext(expectedSftpMessage) //
            .verifyComplete();

        verify(messageRouterSubscriber, times(1))
                .getElements(dmaapConsumerConfiguration.getMessageRouterSubscribeRequest());
        verifyNoMoreInteractions(messageRouterSubscriber);
    }

    private void prepareMocksForDmaapConsumer(Optional<JsonElement> message,
        FileReadyMessage fileReadyMessageAfterConsume) {
        Flux<JsonElement> messageAsMono = message.isPresent() ? Flux.just(message.get()) : Flux.empty();

        JsonMessageParser jsonMessageParserMock = mock(JsonMessageParser.class);
        when(messageRouterSubscriber.getElements(dmaapConsumerConfiguration.getMessageRouterSubscribeRequest()))
                .thenReturn(messageAsMono);
        when(appConfig.getDmaapConsumerConfiguration()).thenReturn(dmaapConsumerConfiguration);

        if (message.isPresent()) {
            when(jsonMessageParserMock.getMessagesFromJson(any())).thenReturn(Flux.just(fileReadyMessageAfterConsume));
        } else {
            when(jsonMessageParserMock.getMessagesFromJson(any()))
                .thenReturn(Flux.error(new DatafileTaskException("problemas")));
        }

        messageConsumer = spy(new DMaaPMessageConsumer(appConfig, jsonMessageParserMock));
    }

}
