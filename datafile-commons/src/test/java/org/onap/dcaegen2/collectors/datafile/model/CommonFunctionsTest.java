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

import org.junit.jupiter.api.Test;

class CommonFunctionsTest {
    // @formatter:off
    private ConsumerDmaapModel model = ImmutableConsumerDmaapModel.builder()
            .productName("NrRadio")
            .vendorName("Ericsson")
            .lastEpochMicrosec("8745745764578")
            .sourceName("oteNB5309")
            .startEpochMicrosec("8745745764578")
            .timeZoneOffset("UTC+05:00")
            .name("A20161224.1030-1045.bin.gz")
            .location("ftpes://192.168.0.101:22/ftp/rop/A20161224.1030-1145.bin.gz")
            .internalLocation("target/A20161224.1030-1045.bin.gz")
            .compression("gzip")
            .fileFormatType("org.3GPP.32.435#measCollec")
            .fileFormatVersion("V10")
            .build();
    
    private static final String EXPECTED_RESULT =
             "{\"productName\":\"NrRadio\","
            + "\"vendorName\":\"Ericsson\","
            + "\"lastEpochMicrosec\":\"8745745764578\","
            + "\"sourceName\":\"oteNB5309\","
            + "\"startEpochMicrosec\":\"8745745764578\","
            + "\"timeZoneOffset\":\"UTC+05:00\","
            + "\"name\":\"A20161224.1030-1045.bin.gz\","
            + "\"location\":\"ftpes://192.168.0.101:22/ftp/rop/A20161224.1030-1145.bin.gz\","
            + "\"internalLocation\":\"target/A20161224.1030-1045.bin.gz\","
            + "\"compression\":\"gzip\","
            + "\"fileFormatType\":\"org.3GPP.32.435#measCollec\","
            + "\"fileFormatVersion\":\"V10\"}";
    // @formatter:on
    @Test
    void createJsonBody_shouldReturnJsonInString() {
        assertEquals(EXPECTED_RESULT, CommonFunctions.createJsonBody(model));
    }
}
