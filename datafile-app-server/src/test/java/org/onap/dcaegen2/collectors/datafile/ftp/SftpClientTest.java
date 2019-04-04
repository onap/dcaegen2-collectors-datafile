/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018-2019 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.ftp;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.stefanbirkner.fakesftpserver.rule.FakeSftpServerRule;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Rule;
import org.junit.Test;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;

public class SftpClientTest {
    private static final String USERNAME = "bob";
    private static final String PASSWORD = "123";
    private static final String DUMMY_CONTENT = "dummy content";
    private static final Path LOCAL_DUMMY_FILE = Paths.get("target/dummy.txt");
    private static final String REMOTE_DUMMY_FILE = "/dummy_directory/dummy_file.txt";
    private static final JSch JSCH = new JSch();
    private static final int TIMEOUT = 2000;

    @Rule
    public final FakeSftpServerRule sftpServer = new FakeSftpServerRule().addUser(USERNAME, PASSWORD);

    @Test
    public void collectFile_withOKresponse()
        throws DatafileTaskException, IOException, JSchException, SftpException, Exception {
        FileServerData expectedFileServerData = ImmutableFileServerData.builder().serverAddress("127.0.0.1")
            .userId(USERNAME).password(PASSWORD).port(sftpServer.getPort()).build();
        try (SftpClient sftpClient = new SftpClient(expectedFileServerData)) {
            sftpClient.open();
            sftpServer.putFile(REMOTE_DUMMY_FILE, DUMMY_CONTENT, UTF_8);
            byte[] file = downloadFile(sftpServer, REMOTE_DUMMY_FILE);

            sftpClient.collectFile(REMOTE_DUMMY_FILE, LOCAL_DUMMY_FILE);
            byte[] localFile = Files.readAllBytes(LOCAL_DUMMY_FILE.toFile().toPath());
            assertThat(new String(file, UTF_8)).isEqualTo(DUMMY_CONTENT);
            assertThat(new String(localFile, UTF_8)).isEqualTo(DUMMY_CONTENT);
        }
    }

    @Test
    public void collectFile_withWrongUserName_shouldFail() throws DatafileTaskException, IOException {
        FileServerData expectedFileServerData = ImmutableFileServerData.builder().serverAddress("127.0.0.1")
            .userId("wrong").password(PASSWORD).port(sftpServer.getPort()).build();
        try (SftpClient sftpClient = new SftpClient(expectedFileServerData)) {

            sftpServer.putFile(REMOTE_DUMMY_FILE, DUMMY_CONTENT, UTF_8);

            assertThatThrownBy(() -> sftpClient.open())
                .hasMessageContaining("Could not open Sftp clientcom.jcraft.jsch.JSchException: Auth fail");
        }
    }

    @Test
    public void collectFile_withWrongFileName_shouldFail()
        throws IOException, JSchException, SftpException, DatafileTaskException {
        FileServerData expectedFileServerData = ImmutableFileServerData.builder().serverAddress("127.0.0.1")
            .userId(USERNAME).password(PASSWORD).port(sftpServer.getPort()).build();
        try (SftpClient sftpClient = new SftpClient(expectedFileServerData)) {
            sftpServer.putFile(REMOTE_DUMMY_FILE, DUMMY_CONTENT, UTF_8);
            sftpClient.open();

            assertThatThrownBy(() -> sftpClient.collectFile("wrong", LOCAL_DUMMY_FILE))
                .hasMessageStartingWith("Unable to get file from xNF. Data: FileServerData{serverAddress=127.0.0.1, "
                    + "userId=bob, password=123, port=");
        }
    }

    private static Session connectToServer(FakeSftpServerRule sftpServer) throws JSchException {
        return connectToServerAtPort(sftpServer.getPort());
    }

    private static Session connectToServerAtPort(int port) throws JSchException {
        Session session = createSessionWithCredentials(USERNAME, PASSWORD, port);
        session.connect(TIMEOUT);
        return session;
    }

    private static ChannelSftp connectSftpChannel(Session session) throws JSchException {
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
        return channel;
    }

    private static Session createSessionWithCredentials(String username, String password, int port)
        throws JSchException {
        Session session = JSCH.getSession(username, "127.0.0.1", port);
        session.setConfig("StrictHostKeyChecking", "no");
        session.setPassword(password);
        return session;
    }

    private static byte[] downloadFile(FakeSftpServerRule server, String path)
        throws JSchException, SftpException, IOException {
        Session session = connectToServer(server);
        ChannelSftp channel = connectSftpChannel(session);
        try {
            InputStream is = channel.get(path);
            return toByteArray(is);
        } finally {
            channel.disconnect();
            session.disconnect();
        }
    }
}
