/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018 Nordix Foundation. All rights reserved.
 * ================================================================================
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
 * ============LICENSE_END=========================================================
 */
package org.onap.dcaegen2.collectors.datafile.tasks;

import java.net.URI;
import java.util.ArrayList;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.onap.dcaegen2.collectors.datafile.config.DmaapConsumerConfiguration;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.configuration.Config;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.exceptions.DmaapNotFoundException;
import org.onap.dcaegen2.collectors.datafile.ftp.FtpsClient;
import org.onap.dcaegen2.collectors.datafile.ftp.SftpClient;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.service.DmaapConsumerJsonParser;
import org.onap.dcaegen2.collectors.datafile.service.FileData;
import org.onap.dcaegen2.collectors.datafile.service.consumer.ExtendedDmaapConsumerHttpClientImpl;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableConsumerDmaapModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 3/23/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
@Component
public class DmaapConsumerTaskImpl
        extends DmaapConsumerTask<String, ArrayList<ConsumerDmaapModel>, DmaapConsumerConfiguration> {

    private static final String FTPES = "ftpes";
    private static final String FTPS = "ftps";
    private static final String SFTP = "sftp";

    private static final Logger logger = LoggerFactory.getLogger(DmaapConsumerTaskImpl.class);
    private Config datafileAppConfig;
    private ExtendedDmaapConsumerHttpClientImpl extendedDmaapConsumerHttpClient;
    private DmaapConsumerJsonParser dmaapConsumerJsonParser;

    private FtpsClient ftpsClient;
    private SftpClient sftpClient;

    @Autowired
    public DmaapConsumerTaskImpl(AppConfig datafileAppConfig) {
        this.datafileAppConfig = datafileAppConfig;
        this.dmaapConsumerJsonParser = new DmaapConsumerJsonParser();
    }

    protected DmaapConsumerTaskImpl(ExtendedDmaapConsumerHttpClientImpl extendedDmaapConsumerHttpClient, DmaapConsumerJsonParser dmaapConsumerJsonParser,
            FtpsClient ftpsClient, SftpClient sftpClient) {
        this.extendedDmaapConsumerHttpClient = extendedDmaapConsumerHttpClient;
        this.dmaapConsumerJsonParser = dmaapConsumerJsonParser;
        this.ftpsClient = ftpsClient;
        this.sftpClient = sftpClient;
    }

    @Override
    ArrayList<FileData> consume(String message) throws DmaapNotFoundException {
        logger.trace("Method called with arg {}", message);
        return dmaapConsumerJsonParser.getJsonObject(message);
    }

    @Override
    public ArrayList<ConsumerDmaapModel> execute(String object) throws DatafileTaskException {
        ArrayList<ConsumerDmaapModel> res = new ArrayList<ConsumerDmaapModel>();
        extendedDmaapConsumerHttpClient = resolveClient();
        logger.trace("Method called with arg {}", object);
        ArrayList<FileData> listOfFileData = consume((extendedDmaapConsumerHttpClient.getHttpConsumerResponse()
                .orElseThrow(() -> new DatafileTaskException("DmaapConsumerTask has returned null"))));
        for (int i = 0; i < listOfFileData.size(); i++) {
            String compression = listOfFileData.get(i).getCompression();
            String fileFormatType = listOfFileData.get(i).getFileFormatType();
            String fileFormatVersion = listOfFileData.get(i).getFileFormatVersion();

            String location = listOfFileData.get(i).getLocation();
            URI uri = URI.create(location);
            String serverAddress = uri.getHost();
            String userInfoString = uri.getUserInfo();
            String[] userInfo = new String[2];
            String userId = new String();
            String password = new String();
            if (userInfoString != null && !userInfoString.isEmpty()) {
                userInfo = userInfoString.split(":");
                userId = userInfo[0];
                password = userInfo[1];
            }
            int port = uri.getPort();
            String remoteFile = uri.getPath();
            String localFile = "target/" + FilenameUtils.getName(remoteFile);
            String scheme = uri.getScheme();

            // TODO: Refactor for better error handling.
            if (FTPES.equals(scheme) || FTPS.equals(scheme)) {
                FtpsClient ftpsClient = getFtpsClient();
                ftpsClient.collectFile(serverAddress, userId, password, port, remoteFile, localFile);
            } else if (SFTP.equals(scheme)) {
                SftpClient sftpClient = getSftpClient();
                sftpClient.collectFile(serverAddress, userId, password, port, remoteFile, localFile);
            } else {
                logger.trace("DFC does not support protocol {}. Supported protocols are " + FTPES + ", " + FTPS
                        + ", and " + SFTP + ".", scheme);
                continue;
            }
            ConsumerDmaapModel consumerDmaapModel =
                    ImmutableConsumerDmaapModel.builder().location(localFile).compression(compression)
                            .fileFormatType(fileFormatType).fileFormatVersion(fileFormatVersion).build();
            res.add(consumerDmaapModel);
        }
        return res;
    }

    /**
     * @return the ftpsClient
     */
    private FtpsClient getFtpsClient() {
        if (ftpsClient == null) {
            ftpsClient = new FtpsClient();
        }
        return ftpsClient;
    }

    /**
     * @return the sftpClient
     */
    private SftpClient getSftpClient() {
        if (sftpClient == null) {
            sftpClient = new SftpClient();
        }
        return sftpClient;
    }

    @Override
    void initConfigs() {
        datafileAppConfig.initFileStreamReader();
    }

    @Override
    DmaapConsumerConfiguration resolveConfiguration() {
        return datafileAppConfig.getDmaapConsumerConfiguration();
    }

    @Override
    ExtendedDmaapConsumerHttpClientImpl resolveClient() {
        return Optional.ofNullable(extendedDmaapConsumerHttpClient)
                .orElseGet(() -> new ExtendedDmaapConsumerHttpClientImpl(resolveConfiguration()));
    }
}
