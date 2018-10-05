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

package org.onap.dcaegen2.collectors.datafile.ftp;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Gets file from xNF with SFTP protocol.
 *
 * @author <a href="mailto:martin.c.yan@est.tech">Martin Yan</a>
 *
 */
@Component
public class SftpClient {
    private static final Logger logger = LoggerFactory.getLogger(SftpClient.class);

    public boolean collectFile(FileServerData fileServerData, String remoteFile, String localFile) {
        boolean result = true;
        Session session = setUpSession(fileServerData);

        if (session != null) {
            ChannelSftp sftpChannel = getChannel(session, fileServerData);
            if (sftpChannel != null) {
                try {
                    sftpChannel.get(remoteFile, localFile);
                    logger.debug("File {} Download Successfull from xNF", FilenameUtils.getName(localFile));
                } catch (SftpException e) {
                    logger.error("Unable to get file from xNF. Data: {}", fileServerData, e);
                    result = false;
                }

                sftpChannel.exit();
            } else {
                result = false;
            }
            session.disconnect();
        } else {
            result = false;
        }
        return result;
    }

    private Session setUpSession(FileServerData fileServerData) {
        JSch jsch = new JSch();

        Session session = null;
        try {
            session = jsch.getSession(fileServerData.userId(), fileServerData.serverAddress(), fileServerData.port());
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(fileServerData.password());
            session.connect();
        } catch (JSchException e) {
            logger.error("Unable to set up SFTP connection to xNF. Data: {}", fileServerData, e);
        }
        return session;
    }

    private ChannelSftp getChannel(Session session, FileServerData fileServerData) {
        ChannelSftp sftpChannel = null;
        try {
            Channel channel;
            channel = session.openChannel("sftp");
            channel.connect();
            sftpChannel = (ChannelSftp) channel;
        } catch (JSchException e) {
            logger.error("Unable to get sftp channel to xNF. Data: {}", fileServerData, e);
        }
        return sftpChannel;
    }
}
