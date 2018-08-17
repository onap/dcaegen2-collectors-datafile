/*
 * ============LICENSE_START=======================================================
 * PNF-REGISTRATION-HANDLER
 * ================================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property. All rights reserved.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.dcaegen2.collectors.datafile.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.model.CommonFunctions;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModelForUnitTest;

class CommonFunctionsTest {

    // Given
    private ConsumerDmaapModel model = new ConsumerDmaapModelForUnitTest();

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
        String expectedResult = "{\"pnf-name\":\"NOKnhfsadhff\",\"ipaddress-v4-oam\":\"256.22.33.155\""
            + ",\"ipaddress-v6-oam\":\"2001:0db8:85a3:0000:0000:8a2e:0370:7334\"}";
        assertEquals(expectedResult, CommonFunctions.createJsonBody(model));
    }
}
