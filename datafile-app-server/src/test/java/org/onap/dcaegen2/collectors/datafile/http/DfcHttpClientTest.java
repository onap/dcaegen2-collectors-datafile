/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2020-2021 Nokia. All rights reserved.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.dcaegen2.collectors.datafile.commons.ImmutableFileServerData;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.service.HttpUtils;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClientConfig;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DfcHttpClientTest {

    private static final String USERNAME = "bob";
    private static final String PASSWORD = "123";
    private static final String XNF_ADDRESS = "127.0.0.1";
    private static final int PORT = 80;

    @Mock
    private Path pathMock;

    DfcHttpClient dfcHttpClientSpy;

    @BeforeEach
    public void setup() {
        dfcHttpClientSpy = spy(new DfcHttpClient(createFileServerData()));
    }

    @Test
    void openConnection_successAuthSetup() throws DatafileTaskException {
        dfcHttpClientSpy.open();
        HttpClientConfig config = dfcHttpClientSpy.client.configuration();
        assertEquals(HttpUtils.basicAuth(USERNAME, PASSWORD), config.headers().get("Authorization"));
    }

    @Test
    void openConnection_failedBasicAuthSetupThrowException() {
        ImmutableFileServerData serverData = ImmutableFileServerData.builder()
            .serverAddress(XNF_ADDRESS)
            .userId(USERNAME).password("")
            .port(PORT)
            .build();

        DfcHttpClient dfcHttpClientSpy = spy(new DfcHttpClient(serverData));

        assertThatThrownBy(() -> dfcHttpClientSpy.open())
            .hasMessageContaining("Not sufficient basic auth data for file.");
    }

    @Test
    void prepareUri_UriWithoutPort() {
        ImmutableFileServerData serverData = ImmutableFileServerData.builder()
            .serverAddress(XNF_ADDRESS)
            .userId(USERNAME).password(PASSWORD)
            .build();
        DfcHttpClient clientNoPortSpy = spy(new DfcHttpClient(serverData));
        String REMOTE_FILE = "any";

        String retrievedUri = clientNoPortSpy.prepareUri(REMOTE_FILE);
        assertTrue(retrievedUri.startsWith("http://" + XNF_ADDRESS + ":80"));
    }

    @Test
    void collectFile_AllOk() throws Exception {
        String REMOTE_FILE = "any";
        Flux<InputStream> fis = Flux.just(new ByteArrayInputStream("ReturnedString".getBytes()));

        dfcHttpClientSpy.open();

        when(dfcHttpClientSpy.getServerResponse(any())).thenReturn(fis);
        doReturn(false).when(dfcHttpClientSpy).isDownloadFailed(any());

        dfcHttpClientSpy.collectFile(REMOTE_FILE, pathMock);
        dfcHttpClientSpy.close();

        verify(dfcHttpClientSpy, times(1)).getServerResponse(ArgumentMatchers.eq(REMOTE_FILE));
        verify(dfcHttpClientSpy, times(1)).processDataFromServer(any(), any(), any());
        verify(dfcHttpClientSpy, times(1)).isDownloadFailed(any());
    }

    @Test
    void collectFile_No200ResponseWriteToErrorMessage() throws DatafileTaskException {
        String ERROR_RESPONSE = "This is unexpected message";
        String REMOTE_FILE = "any";
        Flux<Throwable> fis = Flux.error(new Throwable(ERROR_RESPONSE));

        dfcHttpClientSpy.open();

        doReturn(fis).when(dfcHttpClientSpy).getServerResponse(any());

        assertThatThrownBy(() -> dfcHttpClientSpy.collectFile(REMOTE_FILE, pathMock))
            .hasMessageContaining("Error occured during datafile download: ");
        verify(dfcHttpClientSpy, times(1)).getServerResponse(REMOTE_FILE);
        verify(dfcHttpClientSpy, times(1)).processFailedConnectionWithServer(any(), any());
        dfcHttpClientSpy.close();
    }

    @Test
    void isResponseOk_validateResponse() {
        assertTrue(dfcHttpClientSpy.isResponseOk(HttpClientResponseHelper.NETTY_RESPONSE_OK));
        assertFalse(dfcHttpClientSpy.isResponseOk(HttpClientResponseHelper.RESPONSE_ANY_NO_OK));
    }

    private ImmutableFileServerData createFileServerData() {
        return ImmutableFileServerData.builder()
                .serverAddress(XNF_ADDRESS)
                .userId(USERNAME).password(PASSWORD)
                .port(PORT)
                .build();
    }
}
