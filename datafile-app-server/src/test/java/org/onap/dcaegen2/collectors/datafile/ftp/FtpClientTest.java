/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 Nordix Foundation. All rights reserved.
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
package org.onap.dcaegen2.collectors.datafile.ftp;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

public class FtpClientTest {

    private static final String HOME_DIR = "/";
    private static final String FILE = "/dir/sample.txt";
    private static final String CONTENTS = "abcdef 1234567890";
    private static final int PORT = 8021;

    private static final String USERNAME = "bob";
    private static final String PASSWORD = "123";

    // private FtpsClient ftpsClient;
    private FakeFtpServer fakeFtpServer;

    // @Test
    // public void testReadFile() throws Exception {
    //// String contents = remoteFile.readFile(FILE);
    //// assertEquals("contents", CONTENTS, contents);
    // String fileName = FilenameUtils.getName(FILE);
    // FtpsClient ftpsClient = new FtpsClient("127.0.0.1", USERNAME, PASSWORD, PORT, FILE, "target/"
    // + fileName);
    // ftpsClient.collectFile();
    // String contents = FileUtils.readFileToString(new File("target/"+fileName));
    // Assertions.assertEquals( CONTENTS, contents);
    // }

    // @Test
    // public void testReadFileThrowsException() {
    // try {
    // remoteFile.readFile("NoSuchFile.txt");
    // fail("Expected IOException");
    // } catch (IOException expected) {
    // // Expected this
    // }
    // }

    @BeforeAll
    protected void setUp() throws Exception {
        fakeFtpServer = new FakeFtpServer();
        fakeFtpServer.setServerControlPort(PORT); // use any free port

        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new FileEntry(FILE, CONTENTS));
        fakeFtpServer.setFileSystem(fileSystem);
        UserAccount userAccount = new UserAccount(USERNAME, PASSWORD, HOME_DIR);
        fakeFtpServer.addUserAccount(userAccount);

        fakeFtpServer.start();
        // int port = fakeFtpServer.getServerControlPort();
        // remoteFile = new RemoteFile();
        // remoteFile.setServer("localhost");
        // remoteFile.setPort(port);
    }

    @AfterAll
    protected void tearDown() throws Exception {
        fakeFtpServer.stop();
    }
}
