/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018 Nordix Foundation. All rights reserved.
 * ===============================================================================================
 * Modifications Copyright (c) 2020 Nordix
 * ===============================================================================================
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
 * ============LICENSE_END========================================================================
 */

package org.onap.dcaegen2.collectors.datafile.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@OpenAPIDefinition(
    info = @Info(title = SwaggerConfig.API_TITLE, version = SwaggerConfig.VERSION,
        description = SwaggerConfig.DESCRIPTION, license = @License(name = "Copyright (C) 2020 Nordix Foundation. Licensed under the Apache License.",
        url = "http://www.apache.org/licenses/LICENSE-2.0"))
)
public class SwaggerConfig {

    public static final String VERSION = "1.0";
    public static final String API_TITLE = "DATAFILE App Server";
    static final String DESCRIPTION = "<p>This page lists all the rest apis for DATAFILE app server.</p>";

    private SwaggerConfig() {
    }
}
