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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.io.IFile;
import org.onap.dcaegen2.collectors.datafile.io.IFileSystemResource;
import org.onap.dcaegen2.collectors.datafile.io.IOutputStream;
import org.onap.dcaegen2.collectors.datafile.ssl.IKeyManagerUtils;
import org.onap.dcaegen2.collectors.datafile.ssl.IKeyStore;
import org.onap.dcaegen2.collectors.datafile.ssl.ITrustManagerFactory;
import org.springframework.http.HttpStatus;

public class FtpsClientTest {

    private static final String REMOTE_FILE_PATH = "/dir/sample.txt";
    private static final String LOCAL_FILE_PATH = "target/sample.txt";
    private static final String XNF_ADDRESS = "127.0.0.1";
    private static final int PORT = 8021;
    private static final String FTP_KEY_PATH = "ftpKeyPath";
    private static final String FTP_KEY_PASSWORD = "ftpKeyPassword";
    private static final String TRUSTED_CA_PATH = "trustedCAPath";
    private static final String TRUSTED_CA_PASSWORD = "trustedCAPassword";

    private static final String USERNAME = "bob";
    private static final String PASSWORD = "123";

    private IFTPSClient ftpsClientMock = mock(IFTPSClient.class);
    private IKeyManagerUtils keyManagerUtilsMock = mock(IKeyManagerUtils.class);
    private KeyManager keyManagerMock = mock(KeyManager.class);
    private IKeyStore keyStoreWrapperMock = mock(IKeyStore.class);
    private KeyStore keyStoreMock = mock(KeyStore.class);
    private ITrustManagerFactory trustManagerFactoryMock = mock(ITrustManagerFactory.class);
    private TrustManager trustManagerMock = mock(TrustManager.class);
    private IFile localFileMock = mock(IFile.class);
    private IFileSystemResource fileResourceMock = mock(IFileSystemResource.class);
    private IOutputStream outputStreamMock = mock(IOutputStream.class);
    private InputStream inputStreamMock = mock(InputStream.class);

    FtpsClient clientUnderTest = new FtpsClient();

    @BeforeEach
    protected void setUp() throws Exception {
        clientUnderTest.setFtpsClient(ftpsClientMock);
        clientUnderTest.setKeyManagerUtils(keyManagerUtilsMock);
        clientUnderTest.setKeyStore(keyStoreWrapperMock);
        clientUnderTest.setTrustManagerFactory(trustManagerFactoryMock);
        clientUnderTest.setFile(localFileMock);
        clientUnderTest.setFileSystemResource(fileResourceMock);
        clientUnderTest.setOutputStream(outputStreamMock);

        clientUnderTest.setKeyCertPath(FTP_KEY_PATH);
        clientUnderTest.setKeyCertPassword(FTP_KEY_PASSWORD);
        clientUnderTest.setTrustedCAPath(TRUSTED_CA_PATH);
        clientUnderTest.setTrustedCAPassword(TRUSTED_CA_PASSWORD);
}

    @Test
    public void collectFile_allOk() throws Exception {
        when(keyManagerUtilsMock.getClientKeyManager()).thenReturn(keyManagerMock);
        when(fileResourceMock.getInputStream()).thenReturn(inputStreamMock);
        when(keyStoreWrapperMock.getKeyStore()).thenReturn(keyStoreMock);
        when(trustManagerFactoryMock.getTrustManagers()).thenReturn(new TrustManager[] {trustManagerMock});
        when(ftpsClientMock.login(USERNAME, PASSWORD)).thenReturn(true);
        when(ftpsClientMock.getReplyCode()).thenReturn(HttpStatus.OK.value());
        File fileMock = mock(File.class);
        when(localFileMock.getFile()).thenReturn(fileMock);
        OutputStream osMock = mock(OutputStream.class);
        when(outputStreamMock.getOutputStream(fileMock)).thenReturn(osMock);
        when(ftpsClientMock.retrieveFile(REMOTE_FILE_PATH, osMock)).thenReturn(true);

        ImmutableFileServerData fileServerData = ImmutableFileServerData.builder().serverAddress(XNF_ADDRESS)
                .userId(USERNAME).password(PASSWORD).port(PORT).build();

        FileCollectResult result = clientUnderTest.collectFile(fileServerData, REMOTE_FILE_PATH, LOCAL_FILE_PATH);

        assertTrue(result.downloadSuccessful());
        verify(ftpsClientMock).setNeedClientAuth(true);
        verify(keyManagerUtilsMock).setCredentials(FTP_KEY_PATH, FTP_KEY_PASSWORD);
        verify(ftpsClientMock).setKeyManager(keyManagerMock);
        verify(fileResourceMock).setPath(TRUSTED_CA_PATH);
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
        verify(localFileMock).setPath(LOCAL_FILE_PATH);
        verify(localFileMock, times(1)).createNewFile();
        verify(ftpsClientMock).retrieveFile(REMOTE_FILE_PATH, osMock);
        verify(osMock, times(1)).close();
        verify(ftpsClientMock, times(1)).logout();
        verify(ftpsClientMock, times(1)).disconnect();
        verifyNoMoreInteractions(ftpsClientMock);
    }

    @Test
    public void collectFileFaultyOwnKey_shouldFail() throws Exception {
        doThrow(new GeneralSecurityException())
                .when(keyManagerUtilsMock).setCredentials(FTP_KEY_PATH, FTP_KEY_PASSWORD);

        ImmutableFileServerData fileServerData = ImmutableFileServerData.builder().serverAddress(XNF_ADDRESS)
                .userId(USERNAME).password(PASSWORD).port(PORT).build();

        FileCollectResult result = clientUnderTest.collectFile(fileServerData, REMOTE_FILE_PATH, LOCAL_FILE_PATH);

        assertFalse(result.downloadSuccessful());
    }

    @Test
    public void collectFileFaultTrustedCA_shouldFail() throws Exception {
        when(keyManagerUtilsMock.getClientKeyManager()).thenReturn(keyManagerMock);
        when(fileResourceMock.getInputStream()).thenReturn(inputStreamMock);
        when(keyStoreWrapperMock.getKeyStore()).thenReturn(keyStoreMock);

        doThrow(new KeyStoreException()).when(trustManagerFactoryMock).init(keyStoreMock);

        ImmutableFileServerData fileServerData = ImmutableFileServerData.builder().serverAddress(XNF_ADDRESS)
                .userId(USERNAME).password(PASSWORD).port(PORT).build();

        FileCollectResult result = clientUnderTest.collectFile(fileServerData, REMOTE_FILE_PATH, LOCAL_FILE_PATH);

        assertFalse(result.downloadSuccessful());
    }

    @Test
    public void collectFileFaultyLogin_shouldFail() throws Exception {
        when(keyManagerUtilsMock.getClientKeyManager()).thenReturn(keyManagerMock);
        when(fileResourceMock.getInputStream()).thenReturn(inputStreamMock);
        when(keyStoreWrapperMock.getKeyStore()).thenReturn(keyStoreMock);
        when(trustManagerFactoryMock.getTrustManagers()).thenReturn(new TrustManager[] {trustManagerMock});
        when(ftpsClientMock.login(USERNAME, PASSWORD)).thenReturn(false);

        ImmutableFileServerData fileServerData = ImmutableFileServerData.builder().serverAddress(XNF_ADDRESS)
                .userId(USERNAME).password(PASSWORD).port(PORT).build();

        FileCollectResult result = clientUnderTest.collectFile(fileServerData, REMOTE_FILE_PATH, LOCAL_FILE_PATH);

        verify(ftpsClientMock, times(1)).logout();
        assertFalse(result.downloadSuccessful());
    }

    @Test
    public void collectFileBadRequestResponse_shouldFail() throws Exception {
        when(keyManagerUtilsMock.getClientKeyManager()).thenReturn(keyManagerMock);
        when(fileResourceMock.getInputStream()).thenReturn(inputStreamMock);
        when(keyStoreWrapperMock.getKeyStore()).thenReturn(keyStoreMock);
        when(trustManagerFactoryMock.getTrustManagers()).thenReturn(new TrustManager[] {trustManagerMock});
        when(ftpsClientMock.login(USERNAME, PASSWORD)).thenReturn(true);
        when(ftpsClientMock.getReplyCode()).thenReturn(HttpStatus.BAD_REQUEST.value());

        ImmutableFileServerData fileServerData = ImmutableFileServerData.builder().serverAddress(XNF_ADDRESS)
                .userId(USERNAME).password(PASSWORD).port(PORT).build();

        FileCollectResult result = clientUnderTest.collectFile(fileServerData, REMOTE_FILE_PATH, LOCAL_FILE_PATH);

        verify(ftpsClientMock, times(1)).disconnect();
        assertFalse(result.downloadSuccessful());
    }

    @Test
    public void collectFileFaultyConnection_shouldFail() throws Exception {
        when(keyManagerUtilsMock.getClientKeyManager()).thenReturn(keyManagerMock);
        when(fileResourceMock.getInputStream()).thenReturn(inputStreamMock);
        when(keyStoreWrapperMock.getKeyStore()).thenReturn(keyStoreMock);
        when(trustManagerFactoryMock.getTrustManagers()).thenReturn(new TrustManager[] {trustManagerMock});

        doThrow(new IOException()).when(ftpsClientMock).connect(XNF_ADDRESS, PORT);

        ImmutableFileServerData fileServerData = ImmutableFileServerData.builder().serverAddress(XNF_ADDRESS)
                .userId(USERNAME).password(PASSWORD).port(PORT).build();

        FileCollectResult result = clientUnderTest.collectFile(fileServerData, REMOTE_FILE_PATH, LOCAL_FILE_PATH);

        assertFalse(result.downloadSuccessful());
    }

    @Test
    public void collectFileFailingFileCollect_shouldFail() throws Exception {
        when(keyManagerUtilsMock.getClientKeyManager()).thenReturn(keyManagerMock);
        when(fileResourceMock.getInputStream()).thenReturn(inputStreamMock);
        when(keyStoreWrapperMock.getKeyStore()).thenReturn(keyStoreMock);
        when(trustManagerFactoryMock.getTrustManagers()).thenReturn(new TrustManager[] {trustManagerMock});
        when(ftpsClientMock.login(USERNAME, PASSWORD)).thenReturn(true);
        when(ftpsClientMock.getReplyCode()).thenReturn(HttpStatus.OK.value());

        doThrow(new IOException()).when(localFileMock).createNewFile();

        ImmutableFileServerData fileServerData = ImmutableFileServerData.builder().serverAddress(XNF_ADDRESS)
                .userId(USERNAME).password(PASSWORD).port(PORT).build();

        FileCollectResult result = clientUnderTest.collectFile(fileServerData, REMOTE_FILE_PATH, LOCAL_FILE_PATH);

        assertFalse(result.downloadSuccessful());
        verify(localFileMock, times(1)).delete();
    }

    @Test
    public void collectFileFailingFileRetrieve_shouldFail() throws Exception {
        when(keyManagerUtilsMock.getClientKeyManager()).thenReturn(keyManagerMock);
        when(fileResourceMock.getInputStream()).thenReturn(inputStreamMock);
        when(keyStoreWrapperMock.getKeyStore()).thenReturn(keyStoreMock);
        when(trustManagerFactoryMock.getTrustManagers()).thenReturn(new TrustManager[] {trustManagerMock});
        when(ftpsClientMock.login(USERNAME, PASSWORD)).thenReturn(true);
        when(ftpsClientMock.getReplyCode()).thenReturn(HttpStatus.OK.value());
        File fileMock = mock(File.class);
        when(localFileMock.getFile()).thenReturn(fileMock);
        OutputStream osMock = mock(OutputStream.class);
        when(outputStreamMock.getOutputStream(fileMock)).thenReturn(osMock);
        when(ftpsClientMock.retrieveFile(REMOTE_FILE_PATH, osMock)).thenReturn(false);

        ImmutableFileServerData fileServerData = ImmutableFileServerData.builder().serverAddress(XNF_ADDRESS)
                .userId(USERNAME).password(PASSWORD).port(PORT).build();

        FileCollectResult result = clientUnderTest.collectFile(fileServerData, REMOTE_FILE_PATH, LOCAL_FILE_PATH);

        assertFalse(result.downloadSuccessful());
    }
}