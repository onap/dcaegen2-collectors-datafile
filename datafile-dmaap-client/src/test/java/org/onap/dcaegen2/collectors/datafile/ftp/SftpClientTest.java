/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 Nordix Foundation. All rights reserved.
 * ===============================================================================================
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
 * ============LICENSE_END========================================================================
 */

package org.onap.dcaegen2.collectors.datafile.ftp;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.stefanbirkner.fakesftpserver.rule.FakeSftpServerRule;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;

public class SftpClientTest {
    private static final String USERNAME = "bob";
    private static final String PASSWORD = "123";
    private static final String DUMMY_CONTENT = "dummy content";
    private static final String LOCAL_DUMMY_FILE = "target/dummy.txt";
    private static final String REMOTE_DUMMY_FILE = "/dummy_directory/dummy_file.txt";
    private static final JSch JSCH = new JSch();
    private static final int TIMEOUT = 2000;

    @Rule
    public final FakeSftpServerRule sftpServer = new FakeSftpServerRule().addUser(USERNAME, PASSWORD);

    @Test
    public void collectFile_withOKresponse() throws IOException, JSchException, SftpException {
        SftpClient sftpClient = new SftpClient();
        sftpServer.putFile(REMOTE_DUMMY_FILE, DUMMY_CONTENT, UTF_8);
        byte[] file = downloadFile(sftpServer, REMOTE_DUMMY_FILE);
        FileServerData expectedFileServerData = ImmutableFileServerData.builder().serverAddress("127.0.0.1")
                .userId(USERNAME).password(PASSWORD).port(sftpServer.getPort()).build();
        sftpClient.collectFile(expectedFileServerData, REMOTE_DUMMY_FILE,
                LOCAL_DUMMY_FILE);
        byte[] localFile = Files.readAllBytes(new File(LOCAL_DUMMY_FILE).toPath());
        assertThat(new String(file, UTF_8)).isEqualTo(DUMMY_CONTENT);
        assertThat(new String(localFile, UTF_8)).isEqualTo(DUMMY_CONTENT);
    }

    @Test
    public void collectFile_withWrongUserName_shouldFail() throws IOException, JSchException, SftpException {
        SftpClient sftpClient = new SftpClient();
        sftpServer.putFile(REMOTE_DUMMY_FILE, DUMMY_CONTENT, UTF_8);
        byte[] file = downloadFile(sftpServer, REMOTE_DUMMY_FILE);
        FileServerData expectedFileServerData = ImmutableFileServerData.builder().serverAddress("127.0.0.1")
                .userId("Wrong").password(PASSWORD).port(sftpServer.getPort()).build();
        FileCollectResult actualResult = sftpClient.collectFile(expectedFileServerData, REMOTE_DUMMY_FILE,
                LOCAL_DUMMY_FILE);

        assertFalse(actualResult.downloadSuccessful());
        String expectedErrorMessage = "Unable to set up SFTP connection to xNF. Data: "
                + "FileServerData{serverAddress=127.0.0.1, userId=Wrong, password=123, port=";
        assertTrue(actualResult.getErrorData().toString().startsWith(expectedErrorMessage));
    }

    @Test
    public void collectFile_withWrongFileName_shouldFail() throws IOException, JSchException, SftpException {
        SftpClient sftpClient = new SftpClient();
        sftpServer.putFile(REMOTE_DUMMY_FILE, DUMMY_CONTENT, UTF_8);
        byte[] file = downloadFile(sftpServer, REMOTE_DUMMY_FILE);
        FileServerData expectedFileServerData = ImmutableFileServerData.builder().serverAddress("127.0.0.1")
                .userId(USERNAME).password(PASSWORD).port(sftpServer.getPort()).build();
        FileCollectResult actualResult = sftpClient.collectFile(expectedFileServerData, "wrong",
                LOCAL_DUMMY_FILE);

        assertFalse(actualResult.downloadSuccessful());
        String expectedErrorMessage = "Unable to get file from xNF. Data: FileServerData{serverAddress=127.0.0.1, "
                + "userId=bob, password=123, port=";
        assertTrue(actualResult.getErrorData().toString().startsWith(expectedErrorMessage));
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
