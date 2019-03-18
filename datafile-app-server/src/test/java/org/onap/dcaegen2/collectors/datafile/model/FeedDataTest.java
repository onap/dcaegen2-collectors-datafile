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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpPost;
import org.junit.jupiter.api.Test;

public class FeedDataTest {
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    @Test
    public void addUserCredentialsWhenProvided_shouldBeAdded() {
        ImmutableFeedData feedData = ImmutableFeedData.builder() //
            .publishUrl("") //
            .publishedCheckUrl("") //
            .username(USERNAME) //
            .password(PASSWORD) //
            .build();

        String plainCreds = USERNAME + ":" + PASSWORD;
        byte[] plainCredsBytes = plainCreds.getBytes(StandardCharsets.ISO_8859_1);
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        String base64Creds = "Basic " + new String(base64CredsBytes);
        HttpPost request = new HttpPost();
        feedData.addUserCredentialsToHead(request);
        Header[] authorizationHeaders = request.getHeaders("Authorization");
        assertEquals(base64Creds, authorizationHeaders[0].getValue());
    }

    @Test
    public void addUserCredentialsWhenNotProvided_shouldNotBeAdded() {
        ImmutableFeedData feedData = ImmutableFeedData.builder() //
                .publishUrl("") //
                .publishedCheckUrl("") //
                .username("") //
                .password("") //
                .build();

        HttpPost request = new HttpPost();
        feedData.addUserCredentialsToHead(request);
        Header[] authorizationHeaders = request.getAllHeaders();
        assertEquals(0, authorizationHeaders.length);
    }
}
