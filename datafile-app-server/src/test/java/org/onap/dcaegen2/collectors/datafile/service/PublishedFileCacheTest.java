/*-
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

package org.onap.dcaegen2.collectors.datafile.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PublishedFileCacheTest {

    private static PublishedFileCache testObject;

    @BeforeAll
    public static void setUp() {
        testObject = new PublishedFileCache();
    }

    @Test
    public void purgeFiles_timeNotExpired() {
        Assertions.assertNull(testObject.put(Paths.get("A")));
        Assertions.assertNotNull(testObject.put(Paths.get("A")));
        testObject.put(Paths.get("B"));

        testObject.purge(Instant.now());
        Assertions.assertEquals(2, testObject.size());
    }

    @Test
    public void purgeFiles_timeExpired() {
        testObject.put(Paths.get("A"));
        testObject.put(Paths.get("B"));
        testObject.put(Paths.get("C"));

        testObject.purge(Instant.MAX);
        Assertions.assertEquals(0, testObject.size());
    }

    @Test
    public void purgeFiles_remove() {
        Path path = Paths.get("A");
        testObject.put(path);
        Assertions.assertEquals(1, testObject.size());
        testObject.remove(path);
        Assertions.assertEquals(0, testObject.size());
    }
}
