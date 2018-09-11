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

    // Given
    private ConsumerDmaapModel consumerDmaapModel;
    private final static String LOCATION = "target/A20161224.1030-1045.bin.gz";
    private final static String COMPRESSION = "gzip";
    private final static String FILE_FORMAT_TYPE = "org.3GPP.32.435#measCollec";
    private final static String FILE_FORMAT_VERSION = "V10";

    @Test
    public void consumerDmaapModelBuilder_shouldBuildAnObject() {

        // When
        consumerDmaapModel = ImmutableConsumerDmaapModel.builder().location(LOCATION).compression(COMPRESSION)
                .fileFormatType(FILE_FORMAT_TYPE).fileFormatVersion(FILE_FORMAT_VERSION).build();

        // Then
        Assertions.assertNotNull(consumerDmaapModel);
        Assertions.assertEquals(LOCATION, consumerDmaapModel.getLocation());
        Assertions.assertEquals(COMPRESSION, consumerDmaapModel.getCompression());
        Assertions.assertEquals(FILE_FORMAT_TYPE, consumerDmaapModel.getFileFormatType());
        Assertions.assertEquals(FILE_FORMAT_VERSION, consumerDmaapModel.getFileFormatVersion());
    }
}
