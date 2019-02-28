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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.onap.dcaegen2.collectors.datafile.ssl.IKeyManagerUtils;
import org.onap.dcaegen2.collectors.datafile.ssl.IKeyStore;
import org.onap.dcaegen2.collectors.datafile.ssl.ITrustManagerFactory;
import org.springframework.http.HttpStatus;

public class FtpsClientTest {

    private static final String REMOTE_FILE_PATH = "/dir/sample.txt";
    private static final Path LOCAL_FILE_PATH = Paths.get("target/sample.txt");
    private static final String XNF_ADDRESS = "127.0.0.1";
    private static final int PORT = 8021;
    private static final String FTP_KEY_PATH = "ftpKeyPath";
    private static final String FTP_KEY_PASSWORD = "ftpKeyPassword";
    private static final Path TRUSTED_CA_PATH = Paths.get("trustedCAPath");
    private static final String TRUSTED_CA_PASSWORD = "trustedCAPassword";

    private static final String USERNAME = "bob";
    private static final String PASSWORD = "123";

    private FTPSClient ftpsClientMock = mock(FTPSClient.class);
    private IKeyManagerUtils keyManagerUtilsMock = mock(IKeyManagerUtils.class);
    private KeyManager keyManagerMock = mock(KeyManager.class);
    private IKeyStore keyStoreWrapperMock = mock(IKeyStore.class);
    private KeyStore keyStoreMock = mock(KeyStore.class);
    private ITrustManagerFactory trustManagerFactoryMock = mock(ITrustManagerFactory.class);
    private TrustManager trustManagerMock = mock(TrustManager.class);
    private OutputStream outputStreamMock = mock(OutputStream.class);
    private InputStream inputStreamMock = mock(InputStream.class);

    FtpsClient clientUnderTestSpy = spy(new FtpsClient(createFileServerData()));


    private ImmutableFileServerData createFileServerData() {
        return ImmutableFileServerData.builder() //
                .serverAddress(XNF_ADDRESS) //
                .userId(USERNAME) //
                .password(PASSWORD) //
                .port(PORT) //
                .build();
    }

    @BeforeEach
    protected void setUp() throws Exception {
        clientUnderTestSpy.setFtpsClient(ftpsClientMock);
        clientUnderTestSpy.setKeyManagerUtils(keyManagerUtilsMock);
        clientUnderTestSpy.setKeyStore(keyStoreWrapperMock);
        clientUnderTestSpy.setTrustManagerFactory(trustManagerFactoryMock);

        doReturn(outputStreamMock).when(clientUnderTestSpy).createOutputStream(LOCAL_FILE_PATH);
        clientUnderTestSpy.setKeyCertPath(FTP_KEY_PATH);
        clientUnderTestSpy.setKeyCertPassword(FTP_KEY_PASSWORD);
        clientUnderTestSpy.setTrustedCAPath(TRUSTED_CA_PATH.toString());
        clientUnderTestSpy.setTrustedCAPassword(TRUSTED_CA_PASSWORD);
        doReturn(inputStreamMock).when(clientUnderTestSpy).createInputStream(TRUSTED_CA_PATH);
    }

    @Test
    public void collectFile_allOk() throws Exception {
        when(keyManagerUtilsMock.getClientKeyManager()).thenReturn(keyManagerMock);
        when(keyStoreWrapperMock.getKeyStore()).thenReturn(keyStoreMock);
        when(trustManagerFactoryMock.getTrustManagers()).thenReturn(new TrustManager[] {trustManagerMock});
        when(ftpsClientMock.login(USERNAME, PASSWORD)).thenReturn(true);
        when(ftpsClientMock.getReplyCode()).thenReturn(HttpStatus.OK.value());
        when(ftpsClientMock.isConnected()).thenReturn(false, true);
        when(ftpsClientMock.retrieveFile(anyString(), ArgumentMatchers.any(OutputStream.class))).thenReturn(true);

        clientUnderTestSpy.collectFile(REMOTE_FILE_PATH, LOCAL_FILE_PATH);

        verify(ftpsClientMock).setNeedClientAuth(true);
        verify(keyManagerUtilsMock).setCredentials(FTP_KEY_PATH, FTP_KEY_PASSWORD);
        verify(ftpsClientMock).setKeyManager(keyManagerMock);
        verify(keyStoreWrapperMock).load(inputStreamMock, TRUSTED_CA_PASSWORD.toCharArray());
        verify(inputStreamMock, times(1)).close();
        verify(trustManagerFactoryMock).init(keyStoreMock);
        verify(ftpsClientMock).setTrustManager(trustManagerMock);
        verify(ftpsClientMock).connect(XNF_ADDRESS, PORT);
        verify(ftpsClientMock).login(USERNAME, PASSWORD);
        verify(ftpsClientMock).getReplyCode();
        verify(ftpsClientMock, times(1)).enterLocalPassiveMode();
        verify(ftpsClientMock).execPBSZ(0);
        verify(ftpsClientMock).execPROT("P");
        verify(ftpsClientMock).setFileType(FTP.BINARY_FILE_TYPE);
        verify(ftpsClientMock).setBufferSize(1024 * 1024);
        verify(ftpsClientMock).retrieveFile(REMOTE_FILE_PATH, outputStreamMock);
        verify(outputStreamMock, times(1)).close();
        verify(ftpsClientMock, times(1)).logout();
        verify(ftpsClientMock, times(1)).disconnect();
        verify(ftpsClientMock, times(2)).isConnected();
        verifyNoMoreInteractions(ftpsClientMock);
    }

    @Test
    public void collectFileFaultyOwnKey_shouldFail() throws Exception {
        doThrow(new IKeyManagerUtils.KeyManagerException(new GeneralSecurityException())).when(keyManagerUtilsMock)
                .setCredentials(FTP_KEY_PATH, FTP_KEY_PASSWORD);
        when(ftpsClientMock.isConnected()).thenReturn(false);

        assertThatThrownBy(() -> clientUnderTestSpy.collectFile(REMOTE_FILE_PATH, LOCAL_FILE_PATH))
                .hasMessage("org.onap.dcaegen2.collectors.datafile.ssl.IKeyManagerUtils$KeyManagerException: "
                        + "java.security.GeneralSecurityException");

        verify(ftpsClientMock).setNeedClientAuth(true);
        verify(keyManagerUtilsMock).setCredentials(FTP_KEY_PATH, FTP_KEY_PASSWORD);
        verify(ftpsClientMock).isConnected();
        verifyNoMoreInteractions(ftpsClientMock);
    }

    @Test
    public void collectFileFaultTrustedCA_shouldFail() throws Exception {
        when(keyManagerUtilsMock.getClientKeyManager()).thenReturn(keyManagerMock);
        when(keyStoreWrapperMock.getKeyStore()).thenReturn(keyStoreMock);

        doThrow(new KeyStoreException()).when(trustManagerFactoryMock).init(keyStoreMock);

        assertThatThrownBy(() -> clientUnderTestSpy.collectFile(REMOTE_FILE_PATH, LOCAL_FILE_PATH))
                .hasMessage("Unable to trust xNF's CA, trustedCAPath java.security.KeyStoreException");
    }

    @Test
    public void collectFileFaultyLogin_shouldFail() throws Exception {
        when(keyManagerUtilsMock.getClientKeyManager()).thenReturn(keyManagerMock);
        when(keyStoreWrapperMock.getKeyStore()).thenReturn(keyStoreMock);
        when(trustManagerFactoryMock.getTrustManagers()).thenReturn(new TrustManager[] {trustManagerMock});
        when(ftpsClientMock.login(USERNAME, PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> clientUnderTestSpy.collectFile(REMOTE_FILE_PATH, LOCAL_FILE_PATH))
                .hasMessage("Unable to log in to xNF. 127.0.0.1");

        verify(ftpsClientMock).setNeedClientAuth(true);
        verify(keyManagerUtilsMock).setCredentials(FTP_KEY_PATH, FTP_KEY_PASSWORD);
        verify(ftpsClientMock).setKeyManager(keyManagerMock);
        verify(keyStoreWrapperMock).load(inputStreamMock, TRUSTED_CA_PASSWORD.toCharArray());
        verify(inputStreamMock, times(1)).close();
        verify(trustManagerFactoryMock).init(keyStoreMock);
        verify(ftpsClientMock).setTrustManager(trustManagerMock);
        verify(ftpsClientMock).connect(XNF_ADDRESS, PORT);
        verify(ftpsClientMock).login(USERNAME, PASSWORD);
    }

    @Test
    public void collectFileBadRequestResponse_shouldFail() throws Exception {
        when(keyManagerUtilsMock.getClientKeyManager()).thenReturn(keyManagerMock);
        when(keyStoreWrapperMock.getKeyStore()).thenReturn(keyStoreMock);
        when(trustManagerFactoryMock.getTrustManagers()).thenReturn(new TrustManager[] {trustManagerMock});
        when(ftpsClientMock.login(USERNAME, PASSWORD)).thenReturn(true);
        when(ftpsClientMock.getReplyCode()).thenReturn(FTPReply.BAD_COMMAND_SEQUENCE);

        assertThatThrownBy(() -> clientUnderTestSpy.collectFile(REMOTE_FILE_PATH, LOCAL_FILE_PATH))
                .hasMessage("Unable to connect to xNF. 127.0.0.1 xNF reply code: 503");

        verify(ftpsClientMock).setNeedClientAuth(true);
        verify(keyManagerUtilsMock).setCredentials(FTP_KEY_PATH, FTP_KEY_PASSWORD);
        verify(ftpsClientMock).setKeyManager(keyManagerMock);
        verify(keyStoreWrapperMock).load(inputStreamMock, TRUSTED_CA_PASSWORD.toCharArray());
        verify(inputStreamMock, times(1)).close();
        verify(trustManagerFactoryMock).init(keyStoreMock);
        verify(ftpsClientMock).setTrustManager(trustManagerMock);
        verify(ftpsClientMock).connect(XNF_ADDRESS, PORT);
        verify(ftpsClientMock).login(USERNAME, PASSWORD);
        verify(ftpsClientMock, times(2)).getReplyCode();
        verify(ftpsClientMock, times(2)).isConnected();
        verifyNoMoreInteractions(ftpsClientMock);
    }

    @Test
    public void collectFileFaultyConnection_shouldFail() throws Exception {
        when(keyManagerUtilsMock.getClientKeyManager()).thenReturn(keyManagerMock);
        when(keyStoreWrapperMock.getKeyStore()).thenReturn(keyStoreMock);
        when(trustManagerFactoryMock.getTrustManagers()).thenReturn(new TrustManager[] {trustManagerMock});

        doThrow(new IOException()).when(ftpsClientMock).connect(XNF_ADDRESS, PORT);

        assertThatThrownBy(() -> clientUnderTestSpy.collectFile(REMOTE_FILE_PATH, LOCAL_FILE_PATH))
                .hasMessage("Could not open connection: java.io.IOException");

        verify(ftpsClientMock).setNeedClientAuth(true);
        verify(keyManagerUtilsMock).setCredentials(FTP_KEY_PATH, FTP_KEY_PASSWORD);
        verify(ftpsClientMock).setKeyManager(keyManagerMock);
        verify(keyStoreWrapperMock).load(inputStreamMock, TRUSTED_CA_PASSWORD.toCharArray());
        verify(inputStreamMock, times(1)).close();
        verify(trustManagerFactoryMock).init(keyStoreMock);
        verify(ftpsClientMock).setTrustManager(trustManagerMock);
        verify(ftpsClientMock).connect(XNF_ADDRESS, PORT);
        verify(ftpsClientMock, times(2)).isConnected();
        verifyNoMoreInteractions(ftpsClientMock);
    }

    @Test
    public void collectFileFailingFileCollect_shouldFail() throws Exception {
        when(keyManagerUtilsMock.getClientKeyManager()).thenReturn(keyManagerMock);
        when(keyStoreWrapperMock.getKeyStore()).thenReturn(keyStoreMock);
        when(trustManagerFactoryMock.getTrustManagers()).thenReturn(new TrustManager[] {trustManagerMock});
        when(ftpsClientMock.login(USERNAME, PASSWORD)).thenReturn(true);
        when(ftpsClientMock.getReplyCode()).thenReturn(HttpStatus.OK.value());

        doThrow(new IOException()).when(clientUnderTestSpy).createOutputStream(LOCAL_FILE_PATH);
        assertThatThrownBy(() -> clientUnderTestSpy.collectFile(REMOTE_FILE_PATH, LOCAL_FILE_PATH))
                .hasMessage("Could not open connection: java.io.IOException");
    }

    @Test
    public void collectFileFailingFileRetrieve_shouldFail() throws Exception {
        when(keyManagerUtilsMock.getClientKeyManager()).thenReturn(keyManagerMock);
        when(keyStoreWrapperMock.getKeyStore()).thenReturn(keyStoreMock);
        when(trustManagerFactoryMock.getTrustManagers()).thenReturn(new TrustManager[] {trustManagerMock});
        when(ftpsClientMock.login(anyString(), anyString())).thenReturn(true);
        when(ftpsClientMock.getReplyCode()).thenReturn(HttpStatus.OK.value());

        doThrow(new IOException("problemas")).when(ftpsClientMock).retrieveFile(anyString(), any(OutputStream.class));

        assertThatThrownBy(() -> clientUnderTestSpy.collectFile(REMOTE_FILE_PATH, LOCAL_FILE_PATH))
                .hasMessage("Could not open connection: java.io.IOException: problemas");

        verify(ftpsClientMock).setNeedClientAuth(true);
        verify(keyManagerUtilsMock).setCredentials(FTP_KEY_PATH, FTP_KEY_PASSWORD);
        verify(ftpsClientMock).setKeyManager(keyManagerMock);
        verify(keyStoreWrapperMock).load(inputStreamMock, TRUSTED_CA_PASSWORD.toCharArray());
        verify(inputStreamMock, times(1)).close();
        verify(trustManagerFactoryMock).init(keyStoreMock);
        verify(ftpsClientMock).setTrustManager(trustManagerMock);
        verify(ftpsClientMock).connect(XNF_ADDRESS, PORT);
        verify(ftpsClientMock).login(USERNAME, PASSWORD);
        verify(ftpsClientMock).getReplyCode();
        verify(ftpsClientMock, times(1)).enterLocalPassiveMode();
        verify(ftpsClientMock).execPBSZ(0);
        verify(ftpsClientMock).execPROT("P");
        verify(ftpsClientMock).setFileType(FTP.BINARY_FILE_TYPE);
        verify(ftpsClientMock).setBufferSize(1024 * 1024);
        verify(ftpsClientMock).retrieveFile(REMOTE_FILE_PATH, outputStreamMock);
        verify(ftpsClientMock, times(2)).isConnected();
        verifyNoMoreInteractions(ftpsClientMock);
    }
}
