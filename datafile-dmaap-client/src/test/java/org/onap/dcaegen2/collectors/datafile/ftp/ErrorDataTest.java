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

package org.onap.dcaegen2.collectors.datafile.ftp;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ErrorDataTest {
    private List<String> errorMessages = new ArrayList<>();
    private List<Throwable> errorCauses = new ArrayList<>();
    private ErrorData errorData = new ErrorData();

    @BeforeEach
    protected void setUp() {
        int testSize = 3;
        for (int i = 0; i < testSize; i++) {
            errorMessages.add("test");
            errorCauses.add(mock(Throwable.class));
        }
        for (int i = 0; i < testSize; i++) {
            errorData.addError(errorMessages.get(i), errorCauses.get(i));
        }
    }

    public String getMessageAsString() {
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < errorMessages.size(); i++) {
            message.append(errorMessages.get(i)).append(" Cause: ").append(errorCauses.get(i)).append("\n");
        }
        return message.toString();
    }

    @Test
    public void testToString_returnExpectedString() {
        Assertions.assertEquals(getMessageAsString(), errorData.toString());
    }
}
