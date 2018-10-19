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

package org.onap.dcaegen2.collectors.datafile.ftp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public abstract class FileCollectClient {
    protected static final Logger logger = LoggerFactory.getLogger(FtpsClient.class);

    protected FileServerData fileServerData;
    protected String remoteFile;
    protected String localFile;
    protected ErrorData errorData;

    public FileCollectResult collectFile(FileServerData fileServerData, String remoteFile, String localFile) {
        logger.trace("collectFile called with fileServerData: {}, remoteFile: {}, localFile: {}", fileServerData,
                remoteFile, localFile);

        this.fileServerData = fileServerData;
        this.remoteFile = remoteFile;
        this.localFile = localFile;

        return retryCollectFile();
    }

    public abstract FileCollectResult retryCollectFile();

    protected void addError(String errorMessage, Throwable errorCause) {
        if (errorData == null) {
            errorData = new ErrorData();
        }
        errorData.addError(errorMessage, errorCause);
    }
}
