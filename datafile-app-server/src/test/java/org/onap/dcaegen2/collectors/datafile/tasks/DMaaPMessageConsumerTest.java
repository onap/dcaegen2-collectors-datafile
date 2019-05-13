/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2018 NOKIA Intellectual Property, 2018-2019 Nordix Foundation.
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.service.consumer.DMaaPConsumerReactiveHttpClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
    private static List<FilePublishInformation> listOfFilePublishInformation = new ArrayList<FilePublishInformation>();

    private DMaaPConsumerReactiveHttpClient httpClientMock;

    private DMaaPMessageConsumer messageConsumer;
    private static String ftpesMessageString;
    private static FileData ftpesFileData;
    private static FileReadyMessage expectedFtpesMessage;

    private static String sftpMessageString;
    private static FileData sftpFileData;
    private static FileReadyMessage expectedSftpMessage;

    /**
     * Sets up data for the test.
     */
    @BeforeAll
    public static void setUp() {
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
                .scheme(Scheme.FTPS) //
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
        sftpFileData = ImmutableFileData.builder() //
                .name(PM_FILE_NAME) //
                .location(SFTP_LOCATION) //
                .scheme(Scheme.FTPS) //
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
                .context(new HashMap<String,String>()) //
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
        prepareMocksForDmaapConsumer("", null);

        StepVerifier.create(messageConsumer.getMessageRouterResponse()) //
                .expectSubscription() //
                .expectError(DatafileTaskException.class) //
                .verify();

        verify(httpClientMock, times(1)).getDMaaPConsumerResponse();
    }

    @Test
    public void whenFtpes_ReturnsCorrectResponse() throws DatafileTaskException {
        prepareMocksForDmaapConsumer(ftpesMessageString, expectedFtpesMessage);

        StepVerifier.create(messageConsumer.getMessageRouterResponse()) //
                .expectNext(expectedFtpesMessage) //
                .verifyComplete();

        verify(httpClientMock, times(1)).getDMaaPConsumerResponse();
        verifyNoMoreInteractions(httpClientMock);
    }

    @Test
    public void whenSftp_ReturnsCorrectResponse() throws DatafileTaskException {
        prepareMocksForDmaapConsumer(sftpMessageString, expectedSftpMessage);

        StepVerifier.create(messageConsumer.getMessageRouterResponse()) //
                .expectNext(expectedSftpMessage) //
                .verifyComplete();

        verify(httpClientMock, times(1)).getDMaaPConsumerResponse();
        verifyNoMoreInteractions(httpClientMock);
    }

    private void prepareMocksForDmaapConsumer(String message, FileReadyMessage fileReadyMessageAfterConsume) {
        Mono<String> messageAsMono = Mono.just(message);
        JsonMessageParser jsonMessageParserMock = mock(JsonMessageParser.class);
        httpClientMock = mock(DMaaPConsumerReactiveHttpClient.class);
        when(httpClientMock.getDMaaPConsumerResponse()).thenReturn(messageAsMono);

        if (!message.isEmpty()) {
            when(jsonMessageParserMock.getMessagesFromJson(messageAsMono))
                    .thenReturn(Flux.just(fileReadyMessageAfterConsume));
        } else {
            when(jsonMessageParserMock.getMessagesFromJson(messageAsMono))
                    .thenReturn(Flux.error(new DatafileTaskException("problemas")));
        }

        messageConsumer = spy(new DMaaPMessageConsumer(httpClientMock, jsonMessageParserMock));
    }
}