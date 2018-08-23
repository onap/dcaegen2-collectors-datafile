/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018 Nordix Foundation. All rights reserved.
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.exceptions.DmaapNotFoundException;
import org.onap.dcaegen2.collectors.datafile.ftp.FtpsClient;
import org.onap.dcaegen2.collectors.datafile.ftp.SftpClient;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.service.DmaapConsumerJsonParser;
import org.onap.dcaegen2.collectors.datafile.service.FileData;
import org.onap.dcaegen2.collectors.datafile.service.consumer.ExtendedDmaapConsumerHttpClientImpl;
import org.onap.dcaegen2.collectors.datafile.utils.JsonMessage;
import org.onap.dcaegen2.collectors.datafile.utils.JsonMessage.AdditionalField;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 5/17/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
class DmaapConsumerTaskImplTest {
    private static final String CHANGE_IDINTIFIER = "PM_MEAS_FILES";
    private static final String CHANGE_TYPE = "FileReady";
    private static final String FTPES_SCHEME = "ftpes://";
    private static final String SFTP_SCHEME = "sftp://";
    private static final String SERVER_ADDRESS = "192.168.0.101";
    private static final String PORT = "22";
    private static final String FILE_NAME = "A20161224.1030-1045.bin.gz";
    private static final String REMOTE_FILE = "/ftp/rop/" + FILE_NAME;
    private static final String LOCAL_FILE = "target/" + FILE_NAME;
    private static final String FTPES_LOCATION = FTPES_SCHEME + SERVER_ADDRESS + ":" + PORT + REMOTE_FILE;
    private static final String SFTP_LOCATION = SFTP_SCHEME + SERVER_ADDRESS + ":" + PORT + REMOTE_FILE;
    private static final String GZIP_COMPRESSION = "gzip";
    private static final String FILE_FORMAT_TYPE = "org.3GPP.32.435#measCollec";
    private static final String FILE_FORMAT_VERSION = "V10";

    private static ArrayList<ConsumerDmaapModel> listOfConsumerDmaapModel = new ArrayList<ConsumerDmaapModel>();
    private static DmaapConsumerTaskImpl dmaapConsumerTask;
    private static ExtendedDmaapConsumerHttpClientImpl extendedDmaapConsumerHttpClient;
    private static FtpsClient ftpsClient;
    private static SftpClient sftpClient;

    private static String ftpesMessage;
    private static String sftpMessage;

    @BeforeAll
    public static void setUp() {
        ImmutableConsumerDmaapModel consumerDmaapModel =
                ImmutableConsumerDmaapModel.builder().location(LOCAL_FILE).compression(GZIP_COMPRESSION)
                        .fileFormatType(FILE_FORMAT_TYPE).fileFormatVersion(FILE_FORMAT_VERSION).build();
        listOfConsumerDmaapModel.add(consumerDmaapModel);

        AdditionalField ftpesAdditionalField =
                new JsonMessage.AdditionalFieldBuilder().location(FTPES_LOCATION).compression(GZIP_COMPRESSION)
                        .fileFormatType(FILE_FORMAT_TYPE).fileFormatVersion(FILE_FORMAT_VERSION).build();
        JsonMessage ftpesJsonMessage = new JsonMessage.JsonMessageBuilder().changeIdentifier(CHANGE_IDINTIFIER)
                .changeType(CHANGE_TYPE).notificationFieldsVersion("1.0").addAdditionalField(ftpesAdditionalField).build();
        ftpesMessage = ftpesJsonMessage.toString();

        AdditionalField sftpAdditionalField =
                new JsonMessage.AdditionalFieldBuilder().location(SFTP_LOCATION).compression(GZIP_COMPRESSION)
                        .fileFormatType(FILE_FORMAT_TYPE).fileFormatVersion(FILE_FORMAT_VERSION).build();
        JsonMessage sftpJsonMessage = new JsonMessage.JsonMessageBuilder().changeIdentifier(CHANGE_IDINTIFIER)
                .changeType(CHANGE_TYPE).notificationFieldsVersion("1.0").addAdditionalField(ftpesAdditionalField).build();
        sftpMessage = sftpJsonMessage.toString();

        ftpsClient = mock(FtpsClient.class);
        sftpClient = mock(SftpClient.class);
    }

    @Test
    public void whenPassedObjectDoesntFit_ThrowsDatafileTaskException() {
        // given
        prepareMocksForDmaapConsumer(null, null);

        // when
        Executable executableFunction = () -> dmaapConsumerTask.execute("Sample input");

        // then
        Assertions.assertThrows(DatafileTaskException.class, executableFunction,
                "Throwing exception when http response code won't fit to assignment range");
        verify(extendedDmaapConsumerHttpClient, times(1)).getHttpConsumerResponse();
        verifyNoMoreInteractions(extendedDmaapConsumerHttpClient);
    }

    @Test
    public void whenFtpes_ReturnsCorrectResponse() throws DatafileTaskException {
        // given
        prepareMocksForDmaapConsumer(ftpesMessage, FTPES_LOCATION);
        // when
        ArrayList<ConsumerDmaapModel> arrayOfResponse = dmaapConsumerTask.execute("Sample input");
        // then
        verify(extendedDmaapConsumerHttpClient, times(1)).getHttpConsumerResponse();
        verify(ftpsClient, times(1)).collectFile(SERVER_ADDRESS, "", "", Integer.parseInt(PORT), REMOTE_FILE,
                LOCAL_FILE);
        verifyNoMoreInteractions(extendedDmaapConsumerHttpClient);
        Assertions.assertEquals(listOfConsumerDmaapModel, arrayOfResponse);

    }

    @Test
    public void whenSftp_ReturnsCorrectResponse() throws DatafileTaskException {
        // given
        prepareMocksForDmaapConsumer(sftpMessage, SFTP_LOCATION);
        // when
        ArrayList<ConsumerDmaapModel> arrayOfResponse = dmaapConsumerTask.execute("Sample input");
        // then
        verify(extendedDmaapConsumerHttpClient, times(1)).getHttpConsumerResponse();
        verify(sftpClient, times(1)).collectFile(SERVER_ADDRESS, "", "", Integer.parseInt(PORT), REMOTE_FILE,
                LOCAL_FILE);
        verifyNoMoreInteractions(extendedDmaapConsumerHttpClient);
        Assertions.assertEquals(listOfConsumerDmaapModel, arrayOfResponse);

    }

    private void prepareMocksForDmaapConsumer(String message, String location) {
        DmaapConsumerJsonParser dmaapConsumerJsonParser = mock(DmaapConsumerJsonParser.class);
        ArrayList<FileData> fileData = new ArrayList<FileData>();
        fileData.add(new FileData(CHANGE_IDINTIFIER, CHANGE_TYPE, location, GZIP_COMPRESSION, FILE_FORMAT_TYPE,
                FILE_FORMAT_VERSION));
        try {
            when(dmaapConsumerJsonParser.getJsonObject(message)).thenReturn(fileData);
        } catch (DmaapNotFoundException e) {
            // Cannot happen since we provide a valid message.
        }

        extendedDmaapConsumerHttpClient = mock(ExtendedDmaapConsumerHttpClientImpl.class);
        when(extendedDmaapConsumerHttpClient.getHttpConsumerResponse()).thenReturn(Optional.ofNullable(message));

        dmaapConsumerTask =
                new DmaapConsumerTaskImpl(extendedDmaapConsumerHttpClient, dmaapConsumerJsonParser, ftpsClient, sftpClient);
    }
}
