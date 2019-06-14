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
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A cache of all files that already has been published. Key is the local file path and the value is a time stamp, when
 * the key was last used.
 */
public class PublishedFileCache {
    private final Map<Path, Instant> publishedFiles = new HashMap<Path, Instant>();

    /**
     * Adds a file to the cache.
     *
     * @param path the name of the file to add.
     * @return <code>null</code> if the file is not already in the cache.
     */
    public synchronized Instant put(Path path) {
        return publishedFiles.put(path, Instant.now());
    }

    /**
     * Removes a file from the cache.
     *
     * @param localFileName name of the file to remove.
     */
    public synchronized void remove(Path localFileName) {
        publishedFiles.remove(localFileName);
    }

    /**
     * Removes files 24 hours older than the given instant.
     *
     * @param now the instant will determine which files that will be purged.
     */
    public synchronized void purge(Instant now) {
        for (Iterator<Map.Entry<Path, Instant>> it = publishedFiles.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Path, Instant> pair = it.next();
            if (isCachedPublishedFileOutdated(now, pair.getValue())) {
                it.remove();
            }
        }
    }

    public synchronized int size() {
        return publishedFiles.size();
    }

    private static boolean isCachedPublishedFileOutdated(Instant now, Instant then) {
        final int timeToKeepInfoInSeconds = 60 * 60 * 24;
        return now.getEpochSecond() - then.getEpochSecond() > timeToKeepInfoInSeconds;
    }
}
