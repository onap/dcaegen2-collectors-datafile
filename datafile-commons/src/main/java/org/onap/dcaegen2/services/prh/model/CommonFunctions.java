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

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import java.util.ServiceLoader;


public class CommonFunctions {

    private CommonFunctions() {
    }

    /**
     * Method for serialization object by GSON.
     *
     * @param consumerDmaapModel - object which will be serialized
     * @return string from serialization
     */
    public static String createJsonBody(ConsumerDmaapModel consumerDmaapModel) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        ServiceLoader.load(TypeAdapterFactory.class).forEach(gsonBuilder::registerTypeAdapterFactory);
        return gsonBuilder.create().toJson(ImmutableConsumerDmaapModel.builder().ipv4(consumerDmaapModel.getIpv4())
            .ipv6(consumerDmaapModel.getIpv6()).pnfName(consumerDmaapModel.getPnfName()).build());
    }
}
