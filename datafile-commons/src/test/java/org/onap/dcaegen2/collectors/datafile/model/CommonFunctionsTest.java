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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class CommonFunctionsTest {
    @Test
    public void createJsonBody_success() {
        ImmutableConsumerDmaapModel consumerDmaapModel = ImmutableConsumerDmaapModel //
        .builder() //
        .productName("") //
        .vendorName("") //
        .lastEpochMicrosec("") //
        .sourceName("") //
        .startEpochMicrosec("") //
        .timeZoneOffset("") //
        .name("") //
        .location("") //
        .internalLocation(Paths.get("internalLocation")) //
        .compression("") //
        .fileFormatType("") //
        .fileFormatVersion("") //
        .build();
        String actualBody = CommonFunctions.createJsonBody(consumerDmaapModel);

        assertTrue(actualBody.contains("\"internalLocation\":\"internalLocation\""));
    }
}
