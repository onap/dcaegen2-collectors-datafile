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

import java.io.IOException;
import java.io.OutputStream;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import org.apache.commons.net.ftp.FTPSClient;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;

public class FTPSClientWrapper implements IFTPSClient {
    private FTPSClient ftpsClient = new FTPSClient();

    @Override
    public void setNeedClientAuth(boolean isNeedClientAuth) {
        ftpsClient.setNeedClientAuth(isNeedClientAuth);
    }

    @Override
    public void setKeyManager(KeyManager keyManager) {
        ftpsClient.setKeyManager(keyManager);
    }

    @Override
    public void setTrustManager(TrustManager trustManager) {
        ftpsClient.setTrustManager(trustManager);
    }

    @Override
    public void connect(String hostName, int port) throws IOException {
        ftpsClient.connect(hostName, port);
    }

    @Override
    public boolean login(String username, String password) throws IOException {
        return ftpsClient.login(username, password);
    }

    @Override
    public boolean logout() throws IOException {
        return ftpsClient.logout();
    }

    @Override
    public int getReplyCode() {
        return ftpsClient.getReplyCode();
    }

    @Override
    public void disconnect() throws IOException {
        ftpsClient.disconnect();
    }

    @Override
    public void enterLocalPassiveMode() {
        ftpsClient.enterLocalPassiveMode();
    }

    @Override
    public void setFileType(int fileType) throws IOException {
        ftpsClient.setFileType(fileType);
    }

    @Override
    public void execPBSZ(int psbz) throws IOException {
        ftpsClient.execPBSZ(psbz);
    }

    @Override
    public void execPROT(String prot) throws IOException {
        ftpsClient.execPROT(prot);
    }

    @Override
    public void retrieveFile(String remote, OutputStream local) throws DatafileTaskException {
        try {
            if (!ftpsClient.retrieveFile(remote, local)) {
                throw new DatafileTaskException("could not retrieve file");
            }
        } catch (IOException e) {
            throw new DatafileTaskException(e);
        }
    }

    @Override
    public void setTimeout(Integer t) {
        this.ftpsClient.setDefaultTimeout(t);
    }

    @Override
    public boolean isConnected() {
        return ftpsClient.isConnected();
    }

    @Override
    public void setBufferSize(int bufSize) {
        ftpsClient.setBufferSize(bufSize);
    }
}
