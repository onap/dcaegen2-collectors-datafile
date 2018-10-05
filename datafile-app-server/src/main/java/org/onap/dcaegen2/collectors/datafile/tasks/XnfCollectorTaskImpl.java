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

package org.onap.dcaegen2.collectors.datafile.tasks;

import java.io.File;
import java.net.URI;

import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.configuration.Config;
import org.onap.dcaegen2.collectors.datafile.configuration.FtpesConfig;
import org.onap.dcaegen2.collectors.datafile.ftp.FileServerData;
import org.onap.dcaegen2.collectors.datafile.ftp.FtpsClient;
import org.onap.dcaegen2.collectors.datafile.ftp.ImmutableFileServerData;
import org.onap.dcaegen2.collectors.datafile.ftp.SftpClient;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.FileData;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableConsumerDmaapModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;

/**
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
@Component
public class XnfCollectorTaskImpl implements XnfCollectorTask {

    private static final String FTPES = "ftpes";
    private static final String FTPS = "ftps";
    private static final String SFTP = "sftp";

    private static final Logger logger = LoggerFactory.getLogger(XnfCollectorTaskImpl.class);
    private Config datafileAppConfig;

    private final FtpsClient ftpsClient;
    private final SftpClient sftpClient;

    @Autowired
    protected XnfCollectorTaskImpl(AppConfig datafileAppConfig, FtpsClient ftpsCleint, SftpClient sftpClient) {
        this.datafileAppConfig = datafileAppConfig;
        this.ftpsClient = ftpsCleint;
        this.sftpClient = sftpClient;
    }

    @Override
    public Flux<ConsumerDmaapModel> execute(FileData fileData) {
        logger.trace("Entering execute with {}", fileData);
        resolveKeyStore();

        String localFile = collectFile(fileData);

        if (localFile != null) {
            ConsumerDmaapModel consumerDmaapModel = getConsumerDmaapModel(fileData, localFile);
            logger.trace("Exiting execute with {}", consumerDmaapModel);
            return Flux.just(consumerDmaapModel);
        }
        logger.trace("Exiting execute with empty");
        return Flux.empty();
    }

    @Override
    public FtpesConfig resolveConfiguration() {
        return datafileAppConfig.getFtpesConfiguration();
    }

    private void resolveKeyStore() {
        FtpesConfig ftpesConfig = resolveConfiguration();
        ftpsClient.setKeyCertPath(ftpesConfig.keyCert());
        ftpsClient.setKeyCertPassword(ftpesConfig.keyPassword());
        ftpsClient.setTrustedCAPath(ftpesConfig.trustedCA());
        ftpsClient.setTrustedCAPassword(ftpesConfig.trustedCAPassword());
    }

    private String collectFile(FileData fileData) {
        logger.info("starting to collectFile");
        String location = fileData.location();
        URI uri = URI.create(location);
        FileServerData fileServerData = getFileServerData(uri);
        String remoteFile = uri.getPath();
        String localFile = "target" + File.separator + fileData.name();
        String scheme = uri.getScheme();
        boolean fileDownloaded = false;
        if (FTPES.equals(scheme) || FTPS.equals(scheme)) {
            fileDownloaded = ftpsClient.collectFile(fileServerData, remoteFile, localFile);
        } else if (SFTP.equals(scheme)) {
            fileDownloaded = sftpClient.collectFile(fileServerData, remoteFile, localFile);
        } else {

            logger.error("DFC does not support protocol {}. Supported protocols are {}, {}, and {}. Data: {}", scheme,
                    FTPES, FTPS, SFTP, fileData);
            localFile = null;
        }
        if (!fileDownloaded) {
            localFile = null;
        }
        return localFile;
    }

    private FileServerData getFileServerData(URI uri) {
        String[] userInfo = getUserNameAndPasswordIfGiven(uri.getUserInfo());
        return ImmutableFileServerData.builder().serverAddress(uri.getHost())
                .userId(userInfo != null ? userInfo[0] : "").password(userInfo != null ? userInfo[1] : "")
                .port(uri.getPort()).build();
    }

    private String[] getUserNameAndPasswordIfGiven(String userInfoString) {
        String[] userInfo = null;
        if (userInfoString != null && !userInfoString.isEmpty()) {
            userInfo = userInfoString.split(":");
        }
        return userInfo;
    }

    private ConsumerDmaapModel getConsumerDmaapModel(FileData fileData, String localFile) {
        String name = fileData.name();
        String compression = fileData.compression();
        String fileFormatType = fileData.fileFormatType();
        String fileFormatVersion = fileData.fileFormatVersion();

        return ImmutableConsumerDmaapModel.builder().name(name).location(localFile).compression(compression)
                .fileFormatType(fileFormatType).fileFormatVersion(fileFormatVersion).build();
    }
}
