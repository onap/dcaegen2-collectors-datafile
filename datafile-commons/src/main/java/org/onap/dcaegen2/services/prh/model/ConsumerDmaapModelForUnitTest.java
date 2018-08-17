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

package org.onap.dcaegen2.services.prh.model;

public class ConsumerDmaapModelForUnitTest implements ConsumerDmaapModel {

    private final String pnfName;
    private final String ipv4;
    private final String ipv6;

    /**
     * Class for testing serialization of ConsumerDmaapModel.
     */
    public ConsumerDmaapModelForUnitTest() {
        this.pnfName = "NOKnhfsadhff";
        this.ipv4 = "256.22.33.155";
        this.ipv6 = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";

    }

    public String getPnfName() {
        return pnfName;
    }

    public String getIpv4() {
        return ipv4;
    }

    public String getIpv6() {
        return ipv6;
    }

}
