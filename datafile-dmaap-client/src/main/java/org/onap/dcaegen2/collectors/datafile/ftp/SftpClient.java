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
import org.springframework.stereotype.Component;

/**
 * Gets file from xNF with SFTP protocol.
 *
 * @author <a href="mailto:martin.c.yan@est.tech">Martin Yan</a>
 *
 */
@Component
public class SftpClient extends FileCollectClient {
    @Override
    public FileCollectResult retryCollectFile() {
        logger.trace("retryCollectFile called");

        FileCollectResult result;
        Session session = setUpSession(fileServerData);

        if (session != null) {
            ChannelSftp sftpChannel = getChannel(session, fileServerData);
            if (sftpChannel != null) {
                try {
                    sftpChannel.get(remoteFile, localFile);
                    result = new FileCollectResult();
                    logger.debug("File {} Download Successfull from xNF", FilenameUtils.getName(localFile));
                } catch (SftpException e) {
                    addError("Unable to get file from xNF. Data: " + fileServerData, e);
                    result = new FileCollectResult(errorData);
                }

                sftpChannel.exit();
            } else {
                result = new FileCollectResult(errorData);
            }
            session.disconnect();
        } else {
            result = new FileCollectResult(errorData);
        }
        logger.trace("retryCollectFile left with result: {}", result);
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
            addError("Unable to set up SFTP connection to xNF. Data: " + fileServerData, e);
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
            addError("Unable to get sftp channel to xNF. Data: " + fileServerData, e);
        }
        return sftpChannel;
    }
}
