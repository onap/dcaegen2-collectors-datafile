/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018-2019 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.model;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.immutables.gson.Gson;
import org.immutables.value.Value;
import org.onap.dcaegen2.collectors.datafile.ftp.FileServerData;
import org.onap.dcaegen2.collectors.datafile.ftp.ImmutableFileServerData;
import org.onap.dcaegen2.collectors.datafile.ftp.Scheme;

/**
 * Contains data, from the fileReady event, about the file to collect from the xNF.
 *
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
@Value.Immutable
@Gson.TypeAdapters
public abstract class FileData {
    private static final String DATAFILE_TMPDIR = "/tmp/onap_datafile/";

    /**
     * @return the file name with no path
     */
    public abstract String name();

    /**
     * @return the URL to use to fetch the file from the PNF
     */
    public abstract String location();

    /**
     * @return the file transfer protocol to use for fetching the file
     */
    public abstract Scheme scheme();

    public abstract String compression();

    public abstract String fileFormatType();

    public abstract String fileFormatVersion();

    public abstract MessageMetaData messageMetaData();

    /**
     * @return the name of the PNF, must be unique in the network
     */
    public String sourceName() {
        return messageMetaData().sourceName();
    }

    public String remoteFilePath() {
        return URI.create(location()).getPath();
    }

    public Path getLocalFileName() {
       return createLocalFileName(messageMetaData().sourceName(), name());
    }

    public static Path createLocalFileName(String sourceName, String fileName) {
        return Paths.get(DATAFILE_TMPDIR, sourceName + "_" + fileName);
    }

    public FileServerData fileServerData() {
        URI uri = URI.create(location());
        Optional<String[]> userInfo = getUserNameAndPasswordIfGiven(uri.getUserInfo());
        // @formatter:off
        ImmutableFileServerData.Builder builder = ImmutableFileServerData.builder()
                .serverAddress(uri.getHost())
                .userId(userInfo.isPresent() ? userInfo.get()[0] : "")
                .password(userInfo.isPresent() ? userInfo.get()[1] : "");
        if (uri.getPort() > 0) {
            builder.port(uri.getPort());
        }
        return builder.build();
        // @formatter:on
    }

    private Optional<String[]> getUserNameAndPasswordIfGiven(String userInfoString) {
        if (userInfoString != null) {
            String[] userAndPassword = userInfoString.split(":");
            if (userAndPassword.length == 2) {
                return Optional.of(userAndPassword);
            }
        }
        return Optional.empty();
    }
}
