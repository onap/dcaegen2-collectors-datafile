/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
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

import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.HttpUriRequest;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

/**
 * Immutable data about the feed used to publish to DataRouter.
 *
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
@Value.Immutable
@Gson.TypeAdapters
public interface FeedData {

    /**
     * The URL to use when publishing to DataRouter.
     *
     * @return the URL to use when publishing to DataRouter.
     */
    public String publishUrl();

    /**
     * The URL to use when asking DataRouter if a file has been published.
     *
     * @return the URL to use when asking DataRouter if a file has been published.
     */
    public String publishedCheckUrl();

    /**
     * The username to use when communicating with DataRouter.
     *
     * @return the username to use when communicating with DataRouter.
     */
    public String username();

    /**
     * The password to use when communicating with DataRouter.
     * @return the password to use when communicating with DataRouter.
     */
    public String password();

    /**
     * Adds the user credentials from this feed data to the given request.
     *
     * @param request the request to add the user credentials to.
     */
    public default void addUserCredentialsToHead(HttpUriRequest request) {
        if (!username().isEmpty() || !password().isEmpty()) {
            String plainCreds = username() + ":" + password();
            byte[] plainCredsBytes = plainCreds.getBytes(StandardCharsets.ISO_8859_1);
            byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
            String base64Creds = new String(base64CredsBytes);
            request.addHeader("Authorization", "Basic " + base64Creds);
        }
    }
}
