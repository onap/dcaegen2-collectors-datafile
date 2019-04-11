/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.ftp;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;

public class SchemeTest {
    @Test
    public void getSchemeFromString_properScheme() throws DatafileTaskException {

        Scheme actualScheme = Scheme.getSchemeFromString("FTPES");
        assertEquals(Scheme.FTPS, actualScheme);

        actualScheme = Scheme.getSchemeFromString("FTPS");
        assertEquals(Scheme.FTPS, actualScheme);

        actualScheme = Scheme.getSchemeFromString("SFTP");
        assertEquals(Scheme.SFTP, actualScheme);
    }

    @Test
    public void getSchemeFromString_invalidScheme() {
        assertTrue(assertThrows(DatafileTaskException.class, () -> Scheme.getSchemeFromString("invalid")).getMessage()
            .startsWith("DFC does not support protocol invalid"));
    }
}
