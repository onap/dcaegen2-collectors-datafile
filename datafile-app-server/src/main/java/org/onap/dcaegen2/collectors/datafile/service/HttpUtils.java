/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018-2019 Nordix Foundation. All rights reserved.
 * Modifications Copyright (C) 2020 Nokia. All rights reserved
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
 * ============LICENSE_END========================================================================
 */

package org.onap.dcaegen2.collectors.datafile.service;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.onap.dcaegen2.collectors.datafile.commons.FileServerData;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;

public final class HttpUtils implements HttpStatus {

    public static final int HTTP_DEFAULT_PORT = 80;
    public static final int HTTPS_DEFAULT_PORT = 443;
    public static final String JWT_TOKEN_NAME = "access_token";
    public static final String AUTH_JWT_WARN = "Both JWT token and Basic auth data present. Omitting basic auth info.";
    public static final String AUTH_JWT_ERROR = "More than one JWT token present in the query. Omitting JWT token.";

    private HttpUtils() {
    }

    @NotNull
    public static String nonRetryableResponse(int responseCode) {
        return "Unexpected response code - " + responseCode;
    }

    @NotNull
    public static String retryableResponse(int responseCode) {
        return "Unexpected response code - " + responseCode + ". No retry attempts will be done.";
    }

    public static boolean isSuccessfulResponseCodeWithDataRouter(Integer statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    public static boolean isBasicAuthDataFilled(final FileServerData fileServerData) {
        return !fileServerData.userId().isEmpty() && !fileServerData.password().isEmpty();
    }

    public static String basicAuthContent(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    public static String jwtAuthContent(String token) {
        return "Bearer " + token;
    }

    /**
     * Prepare uri to retrieve file from xNF. If initially JWT token was included within the query, it is removed.
     * Other entries are rewritten.
     *
     * @param scheme scheme which is used during the connection
     * @param fileServerData fileServerData including - server address, port, query and fragment
     * @param remoteFile file which has to be downloaded
     * @param defaultPort default port which exchange empty entry for given connection type
     * @return true if query contains single JWT token entry
     */
    @NotNull public static String prepareUri(String scheme, FileServerData fileServerData, String remoteFile, int defaultPort) {
        int port = fileServerData.port().orElse(defaultPort);
        String query = rewriteQueryWithoutToken(fileServerData.query().orElse(new ArrayList<>()));
        String fragment = fileServerData.fragment().orElse("");
        if (!query.isEmpty()) {
            query = "?" + query;
        }
        if (!fragment.isEmpty()) {
            fragment = "#" + fragment;
        }
        return scheme + "://" + fileServerData.serverAddress() + ":" + port + remoteFile + query + fragment;
    }

    /**
     * Checks if the query contains single JWT token entry. Valid query contains only one token entry.
     *
     * @param query query
     * @param logger logger
     * @return true if query contains single token
     */
    public static boolean isQueryWithSingleJWT(Optional<List<NameValuePair>> query, Logger logger) {
        if (!query.isPresent()) {
            return false;
        }
        List<NameValuePair> queryElements = query.get();
        int i = getJWTTokenCount(queryElements);
        if (i == 0) {
            return false;
        }
        if (i > 1) {
            logger.error(AUTH_JWT_ERROR);
            return false;
        }
        return true;
    }

    /**
     * Checks if the query contains single JWT token entry. Valid query contains only one token entry.
     *
     * @param queryElements elements of the query
     * @return true if query contains single JWT token entry
     */
    public static int getJWTTokenCount(List<NameValuePair> queryElements) {
        int i = 0;
        for (NameValuePair element : queryElements) {
            if (element.getName().equals(JWT_TOKEN_NAME)) {
                i++;
            }
        }
        return i;
    }

    /**
     * Returns JWT token string (if single exist) from the query.
     *
     * @param fileServerData file server data which contain query where JWT token may exist
     * @param logger logger
     * @return JWT token value if single token entry exist or empty string elsewhere.
     *         If JWT token key has no value, empty string will be returned.
     */
    public static String getJWTToken(FileServerData fileServerData, Logger logger) {
        Optional<List<NameValuePair>> query = fileServerData.query();
        boolean jwtTokenKeyPresent = HttpUtils.isQueryWithSingleJWT(query, logger);
        if (!jwtTokenKeyPresent) {
            return "";
        }
        String token = HttpUtils.getJWTToken(query.get());
        if (HttpUtils.isBasicAuthDataFilled(fileServerData)) {
            logger.warn(HttpUtils.AUTH_JWT_WARN);
        }
        return token;
    }

    private static String getJWTToken(List<NameValuePair> query) {
        if (query.isEmpty()) {
            return "";
        }
        for (NameValuePair element : query) {
            if (!element.getName().equals(JWT_TOKEN_NAME)) {
                continue;
            }
            if (element.getValue() != null) {
                return element.getValue();
            }
            return "";
        }
        return "";
    }

    /**
     * Rewrites HTTP query without JWT token
     *
     * @param query list of NameValuePair of elements sent in the query
     * @return String representation of query elements which were provided in the input
     *     Empty string is possible when query is empty or contains only access_token key.
     */
    public static String rewriteQueryWithoutToken(List<NameValuePair> query) {
        if (query.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (NameValuePair nvp : query) {
            if (nvp.getName().equals(JWT_TOKEN_NAME)) {
                continue;
            }
            sb.append(nvp.getName());
            if (nvp.getValue() != null) {
                sb.append("=");
                sb.append(nvp.getValue());
            }
            sb.append("&");
        }
        if ((sb.length() > 0) && (sb.charAt(sb.length() - 1 ) == '&')) {
            sb.deleteCharAt(sb.length() - 1 );
        }
        return sb.toString();
    }
}
