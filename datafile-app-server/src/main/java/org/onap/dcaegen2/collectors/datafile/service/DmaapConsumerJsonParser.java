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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.onap.dcaegen2.collectors.datafile.exceptions.DmaapEmptyResponseException;
import org.onap.dcaegen2.collectors.datafile.exceptions.DmaapNotFoundException;
import org.onap.dcaegen2.collectors.datafile.model.FileData;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFileData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Parses the fileReady event and creates an array of FileData containing the information.
 *
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 5/8/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class DmaapConsumerJsonParser {
    private static final Logger logger = LoggerFactory.getLogger(DmaapConsumerJsonParser.class);

    private static final String EVENT = "event";
    private static final String NOTIFICATION_FIELDS = "notificationFields";
    private static final String CHANGE_IDENTIFIER = "changeIdentifier";
    private static final String CHANGE_TYPE = "changeType";
    private static final String NOTIFICATION_FIELDS_VERSION = "notificationFieldsVersion";

    private static final String ARRAY_OF_NAMED_HASH_MAP = "arrayOfNamedHashMap";
    private static final String NAME = "name";
    private static final String HASH_MAP = "hashMap";
    private static final String LOCATION = "location";
    private static final String COMPRESSION = "compression";
    private static final String FILE_FORMAT_TYPE = "fileFormatType";
    private static final String FILE_FORMAT_VERSION = "fileFormatVersion";

    /**
     * Extract info from string and create @see {@link FileData}.
     *
     * @param rawMessage - results from DMaaP
     * @return reactive Mono with an array of FileData
     */
    public Flux<FileData> getJsonObject(Mono<String> rawMessage) {
        return rawMessage.flatMap(this::getJsonParserMessage).flatMapMany(this::createJsonConsumerModel);
    }

    private Mono<JsonElement> getJsonParserMessage(String message) {
        logger.trace("original message from message router: {}", message);
        return StringUtils.isEmpty(message) ? Mono.error(new DmaapEmptyResponseException())
            : Mono.fromSupplier(() -> new JsonParser().parse(message));
    }

    private Flux<FileData> createJsonConsumerModel(JsonElement jsonElement) {
        return jsonElement.isJsonObject() ? create(Mono.fromSupplier(jsonElement::getAsJsonObject))
            : getFileDataFromJsonArray(jsonElement);
    }

    private Flux<FileData> getFileDataFromJsonArray(JsonElement jsonElement) {
        return create(Mono.fromCallable(() -> StreamSupport.stream(jsonElement.getAsJsonArray().spliterator(), false)
            .findFirst().flatMap(this::getJsonObjectFromAnArray).orElseThrow(DmaapEmptyResponseException::new)));
    }

    public Optional<JsonObject> getJsonObjectFromAnArray(JsonElement element) {
        logger.trace("starting to getJsonObjectFromAnArray!");

        return Optional.of(new JsonParser().parse(element.getAsString()).getAsJsonObject());
    }

    private Flux<FileData> create(Mono<JsonObject> jsonObject) {
        return jsonObject.flatMapMany(monoJsonP -> !containsHeader(monoJsonP)
            ? Flux.error(new DmaapNotFoundException("Incorrect JsonObject - missing header"))
            : transform(monoJsonP));
    }

    private Flux<FileData> transform(JsonObject jsonObject) {
        if (containsHeader(jsonObject, EVENT, NOTIFICATION_FIELDS)) {
            JsonObject notificationFields = jsonObject.getAsJsonObject(EVENT).getAsJsonObject(NOTIFICATION_FIELDS);
            String changeIdentifier = getValueFromJson(notificationFields, CHANGE_IDENTIFIER);
            String changeType = getValueFromJson(notificationFields, CHANGE_TYPE);
            String notificationFieldsVersion = getValueFromJson(notificationFields, NOTIFICATION_FIELDS_VERSION);
            JsonArray arrayOfNamedHashMap = notificationFields.getAsJsonArray(ARRAY_OF_NAMED_HASH_MAP);
            if (isNotificationFieldsHeaderNotEmpty(changeIdentifier, changeType, notificationFieldsVersion)
                && arrayOfNamedHashMap != null && isChangeIdentifierCorrect(changeIdentifier)
                && isChangeTypeCorrect(changeType)) {
                return getAllFileDataFromJson(changeIdentifier, changeType, arrayOfNamedHashMap);
            }

            if (!isNotificationFieldsHeaderNotEmpty(changeIdentifier, changeType, notificationFieldsVersion)) {
                return Flux.error(
                    new DmaapNotFoundException("FileReady event header is missing information. " + jsonObject));
            }

            if (arrayOfNamedHashMap == null) {
                return Flux.error(
                    new DmaapNotFoundException("FileReady event arrayOfNamedHashMap is missing. " + jsonObject));
            }

            if (!isChangeIdentifierCorrect(changeIdentifier)) {
                return Flux.error(
                        new DmaapNotFoundException("FileReady event changeIdentifier is incorrect. " + jsonObject));
            }

            if (!isChangeTypeCorrect(changeType)) {
                return Flux.error(
                        new DmaapNotFoundException("FileReady event changeType is incorrect. " + jsonObject));
            }

            return Flux.error(
                new DmaapNotFoundException("FileReady event does not contain correct information. " + jsonObject));
        }
        return Flux.error(
            new DmaapNotFoundException("FileReady event has incorrect JsonObject - missing header. " + jsonObject));
    }

    private boolean isChangeTypeCorrect(String changeType) {
        return "FileReady".equals(changeType);
    }

    private boolean isChangeIdentifierCorrect(String changeIdentifier) {
        return "PM_MEAS_FILES".equals(changeIdentifier);
    }

    private Flux<FileData> getAllFileDataFromJson(String changeIdentifier, String changeType,
        JsonArray arrayOfAdditionalFields) {
        List<FileData> res = new ArrayList<>();
        for (int i = 0; i < arrayOfAdditionalFields.size(); i++) {
            if (arrayOfAdditionalFields.get(i) != null) {
                JsonObject fileInfo = (JsonObject) arrayOfAdditionalFields.get(i);
                FileData fileData = getFileDataFromJson(fileInfo, changeIdentifier, changeType);

                if (fileData != null) {
                    res.add(fileData);
                } else {
                    logger.error("Unable to collect file from xNF. File information wrong. Data: {}", fileInfo);
                }
            }
        }
        return Flux.fromIterable(res);
    }

    private FileData getFileDataFromJson(JsonObject fileInfo, String changeIdentifier, String changeType) {
        logger.trace("starting to getFileDataFromJson!");

        FileData fileData = null;

        String name = getValueFromJson(fileInfo, NAME);
        JsonObject data = fileInfo.getAsJsonObject(HASH_MAP);
        String fileFormatType = getValueFromJson(data, FILE_FORMAT_TYPE);
        String fileFormatVersion = getValueFromJson(data, FILE_FORMAT_VERSION);
        String location = getValueFromJson(data, LOCATION);
        String compression = getValueFromJson(data, COMPRESSION);

        if (isFileFormatFieldsNotEmpty(fileFormatVersion, fileFormatType)
            && isNameAndLocationAndCompressionNotEmpty(name, location, compression)) {
            fileData = ImmutableFileData.builder().name(name).changeIdentifier(changeIdentifier).changeType(changeType)
                .location(location).compression(compression).fileFormatType(fileFormatType)
                .fileFormatVersion(fileFormatVersion).build();
        }
        return fileData;
    }

    private String getValueFromJson(JsonObject jsonObject, String jsonKey) {
        return jsonObject.has(jsonKey) ? jsonObject.get(jsonKey).getAsString() : "";
    }

    private boolean isNotificationFieldsHeaderNotEmpty(String changeIdentifier, String changeType,
        String notificationFieldsVersion) {
        return isStringIsNotNullAndNotEmpty(changeIdentifier) && isStringIsNotNullAndNotEmpty(changeType)
            && isStringIsNotNullAndNotEmpty(notificationFieldsVersion);
    }

    private boolean isFileFormatFieldsNotEmpty(String fileFormatVersion, String fileFormatType) {
        return isStringIsNotNullAndNotEmpty(fileFormatVersion) && isStringIsNotNullAndNotEmpty(fileFormatType);
    }

    private boolean isNameAndLocationAndCompressionNotEmpty(String name, String location, String compression) {
        return isStringIsNotNullAndNotEmpty(name) && isStringIsNotNullAndNotEmpty(location) &&
                isStringIsNotNullAndNotEmpty(compression);
    }

    private boolean containsHeader(JsonObject jsonObject) {
        return jsonObject.has(EVENT) && jsonObject.getAsJsonObject(EVENT).has(NOTIFICATION_FIELDS);
    }

    private boolean containsHeader(JsonObject jsonObject, String topHeader, String header) {
        return jsonObject.has(topHeader) && jsonObject.getAsJsonObject(topHeader).has(header);
    }

    private boolean isStringIsNotNullAndNotEmpty(String string) {
        return string != null && !string.isEmpty();
    }
}
