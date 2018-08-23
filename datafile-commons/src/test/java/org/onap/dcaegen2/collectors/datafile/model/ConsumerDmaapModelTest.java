/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018 Nordix Foundation. All rights reserved.
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableConsumerDmaapModel;

public class ConsumerDmaapModelTest {

    // Given
    private ConsumerDmaapModel consumerDmaapModel;
    private String location="target/A20161224.1030-1045.bin.gz";
    private String compression = "gzip";
    private String fileFormatType = "org.3GPP.32.435#measCollec";
    private String fileFormatVersion = "V10";

    @Test
    public void consumerDmaapModelBuilder_shouldBuildAnObject() {

        // When
        consumerDmaapModel = ImmutableConsumerDmaapModel.builder().location(location).compression(compression).fileFormatType(fileFormatType).fileFormatVersion(fileFormatVersion).build();

        // Then
        Assertions.assertNotNull(consumerDmaapModel);
        Assertions.assertEquals(location, consumerDmaapModel.getLocation());
        Assertions.assertEquals(compression, consumerDmaapModel.getCompression());
        Assertions.assertEquals(fileFormatType, consumerDmaapModel.getFileFormatType());
        Assertions.assertEquals(fileFormatVersion, consumerDmaapModel.getFileFormatVersion());
    }
}
