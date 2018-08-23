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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;

/**
 * Gets file from xNF with FTPS protocoll.
 * 
 * TODO: Refactor for better test and error handling.
 * 
 * @author  <a href="mailto:martin.c.yan@est.tech">Martin Yan</a>
 *
 */
public class FtpsClient {
    public void collectFile(String serverAddress, String userId, String password, int port, String remoteFile,
            String localFile) {
        try {
            FTPSClient ftps = new FTPSClient("TLS");
            ftps.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
            // try to connect
            try {
                ftps.connect(serverAddress, port);
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }

            // login to server
            if (!ftps.login(userId, password)) {
                ftps.logout();
                System.out.println("Login Error");
                return;
            }

            int reply = ftps.getReplyCode();
            // FTPReply stores a set of constants for FTP reply codes.
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftps.disconnect();
                System.out.println("Connection Error");
                return;
            }
            // enter passive mode
            ftps.enterLocalPassiveMode();
            // get system name
            // System.out.println("Remote system is " + ftps.getSystemType());
            // get current directory
            // System.out.println("Current directory is " + ftps.printWorkingDirectory());

            // get output stream
            OutputStream output;
            File outfile = new File(localFile);
            outfile.createNewFile();

            output = new FileOutputStream(outfile);
            // get the file from the remote system
            ftps.retrieveFile(remoteFile, output);
            // close output stream
            output.close();
            System.out.println("File " + outfile.getName() + " Download Successfull");

            ftps.logout();
            ftps.disconnect();
        } catch (IOException ex) {
            ex.printStackTrace();

        }
    }
}
