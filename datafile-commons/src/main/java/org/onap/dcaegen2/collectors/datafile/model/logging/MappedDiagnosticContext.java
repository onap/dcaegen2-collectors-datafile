/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2019 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.model.logging;

import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.http.HttpHeaders;

/**
 * Support functions for MDC
 */
public final class MappedDiagnosticContext {

    private static final String X_ONAP_REQUEST_ID = "X-ONAP-RequestID";
    private static final String X_INVOCATION_ID = "X-InvocationID";
    private static final String REQUEST_ID = "RequestID";
    private static final String INVOCATION_ID = "InvocationID";
    public static final String RESPONSE_CODE = "ResponseCode";
    public static final String SERVICE_NAME = "ServiceName";

    public static final Marker ENTRY = MarkerFactory.getMarker("ENTRY");
    public static final Marker EXIT = MarkerFactory.getMarker("EXIT");
    private static final Marker INVOKE = MarkerFactory.getMarker("INVOKE");

    private static final Logger logger = LoggerFactory.getLogger(MappedDiagnosticContext.class);

    private MappedDiagnosticContext() {}

    /**
     * Inserts the relevant trace information in the HTTP header
     * @param httpRequest a request
     */
    public static void appendTraceInfo(HttpRequestBase httpRequest) {
        String requestId = MDC.get(REQUEST_ID);
        httpRequest.addHeader(X_ONAP_REQUEST_ID, requestId);
        httpRequest.addHeader("X-RequestID", requestId); // deprecated
        httpRequest.addHeader("X-TransactionID", requestId); // deprecated

        String invocationId = UUID.randomUUID().toString();
        httpRequest.addHeader(X_INVOCATION_ID, invocationId);
        logger.info(INVOKE, "Invoking request with invocation ID {}", invocationId);
    }

    /**
     * Initialize MDC from relevant information in a received HTTP header
     * @param headers a received HTPP header
     */
    public static void initializeTraceContext(HttpHeaders headers) {
        String requestId = headers.getFirst(X_ONAP_REQUEST_ID);
        if (StringUtils.isBlank(requestId)) {
            requestId = UUID.randomUUID().toString();
        }
        String invocationId = headers.getFirst(X_INVOCATION_ID);
        if (StringUtils.isBlank(invocationId)) {
            invocationId = UUID.randomUUID().toString();
        }
        MDC.put(REQUEST_ID, requestId);
        MDC.put(INVOCATION_ID, invocationId);
    }

    /**
     * Initialize the MDC when a new context is started.
     * @return a copy of the new trace context
     */
    public static Map<String, String> initializeTraceContext() {
        MDC.clear();
        MDC.put(REQUEST_ID, UUID.randomUUID().toString());
        return MDC.getCopyOfContextMap();
    }
}
