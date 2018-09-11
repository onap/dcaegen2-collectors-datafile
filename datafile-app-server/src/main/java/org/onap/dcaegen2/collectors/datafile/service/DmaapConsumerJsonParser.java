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

package org.onap.dcaegen2.collectors.datafile.service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.onap.dcaegen2.collectors.datafile.exceptions.DmaapEmptyResponseException;
import org.onap.dcaegen2.collectors.datafile.exceptions.DmaapNotFoundException;
import org.springframework.util.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import reactor.core.publisher.Mono;

/**
 * Parses the fileReady event and creates an array of FileData containing the information.
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

    /**
     * Extract info from string and create @see
     * {@link org.onap.dcaegen2.collectors.datafile.service.FileData}.
     *
     * @param monoMessage - results from DMaaP
     * @return reactive Mono with an array of FileData
     */
    public Mono<ArrayList<FileData>> getJsonObject(Mono<String> monoMessage) {
        return monoMessage.flatMap(this::getJsonParserMessage).flatMap(this::createJsonConsumerModel);
    }

    private Mono<JsonElement> getJsonParserMessage(String message) {
        return StringUtils.isEmpty(message) ? Mono.error(new DmaapEmptyResponseException())
                : Mono.fromSupplier(() -> new JsonParser().parse(message));
    }

    private Mono<ArrayList<FileData>> createJsonConsumerModel(JsonElement jsonElement) {
        return jsonElement.isJsonObject() ? create(Mono.fromSupplier(jsonElement::getAsJsonObject))
                : getFileDataFromJsonArray(jsonElement);
    }

    private Mono<ArrayList<FileData>> getFileDataFromJsonArray(JsonElement jsonElement) {
        return create(Mono.fromCallable(() -> StreamSupport.stream(jsonElement.getAsJsonArray().spliterator(), false)
                .findFirst().flatMap(this::getJsonObjectFromAnArray).orElseThrow(DmaapEmptyResponseException::new)));
    }

    public Optional<JsonObject> getJsonObjectFromAnArray(JsonElement element) {
        return Optional.of(new JsonParser().parse(element.getAsString()).getAsJsonObject());
    }

    private Mono<ArrayList<FileData>> create(Mono<JsonObject> jsonObject) {
        return jsonObject.flatMap(monoJsonP -> !containsHeader(monoJsonP)
                ? Mono.error(new DmaapNotFoundException("Incorrect JsonObject - missing header"))
                : transform(monoJsonP));
    }

    private Mono<ArrayList<FileData>> transform(JsonObject jsonObject) {
        if (containsHeader(jsonObject, EVENT, NOTIFICATION_FIELDS)) {
            JsonObject notificationFields = jsonObject.getAsJsonObject(EVENT).getAsJsonObject(NOTIFICATION_FIELDS);
            String changeIdentifier = getValueFromJson(notificationFields, CHANGE_IDENTIFIER);
            String changeType = getValueFromJson(notificationFields, CHANGE_TYPE);
            String notificationFieldsVersion = getValueFromJson(notificationFields, NOTIFICATION_FIELDS_VERSION);
            JsonArray arrayOfAdditionalFields = notificationFields.getAsJsonArray("arrayOfAdditionalFields");

            if (isNotificationFieldsHeaderNotEmpty(changeIdentifier, changeType, notificationFieldsVersion)
                    && arrayOfAdditionalFields != null) {
                Mono<ArrayList<FileData>> res =
                        getFileDataFromJson(changeIdentifier, changeType, arrayOfAdditionalFields);
                return res;
            }

            if (!isNotificationFieldsHeaderNotEmpty(changeIdentifier, changeType, notificationFieldsVersion)) {
                return Mono.error(
                        new DmaapNotFoundException("FileReady event header is missing information. " + jsonObject));
            } else if (arrayOfAdditionalFields != null) {
                return Mono.error(new DmaapNotFoundException(
                        "FileReady event arrayOfAdditionalFields is missing. " + jsonObject));
            }
            return Mono.error(
                    new DmaapNotFoundException("FileReady event does not contain correct information. " + jsonObject));
        }
        return Mono.error(
                new DmaapNotFoundException("FileReady event has incorrect JsonObject - missing header. " + jsonObject));

    }

    private Mono<ArrayList<FileData>> getFileDataFromJson(String changeIdentifier, String changeType,
            JsonArray arrayOfAdditionalFields) {
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
                    res.add(ImmutableFileData.builder().changeIdentifier(changeIdentifier).changeType(changeType)
                            .location(location).compression(compression).fileFormatType(fileFormatType)
                            .fileFormatVersion(fileFormatVersion).build());
                } else {
                    return Mono.error(new DmaapNotFoundException(
                            "FileReady event does not contain correct file format information. " + fileInfo));
                }
            }
        }
        return Mono.just(res);
    }

    private String getValueFromJson(JsonObject jsonObject, String jsonKey) {
        return jsonObject.has(jsonKey) ? jsonObject.get(jsonKey).getAsString() : "";
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
