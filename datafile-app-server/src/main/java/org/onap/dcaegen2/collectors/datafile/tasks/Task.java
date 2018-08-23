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
package org.onap.dcaegen2.collectors.datafile.tasks;

import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;

/**
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */


public abstract class Task<R, S, C> {

    private Task taskProcess;

    public void setNext(Task task) {
        this.taskProcess = task;
    }

    public void receiveRequest(R body) throws DatafileTaskException {

        S response = execute(body);
        if (taskProcess != null) {
            taskProcess.receiveRequest(response);
        }
    }

    abstract S execute(R object) throws DatafileTaskException;

    abstract C resolveConfiguration();
}
