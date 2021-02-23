/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2021 Nokia. All rights reserved.
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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.onap.dcaegen2.collectors.datafile.commons.FileCollectClient;
import org.onap.dcaegen2.collectors.datafile.commons.FileServerData;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.exceptions.NonRetryableDatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.service.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Gets file from PNF with HTTPS protocol.
 *
 * @author <a href="mailto:krzysztof.gajewski@nokia.com">Krzysztof Gajewski</a>
 */
public class DfcHttpsClient implements FileCollectClient {

    protected CloseableHttpClient httpsClient;

    private static final Logger logger = LoggerFactory.getLogger(DfcHttpsClient.class);
    private static final int FIFTEEN_SECONDS = 15 * 1000;

    private final FileServerData fileServerData;
    private final PoolingHttpClientConnectionManager connectionManager;

    public DfcHttpsClient(FileServerData fileServerData, PoolingHttpClientConnectionManager  connectionManager) {
        this.fileServerData = fileServerData;
        this.connectionManager = connectionManager;
    }

    @Override public void open() {
        logger.trace("Setting httpsClient for file download.");
        SocketConfig socketConfig = SocketConfig.custom()
            .setSoKeepAlive(true)
            .build();

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(FIFTEEN_SECONDS)
            .build();

        httpsClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultSocketConfig(socketConfig)
            .setDefaultRequestConfig(requestConfig)
            .build();

        logger.trace("httpsClient prepared for connection.");
    }

    @Override public void collectFile(String remoteFile, Path localFile) throws DatafileTaskException {
        logger.trace("Prepare to collectFile {}", localFile);
        HttpGet httpGet = new HttpGet(HttpUtils.prepareHttpsUri(fileServerData, remoteFile));

        String authorizationContent = getAuthorizationContent();
        if (!authorizationContent.isEmpty()) {
            httpGet.addHeader("Authorization", authorizationContent);
        }
        try {
            HttpResponse httpResponse = makeCall(httpGet);
            processResponse(httpResponse, localFile);
        } catch (IOException e) {
            throw new DatafileTaskException("Error downloading file from server. ", e);
        }
        logger.trace("HTTPS collectFile OK");
    }

    private String getAuthorizationContent() throws DatafileTaskException {
        String jwtToken = HttpUtils.getJWTToken(fileServerData);
        if (shouldUseBasicAuth(jwtToken)) {
            return HttpUtils.basicAuthContent(this.fileServerData.userId(), this.fileServerData.password());
        }
        return HttpUtils.jwtAuthContent(jwtToken);
    }

    private boolean shouldUseBasicAuth(String jwtToken) throws DatafileTaskException {
        return basicAuthValidNotPresentOrThrow() && jwtToken.isEmpty();
    }

    protected boolean basicAuthValidNotPresentOrThrow() throws DatafileTaskException {
        if (isAuthDataEmpty()) {
            return false;
        }
        if (HttpUtils.isBasicAuthDataFilled(fileServerData)) {
            return true;
        }
        throw new DatafileTaskException("Not sufficient basic auth data for file.");
    }

    private boolean isAuthDataEmpty() {
        return this.fileServerData.userId().isEmpty() && this.fileServerData.password().isEmpty();
    }

    protected HttpResponse makeCall(HttpGet httpGet)
            throws IOException, DatafileTaskException {
        try {
            HttpResponse httpResponse = executeHttpClient(httpGet);
            if (isResponseOk(httpResponse)) {
                return httpResponse;
            }

            EntityUtils.consume(httpResponse.getEntity());
            if (isErrorInConnection(httpResponse)) {
                throw new NonRetryableDatafileTaskException(HttpUtils.retryableResponse(getResponseCode(httpResponse)));
            }
            throw new DatafileTaskException(HttpUtils.nonRetryableResponse(getResponseCode(httpResponse)));
        } catch (ConnectTimeoutException | UnknownHostException | HttpHostConnectException | SSLHandshakeException e) {
            throw new NonRetryableDatafileTaskException(
                    "Unable to get file from xNF. No retry attempts will be done.", e);
        }
    }

    protected CloseableHttpResponse executeHttpClient(HttpGet httpGet)
            throws IOException {
        return httpsClient.execute(httpGet);
    }

    protected boolean isResponseOk(HttpResponse httpResponse) {
        return getResponseCode(httpResponse) == 200;
    }

    private int getResponseCode(HttpResponse httpResponse) {
        return httpResponse.getStatusLine().getStatusCode();
    }

    protected boolean isErrorInConnection(HttpResponse httpResponse) {
        return getResponseCode(httpResponse) >= 400;
    }

    protected void processResponse(HttpResponse response, Path localFile) throws IOException {
        logger.trace("Starting to process response.");
        HttpEntity entity = response.getEntity();
        InputStream stream = entity.getContent();
        long numBytes = writeFile(localFile, stream);
        stream.close();
        EntityUtils.consume(entity);
        logger.trace("Transmission was successful - {} bytes downloaded.", numBytes);
    }

    protected long writeFile(Path localFile, InputStream stream) throws IOException {
        return Files.copy(stream, localFile, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override public void close() {
        logger.trace("Https client has ended downloading process.");
    }
}
