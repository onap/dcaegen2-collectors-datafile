/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018-2019 Nordix Foundation. All rights reserved.
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

import com.google.common.collect.Sets;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Set;

/**
 * Helper class to serialize object.
 */
public abstract class JsonSerializer {

    private static Gson gson = new GsonBuilder() //
            .serializeNulls() //
            .addSerializationExclusionStrategy(new FilePublishInformationExclusionStrategy()) //
            .create(); //

    private JsonSerializer() {}

    /**
     * Serializes a <code>filePublishInformation</code>.
     *
     * @param filePublishInformation info to serialize.
     *
     * @return a string with the serialized info.
     */
    public static String createJsonBodyForDataRouter(FilePublishInformation filePublishInformation) {
        return gson.toJson(filePublishInformation);
    }

    private static class FilePublishInformationExclusionStrategy implements ExclusionStrategy {
        /**
         * Elements in FilePublishInformation to include in the file publishing Json string.
         */
        private final Set<String> inclusions =
                Sets.newHashSet("productName", "vendorName", "lastEpochMicrosec", "sourceName", "startEpochMicrosec",
                        "timeZoneOffset", "location", "compression", "fileFormatType", "fileFormatVersion");

        @Override
        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
            return !inclusions.contains(fieldAttributes.getName());
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }
}
