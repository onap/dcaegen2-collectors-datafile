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

package org.onap.dcaegen2.collectors.datafile.model;

import com.google.gson.annotations.SerializedName;

import org.immutables.gson.Gson;
import org.immutables.value.Value;
import org.onap.dcaegen2.services.sdk.rest.services.model.DmaapModel;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 5/8/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */

@Value.Immutable
@Gson.TypeAdapters
public interface ConsumerDmaapModel extends DmaapModel {

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

    @SerializedName("name")
    String getName();

    @SerializedName("location")
    String getLocation();

    @SerializedName("internalLocation")
    String getInternalLocation();

    @SerializedName("compression")
    String getCompression();

    @SerializedName("fileFormatType")
    String getFileFormatType();

    @SerializedName("fileFormatVersion")
    String getFileFormatVersion();


}
