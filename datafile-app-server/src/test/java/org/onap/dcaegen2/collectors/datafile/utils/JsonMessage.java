/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.utils;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Utility class to produce correctly formatted fileReady event Json messages.
 *
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a> on 7/25/18
 *
 */
public class JsonMessage {
    private String changeIdentifier;
    private String changeType;
    private String notificationFieldsVersion;
    private ArrayList<AdditionalField> arrayOfAdditionalFields;


    @Override
    public String toString() {
        return "[" + getParsed() + "]";
    }

    /**
     * Gets the message in parsed format.
     * @return the massage in parsed format.
     */
    public String getParsed() {
        StringBuffer additionalFieldsString = new StringBuffer();
        if (arrayOfAdditionalFields.size() > 0) {
            additionalFieldsString.append("\"arrayOfAdditionalFields\": [");
            for (Iterator iterator = arrayOfAdditionalFields.iterator(); iterator.hasNext();) {
                AdditionalField additionalField = (AdditionalField) iterator.next();
                additionalFieldsString.append(additionalField.toString());
                if (iterator.hasNext()) {
                    additionalFieldsString.append(",");
                }
            }
            additionalFieldsString.append("]");
        }

        return "{" + "\"event\":{" + "\"commonEventHeader\":{" + "\"domain\":\"notification\","
                + "\"eventId\":\"<<SerialNumber>>-reg\"," + "\"eventName\":\"EriNoti_RnNode_FileReady\","
                + "\"eventType\":\"fileReady\"," + "\"internalHeaderFields\":{},"
                + "\"lastEpochMicrosec\":1519837825682," + "\"nfNamingCode\":\"5GRAN\"," + "\"nfcNamingCode\":\"5DU\","
                + "\"priority\":\"Normal\"," + "\"reportingEntityName\":\"5GRAN_DU\"," + "\"sequence\":0,"
                + "\"sourceId\":\"<<SerialNumber>>\"," + "\"sourceName\":\"5GRAN_DU\","
                + "\"startEpochMicrosec\":\"1519837825682\"," + "\"version\":3" + "}," + "\"notificationFields\":{"
                + getAsStringIfParameterIsSet("changeIdentifier", changeIdentifier,
                        changeType != null || notificationFieldsVersion != null || arrayOfAdditionalFields.size() > 0)
                + getAsStringIfParameterIsSet("changeType", changeType,
                        notificationFieldsVersion != null || arrayOfAdditionalFields.size() > 0)
                + getAsStringIfParameterIsSet("notificationFieldsVersion", notificationFieldsVersion,
                        arrayOfAdditionalFields.size() > 0)
                + additionalFieldsString.toString() + "}" + "}" + "}";
    }

    private JsonMessage(final JsonMessageBuilder builder) {
        this.changeIdentifier = builder.changeIdentifier;
        this.changeType = builder.changeType;
        this.notificationFieldsVersion = builder.notificationFieldsVersion;
        this.arrayOfAdditionalFields = builder.arrayOfAdditionalFields;
    }

    public static class AdditionalField {
        private String location;
        private String compression;
        private String fileFormatType;
        private String fileFormatVersion;

        @Override
        public String toString() {
            return "{"
                    + getAsStringIfParameterIsSet("location", location,
                            compression != null || fileFormatType != null || fileFormatVersion != null)
                    + getAsStringIfParameterIsSet("compression", compression,
                            fileFormatType != null || fileFormatVersion != null)
                    + getAsStringIfParameterIsSet("fileFormatType", fileFormatType, fileFormatVersion != null)
                    + getAsStringIfParameterIsSet("fileFormatVersion", fileFormatVersion, false) + "}";
        }


        private AdditionalField(AdditionalFieldBuilder builder) {
            this.location = builder.location;
            this.compression = builder.compression;
            this.fileFormatType = builder.fileFormatType;
            this.fileFormatVersion = builder.fileFormatVersion;
        }

    }

    public static class AdditionalFieldBuilder {
        private String location;
        private String compression;
        private String fileFormatType;
        private String fileFormatVersion;

        public AdditionalFieldBuilder location(String location) {
            this.location = location;
            return this;
        }

        public AdditionalFieldBuilder compression(String compression) {
            this.compression = compression;
            return this;
        }

        public AdditionalFieldBuilder fileFormatType(String fileFormatType) {
            this.fileFormatType = fileFormatType;
            return this;
        }

        public AdditionalFieldBuilder fileFormatVersion(String fileFormatVersion) {
            this.fileFormatVersion = fileFormatVersion;
            return this;
        }

        public AdditionalField build() {
            return new AdditionalField(this);
        }
    }

    public static class JsonMessageBuilder {
        private String changeIdentifier;
        private String changeType;
        private String notificationFieldsVersion;
        private ArrayList<AdditionalField> arrayOfAdditionalFields = new ArrayList<AdditionalField>();

        public JsonMessageBuilder changeIdentifier(String changeIdentifier) {
            this.changeIdentifier = changeIdentifier;
            return this;
        }

        public JsonMessageBuilder changeType(String changeType) {
            this.changeType = changeType;
            return this;
        }

        public JsonMessageBuilder notificationFieldsVersion(String notificationFieldsVersion) {
            this.notificationFieldsVersion = notificationFieldsVersion;
            return this;
        }

        public JsonMessageBuilder addAdditionalField(AdditionalField additionalField) {
            this.arrayOfAdditionalFields.add(additionalField);
            return this;
        }

        public JsonMessage build() {
            return new JsonMessage(this);
        }
    }

    private static String getAsStringIfParameterIsSet(String parameterName, String parameterValue,
            boolean withSeparator) {
        String result = "";
        if (parameterValue != null) {
            result = "\"" + parameterName + "\":\"" + parameterValue + "\"";

            if (withSeparator) {
                result = result + ",";
            }
        }
        return result;
    }

    /**
     * Can be used to produce a correct test Json message. Tip! Check the formatting with
     * <a href="https://jsonformatter.org/">Json fomatter</a>
     *
     * @param args Not used
     */
    public static void main(String[] args) {
        AdditionalField additionalField = new JsonMessage.AdditionalFieldBuilder()
                .location("ftpes://192.168.0.101:22/ftp/rop/A20161224.1030-1045.bin.gz").compression("gzip")
                .fileFormatType("org.3GPP.32.435#measCollec").fileFormatVersion("V10").build();
        AdditionalField secondAdditionalField = new JsonMessage.AdditionalFieldBuilder()
                .location("sftp://192.168.0.101:22/ftp/rop/A20161224.1030-1045.bin.gz").compression("gzip")
                .fileFormatType("org.3GPP.32.435#measCollec").fileFormatVersion("V10").build();
        JsonMessage message = new JsonMessage.JsonMessageBuilder().changeIdentifier("PM_MEAS_FILES")
                .changeType("FileReady").notificationFieldsVersion("1.0").addAdditionalField(additionalField)
                .addAdditionalField(secondAdditionalField).build();
        System.out.println(message.toString());
    }
}
