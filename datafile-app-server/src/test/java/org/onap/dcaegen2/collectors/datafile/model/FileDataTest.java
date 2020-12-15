/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018-2019 Nordix Foundation. All rights reserved.
 * Copyright (C) 2020 Nokia. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.commons.FileServerData;
import org.onap.dcaegen2.collectors.datafile.commons.ImmutableFileServerData;
import org.onap.dcaegen2.collectors.datafile.commons.Scheme;

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
    private static final String LOCATION_WITH_USER_NO_PASSWORD =
        FTPES_SCHEME + USER + "@" + SERVER_ADDRESS + ":" + PORT_22 + REMOTE_FILE_LOCATION;
    private static final String LOCATION_WITHOUT_USER =
        FTPES_SCHEME + SERVER_ADDRESS + ":" + PORT_22 + REMOTE_FILE_LOCATION;

    private MessageMetaData messageMetaData() {
        return ImmutableMessageMetaData.builder() //
            .productName("PRODUCT_NAME") //
            .vendorName("VENDOR_NAME") //
            .lastEpochMicrosec("LAST_EPOCH_MICROSEC") //
            .sourceName("SOURCE_NAME") //
            .startEpochMicrosec("START_EPOCH_MICROSEC") //
            .timeZoneOffset("TIME_ZONE_OFFSET") //
            .changeIdentifier("PM_MEAS_CHANGE_IDENTIFIER") //
            .changeType("FILE_READY_CHANGE_TYPE") //
            .build();
    }

    private FileData properFileDataWithUser() {
        return ImmutableFileData.builder() //
            .name("name") //
            .location(LOCATION_WITH_USER) //
            .compression("comp") //
            .fileFormatType("type") //
            .fileFormatVersion("version") //
            .scheme(Scheme.FTPES) //
            .messageMetaData(messageMetaData()) //
            .build();
    }

    private FileData properFileDataWithUserNoPassword() {
        return ImmutableFileData.builder() //
            .name("name") //
            .location(LOCATION_WITH_USER_NO_PASSWORD) //
            .compression("comp") //
            .fileFormatType("type") //
            .fileFormatVersion("version") //
            .scheme(Scheme.FTPES) //
            .messageMetaData(messageMetaData()) //
            .build();
    }

    private FileData properFileDataWithoutUser() {
        return ImmutableFileData.builder() //
            .name("name") //
            .location(LOCATION_WITHOUT_USER) //
            .compression("comp") //
            .fileFormatType("type") //
            .fileFormatVersion("version") //
            .scheme(Scheme.FTPES) //
            .messageMetaData(messageMetaData()) //
            .build();
    }

    @Test
    public void fileServerData_properLocationWithUser() {
        ImmutableFileServerData expectedFileServerData = ImmutableFileServerData.builder() //
            .serverAddress(SERVER_ADDRESS) //
            .port(PORT_22) //
            .userId(USER) //
            .password(PWD) //
            .build();

        FileServerData actualFileServerData = properFileDataWithUser().fileServerData();
        assertEquals(expectedFileServerData, actualFileServerData);
    }

    @Test
    public void fileServerData_properLocationWithUserNoPassword() {
        ImmutableFileServerData expectedFileServerData = ImmutableFileServerData.builder() //
            .serverAddress(SERVER_ADDRESS) //
            .port(PORT_22) //
            .userId(USER) //
            .password("") //
            .build();

        FileServerData actualFileServerData = properFileDataWithUserNoPassword().fileServerData();
        assertEquals(expectedFileServerData, actualFileServerData);
    }

    @Test
    public void fileServerData_properLocationWithoutUser() {
        ImmutableFileServerData expectedFileServerData = ImmutableFileServerData.builder() //
            .serverAddress(SERVER_ADDRESS) //
            .port(PORT_22) //
            .userId("") //
            .password("") //
            .build();

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
        ImmutableFileServerData fileServerData = ImmutableFileServerData.builder() //
            .serverAddress(SERVER_ADDRESS) //
            .userId("") //
            .password("") //
            .build();

        assertFalse(fileServerData.port().isPresent());
    }
}
