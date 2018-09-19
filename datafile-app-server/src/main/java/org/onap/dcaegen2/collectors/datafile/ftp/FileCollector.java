/*-
 * ============LICENSE_START======================================================================
 *  Copyright (C) 2018 Ericsson. All rights reserved.
 * ===============================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END========================================================================
 */

package org.onap.dcaegen2.collectors.datafile.ftp;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.FileData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 *
 */
@Component
public class FileCollector { // TODO: Should be final, but that means adding PowerMock or Mockito
                             // 2.x for testing so it is left for later improvement.
    private static final String FTPES = "ftpes";
    private static final String FTPS = "ftps";
    private static final String SFTP = "sftp";

    private static final Logger logger = LoggerFactory.getLogger(FileCollector.class);

    private final FtpsClient ftpsClient;
    private final SftpClient sftpClient;

    @Autowired
    protected FileCollector(FtpsClient ftpsCleint, SftpClient sftpClient) {
        this.ftpsClient = ftpsCleint;
        this.sftpClient = sftpClient;
    }

    public Mono<List<ConsumerDmaapModel>> getFilesFromSender(List<FileData> listOfFileData) {
        List<ConsumerDmaapModel> consumerModels = new ArrayList<ConsumerDmaapModel>();
        for (FileData fileData : listOfFileData) {
            String localFile = collectFile(fileData);

            if (localFile != null) {
                ConsumerDmaapModel consumerDmaapModel = getConsumerDmaapModel(fileData, localFile);
                consumerModels.add(consumerDmaapModel);
            }
        }
        return Mono.just(consumerModels);
    }

    private String collectFile(FileData fileData) {
        String location = fileData.location();
        URI uri = URI.create(location);
        String[] userInfo = getUserNameAndPasswordIfGiven(uri.getUserInfo());
        FileServerData fileServerData = ImmutableFileServerData.builder().serverAddress(uri.getHost())
                .userId(userInfo != null ? userInfo[0] : "").password(userInfo != null ? userInfo[1] : "")
                .port(uri.getPort()).build();
        String remoteFile = uri.getPath();
        String localFile = "target/" + FilenameUtils.getName(remoteFile);
        String scheme = uri.getScheme();

        boolean fileDownloaded = false;
        if (FTPES.equals(scheme) || FTPS.equals(scheme)) {
            fileDownloaded = ftpsClient.collectFile(fileServerData, remoteFile, localFile);
        } else if (SFTP.equals(scheme)) {
            fileDownloaded = sftpClient.collectFile(fileServerData, remoteFile, localFile);
        } else {

            logger.error("DFC does not support protocol {}. Supported protocols are " + FTPES + ", " + FTPS + ", and "
                    + SFTP + ". " + fileData);
            localFile = null;
        }
        if (!fileDownloaded) {
            localFile = null;
        }
        return localFile;
    }

    private String[] getUserNameAndPasswordIfGiven(String userInfoString) {
        String[] userInfo = null;
        if (userInfoString != null && !userInfoString.isEmpty()) {
            userInfo = userInfoString.split(":");
        }
        return userInfo;
    }

    private ConsumerDmaapModel getConsumerDmaapModel(FileData fileData, String localFile) {
        String compression = fileData.compression();
        String fileFormatType = fileData.fileFormatType();
        String fileFormatVersion = fileData.fileFormatVersion();

        return ImmutableConsumerDmaapModel.builder().location(localFile).compression(compression)
                .fileFormatType(fileFormatType).fileFormatVersion(fileFormatVersion).build();
    }
}
