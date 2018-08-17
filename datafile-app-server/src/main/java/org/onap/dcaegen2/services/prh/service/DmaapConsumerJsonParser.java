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

package org.onap.dcaegen2.services.prh.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.onap.dcaegen2.services.prh.exceptions.DmaapEmptyResponseException;
import org.onap.dcaegen2.services.prh.exceptions.DmaapNotFoundException;
import org.onap.dcaegen2.services.prh.model.ConsumerDmaapModel;
import org.onap.dcaegen2.services.prh.model.ImmutableConsumerDmaapModel;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 5/8/18
 */
public class DmaapConsumerJsonParser {

    private static final String EVENT = "event";
    private static final String OTHER_FIELDS = "otherFields";
    private static final String PNF_OAM_IPV_4_ADDRESS = "pnfOamIpv4Address";
    private static final String PNF_OAM_IPV_6_ADDRESS = "pnfOamIpv6Address";
    private static final String PNF_VENDOR_NAME = "pnfVendorName";
    private static final String PNF_SERIAL_NUMBER = "pnfSerialNumber";

    /**
     * Extract info from string and create @see {@link org.onap.dcaegen2.services.prh.model.ConsumerDmaapModel}.
     *
     * @param monoMessage - results from DMaaP
     * @return reactive DMaaPModel
     */
    public Mono<ConsumerDmaapModel> getJsonObject(Mono<String> monoMessage) {
        return monoMessage
            .flatMap(this::getJsonParserMessage)
            .flatMap(this::createJsonConsumerModel);
    }

    private Mono<JsonElement> getJsonParserMessage(String message) {
        return StringUtils.isEmpty(message) ? Mono.error(new DmaapEmptyResponseException())
            : Mono.fromSupplier(() -> new JsonParser().parse(message));
    }

    private Mono<ConsumerDmaapModel> createJsonConsumerModel(JsonElement jsonElement) {
        return jsonElement.isJsonObject()
            ? create(Mono.fromSupplier(jsonElement::getAsJsonObject))
            : getConsumerDmaapModelFromJsonArray(jsonElement);
    }

    private Mono<ConsumerDmaapModel> getConsumerDmaapModelFromJsonArray(JsonElement jsonElement) {
        return create(
            Mono.fromCallable(() -> StreamSupport.stream(jsonElement.getAsJsonArray().spliterator(), false).findFirst()
                .flatMap(this::getJsonObjectFromAnArray)
                .orElseThrow(DmaapEmptyResponseException::new)));
    }

    public Optional<JsonObject> getJsonObjectFromAnArray(JsonElement element) {
        return Optional.of(new JsonParser().parse(element.getAsString()).getAsJsonObject());
    }

    private Mono<ConsumerDmaapModel> create(Mono<JsonObject> jsonObject) {
        return jsonObject.flatMap(monoJsonP ->
            !containsHeader(monoJsonP) ? Mono.error(new DmaapNotFoundException("Incorrect JsonObject - missing header"))
                : transform(monoJsonP));
    }

    private Mono<ConsumerDmaapModel> transform(JsonObject monoJsonP) {
        monoJsonP = monoJsonP.getAsJsonObject(EVENT).getAsJsonObject(OTHER_FIELDS);
        String pnfVendorName = getValueFromJson(monoJsonP, PNF_VENDOR_NAME);
        String pnfSerialNumber = getValueFromJson(monoJsonP, PNF_SERIAL_NUMBER);
        String pnfOamIpv4Address = getValueFromJson(monoJsonP, PNF_OAM_IPV_4_ADDRESS);
        String pnfOamIpv6Address = getValueFromJson(monoJsonP, PNF_OAM_IPV_6_ADDRESS);
        return
            (!vendorAndSerialNotEmpty(pnfSerialNumber, pnfVendorName) || !ipPropertiesNotEmpty(pnfOamIpv4Address,
                pnfOamIpv6Address))
                ? Mono.error(new DmaapNotFoundException("Incorrect json, consumerDmaapModel can not be created: "
                + printMessage(pnfVendorName, pnfSerialNumber, pnfOamIpv4Address, pnfOamIpv6Address))) :
                Mono.just(ImmutableConsumerDmaapModel.builder()
                    .pnfName(pnfVendorName.substring(0, Math.min(pnfVendorName.length(), 3)).toUpperCase()
                        .concat(pnfSerialNumber)).ipv4(pnfOamIpv4Address)
                    .ipv6(pnfOamIpv6Address).build());
    }

    private String getValueFromJson(JsonObject jsonObject, String jsonKey) {
        return jsonObject.has(jsonKey) ? jsonObject.get(jsonKey).getAsString() : "";
    }

    private boolean vendorAndSerialNotEmpty(String pnfSerialNumber, String pnfVendorName) {
        return (!StringUtils.isEmpty(pnfSerialNumber) && !StringUtils.isEmpty(pnfVendorName));
    }

    private boolean ipPropertiesNotEmpty(String ipv4, String ipv6) {
        return (!StringUtils.isEmpty(ipv4)) || !(StringUtils.isEmpty(ipv6));
    }

    private boolean containsHeader(JsonObject jsonObject) {
        return jsonObject.has(EVENT) && jsonObject.getAsJsonObject(EVENT).has(OTHER_FIELDS);
    }

    private String printMessage(String pnfVendorName, String pnfSerialNumber, String pnfOamIpv4Address,
        String pnfOamIpv6Address) {
        return String.format("%n{"
            + "\"pnfVendorName\" : \"%s\","
            + "\"pnfSerialNumber\": \"%s\","
            + "\"pnfOamIpv4Address\": \"%s\","
            + "\"pnfOamIpv6Address\": \"%s\""
            + "%n}", pnfVendorName, pnfSerialNumber, pnfOamIpv4Address, pnfOamIpv6Address);
    }
}
