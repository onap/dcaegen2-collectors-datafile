/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018-2019 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.ftp.FileServerData;
import org.onap.dcaegen2.collectors.datafile.ftp.ImmutableFileServerData;
import org.onap.dcaegen2.collectors.datafile.ftp.Scheme;

/**
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 *
 */
public class FileDataTest {
    private static final String FTPES_SCHEME = "ftpes://";
    private static final String PM_FILE_NAME = "A20161224.1030-1045.bin.gz";
    private static final String REMOTE_FILE_LOCATION = "/ftp/rop/" + PM_FILE_NAME;
    private static final String USER = "usr";
    private static final String PWD = "pwd";
    private static final String SERVER_ADDRESS = "192.168.0.101";
    private static final int PORT_22 = 22;
    private static final String LOCATION_WITH_USER =
            FTPES_SCHEME + USER + ":" + PWD + "@" + SERVER_ADDRESS + ":" + PORT_22 + REMOTE_FILE_LOCATION;
    private static final String LOCATION_WITHOUT_USER =
            FTPES_SCHEME + SERVER_ADDRESS + ":" + PORT_22 + REMOTE_FILE_LOCATION;


    private MessageMetaData messageMetaData() {
        return ImmutableMessageMetaData.builder()
            .productName("PRODUCT_NAME")
            .vendorName("VENDOR_NAME")
            .lastEpochMicrosec("LAST_EPOCH_MICROSEC")
            .sourceName("SOURCE_NAME")
            .startEpochMicrosec("START_EPOCH_MICROSEC")
            .timeZoneOffset("TIME_ZONE_OFFSET")
            .changeIdentifier("PM_MEAS_CHANGE_IDENTIFIER")
            .changeType("FILE_READY_CHANGE_TYPE")
            .build();
    }

    private FileData properFileDataWithUser() {
        // @formatter:off
        return ImmutableFileData.builder()
         .name("name")
         .location(LOCATION_WITH_USER)
         .compression("comp")
         .fileFormatType("type")
         .fileFormatVersion("version")
         .scheme(Scheme.FTPS)
         .messageMetaData(messageMetaData())
         .build();
        // @formatter:on
    }

    private FileData properFileDataWithoutUser() {
        // @formatter:off
        return ImmutableFileData.builder()
            .name("name")
            .location(LOCATION_WITHOUT_USER)
            .compression("comp")
            .fileFormatType("type")
            .fileFormatVersion("version")
            .scheme(Scheme.FTPS)
            .messageMetaData(messageMetaData())
            .build();
        // @formatter:on
    }

    @Test
    public void fileServerData_properLocationWithUser() {
        // @formatter:off
        ImmutableFileServerData expectedFileServerData = ImmutableFileServerData.builder()
                .serverAddress(SERVER_ADDRESS)
                .port(PORT_22)
                .userId(USER)
                .password(PWD)
                .build();
        // @formatter:on

        FileServerData actualFileServerData = properFileDataWithUser().fileServerData();
        assertEquals(expectedFileServerData, actualFileServerData);
    }

    @Test
    public void fileServerData_properLocationWithoutUser() {
        // @formatter:off
        ImmutableFileServerData expectedFileServerData = ImmutableFileServerData.builder()
                .serverAddress(SERVER_ADDRESS)
                .port(PORT_22)
                .userId("")
                .password("")
                .build();
        // @formatter:on

        FileServerData actualFileServerData = properFileDataWithoutUser().fileServerData();
        assertEquals(expectedFileServerData, actualFileServerData);
        assertTrue(expectedFileServerData.port().isPresent());
    }

    @Test
    public void remoteLocation_properLocation() {
        String actualRemoteFilePath = properFileDataWithUser().remoteFilePath();
        assertEquals(REMOTE_FILE_LOCATION, actualRemoteFilePath);
    }

    @Test
    public void fileServerData_properLocationWithoutPort() {
        // @formatter:off
        ImmutableFileServerData fileServerData = ImmutableFileServerData.builder()
                .serverAddress(SERVER_ADDRESS)
                .userId("")
                .password("")
                .build();
        // @formatter:on

        assertFalse(fileServerData.port().isPresent());
    }


}

