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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.net.ftp.FTPReply;
import org.onap.dcaegen2.collectors.datafile.io.FileSystemResourceWrapper;
import org.onap.dcaegen2.collectors.datafile.io.FileWrapper;
import org.onap.dcaegen2.collectors.datafile.io.IFile;
import org.onap.dcaegen2.collectors.datafile.io.IFileSystemResource;
import org.onap.dcaegen2.collectors.datafile.io.IOutputStream;
import org.onap.dcaegen2.collectors.datafile.io.OutputStreamWrapper;
import org.onap.dcaegen2.collectors.datafile.ssl.IKeyManagerUtils;
import org.onap.dcaegen2.collectors.datafile.ssl.IKeyStore;
import org.onap.dcaegen2.collectors.datafile.ssl.ITrustManagerFactory;
import org.onap.dcaegen2.collectors.datafile.ssl.KeyManagerUtilsWrapper;
import org.onap.dcaegen2.collectors.datafile.ssl.KeyStoreWrapper;
import org.onap.dcaegen2.collectors.datafile.ssl.TrustManagerFactoryWrapper;
import org.springframework.stereotype.Component;

/**
 * Gets file from xNF with FTPS protocol.
 *
 * @author <a href="mailto:martin.c.yan@est.tech">Martin Yan</a>
 */
@Component
public class FtpsClient extends FileCollectClient {
    private String keyCertPath;
    private String keyCertPassword;
    private String trustedCAPath;
    private String trustedCAPassword;

    private IFTPSClient realFtpsClient;
    private IKeyManagerUtils kmu;
    private IKeyStore keyStore;
    private ITrustManagerFactory trustManagerFactory;
    private IFile lf;
    private IFileSystemResource fileResource;
    private IOutputStream os;
    private boolean keyManagerSet = false;
    private boolean trustManagerSet = false;

    @Override
    public FileCollectResult retryCollectFile() {
        logger.trace("retryCollectFile called");

        FileCollectResult fileCollectResult;

        IFTPSClient ftps = getFtpsClient();

        ftps.setNeedClientAuth(true);

        if (setUpKeyManager(ftps) && setUpTrustedCA(ftps) && setUpConnection(ftps)) {
            if (getFileFromxNF(ftps)) {
                closeDownConnection(ftps);
                fileCollectResult = new FileCollectResult();
            } else {
                fileCollectResult = new FileCollectResult(errorData);
            }
        } else {
            fileCollectResult = new FileCollectResult(errorData);
        }
        logger.trace("retryCollectFile left with result: {}", fileCollectResult);
        return fileCollectResult;
    }

    private boolean setUpKeyManager(IFTPSClient ftps) {
        boolean result = true;
        if(keyManagerSet) {
            return result;
        }
        try {
            IKeyManagerUtils keyManagerUtils = getKeyManagerUtils();
            keyManagerUtils.setCredentials(keyCertPath, keyCertPassword);
            ftps.setKeyManager(keyManagerUtils.getClientKeyManager());
            keyManagerSet = true;
        } catch (GeneralSecurityException | IOException e) {
            addError("Unable to use own key store " + keyCertPath, e);
            result = false;
        }
        logger.trace("complete setUpKeyManager");
        return result;
    }

    private boolean setUpTrustedCA(IFTPSClient ftps) {
        boolean result = true;
        if(trustManagerSet) {
            return result;
        }
        try {
            IFileSystemResource fileSystemResource = getFileSystemResource();
            fileSystemResource.setPath(trustedCAPath);
            InputStream fis = fileSystemResource.getInputStream();
            IKeyStore ks = getKeyStore();
            ks.load(fis, trustedCAPassword.toCharArray());
            fis.close();
            ITrustManagerFactory tmf = getTrustManagerFactory();
            tmf.init(ks.getKeyStore());
            ftps.setTrustManager(tmf.getTrustManagers()[0]);
            trustManagerSet = true;

        } catch (Exception e) {
            addError("Unable to trust xNF's CA, " + trustedCAPath, e);
            result = false;
        }
        logger.trace("complete setUpTrustedCA");
        return result;
    }

    private boolean setUpConnection(IFTPSClient ftps) {
        boolean result = true;
        try {
            ftps.connect(fileServerData.serverAddress(), fileServerData.port());
            logger.trace("after ftp connect");
            boolean loginSuccesful = ftps.login(fileServerData.userId(), fileServerData.password());
            if (!loginSuccesful) {
                ftps.logout();
                addError("Unable to log in to xNF. " + fileServerData, null);
                result = false;
            }

            if (loginSuccesful && FTPReply.isPositiveCompletion(ftps.getReplyCode())) {
                ftps.enterLocalPassiveMode();
                // Set protection buffer size
                ftps.execPBSZ(0);
                // Set data channel protection to private
                ftps.execPROT("P");
            } else {
                ftps.disconnect();
                addError("Unable to connect to xNF. " + fileServerData + "xNF reply code: " + ftps.getReplyCode(), null);
                result = false;
            }
        } catch (Exception ex) {
            addError("Unable to connect to xNF. Data: " + fileServerData, ex);
            result = false;
        }
        logger.trace("setUpConnection return value: {}", result);
        return result;
    }

    private boolean getFileFromxNF(IFTPSClient ftps) {
        logger.trace("starting to getFile");
        boolean result = true;
        IFile outfile = getFile();
        try {
            outfile.setPath(localFile);
            outfile.createNewFile();

            IOutputStream outputStream = getOutputStream();
            OutputStream output = outputStream.getOutputStream(outfile.getFile());

            result = ftps.retrieveFile(remoteFile, output);
            if(!result) {
                output.close();
                logger.debug("retriveFile from ftp server failed");
                return result;
            }
            output.close();
            logger.debug("File {} Download Successfull from xNF", localFile);
        } catch (IOException ex) {
            addError("Unable to collect file from xNF. Data: " + fileServerData, ex);
            try {
                outfile.delete();
            } catch (Exception e) {
                // Nothing
            }
            result = false;
        }
        return result;
    }

    private void closeDownConnection(IFTPSClient ftps) {
        logger.trace("starting to closeDownConnection");
        try {
            if (ftps != null) {
                ftps.logout();
                ftps.disconnect();
            }
        } catch (Exception e) {
            // Do nothing, file has been collected.
        }
    }

    public void setKeyCertPath(String keyCertPath) {
        this.keyCertPath = keyCertPath;
    }

    public void setKeyCertPassword(String keyCertPassword) {
        this.keyCertPassword = keyCertPassword;
    }

    public void setTrustedCAPath(String trustedCAPath) {
        this.trustedCAPath = trustedCAPath;
    }

    public void setTrustedCAPassword(String trustedCAPassword) {
        this.trustedCAPassword = trustedCAPassword;
    }

    private ITrustManagerFactory getTrustManagerFactory() throws NoSuchAlgorithmException {
        if (trustManagerFactory == null) {
            trustManagerFactory = new TrustManagerFactoryWrapper();
        }
        return trustManagerFactory;
    }

    private IFTPSClient getFtpsClient() {
        if (realFtpsClient == null) {
            realFtpsClient = new FTPSClientWrapper();
        }
        return realFtpsClient;
    }

    private IKeyManagerUtils getKeyManagerUtils() {
        if (kmu == null) {
            kmu = new KeyManagerUtilsWrapper();
        }

        return kmu;
    }

    private IKeyStore getKeyStore() throws KeyStoreException {
        if (keyStore == null) {
            keyStore = new KeyStoreWrapper();
        }

        return keyStore;
    }

    private IFile getFile() {
        if (lf == null) {
            lf = new FileWrapper();
        }

        return lf;
    }

    private IOutputStream getOutputStream() {
        if (os == null) {
            os = new OutputStreamWrapper();
        }

        return os;
    }

    private IFileSystemResource getFileSystemResource() {
        if (fileResource == null) {
            fileResource = new FileSystemResourceWrapper();
        }
        return fileResource;
    }

    protected void setFtpsClient(IFTPSClient ftpsClient) {
        this.realFtpsClient = ftpsClient;
    }

    protected void setKeyManagerUtils(IKeyManagerUtils keyManagerUtils) {
        this.kmu = keyManagerUtils;
    }

    protected void setKeyStore(IKeyStore keyStore) {
        this.keyStore = keyStore;
    }

    protected void setTrustManagerFactory(ITrustManagerFactory tmf) {
        trustManagerFactory = tmf;
    }

    protected void setFile(IFile file) {
        lf = file;
    }

    protected void setOutputStream(IOutputStream outputStream) {
        os = outputStream;
    }

    protected void setFileSystemResource(IFileSystemResource fileSystemResource) {
        fileResource = fileSystemResource;
    }
}
