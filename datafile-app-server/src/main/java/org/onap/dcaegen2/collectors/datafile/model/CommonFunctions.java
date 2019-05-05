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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.nio.file.Path;

/**
 * Helper class to serialize object.
 */
public class CommonFunctions {

    private static Gson gson =
        new GsonBuilder().registerTypeHierarchyAdapter(Path.class, new PathConverter()).serializeNulls().create();

    private CommonFunctions() {
    }

    /**
     * Serializes a <code>filePublishInformation</code>.
     *
     * @param filePublishInformation info to serialize.
     *
     * @return a string with the serialized info.
     */
    public static String createJsonBody(FilePublishInformation filePublishInformation) {
        return gson.toJson(filePublishInformation);
    }

    /**
     * Json serializer that handles Path serializations, since <code>Path</code> does not implement the
     * <code>Serializable</code> interface.
     */
    public static class PathConverter implements JsonSerializer<Path> {
        @Override
        public JsonElement serialize(Path path, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(path.toString());
        }
    }
}