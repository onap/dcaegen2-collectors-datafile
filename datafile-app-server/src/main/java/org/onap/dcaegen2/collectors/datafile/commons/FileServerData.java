/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018-2019 Nordix Foundation. All rights reserved.
 * Modifications copyright (C) 2021 Nokia. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.commons;

import java.util.List;
import java.util.Optional;

import org.apache.hc.core5.http.NameValuePair;
import org.immutables.value.Value;

/**
 * Data about the file server to collect a file from.
 * In case of http protocol it also contains data required to recreate target uri
 *
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 *
 */
@Value.Immutable
@Value.Style(redactedMask = "####")
public interface FileServerData {
    public String serverAddress();

    public String userId();

    @Value.Redacted
    public String password();

    public Optional<Integer> port();

    @Value.Redacted
    public Optional<List<NameValuePair>> query();

    public Optional<String> fragment();
}
