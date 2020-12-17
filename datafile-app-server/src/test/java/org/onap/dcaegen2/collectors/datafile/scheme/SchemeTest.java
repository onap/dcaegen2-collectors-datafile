/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019 Nordix Foundation. All rights reserved.
 * Copyright (C) 2020 Nokia. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.scheme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.commons.Scheme;

public class SchemeTest {

    @Test
    public void shouldReturnSchemeForSupportedProtocol() throws DatafileTaskException {
        Assertions.assertEquals(Scheme.FTPES, Scheme.getSchemeFromString("FTPES"));
        assertEquals(Scheme.SFTP, Scheme.getSchemeFromString("SFTP"));
        assertEquals(Scheme.HTTP, Scheme.getSchemeFromString("HTTP"));
    }

    @Test
    public void shouldThrowExceptionForUnsupportedProtocol() {
        assertThrows(DatafileTaskException.class, () -> Scheme.getSchemeFromString("FTPS"));
    }

    @Test
    public void shouldThrowExceptionForInvalidProtocol() {
        assertThrows(DatafileTaskException.class, () -> Scheme.getSchemeFromString("invalid"));
    }
}
