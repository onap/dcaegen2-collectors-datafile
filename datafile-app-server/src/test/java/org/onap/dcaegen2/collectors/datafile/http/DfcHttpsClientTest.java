/*-
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
package org.onap.dcaegen2.collectors.datafile.http;


import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.dcaegen2.collectors.datafile.commons.ImmutableFileServerData;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.exceptions.NonRetryableDatafileTaskException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DfcHttpsClientTest {

    private static final String USERNAME = "bob";
    private static final String PASSWORD = "123";
    private static final String XNF_ADDRESS = "127.0.0.1";
    private static final int PORT = 443;
    private static String remoteFile = "remoteFile";

    @Mock
    private PoolingHttpClientConnectionManager connectionManager;
    @Mock
    private Path localFile;

    DfcHttpsClient dfcHttpsClientSpy;

    @BeforeEach
    public void setup() {
        dfcHttpsClientSpy = spy(new DfcHttpsClient(createFileServerData(), connectionManager));
    }

    private ImmutableFileServerData createFileServerData() {
        return ImmutableFileServerData.builder()
            .serverAddress(XNF_ADDRESS)
            .userId(USERNAME).password(PASSWORD)
            .port(PORT)
            .build();
    }

    private ImmutableFileServerData emptyUserInFileServerData() {
        return ImmutableFileServerData.builder()
            .serverAddress(XNF_ADDRESS)
            .userId("").password("")
            .port(PORT)
            .build();
    }

    private ImmutableFileServerData invalidUserInFileServerData() {
        return ImmutableFileServerData.builder()
            .serverAddress(XNF_ADDRESS)
            .userId("demo").password("")
            .port(PORT)
            .build();
    }

    @Test
    void fileServerData_properLocationBasicAuth() throws Exception {
        boolean result  = dfcHttpsClientSpy.basicAuthValidNotPresentOrThrow();
        assertEquals(true, result);
    }

    @Test
    void fileServerData_properLocationNoBasicAuth() throws Exception {
        dfcHttpsClientSpy = spy(new DfcHttpsClient(emptyUserInFileServerData(), connectionManager));

        boolean result  = dfcHttpsClientSpy.basicAuthValidNotPresentOrThrow();
        assertEquals(false, result);
    }

    @Test
    void fileServerData_improperAuthDataExceptionOccurred() throws Exception {
        dfcHttpsClientSpy = spy(new DfcHttpsClient(invalidUserInFileServerData(), connectionManager));

        assertThrows(DatafileTaskException.class, () -> dfcHttpsClientSpy.basicAuthValidNotPresentOrThrow());
    }

    @Test
    void DfcHttpsClient_flow_successfulCallAndResponseProcessing() throws Exception {
        doReturn(HttpClientResponseHelper.APACHE_RESPONSE).when(dfcHttpsClientSpy)
            .executeHttpClient(any(HttpGet.class));
        doReturn((long)3).when(dfcHttpsClientSpy).writeFile(eq(localFile), any(InputStream.class));

        dfcHttpsClientSpy.open();
        dfcHttpsClientSpy.collectFile(remoteFile, localFile);
        dfcHttpsClientSpy.close();

        verify(dfcHttpsClientSpy, times(1)).makeCall(any(HttpGet.class));
        verify(dfcHttpsClientSpy, times(1))
            .executeHttpClient(any(HttpGet.class));
        verify(dfcHttpsClientSpy, times(1))
            .processResponse(HttpClientResponseHelper.APACHE_RESPONSE, localFile);
        verify(dfcHttpsClientSpy, times(1))
            .writeFile(eq(localFile), any(InputStream.class));
    }

    @Test
    void DfcHttpsClient_flow_failedCallUnexpectedResponseCode() throws Exception {
        doReturn(HttpClientResponseHelper.APACHE_RESPONSE).when(dfcHttpsClientSpy)
            .executeHttpClient(any(HttpGet.class));
        doReturn(false).when(dfcHttpsClientSpy).isResponseOk(any(HttpResponse.class));

        dfcHttpsClientSpy.open();

        assertThrows(NonRetryableDatafileTaskException.class,
                () -> dfcHttpsClientSpy.collectFile(remoteFile, localFile));
    }

    @Test
    void DfcHttpsClient_flow_failedCallConnectionTimeout() throws Exception {
        doThrow(ConnectTimeoutException.class).when(dfcHttpsClientSpy)
            .executeHttpClient(any(HttpGet.class));

        dfcHttpsClientSpy.open();

        assertThrows(NonRetryableDatafileTaskException.class,
                () -> dfcHttpsClientSpy.collectFile(remoteFile, localFile));
    }

    @Test
    void DfcHttpsClient_flow_failedCallIOExceptionForExecuteHttpClient() throws Exception {
        doThrow(IOException.class).when(dfcHttpsClientSpy)
            .executeHttpClient(any(HttpGet.class));

        dfcHttpsClientSpy.open();

        assertThrows(DatafileTaskException.class,
                () -> dfcHttpsClientSpy.collectFile(remoteFile, localFile));
    }

}
