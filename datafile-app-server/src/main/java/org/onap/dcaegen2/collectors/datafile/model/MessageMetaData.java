/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018-2019 Nordix Foundation. All rights reserved.
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

import org.immutables.gson.Gson;
import org.immutables.value.Value;

/**
 * Meta data about a fileReady message.
 *
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
@Value.Immutable
@Gson.TypeAdapters
public interface MessageMetaData {
    public String productName();

    public String vendorName();

    public String lastEpochMicrosec();

    public String sourceName();

    public String startEpochMicrosec();

    public String timeZoneOffset();

    public String changeIdentifier();

    public String changeType();
}
