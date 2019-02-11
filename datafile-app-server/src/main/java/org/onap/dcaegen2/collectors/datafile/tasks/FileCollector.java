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

package org.onap.dcaegen2.collectors.datafile.tasks;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.configuration.FtpesConfig;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.ftp.FileCollectClient;
import org.onap.dcaegen2.collectors.datafile.ftp.FtpsClient;
import org.onap.dcaegen2.collectors.datafile.ftp.SftpClient;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.FileData;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.MessageMetaData;
import org.onap.dcaegen2.collectors.datafile.model.logging.MdcVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class FileCollector {

    private static final Logger logger = LoggerFactory.getLogger(FileCollector.class);
    private AppConfig datafileAppConfig;
    private final FtpsClient ftpsClient;
    private final SftpClient sftpClient;


    public FileCollector(AppConfig datafileAppConfig, FtpsClient ftpsClient, SftpClient sftpClient) {
        this.datafileAppConfig = datafileAppConfig;
        this.ftpsClient = ftpsClient;
        this.sftpClient = sftpClient;
    }

    public Mono<ConsumerDmaapModel> execute(FileData fileData, MessageMetaData metaData, long maxNumberOfRetries,
            Duration firstBackoffTimeout, Map<String, String> contextMap) {
        MdcVariables.setMdcContextMap(contextMap);
        logger.trace("Entering execute with {}", fileData);
        resolveKeyStore();

        return Mono.just(fileData) //
                .cache() //
                .flatMap(fd -> collectFile(fileData, metaData, contextMap)) //
                .retryBackoff(maxNumberOfRetries, firstBackoffTimeout);
    }

    private FtpesConfig resolveConfiguration() {
        return datafileAppConfig.getFtpesConfiguration();
    }

    private void resolveKeyStore() {
        FtpesConfig ftpesConfig = resolveConfiguration();
        ftpsClient.setKeyCertPath(ftpesConfig.keyCert());
        ftpsClient.setKeyCertPassword(ftpesConfig.keyPassword());
        ftpsClient.setTrustedCAPath(ftpesConfig.trustedCA());
        ftpsClient.setTrustedCAPassword(ftpesConfig.trustedCAPassword());
    }

    private Mono<ConsumerDmaapModel> collectFile(FileData fileData, MessageMetaData metaData,
            Map<String, String> contextMap) {
        MdcVariables.setMdcContextMap(contextMap);
        logger.trace("starting to collectFile");

        final String remoteFile = fileData.remoteFilePath();
        final Path localFile = fileData.getLocalFileName();

        try {
            localFile.getParent().toFile().mkdir(); // Create parent directories

            FileCollectClient currentClient = selectClient(fileData);

            currentClient.collectFile(remoteFile, localFile);
            return Mono.just(getConsumerDmaapModel(fileData, metaData, localFile));
        } catch (Exception throwable) {
            logger.warn("Failed to download file: {}, reason: {}", fileData.name(), throwable);
            return Mono.error(throwable);
        }
    }

    private FileCollectClient selectClient(FileData fileData) throws DatafileTaskException {
        switch (fileData.scheme()) {
            case SFTP:
                return sftpClient;
            case FTPS:
                return ftpsClient;
            default:
                throw new DatafileTaskException("Unhandeled protocol: " + fileData.scheme());
        }
    }

    private ConsumerDmaapModel getConsumerDmaapModel(FileData fileData, MessageMetaData metaData, Path localFile) {
        String location = fileData.location();

        return ImmutableConsumerDmaapModel.builder() //
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
                .build();
    }
}
