/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018-2019 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.model;

import com.google.gson.annotations.SerializedName;

import java.nio.file.Path;
import java.util.Map;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

/**
 * Information needed to publish a file to DataRouter.
 *
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 5/8/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */

@Value.Immutable
@Gson.TypeAdapters
@Value.Style(redactedMask = "####")
public interface FilePublishInformation {

    @SerializedName("productName")
    String getProductName();

    @SerializedName("vendorName")
    String getVendorName();

    @SerializedName("lastEpochMicrosec")
    String getLastEpochMicrosec();

    @SerializedName("sourceName")
    String getSourceName();

    @SerializedName("startEpochMicrosec")
    String getStartEpochMicrosec();

    @SerializedName("timeZoneOffset")
    String getTimeZoneOffset();

    @SerializedName("location")
    @Value.Redacted
    String getLocation();

    @SerializedName("compression")
    String getCompression();

    @SerializedName("fileFormatType")
    String getFileFormatType();

    @SerializedName("fileFormatVersion")
    String getFileFormatVersion();

    Path getInternalLocation();

    String getName();

    Map<String, String> getContext();

    String getChangeIdentifier();
}
