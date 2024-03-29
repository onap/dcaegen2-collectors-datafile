/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018-2022 Nokia. All rights reserved.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.dcaegen2.collectors.datafile.configuration;

import java.io.Serializable;

import org.immutables.gson.Gson;
import org.immutables.value.Value;
import org.springframework.stereotype.Component;

@Component
@Value.Immutable
@Value.Style(builder = "new", redactedMask = "####")
@Gson.TypeAdapters
public abstract class CertificateConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Value.Parameter
    public abstract String keyCert();

    @Value.Parameter
    @Value.Redacted
    public abstract String keyPasswordPath();

    @Value.Parameter
    public abstract String trustedCa();

    @Value.Parameter
    @Value.Redacted
    public abstract String trustedCaPasswordPath();

    @Value.Parameter
    public abstract Boolean httpsHostnameVerify();

    @Value.Parameter
    public abstract Boolean enableCertAuth();
}
