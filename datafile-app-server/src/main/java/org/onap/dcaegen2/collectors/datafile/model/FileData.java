/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.model;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

/**
 * Contains data, from the fileReady event, about the file to collect from the xNF.
 *
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
@Value.Immutable
@Gson.TypeAdapters
public interface FileData {
    String productName();

    String vendorName();

    String lastEpochMicrosec();

    String sourceName();

    String startEpochMicrosec();

    String timeZoneOffset();

    String changeIdentifier();

    String changeType();

    String name();

    String location();

    String compression();

    String fileFormatType();

    String fileFormatVersion();
}
