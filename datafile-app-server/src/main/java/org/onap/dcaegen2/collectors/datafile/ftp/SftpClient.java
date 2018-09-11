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

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * Gets file from xNF with SFTP protocoll.
 *
 * TODO: Refactor for better test and error handling.
 *
 * @author <a href="mailto:martin.c.yan@est.tech">Martin Yan</a>
 *
 */
@Component
public class SftpClient { // TODO: Should be final but needs PowerMock to be able to mock then, so
                          // this will be done as an improvement after first version committed.
    private static final Logger logger = LoggerFactory.getLogger(SftpClient.class);

    public void collectFile(FileServerData fileServerData, String remoteFile,
            String localFile) {
        JSch jsch = new JSch(); // TODO: Might be changed to use Spring as an improvement after
                                // first version committed.
        Session session = null;
        try {
            session = jsch.getSession(fileServerData.userId(), fileServerData.serverAddress(), fileServerData.port());
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(fileServerData.password());
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            sftpChannel.get(remoteFile, localFile);
            sftpChannel.exit();
            session.disconnect();
            logger.debug("File " + FilenameUtils.getName(localFile) + " Download Successfull");
        } catch (JSchException e) {
            // TODO: Handle properly. Will be done as an improvement after first version committed.
            logger.debug(e.getMessage());
        } catch (SftpException e) {
            // TODO: Handle properly. Will be done as an improvement after first version committed.
            logger.debug(e.getMessage());
        }
    }
}
