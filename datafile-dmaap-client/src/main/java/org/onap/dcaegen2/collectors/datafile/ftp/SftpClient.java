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

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.nio.file.Path;
import java.util.Optional;

import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets file from xNF with SFTP protocol.
 *
 * @author <a href="mailto:martin.c.yan@est.tech">Martin Yan</a>
 *
 */
public class SftpClient implements FileCollectClient {
    private static final Logger logger = LoggerFactory.getLogger(SftpClient.class);
    private final FileServerData fileServerData;
    private Session session = null;
    private ChannelSftp sftpChannel = null;

    public SftpClient(FileServerData fileServerData) {
        this.fileServerData = fileServerData;
    }

    @Override
    public void collectFile(String remoteFile, Path localFile) throws DatafileTaskException {
        logger.trace("collectFile called");

        try {
            sftpChannel.get(remoteFile, localFile.toString());
            logger.debug("File {} Download Successfull from xNF", localFile.getFileName());
        } catch (Exception e) {
            throw new DatafileTaskException("Unable to get file from xNF. Data: " + fileServerData + e);
        }

        logger.trace("collectFile OK");
    }

    @Override
    public void close() {
        logger.trace("close");
        if (sftpChannel != null) {
            sftpChannel.exit();
            sftpChannel = null;
        }
        if (session != null) {
            session.disconnect();
            session = null;
        }
    }

    public void open() throws DatafileTaskException {
        try {
            if (session == null) {
                session = setUpSession(fileServerData);
                sftpChannel = getChannel(session);
            }
        } catch (JSchException e) {
            throw new DatafileTaskException("Could not open Sftp client", e);
        }
    }

    private int getPort(Optional<Integer> port) {
        final int FTPS_DEFAULT_PORT = 22;
        return port.isPresent() ? port.get() : FTPS_DEFAULT_PORT;
    }

    private Session setUpSession(FileServerData fileServerData) throws JSchException {
        JSch jsch = new JSch();

        Session newSession = jsch.getSession(fileServerData.userId(), fileServerData.serverAddress(),
                getPort(fileServerData.port()));
        newSession.setConfig("StrictHostKeyChecking", "no");
        newSession.setPassword(fileServerData.password());
        newSession.connect();
        return newSession;
    }

    private ChannelSftp getChannel(Session session) throws JSchException {
        Channel channel = session.openChannel("sftp");
        channel.connect();
        return (ChannelSftp) channel;
    }
}
