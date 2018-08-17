/*
 * ============LICENSE_START=======================================================
 * PNF-REGISTRATION-HANDLER
 * ================================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property. All rights reserved.
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

import com.google.gson.annotations.SerializedName;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 5/8/18
 */

@Value.Immutable
@Gson.TypeAdapters(fieldNamingStrategy = true)
public interface ConsumerDmaapModel {

    @SerializedName(value = "pnf-name", alternate = "pnf-name")
    String getPnfName();

    @SerializedName(value = "ipaddress-v4-oam", alternate = "ipaddress-v4-oam")
    String getIpv4();

    @SerializedName(value = "ipaddress-v6-oam", alternate = "ipaddress-v6-oam")
    String getIpv6();
}
