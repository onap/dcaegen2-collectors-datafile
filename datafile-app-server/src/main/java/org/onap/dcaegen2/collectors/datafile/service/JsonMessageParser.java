/*-
 * ============LICENSE_START========================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018-2019 Nordix Foundation. All rights reserved.
 * ==================================================================================================
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.onap.dcaegen2.collectors.datafile.ftp.Scheme;
import org.onap.dcaegen2.collectors.datafile.model.FileData;
import org.onap.dcaegen2.collectors.datafile.model.FileReadyMessage;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFileData;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableFileReadyMessage;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableMessageMetaData;
import org.onap.dcaegen2.collectors.datafile.model.MessageMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Parses the fileReady event and creates a Flux of FileReadyMessage containing the information.
 *
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class JsonMessageParser {
    private static final Logger logger = LoggerFactory.getLogger(JsonMessageParser.class);

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
     * Parses the Json message and returns a stream of messages.
     *
     * @param rawMessage the Json message to parse.
     * @return a <code>Flux</code> containing messages.
     */
    public Flux<FileReadyMessage> getMessagesFromJson(Mono<String> rawMessage) {
        return rawMessage.flatMapMany(JsonMessageParser::getJsonParserMessage).flatMap(this::createMessageData);
    }

    Optional<JsonObject> getJsonObjectFromAnArray(JsonElement element) {
        JsonParser jsonParser = new JsonParser();
        if (element.isJsonPrimitive()) {
            return Optional.of(jsonParser.parse(element.getAsString()).getAsJsonObject());
        } else if (element.isJsonObject()) {
            return Optional.of((JsonObject) element);
        } else {
            return Optional.of(jsonParser.parse(element.toString()).getAsJsonObject());
        }
    }

    private Flux<FileReadyMessage> getMessagesFromJsonArray(JsonElement jsonElement) {
        return createMessages(Flux.fromStream(StreamSupport.stream(jsonElement.getAsJsonArray().spliterator(), false)
                .map(jsonElementFromArray -> getJsonObjectFromAnArray(jsonElementFromArray)
                        .orElseGet(JsonObject::new))));
    }

    /**
     * Extract info from string and create a Flux of {@link FileReadyMessage}.
     *
     * @param rawMessage - results from DMaaP
     * @return reactive Flux of FileReadyMessages
     */
    private Flux<FileReadyMessage> createMessageData(JsonElement jsonElement) {
        return jsonElement.isJsonObject() ? createMessages(Flux.just(jsonElement.getAsJsonObject()))
                : getMessagesFromJsonArray(jsonElement);
    }

    private static Mono<JsonElement> getJsonParserMessage(String message) {
        return StringUtils.isEmpty(message) ? Mono.empty() : Mono.fromSupplier(() -> new JsonParser().parse(message));
    }

    private static Flux<FileReadyMessage> createMessages(Flux<JsonObject> jsonObject) {
        return jsonObject.flatMap(monoJsonP -> containsNotificationFields(monoJsonP) ? transformMessages(monoJsonP)
                : logErrorAndReturnEmptyMessageFlux("Incorrect JsonObject - missing header. " + jsonObject));
    }


    private static Mono<FileReadyMessage> transformMessages(JsonObject message) {
        Optional<MessageMetaData> optionalMessageMetaData = getMessageMetaData(message);
        if (optionalMessageMetaData.isPresent()) {
            MessageMetaData messageMetaData = optionalMessageMetaData.get();
            JsonObject notificationFields = message.getAsJsonObject(EVENT).getAsJsonObject(NOTIFICATION_FIELDS);
            JsonArray arrayOfNamedHashMap = notificationFields.getAsJsonArray(ARRAY_OF_NAMED_HASH_MAP);
            if (arrayOfNamedHashMap != null) {
                List<FileData> allFileDataFromJson = getAllFileDataFromJson(arrayOfNamedHashMap, messageMetaData);
                if (!allFileDataFromJson.isEmpty()) {
                    return Mono.just(ImmutableFileReadyMessage.builder() //
                            .files(allFileDataFromJson) //
                            .build());
                } else {
                    return Mono.empty();
                }
            }

            logger.error("VES event parsing. Missing arrayOfNamedHashMap in message. {}", message);
            return Mono.empty();
        }
        logger.error("VES event parsing. FileReady event has incorrect JsonObject. {}", message);
        return Mono.empty();
    }


    private static Optional<MessageMetaData> getMessageMetaData(JsonObject message) {
        List<String> missingValues = new ArrayList<>();
        JsonObject commonEventHeader = message.getAsJsonObject(EVENT).getAsJsonObject(COMMON_EVENT_HEADER);
        String eventName = getValueFromJson(commonEventHeader, EVENT_NAME, missingValues);

        JsonObject notificationFields = message.getAsJsonObject(EVENT).getAsJsonObject(NOTIFICATION_FIELDS);
        String changeIdentifier = getValueFromJson(notificationFields, CHANGE_IDENTIFIER, missingValues);
        String changeType = getValueFromJson(notificationFields, CHANGE_TYPE, missingValues);

        // Just to check that it is in the message. Might be needed in the future if there is a new
        // version.
        getValueFromJson(notificationFields, NOTIFICATION_FIELDS_VERSION, missingValues);

        MessageMetaData messageMetaData = ImmutableMessageMetaData.builder() //
                .productName(getDataFromEventName(EventNameDataType.PRODUCT_NAME, eventName, missingValues)) //
                .vendorName(getDataFromEventName(EventNameDataType.VENDOR_NAME, eventName, missingValues)) //
                .lastEpochMicrosec(getValueFromJson(commonEventHeader, LAST_EPOCH_MICROSEC, missingValues)) //
                .sourceName(getValueFromJson(commonEventHeader, SOURCE_NAME, missingValues)) //
                .startEpochMicrosec(getValueFromJson(commonEventHeader, START_EPOCH_MICROSEC, missingValues)) //
                .timeZoneOffset(getValueFromJson(commonEventHeader, TIME_ZONE_OFFSET, missingValues)) //
                .changeIdentifier(changeIdentifier) //
                .changeType(changeType) //
                .build();
        if (missingValues.isEmpty() && isChangeTypeCorrect(changeType)) {
            return Optional.of(messageMetaData);
        } else {
            String errorMessage = "VES event parsing.";
            if (!missingValues.isEmpty()) {
                errorMessage += " Missing data: " + missingValues;
            }
            if (!isChangeTypeCorrect(changeType)) {
                errorMessage += " Change type is wrong: " + changeType + " expected: " + FILE_READY_CHANGE_TYPE;
            }
            errorMessage += " Message: {}";
            logger.error(errorMessage, message);
            return Optional.empty();
        }
    }

    private static boolean isChangeTypeCorrect(String changeType) {
        return FILE_READY_CHANGE_TYPE.equals(changeType);
    }

    private static List<FileData> getAllFileDataFromJson(JsonArray arrayOfAdditionalFields,
            MessageMetaData messageMetaData) {
        List<FileData> res = new ArrayList<>();
        for (int i = 0; i < arrayOfAdditionalFields.size(); i++) {
            JsonObject fileInfo = (JsonObject) arrayOfAdditionalFields.get(i);
            Optional<FileData> fileData = getFileDataFromJson(fileInfo, messageMetaData);

            if (fileData.isPresent()) {
                res.add(fileData.get());
            }
        }
        return res;
    }

    private static Optional<FileData> getFileDataFromJson(JsonObject fileInfo, MessageMetaData messageMetaData) {
        logger.trace("starting to getFileDataFromJson!");

        List<String> missingValues = new ArrayList<>();
        JsonObject data = fileInfo.getAsJsonObject(HASH_MAP);

        String location = getValueFromJson(data, LOCATION, missingValues);
        Scheme scheme;
        try {
            scheme = Scheme.getSchemeFromString(URI.create(location).getScheme());
        } catch (Exception e) {
            logger.error("VES event parsing.", e);
            return Optional.empty();
        }
        FileData fileData = ImmutableFileData.builder() //
                .name(getValueFromJson(fileInfo, NAME, missingValues)) //
                .fileFormatType(getValueFromJson(data, FILE_FORMAT_TYPE, missingValues)) //
                .fileFormatVersion(getValueFromJson(data, FILE_FORMAT_VERSION, missingValues)) //
                .location(location) //
                .scheme(scheme) //
                .compression(getValueFromJson(data, COMPRESSION, missingValues)) //
                .messageMetaData(messageMetaData) //
                .build();
        if (missingValues.isEmpty()) {
            return Optional.of(fileData);
        }
        logger.error("VES event parsing. File information wrong. Missing data: {} Data: {}", missingValues, fileInfo);
        return Optional.empty();
    }

    /**
     * Gets data from the event name. Defined as:
     * {DomainAbbreviation}_{productName}-{vendorName}_{Description}, example:
     * Noti_RnNode-Ericsson_FileReady
     *
     * @param dataType The type of data to get, {@link DmaapConsumerJsonParser.EventNameDataType}.
     * @param eventName The event name to get the data from.
     * @param missingValues List of missing values. The dataType will be added if missing.
     * @return String of data from event name
     */
    private static String getDataFromEventName(EventNameDataType dataType, String eventName,
            List<String> missingValues) {
        String[] eventArray = eventName.split("_|-");
        if (eventArray.length >= 4) {
            return eventArray[dataType.index];
        } else {
            missingValues.add(dataType.toString());
            logger.error("Can not get {} from eventName, eventName is not in correct format: {}", dataType, eventName);
        }
        return "";
    }

    private static String getValueFromJson(JsonObject jsonObject, String jsonKey, List<String> missingValues) {
        if (jsonObject.has(jsonKey)) {
            return jsonObject.get(jsonKey).getAsString();
        } else {
            missingValues.add(jsonKey);
            return "";
        }
    }

    private static boolean containsNotificationFields(JsonObject jsonObject) {
        return jsonObject.has(EVENT) && jsonObject.getAsJsonObject(EVENT).has(NOTIFICATION_FIELDS);
    }

    private static Flux<FileReadyMessage> logErrorAndReturnEmptyMessageFlux(String errorMessage) {
        logger.error(errorMessage);
        return Flux.empty();
    }
}
