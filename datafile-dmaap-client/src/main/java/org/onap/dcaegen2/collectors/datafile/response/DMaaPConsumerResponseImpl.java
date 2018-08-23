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
package org.onap.dcaegen2.collectors.datafile.response;

import static java.util.Collections.unmodifiableList;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import org.onap.dcaegen2.collectors.datafile.response.DMaaPConsumerResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class DMaaPConsumerResponseImpl implements DMaaPConsumerResponse {


    private final Integer responseCode;
    private final String responseMessage;
    private final List<String> fetchedMessage;

    public DMaaPConsumerResponseImpl(@NonNull Integer responseCode, @NonNull String responseMessage,
            @Nullable List<String> fetchedMessage) {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.fetchedMessage = Optional.ofNullable(fetchedMessage).orElse(Collections.emptyList());
    }


    @Override
    public String getResponseMessage() {
        return responseMessage;
    }

    @Override
    public Integer getResponseCode() {
        return responseCode;
    }

    @Override
    public List<String> getFetchedMessages() {
        return unmodifiableList(fetchedMessage);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", this.getClass().getSimpleName() + "[", "]").add("responseCode=" + responseCode)
                .add("responseMessage=" + responseMessage).add("fetchedMessage=" + fetchedMessage).toString();
    }
}
