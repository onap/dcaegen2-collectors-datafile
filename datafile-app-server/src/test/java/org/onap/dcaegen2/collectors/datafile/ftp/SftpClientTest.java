/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018-2019 Nordix Foundation. All rights reserved.
 * ===============================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * ============LICENSE_END========================================================================
 */

package org.onap.dcaegen2.collectors.datafile.ftp;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;

@ExtendWith(MockitoExtension.class)
public class SftpClientTest {
    private static final String HOST = "127.0.0.1";
    private static final int SFTP_PORT = 1021;
    private static final String USERNAME = "bob";
    private static final String PASSWORD = "123";

    @Mock
    private JSch jschMock;

    @Mock
    private Session sessionMock;

    @Mock
    private ChannelSftp channelMock;

    @Test
    public void openWithPort_success()
            throws DatafileTaskException, IOException, JSchException, SftpException, Exception {
        FileServerData expectedFileServerData = ImmutableFileServerData.builder() //
                .serverAddress(HOST) //
                .userId(USERNAME) //
                .password(PASSWORD) //
                .port(SFTP_PORT) //
                .build();

        SftpClient sftpClientSpy = spy(new SftpClient(expectedFileServerData));

        doReturn(jschMock).when(sftpClientSpy).createJsch();
        when(jschMock.getSession(anyString(), anyString(), anyInt())).thenReturn(sessionMock);
        when(sessionMock.openChannel(anyString())).thenReturn(channelMock);

        sftpClientSpy.open();

        verify(jschMock).getSession(USERNAME, HOST, SFTP_PORT);
        verify(sessionMock).setConfig("StrictHostKeyChecking", "no");
        verify(sessionMock).setPassword(PASSWORD);
        verify(sessionMock).connect();
        verify(sessionMock).openChannel("sftp");
        verifyNoMoreInteractions(sessionMock);

        verify(channelMock).connect();
        verifyNoMoreInteractions(channelMock);
    }

    @Test
    public void openWithoutPort_success()
            throws DatafileTaskException, IOException, JSchException, SftpException, Exception {
        FileServerData expectedFileServerData = ImmutableFileServerData.builder() //
                .serverAddress(HOST) //
                .userId(USERNAME) //
                .password(PASSWORD) //
                .port(Optional.empty()) //
                .build();

        SftpClient sftpClientSpy = spy(new SftpClient(expectedFileServerData));

        doReturn(jschMock).when(sftpClientSpy).createJsch();
        when(jschMock.getSession(anyString(), anyString(), anyInt())).thenReturn(sessionMock);
        when(sessionMock.openChannel(anyString())).thenReturn(channelMock);

        sftpClientSpy.open();

        verify(jschMock).getSession(USERNAME, HOST, 22);
    }

    @Test
    public void open_throwsException()
            throws DatafileTaskException, IOException, JSchException, SftpException, Exception {
        FileServerData expectedFileServerData = ImmutableFileServerData.builder() //
                .serverAddress(HOST) //
                .userId(USERNAME) //
                .password(PASSWORD) //
                .port(SFTP_PORT) //
                .build();

        SftpClient sftpClientSpy = spy(new SftpClient(expectedFileServerData));

        doReturn(jschMock).when(sftpClientSpy).createJsch();
        when(jschMock.getSession(anyString(), anyString(), anyInt())).thenThrow(new JSchException("Failed"));

        assertThatThrownBy(() -> sftpClientSpy.open()).hasMessageStartingWith("Could not open Sftp client.");
    }

    @SuppressWarnings("resource")
    @Test
    public void collectFile_succes() throws DatafileTaskException, SftpException {
        FileServerData expectedFileServerData = ImmutableFileServerData.builder() //
                .serverAddress(HOST) //
                .userId(USERNAME) //
                .password(PASSWORD) //
                .port(SFTP_PORT) //
                .build();
        SftpClient sftpClient = new SftpClient(expectedFileServerData);

        sftpClient.sftpChannel = channelMock;

        sftpClient.collectFile("remote.xml", Paths.get("local.xml"));

        verify(channelMock).get("remote.xml", "local.xml");
        verifyNoMoreInteractions(channelMock);
    }

    @Test
    public void collectFile_throwsExceptionWithoutRetry()
            throws IOException, JSchException, SftpException, DatafileTaskException {
        FileServerData expectedFileServerData = ImmutableFileServerData.builder() //
                .serverAddress(HOST) //
                .userId(USERNAME) //
                .password(PASSWORD) //
                .port(SFTP_PORT) //
                .build();

        try (SftpClient sftpClient = new SftpClient(expectedFileServerData)) {
            sftpClient.sftpChannel = channelMock;
            doThrow(new SftpException(ChannelSftp.SSH_FX_NO_SUCH_FILE, "Failed")).when(channelMock).get(anyString(),
                    anyString());

            assertThatThrownBy(() -> sftpClient.collectFile("remoteFile", Paths.get("localFile")))
                    .isInstanceOf(DatafileTaskException.class)
                    .hasMessageStartingWith("Unable to get file from xNF. No retry attempts will be done")
                    .hasMessageContaining("Data: FileServerData{serverAddress=" + HOST + ", " + "userId=" + USERNAME
                            + ", password=####, port=" + SFTP_PORT);
        }
    }

    @Test
    public void close_succes() throws DatafileTaskException, SftpException {
        SftpClient sftpClient = new SftpClient(null);

        sftpClient.session = sessionMock;
        sftpClient.sftpChannel = channelMock;

        sftpClient.close();

        verify(sessionMock).disconnect();
        verifyNoMoreInteractions(sessionMock);

        verify(channelMock).exit();;
        verifyNoMoreInteractions(channelMock);
    }
}
