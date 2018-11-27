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
import org.onap.dcaegen2.collectors.datafile.model.FileMetaData;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFileData;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFileMetaData;
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

    private static final String COMMON_EVENT_HEADER = "commonEventHeader";
    private static final String EVENT_NAME = "eventName";
    private static final String LAST_EPOCH_MICROSEC = "lastEpochMicrosec";
    private static final String SOURCE_NAME = "sourceName";
    private static final String START_EPOCH_MICROSEC = "startEpochMicrosec";
    private static final String TIME_ZONE_OFFSET = "timeZoneOffset";

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

    private static final String FILE_READY_CHANGE_TYPE = "FileReady";
    private static final String FILE_READY_CHANGE_IDENTIFIER = "PM_MEAS_FILES";

    /**
     * The data types available in the event name.
     */
    private enum EventNameDataType {
        PRODUCT_NAME(1), VENDOR_NAME(2);

        private int index;

        EventNameDataType(int index) {
            this.index = index;
        }
    }

    /**
     * Extract info from string and create a {@link FileData}.
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
        return jsonObject.flatMapMany(monoJsonP -> !containsNotificationFields(monoJsonP)
                ? Flux.error(new DmaapNotFoundException("Incorrect JsonObject - missing header. " + jsonObject))
                : transform(monoJsonP));
    }

    private Flux<FileData> transform(JsonObject message) {
        Optional<FileMetaData> fileMetaData = getFileMetaData(message);
        if (fileMetaData.isPresent()) {
            JsonObject notificationFields = message.getAsJsonObject(EVENT).getAsJsonObject(NOTIFICATION_FIELDS);
            JsonArray arrayOfNamedHashMap = notificationFields.getAsJsonArray(ARRAY_OF_NAMED_HASH_MAP);
            if (arrayOfNamedHashMap != null) {
                return getAllFileDataFromJson(fileMetaData.get(), arrayOfNamedHashMap);
            }

            return Flux.error(new DmaapNotFoundException(
                    "Unable to collect file from xNF. Missing arrayOfNamedHashMap in message. " + message));
        }
        return Flux.error(new DmaapNotFoundException(
                "Unable to collect file from xNF. FileReady event has incorrect JsonObject"));
    }

    private Optional<FileMetaData> getFileMetaData(JsonObject message) {
        List<String> missingValues = new ArrayList<>();
        JsonObject commonEventHeader = message.getAsJsonObject(EVENT).getAsJsonObject(COMMON_EVENT_HEADER);
        String eventName = getValueFromJson(commonEventHeader, EVENT_NAME, missingValues);

        JsonObject notificationFields = message.getAsJsonObject(EVENT).getAsJsonObject(NOTIFICATION_FIELDS);
        String changeIdentifier = getValueFromJson(notificationFields, CHANGE_IDENTIFIER, missingValues);
        String changeType = getValueFromJson(notificationFields, CHANGE_TYPE, missingValues);

        // Just to check that it is in the message. Might be needed in the future if there is a new
        // version.
        getValueFromJson(notificationFields, NOTIFICATION_FIELDS_VERSION, missingValues);

        // @formatter:off
        FileMetaData fileMetaData = ImmutableFileMetaData.builder()
                .productName(getDataFromEventName(EventNameDataType.PRODUCT_NAME, eventName, missingValues))
                .vendorName(getDataFromEventName(EventNameDataType.VENDOR_NAME, eventName, missingValues))
                .lastEpochMicrosec(getValueFromJson(commonEventHeader, LAST_EPOCH_MICROSEC, missingValues))
                .sourceName(getValueFromJson(commonEventHeader, SOURCE_NAME, missingValues))
                .startEpochMicrosec(getValueFromJson(commonEventHeader, START_EPOCH_MICROSEC, missingValues))
                .timeZoneOffset(getValueFromJson(commonEventHeader, TIME_ZONE_OFFSET, missingValues))
                .changeIdentifier(changeIdentifier)
                .changeType(changeType)
                .build();
        // @formatter:on
        if (missingValues.isEmpty() && isChangeIdentifierCorrect(changeIdentifier) && isChangeTypeCorrect(changeType)) {
            return Optional.of(fileMetaData);
        } else {
            String errorMessage = "Unable to collect file from xNF.";
            if (!missingValues.isEmpty()) {
                errorMessage += " Missing data: " + missingValues;
            }
            if (!isChangeIdentifierCorrect(changeIdentifier) || !isChangeTypeCorrect(changeType)) {
                errorMessage += " Change identifier or change type is wrong.";
            }
            errorMessage += " Message: {}";
            logger.error(errorMessage, message);
            return Optional.empty();
        }
    }

    private boolean isChangeTypeCorrect(String changeType) {
        return FILE_READY_CHANGE_TYPE.equals(changeType);
    }

    private boolean isChangeIdentifierCorrect(String changeIdentifier) {
        return FILE_READY_CHANGE_IDENTIFIER.equals(changeIdentifier);
    }

    private Flux<FileData> getAllFileDataFromJson(FileMetaData fileMetaData, JsonArray arrayOfAdditionalFields) {
        List<FileData> res = new ArrayList<>();
        for (int i = 0; i < arrayOfAdditionalFields.size(); i++) {
            if (arrayOfAdditionalFields.get(i) != null) {
                JsonObject fileInfo = (JsonObject) arrayOfAdditionalFields.get(i);
                Optional<FileData> fileData = getFileDataFromJson(fileMetaData, fileInfo);

                if (fileData.isPresent()) {
                    res.add(fileData.get());
                }
            }
        }
        return Flux.fromIterable(res);
    }

    private Optional<FileData> getFileDataFromJson(FileMetaData fileMetaData, JsonObject fileInfo) {
        logger.trace("starting to getFileDataFromJson!");

        List<String> missingValues = new ArrayList<>();
        JsonObject data = fileInfo.getAsJsonObject(HASH_MAP);

        // @formatter:off
        FileData fileData = ImmutableFileData.builder()
                .fileMetaData(fileMetaData)
                .name(getValueFromJson(fileInfo, NAME, missingValues))
                .fileFormatType(getValueFromJson(data, FILE_FORMAT_TYPE, missingValues))
                .fileFormatVersion(getValueFromJson(data, FILE_FORMAT_VERSION, missingValues))
                .location(getValueFromJson(data, LOCATION, missingValues))
                .compression(getValueFromJson(data, COMPRESSION, missingValues))
                .build();
        // @formatter:on
        if (missingValues.isEmpty()) {
            return Optional.of(fileData);
        }
        logger.error("Unable to collect file from xNF. File information wrong. Missing data: {} Data: {}",
                missingValues, fileInfo);
        return Optional.empty();
    }

    /**
     * Gets data from the event name, defined as:
     * {DomainAbbreviation}_{productName}-{vendorName}_{Description}, example:
     * Noti_RnNode-Ericsson_FileReady
     *
     * @param dataType The type of data to get, {@link DmaapConsumerJsonParser.EventNameDataType}.
     * @param eventName The event name to get the data from.
     * @param missingValues List of missing values. The dataType will be added if missing.
     * @return String of data from event name
     */
    private String getDataFromEventName(EventNameDataType dataType, String eventName, List<String> missingValues) {
        String[] eventArray = eventName.split("_|-");
        if (eventArray.length >= 4) {
            return eventArray[dataType.index];
        } else {
            missingValues.add(dataType.toString());
            logger.error("Can not get {} from eventName, eventName is not in correct format: {}", dataType, eventName);
        }
        return "";
    }

    private String getValueFromJson(JsonObject jsonObject, String jsonKey, List<String> missingValues) {
        if (jsonObject.has(jsonKey)) {
            return jsonObject.get(jsonKey).getAsString();
        } else {
            missingValues.add(jsonKey);
            return "";
        }
    }

    private boolean containsNotificationFields(JsonObject jsonObject) {
        return jsonObject.has(EVENT) && jsonObject.getAsJsonObject(EVENT).has(NOTIFICATION_FIELDS);
    }
}
