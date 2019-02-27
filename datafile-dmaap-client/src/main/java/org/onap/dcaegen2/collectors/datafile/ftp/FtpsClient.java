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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.io.FileSystemResourceWrapper;
import org.onap.dcaegen2.collectors.datafile.io.IFileSystemResource;
import org.onap.dcaegen2.collectors.datafile.ssl.IKeyManagerUtils;
import org.onap.dcaegen2.collectors.datafile.ssl.IKeyManagerUtils.KeyManagerException;
import org.onap.dcaegen2.collectors.datafile.ssl.IKeyStore;
import org.onap.dcaegen2.collectors.datafile.ssl.ITrustManagerFactory;
import org.onap.dcaegen2.collectors.datafile.ssl.KeyManagerUtilsWrapper;
import org.onap.dcaegen2.collectors.datafile.ssl.KeyStoreWrapper;
import org.onap.dcaegen2.collectors.datafile.ssl.TrustManagerFactoryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets file from xNF with FTPS protocol.
 *
 * @author <a href="mailto:martin.c.yan@est.tech">Martin Yan</a>
 */
public class FtpsClient implements FileCollectClient {
    private static final Logger logger = LoggerFactory.getLogger(FtpsClient.class);
    private String keyCertPath;
    private String keyCertPassword;
    private Path trustedCAPath;
    private String trustedCAPassword;

    private FTPSClient realFtpsClient = new FTPSClient();
    private IKeyManagerUtils keyManagerUtils = new KeyManagerUtilsWrapper();
    private IKeyStore keyStore;
    private ITrustManagerFactory trustManagerFactory;
    private IFileSystemResource fileSystemResource = new FileSystemResourceWrapper();
    private boolean keyManagerSet = false;
    private boolean trustManagerSet = false;
    private final FileServerData fileServerData;


    public FtpsClient(FileServerData fileServerData) {
        this.fileServerData = fileServerData;
    }

    @Override
    public void collectFile(String remoteFile, Path localFile) throws DatafileTaskException {
        logger.trace("collectFile called");

        try {
            realFtpsClient.setNeedClientAuth(true);
            setUpKeyManager(realFtpsClient);
            setUpTrustedCA(realFtpsClient);
            setUpConnection(realFtpsClient);
            getFileFromxNF(realFtpsClient, remoteFile, localFile);
        } catch (IOException e) {
            logger.trace("", e);
            throw new DatafileTaskException("Could not open connection: ", e);
        } catch (KeyManagerException e) {
            logger.trace("", e);
            throw new DatafileTaskException(e);
        } finally {
            closeDownConnection(realFtpsClient);
        }
        logger.trace("collectFile fetched: {}", localFile);
    }

    private void setUpKeyManager(FTPSClient ftps) throws KeyManagerException {
        if (keyManagerSet) {
            logger.trace("keyManager already set!");
        } else {
            keyManagerUtils.setCredentials(keyCertPath, keyCertPassword);
            ftps.setKeyManager(keyManagerUtils.getClientKeyManager());
            keyManagerSet = true;
        }
        logger.trace("complete setUpKeyManager");
    }

    private void setUpTrustedCA(FTPSClient ftps) throws DatafileTaskException {
        if (trustManagerSet) {
            logger.trace("trustManager already set!");
        } else {
            try {
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
                throw new DatafileTaskException("Unable to trust xNF's CA, " + trustedCAPath + " " + e);
            }
        }
        logger.trace("complete setUpTrustedCA");
    }

    private int getPort(Optional<Integer> port) {
        final int FTPS_DEFAULT_PORT = 21;
        return port.isPresent() ? port.get() : FTPS_DEFAULT_PORT;
    }

    private void setUpConnection(FTPSClient ftps) throws DatafileTaskException, IOException {
        if (!ftps.isConnected()) {
            ftps.connect(fileServerData.serverAddress(), getPort(fileServerData.port()));
            logger.trace("after ftp connect");

            if (!ftps.login(fileServerData.userId(), fileServerData.password())) {
                throw new DatafileTaskException("Unable to log in to xNF. " + fileServerData.serverAddress());
            }

            if (FTPReply.isPositiveCompletion(ftps.getReplyCode())) {
                ftps.enterLocalPassiveMode();
                ftps.setFileType(FTP.BINARY_FILE_TYPE);
                // Set protection buffer size
                ftps.execPBSZ(0);
                // Set data channel protection to private
                ftps.execPROT("P");
                ftps.setBufferSize(1024 * 1024);
            } else {
                throw new DatafileTaskException("Unable to connect to xNF. " + fileServerData.serverAddress()
                        + " xNF reply code: " + ftps.getReplyCode());
            }
        }
        logger.trace("setUpConnection successfully!");
    }

    private void getFileFromxNF(FTPSClient ftps, String remoteFileName, Path localFileName)
            throws IOException {
        logger.trace("starting to getFile");

        File localFile = localFileName.toFile();
        if (localFile.createNewFile()) {
            logger.warn("Local file {} already created", localFileName);
        }
        OutputStream output = new FileOutputStream(localFile);
        logger.trace("begin to retrieve from xNF.");
        if (!ftps.retrieveFile(remoteFileName, output)) {
            throw new IOException("Could not retrieve file");
        }
        logger.trace("end retrieve from xNF.");
        output.close();
        logger.debug("File {} Download Successfull from xNF", localFileName);
    }


    private void closeDownConnection(FTPSClient ftps) {
        logger.trace("starting to closeDownConnection");
        if (ftps != null && ftps.isConnected()) {
            try {
                boolean logOut = ftps.logout();
                logger.trace("logOut: {}", logOut);
            } catch (Exception e) {
                logger.trace("Unable to logout connection.", e);
            }
            try {
                ftps.disconnect();
                logger.trace("disconnected!");
            } catch (Exception e) {
                logger.trace("Unable to disconnect connection.", e);
            }
        }
    }

    public void setKeyCertPath(String keyCertPath) {
        this.keyCertPath = keyCertPath;
    }

    public void setKeyCertPassword(String keyCertPassword) {
        this.keyCertPassword = keyCertPassword;
    }

    public void setTrustedCAPath(String trustedCAPath) {
        this.trustedCAPath = Paths.get(trustedCAPath);
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

    private IKeyStore getKeyStore() throws KeyStoreException {
        if (keyStore == null) {
            keyStore = new KeyStoreWrapper();
        }

        return keyStore;
    }

    void setFtpsClient(FTPSClient ftpsClient) {
        this.realFtpsClient = ftpsClient;
    }

    void setKeyManagerUtils(IKeyManagerUtils keyManagerUtils) {
        this.keyManagerUtils = keyManagerUtils;
    }

    void setKeyStore(IKeyStore keyStore) {
        this.keyStore = keyStore;
    }

    void setTrustManagerFactory(ITrustManagerFactory tmf) {
        trustManagerFactory = tmf;
    }

    void setFileSystemResource(IFileSystemResource fileSystemResource) {
        this.fileSystemResource = fileSystemResource;
    }
}
