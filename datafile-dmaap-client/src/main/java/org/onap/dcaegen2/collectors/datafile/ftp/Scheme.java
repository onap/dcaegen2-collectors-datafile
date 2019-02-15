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

import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;

/**
 * Enum specifying the schemes that DFC support for downloading files.
 *
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 *
 */
public enum Scheme {
    FTPS, SFTP;

    /**
     * Get a <code>Scheme</code> from a string.
     *
     * @param schemeString the string to convert to <code>Scheme</code>.
     * @return The corresponding <code>Scheme</code>
     * @throws Exception if the value of the string doesn't match any defined scheme.
     */
    public static Scheme getSchemeFromString(String schemeString) throws DatafileTaskException {
        Scheme result;
        if ("FTPS".equalsIgnoreCase(schemeString) || "FTPES".equalsIgnoreCase(schemeString)) {
            result = Scheme.FTPS;
        } else if ("SFTP".equalsIgnoreCase(schemeString)) {
            result = Scheme.SFTP;
        } else {
            throw new DatafileTaskException("DFC does not support protocol " + schemeString
                    + ". Supported protocols are FTPES , FTPS, and SFTP");
        }
        return result;
    }
}
