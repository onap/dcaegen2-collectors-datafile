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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConsumerDmaapModelTest {
    private static final String PRODUCT_NAME = "NrRadio";
    private static final String VENDOR_NAME = "Ericsson";
    private static final String LAST_EPOCH_MICROSEC = "8745745764578";
    private static final String SOURCE_NAME = "oteNB5309";
    private static final String START_EPOCH_MICROSEC = "8745745764578";
    private static final String TIME_ZONE_OFFSET = "UTC+05:00";
    private static final String NAME = "A20161224.1030-1045.bin.gz";
    private static final String LOCATION = "target/A20161224.1030-1045.bin.gz";
    private static final String COMPRESSION = "gzip";
    private static final String FILE_FORMAT_TYPE = "org.3GPP.32.435#measCollec";
    private static final String FILE_FORMAT_VERSION = "V10";

    @Test
    public void consumerDmaapModelBuilder_shouldBuildAnObject() {

        ConsumerDmaapModel consumerDmaapModel = ImmutableConsumerDmaapModel.builder().productName(PRODUCT_NAME)
                .vendorName(VENDOR_NAME).lastEpochMicrosec(LAST_EPOCH_MICROSEC).sourceName(SOURCE_NAME)
                .startEpochMicrosec(START_EPOCH_MICROSEC).timeZoneOffset(TIME_ZONE_OFFSET).name(NAME).location(LOCATION)
                .compression(COMPRESSION).fileFormatType(FILE_FORMAT_TYPE).fileFormatVersion(FILE_FORMAT_VERSION)
                .build();

        Assertions.assertNotNull(consumerDmaapModel);
        Assertions.assertEquals(PRODUCT_NAME, consumerDmaapModel.getProductName());
        Assertions.assertEquals(VENDOR_NAME, consumerDmaapModel.getVendorName());
        Assertions.assertEquals(LAST_EPOCH_MICROSEC, consumerDmaapModel.getLastEpochMicrosec());
        Assertions.assertEquals(SOURCE_NAME, consumerDmaapModel.getSourceName());
        Assertions.assertEquals(START_EPOCH_MICROSEC, consumerDmaapModel.getStartEpochMicrosec());
        Assertions.assertEquals(TIME_ZONE_OFFSET, consumerDmaapModel.getTimeZoneOffset());
        Assertions.assertEquals(NAME, consumerDmaapModel.getName());
        Assertions.assertEquals(LOCATION, consumerDmaapModel.getLocation());
        Assertions.assertEquals(COMPRESSION, consumerDmaapModel.getCompression());
        Assertions.assertEquals(FILE_FORMAT_TYPE, consumerDmaapModel.getFileFormatType());
        Assertions.assertEquals(FILE_FORMAT_VERSION, consumerDmaapModel.getFileFormatVersion());
    }
}
