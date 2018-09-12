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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CommonFunctionsTest {
    // Given
    private ConsumerDmaapModel model = new ConsumerDmaapModelForUnitTest();
    private static final String EXPECTED_RESULT =
            "{\"location\":\"target/A20161224.1030-1045.bin.gz\",\"compression\":\"gzip\","
                    + "\"fileFormatType\":\"org.3GPP.32.435#measCollec\",\"fileFormatVersion\":\"V10\"}";

    private static final HttpResponse httpResponseMock = mock(HttpResponse.class);
    private static final HttpEntity httpEntityMock = mock(HttpEntity.class);
    private static final StatusLine statusLineMock = mock(StatusLine.class);

    @BeforeAll
    static void setup() {
        when(httpResponseMock.getEntity()).thenReturn(httpEntityMock);
        when(httpResponseMock.getStatusLine()).thenReturn(statusLineMock);
    }

    @Test
    void createJsonBody_shouldReturnJsonInString() {
        assertEquals(EXPECTED_RESULT, CommonFunctions.createJsonBody(model));
    }

    @Test
    void handleResponse_shouldReturn200() throws IOException {
        // When
        when(httpResponseMock.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_OK);
        // Then
        assertEquals(Optional.of(HttpStatus.SC_OK), CommonFunctions.handleResponse(httpResponseMock));
    }

    @Test
    void handleResponse_shouldReturn300() throws IOException {
        // When
        when(httpResponseMock.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        // Then
        assertEquals(Optional.of(HttpStatus.SC_BAD_REQUEST), CommonFunctions.handleResponse(httpResponseMock));
    }
}
