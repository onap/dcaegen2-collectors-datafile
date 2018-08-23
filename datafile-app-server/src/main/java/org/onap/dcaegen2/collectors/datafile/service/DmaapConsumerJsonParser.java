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
package org.onap.dcaegen2.collectors.datafile.service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.onap.dcaegen2.collectors.datafile.exceptions.DmaapNotFoundException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Parses the fileReady event and creates a Dmaap model containing the information.
 *
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 5/8/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class DmaapConsumerJsonParser {

    private static final String EVENT = "event";
    private static final String NOTIFICATION_FIELDS = "notificationFields";
    private static final String CHANGE_IDENTIFIER = "changeIdentifier";
    private static final String CHANGE_TYPE = "changeType";
    private static final String NOTIFICATION_FIELDS_VERSION = "notificationFieldsVersion";

    private static final String LOCATION = "location";
    private static final String COMPRESSION = "compression";
    private static final String FILE_FORMAT_TYPE = "fileFormatType";
    private static final String FILE_FORMAT_VERSION = "fileFormatVersion";

    public ArrayList<FileData> getJsonObject(String message) throws DmaapNotFoundException {
        JsonElement jsonElement = new JsonParser().parse(message);
        if (jsonElement.isJsonNull()) {
            throw new DmaapNotFoundException("Json object is null");
        }
        if (jsonElement.isJsonObject()) {
            return create(jsonElement.getAsJsonObject());
        } else {
            return create(StreamSupport.stream(jsonElement.getAsJsonArray().spliterator(), false).findFirst()
                    .flatMap(this::getJsonObjectFromAnArray)
                    .orElseThrow(() -> new DmaapNotFoundException("Json object not found in json array")));
        }
    }

    public Optional<JsonObject> getJsonObjectFromAnArray(JsonElement element) {
        return Optional.of(new JsonParser().parse(element.getAsString()).getAsJsonObject());
    }

    private ArrayList<FileData> create(JsonObject jsonObject) throws DmaapNotFoundException {
        if (containsHeader(jsonObject, EVENT, NOTIFICATION_FIELDS)) {
            JsonObject notificationFields = jsonObject.getAsJsonObject(EVENT).getAsJsonObject(NOTIFICATION_FIELDS);
            String changeIdentifier = getValueFromJson(notificationFields, CHANGE_IDENTIFIER);
            String changeType = getValueFromJson(notificationFields, CHANGE_TYPE);
            String notificationFieldsVersion = getValueFromJson(notificationFields, NOTIFICATION_FIELDS_VERSION);
            JsonArray arrayOfAdditionalFields = notificationFields.getAsJsonArray("arrayOfAdditionalFields");

            if (isNotificationFieldsHeaderNotEmpty(changeIdentifier, changeType, notificationFieldsVersion)
                    && arrayOfAdditionalFields != null) {
                ArrayList<FileData> res = getFileDataFromJson(changeIdentifier, changeType, arrayOfAdditionalFields);
                return res;
            }

            if (!isNotificationFieldsHeaderNotEmpty(changeIdentifier, changeType, notificationFieldsVersion)) {
                throw new DmaapNotFoundException("FileReady event header is missing information. " + jsonObject);
            } else if (arrayOfAdditionalFields != null) {
                throw new DmaapNotFoundException("FileReady event arrayOfAdditionalFields is missing. " + jsonObject);
            }
            throw new DmaapNotFoundException("FileReady event does not contain correct information. " + jsonObject);
        }
        throw new DmaapNotFoundException("FileReady event has incorrect JsonObject - missing header. " + jsonObject);

    }

    private ArrayList<FileData> getFileDataFromJson(String changeIdentifier, String changeType, JsonArray arrayOfAdditionalFields) throws DmaapNotFoundException {
        ArrayList<FileData> res = new ArrayList<>();
        for (int i = 0; i < arrayOfAdditionalFields.size(); i++) {
            if (arrayOfAdditionalFields.get(i) != null) {
                JsonObject fileInfo = (JsonObject) arrayOfAdditionalFields.get(i);
                String fileFormatType = getValueFromJson(fileInfo, FILE_FORMAT_TYPE);
                String fileFormatVersion = getValueFromJson(fileInfo, FILE_FORMAT_VERSION);
                String location = getValueFromJson(fileInfo, LOCATION);
                String compression = getValueFromJson(fileInfo, COMPRESSION);
                if (isFileFormatFieldsNotEmpty(fileFormatVersion, fileFormatType)
                        && isLocationAndCompressionNotEmpty(location, compression)) {
                    res.add(new FileData(changeIdentifier, changeType, location, compression, fileFormatType,
                            fileFormatVersion));
                } else {
                    throw new DmaapNotFoundException(
                            "FileReady event does not contain correct file format information. " + fileInfo);
                }
            }
        }
        return res;
    }

    private String getValueFromJson(JsonObject jsonObject, String jsonKey) {
        if (jsonObject.has(jsonKey)) {
            return jsonObject.get(jsonKey).isJsonNull() ? "" : jsonObject.get(jsonKey).getAsString();
        } else {
            return "";
        }
    }

    private boolean isNotificationFieldsHeaderNotEmpty(String changeIdentifier, String changeType,
            String notificationFieldsVersion) {
        return ((changeIdentifier != null && !changeIdentifier.isEmpty())
                && (changeType != null && !changeType.isEmpty())
                && (notificationFieldsVersion != null && !notificationFieldsVersion.isEmpty()));
    }

    private boolean isFileFormatFieldsNotEmpty(String fileFormatVersion, String fileFormatType) {
        return ((fileFormatVersion != null && !fileFormatVersion.isEmpty())
                && (fileFormatType != null && !fileFormatType.isEmpty()));
    }

    private boolean isLocationAndCompressionNotEmpty(String location, String compression) {
        return (location != null && !location.isEmpty()) && (compression != null && !compression.isEmpty());
    }

    private boolean containsHeader(JsonObject jsonObject) {
        return jsonObject.has(EVENT) && jsonObject.getAsJsonObject(EVENT).has(NOTIFICATION_FIELDS);
    }

    private boolean containsHeader(JsonObject jsonObject, String topHeader, String header) {
        return jsonObject.has(topHeader) && jsonObject.getAsJsonObject(topHeader).has(header);
    }
}
