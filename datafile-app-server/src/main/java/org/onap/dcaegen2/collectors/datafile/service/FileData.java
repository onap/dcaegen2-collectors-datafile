/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 Nordix Foundation. All rights reserved.
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

import java.util.Objects;

/**
 * Contains data, from the fileReady event, about the file to collect from the xNF.
 *
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 *
 */
public class FileData {
    protected String changeIdentifier;
    protected String changeType;
    protected String location;
    protected String compression;
    protected String fileFormatType;
    protected String fileFormatVersion;

    /**
     * @param changeIdentifier
     * @param changeType
     * @param location
     * @param compression
     * @param fileFormatType
     * @param fileFormatVersion
     */
    public FileData(String changeIdentifier, String changeType, String location, String compression,
            String fileFormatType, String fileFormatVersion) {
        this.changeIdentifier = changeIdentifier;
        this.changeType = changeType;
        this.location = location;
        this.compression = compression;
        this.fileFormatType = fileFormatType;
        this.fileFormatVersion = fileFormatVersion;
    }

    public String getCompression() {
        return compression;
    }

    public String getFileFormatType() {
        return fileFormatType;
    }

    public String getFileFormatVersion() {
        return fileFormatVersion;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FileData fileData = (FileData) obj;
        // field comparison
        return Objects.equals(changeIdentifier, fileData.changeIdentifier)
                && Objects.equals(changeType, fileData.changeType) && Objects.equals(location, fileData.location)
                && Objects.equals(compression, fileData.compression)
                && Objects.equals(fileFormatType, fileData.fileFormatType)
                && Objects.equals(fileFormatVersion, fileData.fileFormatVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(changeIdentifier, changeType, location, compression, fileFormatType, fileFormatVersion);
    }

    @Override
    public String toString() {
        return "FileData [changeIdentifier=" + changeIdentifier + ", changeType=" + changeType + ", location="
                + location + ", compression=" + compression + ", fileFormatType=" + fileFormatType
                + ", fileFormatVersion=" + fileFormatVersion + "]";
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }


}
