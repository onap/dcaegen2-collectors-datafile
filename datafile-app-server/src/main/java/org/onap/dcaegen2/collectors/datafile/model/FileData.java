/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018 Nordix Foundation. All rights reserved.
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
    public abstract String name();

    public abstract String location();

    public abstract Scheme scheme();

    public abstract String compression();

    public abstract String fileFormatType();

    public abstract String fileFormatVersion();

    public String remoteFilePath() {
        return URI.create(location()).getPath();
    }

    public Path getLocalFileName() {
        URI uri = URI.create(location());
        return createLocalFileName(uri.getHost(), name());
    }

    public static Path createLocalFileName(String host, String fileName) {
        return Paths.get("/tmp/onap_datafile/", host + "_" + fileName);
    }

    public FileServerData fileServerData() {
        URI uri = URI.create(location());
        Optional<String[]> userInfo = getUserNameAndPasswordIfGiven(uri.getUserInfo());
        // @formatter:off
        return ImmutableFileServerData.builder()
                .serverAddress(uri.getHost())
                .userId(userInfo.isPresent() ? userInfo.get()[0] : "")
                .password(userInfo.isPresent() ? userInfo.get()[1] : "")
                .port(uri.getPort())
                .build();
        // @formatter:on
    }

    private Optional<String[]> getUserNameAndPasswordIfGiven(String userInfoString) {
        if (userInfoString != null) {
            return Optional.of(userInfoString.split(":"));
        }
        return Optional.empty();
    }
}