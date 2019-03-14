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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A cache of all files that already have been published.
 */
public class PublishedFileCache {
    private final Map<Path, Instant> publishedFiles = Collections.synchronizedMap(new HashMap<Path, Instant>());

    /**
     * Adds a file to the cache.
     *
     * @param path the name of the file to add.
     * @return true if the file is in the cache, or false otherwise.
     */
    public boolean put(Path path) {
        boolean isPublished = true;
        if (!publishedFiles.containsKey(path)) {
            publishedFiles.put(path, Instant.now());
            isPublished = false;
        }
        return isPublished;
    }

    /**
     * Removes a file from the cache.
     *
     * @param localFileName name of the file to remove.
     */
    public void remove(Path localFileName) {
        publishedFiles.remove(localFileName);
    }

    /**
     * Removes files 24 hours older than the given instant.
     *
     * @param now the instant that files older than 24 hours of will be removed.
     */
    public void purge(Instant now) {
        for (Iterator<Map.Entry<Path, Instant>> it = publishedFiles.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Path, Instant> pair = it.next();
            if (isCachedPublishedFileOutdated(now, pair.getValue())) {
                it.remove();
            }
        }
    }

    int size() {
        return publishedFiles.size();
    }

    private boolean isCachedPublishedFileOutdated(Instant now, Instant then) {
        final int timeToKeepInfoInSeconds = 60 * 60 * 24;
        return now.getEpochSecond() - then.getEpochSecond() > timeToKeepInfoInSeconds;
    }


}
