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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Gets file from xNF with FTPS protocol.
 *
 * TODO: Refactor for better test.
 *
 * @author <a href="mailto:martin.c.yan@est.tech">Martin Yan</a>
 *
 */
@Component
public class FtpsClient { // TODO: Should be final but needs PowerMock or Mockito 2.x to be able to
                          // mock then, so this will be done as an improvement after first version
                          // committed.
    private static final Logger logger = LoggerFactory.getLogger(FtpsClient.class);

    public boolean collectFile(FileServerData fileServerData, String remoteFile, String localFile) {
        logger.trace("collectFile called with fileServerData: {}, remoteFile: {}, localFile: {}", fileServerData,
                remoteFile, localFile);
        boolean result = true;
        try {
            FTPSClient ftps = new FTPSClient("TLS");

            result = setUpConnection(fileServerData, ftps);

            if (result) {
                getFile(remoteFile, localFile, ftps);

                closeDownConnection(ftps);
            }
        } catch (IOException ex) {
            logger.error("Unable to collect file from xNF. {} Cause: {}", fileServerData, ex);
            result = false;
        }
        logger.trace("collectFile left with result: {}", result);
        return result;
    }

    private boolean setUpConnection(FileServerData fileServerData, FTPSClient ftps) {
        boolean success = true;
        ftps.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());

        try {
            ftps.connect(fileServerData.serverAddress(), fileServerData.port());

            if (!ftps.login(fileServerData.userId(), fileServerData.password())) {
                ftps.logout();
                logger.error("Unable to log in to xNF. {}", fileServerData);
                success = false;
            }

            if (success) {
                int reply = ftps.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    ftps.disconnect();
                    logger.error("Unable to connect in to xNF. {}", fileServerData);
                    success = false;
                }
                ftps.enterLocalPassiveMode();
            }
        } catch (Exception ex) {
            logger.error("Unable to connect to xNF. {} Cause: {}", fileServerData, ex);
            success = false;
        }

        return success;
    }

    private void getFile(String remoteFile, String localFile, FTPSClient ftps)
            throws IOException {
        OutputStream output;
        File outfile = new File(localFile);
        outfile.createNewFile();

        output = new FileOutputStream(outfile);

        ftps.retrieveFile(remoteFile, output);

        output.close();
        logger.debug("File {} Download Successfull from xNF", outfile.getName());
    }

    private void closeDownConnection(FTPSClient ftps) {
        try {
            ftps.logout();
            ftps.disconnect();
        } catch (Exception e) {
            // Do nothing, file has been collected.
        }
    }
}
