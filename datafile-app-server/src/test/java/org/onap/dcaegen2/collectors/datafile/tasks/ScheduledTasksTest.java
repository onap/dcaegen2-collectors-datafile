/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2019 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.tasks;

import static org.mockito.Mockito.mock;

import java.nio.file.Paths;
import java.time.Instant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;

public class ScheduledTasksTest {

    private static AppConfig appConfig = mock(AppConfig.class);
    private static ScheduledTasks testObject;

    @BeforeAll
    public static void setUp() {
        testObject = new ScheduledTasks(appConfig);
    }

    @Test
    public void purgeFiles_timeNotExpired() {
        testObject.alreadyPublishedFiles.put(Paths.get("A"), Instant.now());
        testObject.alreadyPublishedFiles.put(Paths.get("B"), Instant.now());
        testObject.purgeCachedInformation(Instant.now());
        Assertions.assertEquals(2, testObject.alreadyPublishedFiles.size());
    }

    @Test
    public void purgeFiles_timeExpired() {
        testObject.alreadyPublishedFiles.put(Paths.get("A"), Instant.now());
        testObject.alreadyPublishedFiles.put(Paths.get("B"), Instant.now());
        testObject.alreadyPublishedFiles.put(Paths.get("C"), Instant.now());

        testObject.purgeCachedInformation(Instant.MAX);
        Assertions.assertEquals(0, testObject.alreadyPublishedFiles.size());
    }

}
