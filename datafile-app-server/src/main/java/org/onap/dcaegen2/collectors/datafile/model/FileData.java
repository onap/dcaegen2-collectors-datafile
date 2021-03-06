/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 *  Copyright (C) 2021 Nokia. All rights reserved.
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
import java.util.List;
import java.util.Optional;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.immutables.gson.Gson;
import org.immutables.value.Value;
import org.onap.dcaegen2.collectors.datafile.commons.FileServerData;
import org.onap.dcaegen2.collectors.datafile.commons.ImmutableFileServerData;
import org.onap.dcaegen2.collectors.datafile.commons.Scheme;

/**
 * Contains data, from the fileReady event, about the file to collect from the xNF.
 *
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
@Value.Immutable
@Gson.TypeAdapters
@Value.Style(redactedMask = "####")
public abstract class FileData {
    public static final String DATAFILE_TMPDIR = "/tmp/onap_datafile/";

    /**
     * Get the file name with no path.
     *
     * @return the file name with no path
     */
    public abstract String name();

    /**
     * Get the URL to use to fetch the file from the PNF.
     *
     * @return the URL to use to fetch the file from the PNF
     */
    @Value.Redacted
    public abstract String location();

    /**
     * Get the file transfer protocol to use for fetching the file.
     *
     * @return the file transfer protocol to use for fetching the file
     */
    public abstract Scheme scheme();

    public abstract String compression();

    public abstract String fileFormatType();

    public abstract String fileFormatVersion();

    public abstract MessageMetaData messageMetaData();

    /**
     * Get the name of the PNF, must be unique in the network.
     *
     * @return the name of the PNF, must be unique in the network
     */
    public String sourceName() {
        return messageMetaData().sourceName();
    }

    /**
     * Get the path to file to get from the PNF.
     *
     * @return the path to the file on the PNF.
     */
    public String remoteFilePath() {
        return URI.create(location()).getPath();
    }

    /**
     * Get the path to the locally stored file.
     *
     * @return the path to the locally stored file.
     */
    public Path getLocalFilePath() {
        return Paths.get(DATAFILE_TMPDIR, name());
    }

    /**
     * Get the data about the file server where the file should be collected from.
     * Query data included as it can contain JWT token
     *
     * @return the data about the file server where the file should be collected from.
     */
    public FileServerData fileServerData() {
        URI uri = URI.create(location());
        Optional<String[]> userInfo = getUserNameAndPasswordIfGiven(uri.getUserInfo());
        ImmutableFileServerData.Builder builder = ImmutableFileServerData.builder() //
            .serverAddress(uri.getHost()) //
            .userId(userInfo.isPresent() ? userInfo.get()[0] : "") //
            .password(userInfo.isPresent() ? userInfo.get()[1] : "");
        if (uri.getPort() > 0) {
            builder.port(uri.getPort());
        }
        URIBuilder uriBuilder = new URIBuilder(uri);
        List<NameValuePair> query = uriBuilder.getQueryParams();
        if (query != null && !query.isEmpty()) {
            builder.queryParameters(query);
        }
        String fragment = uri.getRawFragment();
        if (fragment != null && fragment.length() > 0) {
            builder.uriRawFragment(fragment);
        }
        return builder.build();
    }

    /**
     * Extracts user name and password from the user info, if it they are given in the URI.
     *
     * @param userInfoString the user info string from the URI.
     *
     * @return An <code>Optional</code> containing a String array with the user name and password if given, or an empty
     *         <code>Optional</code> if not given.
     */
    private static Optional<String[]> getUserNameAndPasswordIfGiven(String userInfoString) {
        if (userInfoString != null) {
            String[] userAndPassword = userInfoString.split(":");
            if (userAndPassword.length == 2) {
                return Optional.of(userAndPassword);
            } else if (userAndPassword.length == 1)// if just user
            {
                String[] tab = new String[2];
                tab[0] = userAndPassword[0];
                tab[1] = "";// add empty password
                return Optional.of(tab);
            }
        }
        return Optional.empty();
    }
}
