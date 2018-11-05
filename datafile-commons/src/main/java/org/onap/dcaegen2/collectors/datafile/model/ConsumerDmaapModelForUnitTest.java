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

package org.onap.dcaegen2.collectors.datafile.model;

public class ConsumerDmaapModelForUnitTest implements ConsumerDmaapModel {
    private final String productName;
    private final String vendorName;
    private final String lastEpochMicrosec;
    private final String sourceName;
    private final String startEpochMicrosec;
    private final String timeZoneOffset;
    private final String name;
    private final String location;
    private final String compression;
    private final String fileFormatType;
    private final String fileFormatVersion;

    public ConsumerDmaapModelForUnitTest() {
        this.productName = "NrRadio";
        this.vendorName = "Ericsson";
        this.lastEpochMicrosec = "8745745764578";
        this.sourceName = "oteNB5309";
        this.startEpochMicrosec = "8745745764578";
        this.timeZoneOffset = "UTC+05:00";
        this.name = "A20161224.1030-1045.bin.gz";
        this.location = "target/A20161224.1030-1045.bin.gz";
        this.compression = "gzip";
        this.fileFormatType = "org.3GPP.32.435#measCollec";
        this.fileFormatVersion = "V10";
    }

    /**
     * @return the productName
     */
    @Override
    public String getProductName() {
        return productName;
    }

    /**
     * @return the vendorName
     */
    @Override
    public String getVendorName() {
        return vendorName;
    }

    /**
     * @return the lastEpochMicrosec
     */
    @Override
    public String getLastEpochMicrosec() {
        return lastEpochMicrosec;
    }

    /**
     * @return the sourceName
     */
    @Override
    public String getSourceName() {
        return sourceName;
    }

    /**
     * @return the startEpochMicrosec
     */
    @Override
    public String getStartEpochMicrosec() {
        return startEpochMicrosec;
    }

    /**
     * @return the timeZoneOffset
     */
    @Override
    public String getTimeZoneOffset() {
        return timeZoneOffset;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public String getCompression() {
        return compression;
    }

    @Override
    public String getFileFormatType() {
        return fileFormatType;
    }

    @Override
    public String getFileFormatVersion() {
        return fileFormatVersion;
    }
}
