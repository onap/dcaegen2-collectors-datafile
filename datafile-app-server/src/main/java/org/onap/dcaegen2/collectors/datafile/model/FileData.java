/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2018-2019 Nordix Foundation.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
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
public interface FileData {

    public String name();

    public String location();

    public Scheme scheme();

    public String compression();

    public String fileFormatType();

    public String fileFormatVersion();

    public default String remoteFilePath() {
        return URI.create(location()).getPath();
    }

    /**
     * Get the path to the locally stored file.
     *
     * @return the path to the locally stored file.
     */
    public default Path getLocalFileName() {
        URI uri = URI.create(location());
        return Paths.get("/tmp/onap_datafile/" + uri.getHost() + "_" + name());
    }

    /**
     * Get the data about the file server where the file should be collected from.
     *
     * @return the data about the file server where the file should be collected from.
     */
    public default FileServerData fileServerData() {
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

    /**
     * Get the user name and password from the user info string.
     *
     * @param userInfoString the user info string from the URI.
     *
     * @return an <code>Optional</code> with an array with the user name and password if the user info string contained
     *         them. An empty <code>Optional</code> if not.
     */
    default Optional<String[]> getUserNameAndPasswordIfGiven(String userInfoString) {
        if (userInfoString != null) {
            String[] userAndPassword = userInfoString.split(":");
            if (userAndPassword.length == 2) {
                return Optional.of(userAndPassword);
            }
        }
        return Optional.empty();
    }
}
