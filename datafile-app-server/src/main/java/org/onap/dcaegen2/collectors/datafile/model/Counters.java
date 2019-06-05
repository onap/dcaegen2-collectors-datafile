/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.dcaegen2.collectors.datafile.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;


/**
 *
 * Various counters that can be shown via a REST API.
 *
 */
public class Counters {

    private final AtomicInteger numberOfTasks = new AtomicInteger();
    private final AtomicInteger numberOfSubscriptions = new AtomicInteger();
    private int noOfCollectedFiles = 0;
    private int noOfFailedFtpAttempts = 0;
    private int noOfFailedFtp = 0;
    private int noOfFailedPublishAttempts = 0;
    private int totalPublishedFiles = 0;
    private int noOfFailedPublish = 0;
    private Instant lastPublishedTime = Instant.MIN;
    private int totalReceivedEvents = 0;
    private Instant lastEventTime = Instant.MIN;

    public AtomicInteger getCurrentNumberOfTasks() {
        return numberOfTasks;
    }

    public AtomicInteger getCurrentNumberOfSubscriptions() {
        return numberOfSubscriptions;
    }

    public synchronized void incNoOfReceivedEvents() {
        totalReceivedEvents++;
        lastEventTime = Instant.now();
    }

    public synchronized void incNoOfCollectedFiles() {
        noOfCollectedFiles++;
    }

    public synchronized void incNoOfFailedFtpAttempts() {
        noOfFailedFtpAttempts++;
    }

    public synchronized void incNoOfFailedFtp() {
        noOfFailedFtp++;
    }

    public synchronized void incNoOfFailedPublishAttempts() {
        noOfFailedPublishAttempts++;
    }

    public synchronized void incTotalPublishedFiles() {
        totalPublishedFiles++;
        lastPublishedTime = Instant.now();
    }

    public synchronized void incNoOfFailedPublish() {
        noOfFailedPublish++;
    }

    public synchronized String toString() {
        StringBuilder str = new StringBuilder();
        str.append(format("totalReceivedEvents", totalReceivedEvents));
        str.append(format("lastEventTime", lastEventTime));
        str.append(format("numberOfTasks", numberOfTasks));
        str.append(format("numberOfSubscriptions", numberOfSubscriptions));
        str.append("\n");
        str.append(format("collectedFiles", noOfCollectedFiles));
        str.append(format("failedFtpAttempts", noOfFailedFtpAttempts));
        str.append(format("failedFtp", noOfFailedFtp));
        str.append("\n");
        str.append(format("totalPublishedFiles", totalPublishedFiles));
        str.append(format("lastPublishedTime", lastPublishedTime));

        str.append(format("failedPublishAttempts", noOfFailedPublishAttempts));
        str.append(format("noOfFailedPublish", noOfFailedPublish));

        return str.toString();
    }

    private String format(String name, Object value) {
        String header = name + ":";
        return String.format("%-24s%-22s\n", header, value);
    }
}
