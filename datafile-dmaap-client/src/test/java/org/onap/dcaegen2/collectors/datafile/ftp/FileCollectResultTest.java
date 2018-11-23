/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.ftp;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

public class FileCollectResultTest {

    @Test
    public void successfulResult() {
        FileCollectResult resultUnderTest = new FileCollectResult();
        assertTrue(resultUnderTest.downloadSuccessful());
        assertEquals("FileCollectResult: successful!", resultUnderTest.toString());
    }

    @Test
    public void unSuccessfulResult() {
        ErrorData errorData = new ErrorData();
        errorData.addError("Error", null);
        errorData.addError("Null", new NullPointerException());
        FileCollectResult resultUnderTest = new FileCollectResult(errorData);
        assertFalse(resultUnderTest.downloadSuccessful());
        assertEquals("FileCollectResult: unsuccessful! Error data: " + errorData.toString(),
                resultUnderTest.toString());
    }
}
