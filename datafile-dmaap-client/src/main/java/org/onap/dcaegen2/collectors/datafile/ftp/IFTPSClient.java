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
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;

public interface IFTPSClient {
    public void setNeedClientAuth(boolean isNeedClientAuth);

    public void setKeyManager(KeyManager keyManager);

    public void setTrustManager(TrustManager trustManager);

    public void connect(String hostname, int port) throws IOException;

    public boolean login(String username, String password) throws IOException;

    public boolean logout() throws IOException;

    public int getReplyCode();

    public void setBufferSize(int bufSize);

    public boolean isConnected();

    public void disconnect() throws IOException;

    public void enterLocalPassiveMode();

    public void setFileType(int fileType) throws IOException;

    public void execPBSZ(int newParam) throws IOException;

    public void execPROT(String prot) throws IOException;

    public void retrieveFile(String remote, OutputStream local) throws DatafileTaskException;

    void setTimeout(Integer t);
}
