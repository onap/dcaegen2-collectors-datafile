/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2020 Nokia. All rights reserved.
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

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.commons.FileCollectClient;
import org.onap.dcaegen2.collectors.datafile.commons.FileServerData;
import org.onap.dcaegen2.collectors.datafile.service.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.resources.ConnectionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public class DfcHttpClient implements FileCollectClient {

  private static final Logger logger = LoggerFactory.getLogger(DfcHttpClient.class);
  private static final ConnectionProvider pool = ConnectionProvider.create("default", 1);

  private final FileServerData fileServerData;
  private Disposable disposableClient;

  protected HttpClient client;

  public DfcHttpClient(FileServerData fileServerData) {
    this.client = HttpClient.create(pool).keepAlive(true);
    this.fileServerData = fileServerData;
  }

  @Override public void open() {
    logger.trace("Setting httpClient for file download.");
    client = HttpClient.create(pool).headers(
            h -> h.add("Authorization", HttpUtils.basicAuth(this.fileServerData.userId(), this.fileServerData.password())));
    logger.trace("httpClient, auth header was set.");
  }

  @Override public void collectFile(String remoteFile, Path localFile) throws DatafileTaskException {
    logger.trace("Prepare to collectFile {}.", localFile);
    CountDownLatch latch = new CountDownLatch(1);

    final HttpClientResponse[] clientResponse = new HttpClientResponse[1];
    final String[] errorMessages = new String[1];
    errorMessages[0] = "";

    Consumer<Throwable> onError = processFailedConnectionWithServer(latch, errorMessages);
    Consumer<InputStream> onSuccess = processDataFromServer(localFile, latch, clientResponse, errorMessages);

    Flux<InputStream> responseContent = getServerResponse(remoteFile, clientResponse);
    disposableClient = responseContent.subscribe(onSuccess, onError);

    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new DatafileTaskException("Interrupted exception after datafile download - " + e.toString());
    }

    if (isDownloadFailed(errorMessages[0])) {
      throw new DatafileTaskException("Error occured during datafile download:\n" + errorMessages[0]);
    }

    logger.trace("HTTP collectFile OK");
  }

  protected boolean isDownloadFailed(String errorMessage) {
    return (errorMessage != null) && (!errorMessage.isEmpty());
  }

  @NotNull protected Consumer<Throwable> processFailedConnectionWithServer(CountDownLatch latch, String[] errorMessages) {
    return (Throwable response) -> {
      logger.trace("Error in connection has occurred during file download - {}", response.getMessage());
      errorMessages[0] = response.toString();
      latch.countDown();
    };
  }

  @NotNull protected Consumer<InputStream> processDataFromServer(Path localFile, CountDownLatch latch,
          HttpClientResponse[] clientResponse, String[] errorMessages) {
    return (InputStream response) -> {
      logger.trace("Starting to process response.");
      try {
        if (isResponseOk(clientResponse[0])) {
          long numBytes = Files.copy(response, localFile);
          logger.trace("Transmission was successful - {} bytes downloaded.", numBytes);
          logger.trace("CollectFile fetched: {}.", localFile.toString());
        } else {
          logger.error("Unexpected HTTP response code. Writing to file aborted.");
          StringWriter writer = new StringWriter();
          String encoding = StandardCharsets.UTF_8.name();
          IOUtils.copy(response, writer, encoding);
          errorMessages[0] = writer.toString();
        }
        response.close();
      } catch (IOException e) {
        errorMessages[0] = "Error fetching file with  - " + e + ".";
        logger.error("Error fetching file with  - {}.", e);
      } finally {
        latch.countDown();
      }
    };
  }

  @NotNull protected Flux<InputStream> getServerResponse(String remoteFile, HttpClientResponse[] clientResponse) {
    return client.get()
            .uri(prepareUri(remoteFile))
            .response((responseReceiver, byteBufFlux) -> {
              clientResponse[0] = responseReceiver;
              logger.trace("HTTP response status - {}", responseReceiver.status());
              return byteBufFlux.aggregate().asInputStream();
            });
  }

  protected boolean isResponseOk(HttpClientResponse httpClientResponse) {
    return httpClientResponse.status().code() == 200;
  }

  @NotNull protected String prepareUri(String remoteFile) {
    String uri;
    if (fileServerData.port().isPresent()) {
      uri = "http://" + fileServerData.serverAddress() + ":" + fileServerData.port().get() + remoteFile;
    } else {
      uri = "http://" + fileServerData.serverAddress() + ":" + HttpUtils.HTTP_DEFAULT_PORT + remoteFile;
      logger.trace("Server port not present using default http port - {}.", HttpUtils.HTTP_DEFAULT_PORT);
    }
    return uri;
  }

  @Override public void close() {
    logger.trace("Starting http client to dispose");
    disposableClient.dispose();
    logger.trace("HTTP client disposed");
  }
}
