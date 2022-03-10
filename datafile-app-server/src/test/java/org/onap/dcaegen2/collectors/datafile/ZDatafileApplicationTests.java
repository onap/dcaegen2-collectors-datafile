/*
 * ============LICENSE_START=======================================================
 * DCAE-datafile-collector
 * ================================================================================
 * Copyright (C) 2022 Nokia. All rights reserved.
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
package org.onap.dcaegen2.collectors.datafile;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
// Fix me: Rename to "DatafileApplicationTests" after fix conflict with org.onap.dcaegen2.collectors.datafile.controllers.ScheduleControllerTest
// Running "ZDatafileApplicationTests" before ScheduleControllerTest is reason of failing ScheduleControllerTests.
// Reason: ScheduleControllerTest analyze application logs, which probably is override by contextLoading.
// Workaround: Running "DatafileApplicationTests" after all units, by adding "Z" prefix.
class ZDatafileApplicationTests {

    @Test
        // This method check Spring context load - it no requires assertion (Sonar rule java:S2699)
    void contextLoads() { // NOSONAR
    }
}
