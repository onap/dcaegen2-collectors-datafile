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

package org.onap.dcaegen2.collectors.datafile.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FilePublishInformationTest {
    private static final String PRODUCT_NAME = "NrRadio";
    private static final String VENDOR_NAME = "Ericsson";
    private static final String LAST_EPOCH_MICROSEC = "8745745764578";
    private static final String SOURCE_NAME = "oteNB5309";
    private static final String START_EPOCH_MICROSEC = "8745745764578";
    private static final String TIME_ZONE_OFFSET = "UTC+05:00";
    private static final String NAME = "A20161224.1030-1045.bin.gz";
    private static final String LOCATION = "ftpes://192.168.0.101:22/ftp/rop/A20161224.1030-1145.bin.gz";
    private static final Path INTERNAL_LOCATION = Paths.get("target/A20161224.1030-1045.bin.gz");
    private static final String COMPRESSION = "gzip";
    private static final String FILE_FORMAT_TYPE = "org.3GPP.32.435#measCollec";
    private static final String FILE_FORMAT_VERSION = "V10";

    @Test
    public void filePublishInformationBuilder_shouldBuildAnObject() {
        FilePublishInformation filePublishInformation = ImmutableFilePublishInformation.builder() //
            .productName(PRODUCT_NAME) //
            .vendorName(VENDOR_NAME) //
            .lastEpochMicrosec(LAST_EPOCH_MICROSEC) //
            .sourceName(SOURCE_NAME) //
            .startEpochMicrosec(START_EPOCH_MICROSEC) //
            .timeZoneOffset(TIME_ZONE_OFFSET) //
            .name(NAME) //
            .location(LOCATION) //
            .internalLocation(INTERNAL_LOCATION) //
            .compression(COMPRESSION) //
            .fileFormatType(FILE_FORMAT_TYPE) //
            .fileFormatVersion(FILE_FORMAT_VERSION) //
            .context(new HashMap<String,String>()) //
            .build();

        Assertions.assertNotNull(filePublishInformation);
        Assertions.assertEquals(PRODUCT_NAME, filePublishInformation.getProductName());
        Assertions.assertEquals(VENDOR_NAME, filePublishInformation.getVendorName());
        Assertions.assertEquals(LAST_EPOCH_MICROSEC, filePublishInformation.getLastEpochMicrosec());
        Assertions.assertEquals(SOURCE_NAME, filePublishInformation.getSourceName());
        Assertions.assertEquals(START_EPOCH_MICROSEC, filePublishInformation.getStartEpochMicrosec());
        Assertions.assertEquals(TIME_ZONE_OFFSET, filePublishInformation.getTimeZoneOffset());
        Assertions.assertEquals(NAME, filePublishInformation.getName());
        Assertions.assertEquals(LOCATION, filePublishInformation.getLocation());
        Assertions.assertEquals(INTERNAL_LOCATION, filePublishInformation.getInternalLocation());
        Assertions.assertEquals(COMPRESSION, filePublishInformation.getCompression());
        Assertions.assertEquals(FILE_FORMAT_TYPE, filePublishInformation.getFileFormatType());
        Assertions.assertEquals(FILE_FORMAT_VERSION, filePublishInformation.getFileFormatVersion());
    }
}
