/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018 Nordix Foundation. All rights reserved.
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

import java.io.IOException;
import java.util.Optional;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.onap.dcaegen2.collectors.datafile.model.utils.HttpUtils;
import org.onap.dcaegen2.collectors.datafile.model.CommonFunctions;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class CommonFunctions {

    private static Logger logger = LoggerFactory.getLogger(CommonFunctions.class);

    private static Gson gson = new GsonBuilder().create();


    private CommonFunctions() {}

    public static String createJsonBody(ConsumerDmaapModel consumerDmaapModel) {
        return gson.toJson(consumerDmaapModel);
    }

    public static Optional<Integer> handleResponse(HttpResponse response) throws IOException {
        final Integer responseCode = response.getStatusLine().getStatusCode();
        logger.info("Status code of operation: {}", responseCode);
        final HttpEntity responseEntity = response.getEntity();

        if (HttpUtils.isSuccessfulResponseCode(responseCode)) {
            logger.trace("HTTP response successful.");
            return Optional.of(responseCode);
        } else {
            String aaiResponse = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
            logger.warn("HTTP response not successful : {}", aaiResponse);
            return Optional.of(responseCode);
        }
    }
}
