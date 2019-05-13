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

package org.onap.dcaegen2.collectors.datafile.tasks;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.configuration.FtpesConfig;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.ftp.FileCollectClient;
import org.onap.dcaegen2.collectors.datafile.ftp.FtpsClient;
import org.onap.dcaegen2.collectors.datafile.ftp.SftpClient;
import org.onap.dcaegen2.collectors.datafile.model.FileData;
import org.onap.dcaegen2.collectors.datafile.model.FilePublishInformation;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFilePublishInformation;
import org.onap.dcaegen2.collectors.datafile.model.MessageMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import reactor.core.publisher.Mono;

/**
 * Collects a file from a PNF.
 *
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class FileCollector {

    private static final Logger logger = LoggerFactory.getLogger(FileCollector.class);
    private final AppConfig datafileAppConfig;

    /**
     * Constructor.
     *
     * @param datafileAppConfig application configuration
     */
    public FileCollector(AppConfig datafileAppConfig) {
        this.datafileAppConfig = datafileAppConfig;
    }

    /**
     * Collects a file from the PNF and stores it in the local file system.
     *
     * @param fileData data about the file to collect.
     * @param numRetries the number of retries if the publishing fails
     * @param firstBackoff the time to delay the first retry
     * @param contextMap context for logging.
     *
     * @return the data needed to publish the file.
     */
    public Mono<FilePublishInformation> collectFile(FileData fileData, long numRetries, Duration firstBackoff,
            Map<String, String> contextMap) {
        MDC.setContextMap(contextMap);
        logger.trace("Entering collectFile with {}", fileData);

        return Mono.just(fileData) //
                .cache() //
                .flatMap(fd -> collectFile(fileData, contextMap)) //
                .retryBackoff(numRetries, firstBackoff);
    }

    private Mono<FilePublishInformation> collectFile(FileData fileData, Map<String, String> context) {
        MDC.setContextMap(context);
        logger.trace("starting to collectFile {}", fileData.name());

        final String remoteFile = fileData.remoteFilePath();
        final Path localFile = fileData.getLocalFilePath();

        try (FileCollectClient currentClient = createClient(fileData)) {
            currentClient.open();
            localFile.getParent().toFile().mkdir(); // Create parent directories
            currentClient.collectFile(remoteFile, localFile);
            return Mono.just(getFilePublishInformation(fileData, localFile, context));
        } catch (Exception throwable) {
            logger.warn("Failed to download file: {} {}, reason: {}", fileData.sourceName(), fileData.name(),
                    throwable.toString());
            return Mono.error(throwable);
        }
    }

    private FileCollectClient createClient(FileData fileData) throws DatafileTaskException {
        switch (fileData.scheme()) {
            case SFTP:
                return createSftpClient(fileData);
            case FTPS:
                return createFtpsClient(fileData);
            default:
                throw new DatafileTaskException("Unhandeled protocol: " + fileData.scheme());
        }
    }

    private FilePublishInformation getFilePublishInformation(FileData fileData, Path localFile,Map<String, String> context) {
        String location = fileData.location();
        MessageMetaData metaData = fileData.messageMetaData();
        return ImmutableFilePublishInformation.builder() //
                .productName(metaData.productName()) //
                .vendorName(metaData.vendorName()) //
                .lastEpochMicrosec(metaData.lastEpochMicrosec()) //
                .sourceName(metaData.sourceName()) //
                .startEpochMicrosec(metaData.startEpochMicrosec()) //
                .timeZoneOffset(metaData.timeZoneOffset()) //
                .name(fileData.name()) //
                .location(location) //
                .internalLocation(localFile) //
                .compression(fileData.compression()) //
                .fileFormatType(fileData.fileFormatType()) //
                .fileFormatVersion(fileData.fileFormatVersion()) //
                .context(context) //
                .build();
    }

    protected SftpClient createSftpClient(FileData fileData) {
        return new SftpClient(fileData.fileServerData());
    }

    protected FtpsClient createFtpsClient(FileData fileData) {
        FtpesConfig config = datafileAppConfig.getFtpesConfiguration();
        return new FtpsClient(fileData.fileServerData(), config.keyCert(), config.keyPassword(),
                Paths.get(config.trustedCa()), config.trustedCaPassword());
    }
}