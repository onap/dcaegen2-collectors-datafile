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

package org.onap.dcaegen2.collectors.datafile.tasks;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.config.DmaapConsumerConfiguration;
import org.onap.dcaegen2.collectors.datafile.config.ImmutableDmaapConsumerConfiguration;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.exceptions.DmaapEmptyResponseException;
import org.onap.dcaegen2.collectors.datafile.ftp.FileCollector;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.service.DmaapConsumerJsonParser;
import org.onap.dcaegen2.collectors.datafile.service.FileData;
import org.onap.dcaegen2.collectors.datafile.service.ImmutableFileData;
import org.onap.dcaegen2.collectors.datafile.service.consumer.DmaapConsumerReactiveHttpClient;
import org.onap.dcaegen2.collectors.datafile.utils.JsonMessage;
import org.onap.dcaegen2.collectors.datafile.utils.JsonMessage.AdditionalField;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 5/17/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
class DmaapConsumerTaskImplTest {
    private static final String PM_MEAS_CHANGE_IDINTIFIER = "PM_MEAS_FILES";
    private static final String FILE_READY_CHANGE_TYPE = "FileReady";
    private static final String FTPES_SCHEME = "ftpes://";
    private static final String SFTP_SCHEME = "sftp://";
    private static final String SERVER_ADDRESS = "192.168.0.101";
    private static final String PORT_22 = "22";
    private static final String PM_FILE_NAME = "A20161224.1030-1045.bin.gz";
    private static final String REMOTE_FILE_LOCATION = "/ftp/rop/" + PM_FILE_NAME;
    private static final String LOCAL_FILE_LOCATION = "target/" + PM_FILE_NAME;
    private static final String FTPES_LOCATION = FTPES_SCHEME + SERVER_ADDRESS + ":" + PORT_22 + REMOTE_FILE_LOCATION;
    private static final String SFTP_LOCATION = SFTP_SCHEME + SERVER_ADDRESS + ":" + PORT_22 + REMOTE_FILE_LOCATION;
    private static final String GZIP_COMPRESSION = "gzip";
    private static final String MEAS_COLLECT_FILE_FORMAT_TYPE = "org.3GPP.32.435#measCollec";
    private static final String FILE_FORMAT_VERSION = "V10";

    private static ArrayList<ConsumerDmaapModel> listOfConsumerDmaapModel = new ArrayList<ConsumerDmaapModel>();

    private static AppConfig appConfig;
    private static DmaapConsumerConfiguration dmaapConsumerConfiguration;
    private DmaapConsumerTaskImpl dmaapConsumerTask;
    private DmaapConsumerReactiveHttpClient dmaapConsumerReactiveHttpClient;

    private static FileCollector fileCollectorMock;

    private static String ftpesMessage;
    private static ArrayList<FileData> ftpesFileDataAfterConsume = new ArrayList<FileData>();

    private static String sftpMessage;
    private static ArrayList<FileData> sftpFileDataAfterConsume = new ArrayList<FileData>();

    @BeforeAll
    public static void setUp() {
        dmaapConsumerConfiguration = new ImmutableDmaapConsumerConfiguration.Builder().consumerGroup("OpenDCAE-c12")
                .consumerId("c12").dmaapContentType("application/json").dmaapHostName("54.45.33.2")
                .dmaapPortNumber(1234).dmaapProtocol("https").dmaapUserName("Datafile").dmaapUserPassword("Datafile")
                .dmaapTopicName("unauthenticated.NOTIFICATION").timeoutMS(-1).messageLimit(-1).build();

        appConfig = mock(AppConfig.class);

        AdditionalField ftpesAdditionalField =
                new JsonMessage.AdditionalFieldBuilder().location(FTPES_LOCATION).compression(GZIP_COMPRESSION)
                        .fileFormatType(MEAS_COLLECT_FILE_FORMAT_TYPE).fileFormatVersion(FILE_FORMAT_VERSION).build();
        JsonMessage ftpesJsonMessage = new JsonMessage.JsonMessageBuilder().changeIdentifier(PM_MEAS_CHANGE_IDINTIFIER)
                .changeType(FILE_READY_CHANGE_TYPE).notificationFieldsVersion("1.0")
                .addAdditionalField(ftpesAdditionalField).build();
        ftpesMessage = ftpesJsonMessage.toString();
        FileData ftpesFileData = ImmutableFileData.builder().changeIdentifier(PM_MEAS_CHANGE_IDINTIFIER)
                .changeType(FILE_READY_CHANGE_TYPE).location(FTPES_LOCATION).compression(GZIP_COMPRESSION)
                .fileFormatType(MEAS_COLLECT_FILE_FORMAT_TYPE).fileFormatVersion(FILE_FORMAT_VERSION).build();
        ftpesFileDataAfterConsume.add(ftpesFileData);

        AdditionalField sftpAdditionalField =
                new JsonMessage.AdditionalFieldBuilder().location(SFTP_LOCATION).compression(GZIP_COMPRESSION)
                        .fileFormatType(MEAS_COLLECT_FILE_FORMAT_TYPE).fileFormatVersion(FILE_FORMAT_VERSION).build();
        JsonMessage sftpJsonMessage = new JsonMessage.JsonMessageBuilder().changeIdentifier(PM_MEAS_CHANGE_IDINTIFIER)
                .changeType(FILE_READY_CHANGE_TYPE).notificationFieldsVersion("1.0")
                .addAdditionalField(sftpAdditionalField).build();
        sftpMessage = sftpJsonMessage.toString();
        FileData sftpFileData = ImmutableFileData.builder().changeIdentifier(PM_MEAS_CHANGE_IDINTIFIER)
                .changeType(FILE_READY_CHANGE_TYPE).location(SFTP_LOCATION).compression(GZIP_COMPRESSION)
                .fileFormatType(MEAS_COLLECT_FILE_FORMAT_TYPE).fileFormatVersion(FILE_FORMAT_VERSION).build();
        sftpFileDataAfterConsume.add(sftpFileData);


        ImmutableConsumerDmaapModel consumerDmaapModel =
                ImmutableConsumerDmaapModel.builder().location(LOCAL_FILE_LOCATION).compression(GZIP_COMPRESSION)
                        .fileFormatType(MEAS_COLLECT_FILE_FORMAT_TYPE).fileFormatVersion(FILE_FORMAT_VERSION).build();
        listOfConsumerDmaapModel.add(consumerDmaapModel);

        fileCollectorMock = mock(FileCollector.class);
    }

    @Test
    public void whenPassedObjectDoesntFit_ThrowsDatafileTaskException() {
        // given
        prepareMocksForDmaapConsumer("", new ArrayList<FileData>());

        // then
        StepVerifier.create(dmaapConsumerTask.execute("Sample input")).expectSubscription()
                .expectError(DmaapEmptyResponseException.class).verify();

        verify(dmaapConsumerReactiveHttpClient, times(1)).getDmaapConsumerResponse();
    }

    @Test
    public void whenFtpes_ReturnsCorrectResponse() throws DatafileTaskException {
        // given
        prepareMocksForDmaapConsumer(ftpesMessage, ftpesFileDataAfterConsume);
        // when
        final ArrayList<ConsumerDmaapModel> arrayOfResponse = dmaapConsumerTask.execute("Sample input").block();
        // then
        verify(dmaapConsumerReactiveHttpClient, times(1)).getDmaapConsumerResponse();
        verifyNoMoreInteractions(dmaapConsumerReactiveHttpClient);
        verify(fileCollectorMock, times(1)).getFilesFromSender(ftpesFileDataAfterConsume);
        verifyNoMoreInteractions(fileCollectorMock);
        Assertions.assertEquals(listOfConsumerDmaapModel, arrayOfResponse);

    }

    @Test
    public void whenSftp_ReturnsCorrectResponse() throws DatafileTaskException {
        // given
        prepareMocksForDmaapConsumer(sftpMessage, sftpFileDataAfterConsume);
        // when
        final ArrayList<ConsumerDmaapModel> arrayOfResponse = dmaapConsumerTask.execute("Sample input").block();
        // then
        verify(dmaapConsumerReactiveHttpClient, times(1)).getDmaapConsumerResponse();
        verifyNoMoreInteractions(dmaapConsumerReactiveHttpClient);
        verify(fileCollectorMock, times(1)).getFilesFromSender(sftpFileDataAfterConsume);
        verifyNoMoreInteractions(fileCollectorMock);
        Assertions.assertEquals(listOfConsumerDmaapModel, arrayOfResponse);

    }

    private void prepareMocksForDmaapConsumer(String message, ArrayList<FileData> fileDataAfterConsume) {
        Mono<String> messageAsMono = Mono.just(message);
        DmaapConsumerJsonParser dmaapConsumerJsonParserMock = mock(DmaapConsumerJsonParser.class);
        dmaapConsumerReactiveHttpClient = mock(DmaapConsumerReactiveHttpClient.class);
        when(dmaapConsumerReactiveHttpClient.getDmaapConsumerResponse()).thenReturn(messageAsMono);

        if (!message.isEmpty()) {
            when(dmaapConsumerJsonParserMock.getJsonObject(messageAsMono)).thenReturn(Mono.just(fileDataAfterConsume));
        } else {
            when(dmaapConsumerJsonParserMock.getJsonObject(messageAsMono))
                    .thenReturn(Mono.error(new DmaapEmptyResponseException()));
        }
        when(fileCollectorMock.getFilesFromSender(fileDataAfterConsume))
                .thenReturn(Mono.just(listOfConsumerDmaapModel));

        dmaapConsumerTask = spy(new DmaapConsumerTaskImpl(appConfig, dmaapConsumerReactiveHttpClient,
                dmaapConsumerJsonParserMock, fileCollectorMock));
        when(dmaapConsumerTask.resolveConfiguration()).thenReturn(dmaapConsumerConfiguration);
        doReturn(dmaapConsumerReactiveHttpClient).when(dmaapConsumerTask).resolveClient();
    }
}
