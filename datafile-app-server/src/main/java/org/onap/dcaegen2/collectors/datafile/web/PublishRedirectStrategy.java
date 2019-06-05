/*-
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

package org.onap.dcaegen2.collectors.datafile.web;

import java.net.URI;
import java.util.Map;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * PublishRedirectStrategy implementation that automatically redirects all HEAD, GET, POST, PUT, and DELETE requests.
 * This strategy relaxes restrictions on automatic redirection of POST methods imposed by the HTTP specification.
 *
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class PublishRedirectStrategy extends DefaultRedirectStrategy {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Map<String, String> contextMap;

    /**
     * Redirectable methods.
     */
    private static final String[] REDIRECT_METHODS = new String[] { //
            HttpPut.METHOD_NAME, //
            HttpGet.METHOD_NAME, //
            HttpPost.METHOD_NAME, //
            HttpHead.METHOD_NAME, //
            HttpDelete.METHOD_NAME //
    };

    /**
     * Constructor PublishRedirectStrategy.
     *
     * @param contextMap - MDC context map
     */
    public PublishRedirectStrategy(Map<String, String> contextMap) {
        this.contextMap = contextMap;
    }

    @Override
    protected boolean isRedirectable(final String method) {
        for (final String m : REDIRECT_METHODS) {
            if (m.equalsIgnoreCase(method)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public HttpUriRequest getRedirect(final HttpRequest request, final HttpResponse response, final HttpContext context)
            throws ProtocolException {
        MDC.setContextMap(contextMap);
        final URI uri = getLocationURI(request, response, context);
        logger.trace("getRedirect...: {}", request);
        return RequestBuilder.copy(request).setUri(uri).build();
    }

}
