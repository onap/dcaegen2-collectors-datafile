/*
 * ============LICENSE_START=======================================================
 * Datafile Collector Service
 * ================================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.config;

import org.immutables.gson.Gson;
import org.immutables.value.Value;
import org.onap.dcaegen2.collectors.datafile.config.ImmutableDmaapConsumerConfiguration;
import org.springframework.stereotype.Component;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 3/23/18
 */
@Component
@Value.Immutable(prehash = true)
@Value.Style(builder = "new")
@Gson.TypeAdapters
public abstract class DmaapConsumerConfiguration implements DmaapCustomConfig {

    private static final long serialVersionUID = 1L;

    @Value.Parameter
    public abstract String consumerId();

    @Value.Parameter
    public abstract String consumerGroup();

    @Value.Parameter
    public abstract Integer timeoutMs();

    @Value.Parameter
    public abstract Integer messageLimit();


    public interface Builder extends
        DmaapCustomConfig.Builder<DmaapConsumerConfiguration, DmaapConsumerConfiguration.Builder> {

        Builder consumerId(String consumerId);

        Builder consumerGroup(String consumerGroup);

        Builder timeoutMs(Integer timeoutMs);

        Builder messageLimit(Integer messageLimit);
    }

    public static DmaapConsumerConfiguration.Builder builder() {
        return ImmutableDmaapConsumerConfiguration.builder();
    }

}