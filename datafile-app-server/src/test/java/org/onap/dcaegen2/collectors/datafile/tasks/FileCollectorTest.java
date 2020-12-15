/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018-2019 Nordix Foundation. All rights reserved.
 * Copyright (C) 2020 Nokia. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.configuration.FtpesConfig;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.exceptions.NonRetryableDatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.ftp.FtpesClient;
import org.onap.dcaegen2.collectors.datafile.commons.Scheme;
import org.onap.dcaegen2.collectors.datafile.ftp.SftpClient;
import org.onap.dcaegen2.collectors.datafile.model.Counters;
import org.onap.dcaegen2.collectors.datafile.model.FileData;
import org.onap.dcaegen2.collectors.datafile.model.FilePublishInformation;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFileData;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFilePublishInformation;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableMessageMetaData;
import org.onap.dcaegen2.collectors.datafile.model.MessageMetaData;
import reactor.test.StepVerifier;

public class FileCollectorTest {
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
    private static final int PORT_22 = 22;
    private static final String PM_FILE_NAME = "A20161224.1030-1045.bin.gz";
    private static final Path LOCAL_FILE_LOCATION = Paths.get(FileData.DATAFILE_TMPDIR, PM_FILE_NAME);
    private static final String REMOTE_FILE_LOCATION = "/ftp/rop/" + PM_FILE_NAME;
    private static final String USER = "usr";
    private static final String PWD = "pwd";
    private static final String FTPES_LOCATION =
        FTPES_SCHEME + USER + ":" + PWD + "@" + SERVER_ADDRESS + ":" + PORT_22 + REMOTE_FILE_LOCATION;

    private static final String FTPES_LOCATION_NO_PORT =
        FTPES_SCHEME + USER + ":" + PWD + "@" + SERVER_ADDRESS + REMOTE_FILE_LOCATION;
    private static final String SFTP_LOCATION = SFTP_SCHEME + SERVER_ADDRESS + ":" + PORT_22 + REMOTE_FILE_LOCATION;
    private static final String SFTP_LOCATION_NO_PORT = SFTP_SCHEME + SERVER_ADDRESS + REMOTE_FILE_LOCATION;

    private static final String GZIP_COMPRESSION = "gzip";
    private static final String MEAS_COLLECT_FILE_FORMAT_TYPE = "org.3GPP.32.435#measCollec";
    private static final String FILE_FORMAT_VERSION = "V10";

    private static final String FTP_KEY_PATH = "ftpKeyPath";
    private static final String FTP_KEY_PASSWORD_PATH = "ftpKeyPassword";
    private static final String TRUSTED_CA_PATH = "trustedCAPath";
    private static final String TRUSTED_CA_PASSWORD_PATH = "trustedCAPassword";
    private static final String CHANGE_IDENTIFIER = "PM_MEAS_FILES";

    private static AppConfig appConfigMock = mock(AppConfig.class);
    private static FtpesConfig ftpesConfigMock = mock(FtpesConfig.class);

    private FtpesClient ftpesClientMock = mock(FtpesClient.class);

    private SftpClient sftpClientMock = mock(SftpClient.class);
    private final Map<String, String> contextMap = new HashMap<>();

    private Counters counters;

    private MessageMetaData createMessageMetaData() {
        return ImmutableMessageMetaData.builder() //
            .productName(PRODUCT_NAME) //
            .vendorName(VENDOR_NAME) //
            .lastEpochMicrosec(LAST_EPOCH_MICROSEC) //
            .sourceName(SOURCE_NAME) //
            .startEpochMicrosec(START_EPOCH_MICROSEC) //
            .timeZoneOffset(TIME_ZONE_OFFSET) //
            .changeIdentifier(PM_MEAS_CHANGE_IDENTIFIER) //
            .changeType(FILE_READY_CHANGE_TYPE) //
            .build();
    }

    private FileData createFileData(String location, Scheme scheme) {
        return ImmutableFileData.builder() //
            .name(PM_FILE_NAME) //
            .location(location) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatType(MEAS_COLLECT_FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .scheme(scheme) //
            .messageMetaData(createMessageMetaData()) //
            .build();
    }

    private FilePublishInformation createExpectedFilePublishInformation(String location) {
        return ImmutableFilePublishInformation.builder() //
            .productName(PRODUCT_NAME) //
            .vendorName(VENDOR_NAME) //
            .lastEpochMicrosec(LAST_EPOCH_MICROSEC) //
            .sourceName(SOURCE_NAME) //
            .startEpochMicrosec(START_EPOCH_MICROSEC) //
            .timeZoneOffset(TIME_ZONE_OFFSET) //
            .name(PM_FILE_NAME) //
            .location(location) //
            .internalLocation(LOCAL_FILE_LOCATION) //
            .compression(GZIP_COMPRESSION) //
            .fileFormatType(MEAS_COLLECT_FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .context(new HashMap<String, String>()) //
            .changeIdentifier(CHANGE_IDENTIFIER) //
            .build();
    }

    @BeforeAll
    static void setUpConfiguration() {
        when(appConfigMock.getFtpesConfiguration()).thenReturn(ftpesConfigMock);
        when(ftpesConfigMock.keyCert()).thenReturn(FTP_KEY_PATH);
        when(ftpesConfigMock.keyPasswordPath()).thenReturn(FTP_KEY_PASSWORD_PATH);
        when(ftpesConfigMock.trustedCa()).thenReturn(TRUSTED_CA_PATH);
        when(ftpesConfigMock.trustedCaPasswordPath()).thenReturn(TRUSTED_CA_PASSWORD_PATH);
    }

    @BeforeEach
    void setUpTest() {
        counters = new Counters();
    }

    @Test
    public void whenFtpesFile_returnCorrectResponse() throws Exception {
        FileCollector collectorUndetTest = spy(new FileCollector(appConfigMock, counters));
        doReturn(ftpesClientMock).when(collectorUndetTest).createFtpesClient(any());

        FileData fileData = createFileData(FTPES_LOCATION_NO_PORT, Scheme.FTPES);

        FilePublishInformation expectedfilePublishInformation =
            createExpectedFilePublishInformation(FTPES_LOCATION_NO_PORT);

        StepVerifier.create(collectorUndetTest.collectFile(fileData, 3, Duration.ofSeconds(0), contextMap))
            .expectNext(expectedfilePublishInformation) //
            .verifyComplete();

        verify(ftpesClientMock, times(1)).open();
        verify(ftpesClientMock, times(1)).collectFile(REMOTE_FILE_LOCATION, LOCAL_FILE_LOCATION);
        verify(ftpesClientMock, times(1)).close();
        verifyNoMoreInteractions(ftpesClientMock);

        assertEquals(1, counters.getNoOfCollectedFiles(),"collectedFiles should have been 1");
        assertEquals(0, counters.getNoOfFailedFtpAttempts(),"failedFtpAttempts should have been 0");
    }

    @Test
    public void whenSftpFile_returnCorrectResponse() throws Exception {
        FileCollector collectorUndetTest = spy(new FileCollector(appConfigMock, counters));
        doReturn(sftpClientMock).when(collectorUndetTest).createSftpClient(any());

        FileData fileData = createFileData(SFTP_LOCATION_NO_PORT, Scheme.SFTP);
        FilePublishInformation expectedfilePublishInformation =
            createExpectedFilePublishInformation(SFTP_LOCATION_NO_PORT);

        StepVerifier.create(collectorUndetTest.collectFile(fileData, 3, Duration.ofSeconds(0), contextMap))
            .expectNext(expectedfilePublishInformation) //
            .verifyComplete();

        // The same again, but with port
        fileData = createFileData(SFTP_LOCATION, Scheme.SFTP);
        expectedfilePublishInformation = createExpectedFilePublishInformation(SFTP_LOCATION);

        StepVerifier.create(collectorUndetTest.collectFile(fileData, 3, Duration.ofSeconds(0), contextMap))
            .expectNext(expectedfilePublishInformation) //
            .verifyComplete();

        verify(sftpClientMock, times(2)).open();
        verify(sftpClientMock, times(2)).collectFile(REMOTE_FILE_LOCATION, LOCAL_FILE_LOCATION);
        verify(sftpClientMock, times(2)).close();
        verifyNoMoreInteractions(sftpClientMock);

        assertEquals(2, counters.getNoOfCollectedFiles(),"collectedFiles should have been 2");
    }

    @Test
    public void whenFtpesFileAlwaysFail_retryAndFail() throws Exception {
        FileCollector collectorUndetTest = spy(new FileCollector(appConfigMock, counters));
        doReturn(ftpesClientMock).when(collectorUndetTest).createFtpesClient(any());

        FileData fileData = createFileData(FTPES_LOCATION, Scheme.FTPES);
        doThrow(new DatafileTaskException("Unable to collect file.")).when(ftpesClientMock)
            .collectFile(REMOTE_FILE_LOCATION, LOCAL_FILE_LOCATION);

        StepVerifier.create(collectorUndetTest.collectFile(fileData, 3, Duration.ofSeconds(0), contextMap))
            .expectErrorMessage("Retries exhausted: 3/3") //
            .verify();

        verify(ftpesClientMock, times(4)).collectFile(REMOTE_FILE_LOCATION, LOCAL_FILE_LOCATION);

        assertEquals(0, counters.getNoOfCollectedFiles(),"collectedFiles should have been 0");
        assertEquals(4, counters.getNoOfFailedFtpAttempts(),"failedFtpAttempts should have been 4");
    }

    @Test
    public void whenFtpesFileAlwaysFail_failWithoutRetry() throws Exception {
        FileCollector collectorUndetTest = spy(new FileCollector(appConfigMock, counters));
        doReturn(ftpesClientMock).when(collectorUndetTest).createFtpesClient(any());

        FileData fileData = createFileData(FTPES_LOCATION, Scheme.FTPES);
        doThrow(new NonRetryableDatafileTaskException("Unable to collect file.")).when(ftpesClientMock)
            .collectFile(REMOTE_FILE_LOCATION, LOCAL_FILE_LOCATION);

        StepVerifier.create(collectorUndetTest.collectFile(fileData, 3, Duration.ofSeconds(0), contextMap))
            .expectErrorMessage("Non retryable file transfer failure") //
            .verify();

        verify(ftpesClientMock, times(1)).collectFile(REMOTE_FILE_LOCATION, LOCAL_FILE_LOCATION);

        assertEquals(0, counters.getNoOfCollectedFiles(),"collectedFiles should have been 0");
        assertEquals(1, counters.getNoOfFailedFtpAttempts(),"failedFtpAttempts should have been 1");
    }

    @Test
    public void whenFtpesFileFailOnce_retryAndReturnCorrectResponse() throws Exception {
        FileCollector collectorUndetTest = spy(new FileCollector(appConfigMock, counters));
        doReturn(ftpesClientMock).when(collectorUndetTest).createFtpesClient(any());
        doThrow(new DatafileTaskException("Unable to collect file.")).doNothing().when(ftpesClientMock)
            .collectFile(REMOTE_FILE_LOCATION, LOCAL_FILE_LOCATION);

        FilePublishInformation expectedfilePublishInformation =
            createExpectedFilePublishInformation(FTPES_LOCATION_NO_PORT);

        FileData fileData = createFileData(FTPES_LOCATION_NO_PORT, Scheme.FTPES);

        StepVerifier.create(collectorUndetTest.collectFile(fileData, 3, Duration.ofSeconds(0), contextMap))
            .expectNext(expectedfilePublishInformation) //
            .verifyComplete();

        verify(ftpesClientMock, times(2)).collectFile(REMOTE_FILE_LOCATION, LOCAL_FILE_LOCATION);

        assertEquals(1, counters.getNoOfCollectedFiles(),"collectedFiles should have been 1");
        assertEquals(1, counters.getNoOfFailedFtpAttempts(),"failedFtpAttempts should have been 1");
    }
}
