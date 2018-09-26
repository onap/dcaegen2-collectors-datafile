/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 Nordix Foundation. All rights reserved.
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.ftp.FileServerData;
import org.onap.dcaegen2.collectors.datafile.ftp.FtpsClient;
import org.onap.dcaegen2.collectors.datafile.ftp.ImmutableFileServerData;
import org.onap.dcaegen2.collectors.datafile.ftp.SftpClient;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.FileData;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFileData;

import reactor.test.StepVerifier;

/**
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 *
 */
public class XnfCollectorTaskImplTest {

    private static final String PM_MEAS_CHANGE_IDINTIFIER = "PM_MEAS_FILES";
    private static final String FILE_READY_CHANGE_TYPE = "FileReady";
    private static final String FTPES_SCHEME = "ftpes://";
    private static final String SFTP_SCHEME = "sftp://";
    private static final String SERVER_ADDRESS = "192.168.0.101";
    private static final int PORT_22 = 22;
    private static final String PM_FILE_NAME = "A20161224.1030-1045.bin.gz";
    private static final String REMOTE_FILE_LOCATION = "/ftp/rop/" + PM_FILE_NAME;
    private static final String LOCAL_FILE_LOCATION = "target" + File.separator + PM_FILE_NAME;
    private static final String FTPES_LOCATION = FTPES_SCHEME + SERVER_ADDRESS + ":" + PORT_22 + REMOTE_FILE_LOCATION;
    private static final String SFTP_LOCATION = SFTP_SCHEME + SERVER_ADDRESS + ":" + PORT_22 + REMOTE_FILE_LOCATION;
    private static final String GZIP_COMPRESSION = "gzip";
    private static final String MEAS_COLLECT_FILE_FORMAT_TYPE = "org.3GPP.32.435#measCollec";
    private static final String FILE_FORMAT_VERSION = "V10";

    private FtpsClient ftpsClientMock = mock(FtpsClient.class);

    private SftpClient sftpClientMock = mock(SftpClient.class);

    private XnfCollectorTask collectorUndetTest = new XnfCollectorTaskImpl(ftpsClientMock, sftpClientMock);

    @Test
    public void whenSingleFtpesFile_returnCorrectResponse() {
        FileData fileData = ImmutableFileData.builder().changeIdentifier(PM_MEAS_CHANGE_IDINTIFIER)
                .changeType(FILE_READY_CHANGE_TYPE).name(PM_FILE_NAME).location(FTPES_LOCATION)
                .compression(GZIP_COMPRESSION).fileFormatType(MEAS_COLLECT_FILE_FORMAT_TYPE)
                .fileFormatVersion(FILE_FORMAT_VERSION).build();

        ConsumerDmaapModel consumerDmaapModel = ImmutableConsumerDmaapModel.builder().name(PM_FILE_NAME)
                .location(LOCAL_FILE_LOCATION).compression(GZIP_COMPRESSION).fileFormatType(MEAS_COLLECT_FILE_FORMAT_TYPE)
                .fileFormatVersion(FILE_FORMAT_VERSION).build();

        FileServerData expectedFileServerData = ImmutableFileServerData.builder().serverAddress(SERVER_ADDRESS)
                .userId("").password("").port(PORT_22).build();
        when(ftpsClientMock.collectFile(expectedFileServerData, REMOTE_FILE_LOCATION, LOCAL_FILE_LOCATION))
                .thenReturn(Boolean.TRUE);

        StepVerifier.create(collectorUndetTest.execute(fileData)).expectNext(consumerDmaapModel).verifyComplete();

        verify(ftpsClientMock, times(1)).collectFile(expectedFileServerData, REMOTE_FILE_LOCATION, LOCAL_FILE_LOCATION);
        verifyNoMoreInteractions(ftpsClientMock);
    }

    @Test
    public void whenSingleSftpFile_returnCorrectResponse() {
        FileData fileData = ImmutableFileData.builder().changeIdentifier(PM_MEAS_CHANGE_IDINTIFIER)
                .changeType(FILE_READY_CHANGE_TYPE).name(PM_FILE_NAME).location(SFTP_LOCATION)
                .compression(GZIP_COMPRESSION).fileFormatType(MEAS_COLLECT_FILE_FORMAT_TYPE)
                .fileFormatVersion(FILE_FORMAT_VERSION).build();

        ConsumerDmaapModel consumerDmaapModel = ImmutableConsumerDmaapModel.builder().name(PM_FILE_NAME)
                .location(LOCAL_FILE_LOCATION).compression(GZIP_COMPRESSION).fileFormatType(MEAS_COLLECT_FILE_FORMAT_TYPE)
                .fileFormatVersion(FILE_FORMAT_VERSION).build();

        FileServerData expectedFileServerData = ImmutableFileServerData.builder().serverAddress(SERVER_ADDRESS)
                .userId("").password("").port(PORT_22).build();
        when(sftpClientMock.collectFile(expectedFileServerData, REMOTE_FILE_LOCATION, LOCAL_FILE_LOCATION))
                .thenReturn(Boolean.TRUE);

        StepVerifier.create(collectorUndetTest.execute(fileData)).expectNext(consumerDmaapModel).verifyComplete();

        verify(sftpClientMock, times(1)).collectFile(expectedFileServerData, REMOTE_FILE_LOCATION, LOCAL_FILE_LOCATION);
        verifyNoMoreInteractions(ftpsClientMock);
    }
}
