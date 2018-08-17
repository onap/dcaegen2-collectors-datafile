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

package org.onap.dcaegen2.collectors.datafile.model.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.model.utils.HttpUtils;

class HttpUtilsTest {

    @Test
    void isSuccessfulResponseCode_shouldReturnTrue() {
        assertTrue(HttpUtils.isSuccessfulResponseCode(HttpUtils.SC_ACCEPTED));
    }

    @Test
    void isSuccessfulResponseCode_shouldReturnFalse() {
        assertFalse(HttpUtils.isSuccessfulResponseCode(HttpStatus.SC_BAD_GATEWAY));
    }
}