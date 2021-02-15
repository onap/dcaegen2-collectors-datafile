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

import org.jetbrains.annotations.NotNull;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.commons.FileCollectClient;
import org.onap.dcaegen2.collectors.datafile.commons.FileServerData;
import org.onap.dcaegen2.collectors.datafile.exceptions.NonRetryableDatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.service.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.resources.ConnectionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Gets file from PNF with HTTP protocol.
 *
 * @author <a href="mailto:krzysztof.gajewski@nokia.com">Krzysztof Gajewski</a>
 */
public class DfcHttpClient implements FileCollectClient {

    //Be aware to be less than ScheduledTasks.NUMBER_OF_WORKER_THREADS
    private static final int MAX_NUMBER_OF_CONNECTIONS = 200;
    private static final Logger logger = LoggerFactory.getLogger(DfcHttpClient.class);
    private static final ConnectionProvider pool = ConnectionProvider.create("default", MAX_NUMBER_OF_CONNECTIONS);

    private final FileServerData fileServerData;
    private Disposable disposableClient;

    protected HttpClient client;

    public DfcHttpClient(FileServerData fileServerData) {
        this.fileServerData = fileServerData;
    }

    @Override public void open() throws DatafileTaskException {
        logger.trace("Setting httpClient for file download.");

        String authorizationContent = getAuthorizationContent();
        this.client = HttpClient.create(pool).keepAlive(true).headers(
            h -> h.add("Authorization", authorizationContent));

        logger.trace("httpClient, auth header was set.");
    }

    protected String getAuthorizationContent() throws DatafileTaskException {
        String jwtToken = HttpUtils.getJWTToken(fileServerData);
        if (!jwtToken.isEmpty()) {
            return HttpUtils.jwtAuthContent(jwtToken);
        }
        if (!HttpUtils.isBasicAuthDataFilled(fileServerData)) {
            throw new DatafileTaskException("Not sufficient basic auth data for file.");
        }
        return HttpUtils.basicAuthContent(this.fileServerData.userId(), this.fileServerData.password());
    }

    @Override public void collectFile(String remoteFile, Path localFile) throws DatafileTaskException {
        logger.trace("Prepare to collectFile {}", localFile);
        CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Exception> errorMessage = new AtomicReference<>();

        Consumer<Throwable> onError = processFailedConnectionWithServer(latch, errorMessage);
        Consumer<InputStream> onSuccess = processDataFromServer(localFile, latch, errorMessage);

        Flux<InputStream> responseContent = getServerResponse(remoteFile);
        disposableClient = responseContent.subscribe(onSuccess, onError);

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new DatafileTaskException("Interrupted exception after datafile download - ", e);
        }

        if (isDownloadFailed(errorMessage)) {
            if (errorMessage.get() instanceof NonRetryableDatafileTaskException) {
                throw (NonRetryableDatafileTaskException) errorMessage.get();
            }
            throw (DatafileTaskException) errorMessage.get();
        }

        logger.trace("HTTP collectFile OK");
    }

    protected boolean isDownloadFailed(AtomicReference<Exception> errorMessage) {
        return (errorMessage.get() != null);
    }

    @NotNull protected Consumer<Throwable> processFailedConnectionWithServer(CountDownLatch latch, AtomicReference<Exception> errorMessages) {
        return (Throwable response) -> {
            Exception e = new Exception("Error in connection has occurred during file download", response);
            errorMessages.set(new DatafileTaskException(response.getMessage(), e));
            if (response instanceof NonRetryableDatafileTaskException) {
                errorMessages.set(new NonRetryableDatafileTaskException(response.getMessage(), e));
            }
            latch.countDown();
        };
    }

    @NotNull protected Consumer<InputStream> processDataFromServer(Path localFile, CountDownLatch latch,
            AtomicReference<Exception> errorMessages) {
        return (InputStream response) -> {
            logger.trace("Starting to process response.");
            try {
                long numBytes = Files.copy(response, localFile);
                logger.trace("Transmission was successful - {} bytes downloaded.", numBytes);
                logger.trace("CollectFile fetched: {}", localFile);
                response.close();
            } catch (IOException e) {
                errorMessages.set(new DatafileTaskException("Error fetching file with", e));
            } finally {
                latch.countDown();
            }
        };
    }

    protected Flux<InputStream> getServerResponse(String remoteFile) {
        return client.get()
            .uri(HttpUtils.prepareHttpUri(fileServerData, remoteFile))
            .response((responseReceiver, byteBufFlux) -> {
                logger.trace("HTTP response status - {}", responseReceiver.status());
                if(isResponseOk(responseReceiver)){
                    return byteBufFlux.aggregate().asInputStream();
                }
                if (isErrorInConnection(responseReceiver)) {
                    return Mono.error(new NonRetryableDatafileTaskException(
                        HttpUtils.nonRetryableResponse(getResponseCode(responseReceiver))));
                }
                return Mono.error(new DatafileTaskException(
                    HttpUtils.retryableResponse(getResponseCode(responseReceiver))));
            });
    }

    protected boolean isResponseOk(HttpClientResponse httpClientResponse) {
        return getResponseCode(httpClientResponse) == 200;
    }

    private int getResponseCode(HttpClientResponse responseReceiver) {
        return responseReceiver.status().code();
    }

    protected boolean isErrorInConnection(HttpClientResponse httpClientResponse) {
        return getResponseCode(httpClientResponse) >= 400;
    }

    @Override public void close() {
        logger.trace("Starting http client disposal.");
        disposableClient.dispose();
        logger.trace("Http client disposed.");
    }
}
