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

import java.io.Serializable;

import java.util.Map;

import org.immutables.gson.Gson;
import org.immutables.value.Value;
import org.springframework.stereotype.Component;


@Component
@Value.Immutable(prehash = true)
@Value.Style(builder = "new")
@Gson.TypeAdapters
public abstract class AaiClientConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    @Value.Parameter
    public abstract String aaiHost();

    @Value.Parameter
    public abstract Integer aaiPort();

    @Value.Parameter
    public abstract String aaiProtocol();

    @Value.Parameter
    public abstract String aaiUserName();

    @Value.Parameter
    public abstract String aaiUserPassword();

    @Value.Parameter
    public abstract Boolean aaiIgnoreSslCertificateErrors();

    @Value.Parameter
    public abstract String aaiBasePath();

    @Value.Parameter
    public abstract String aaiPnfPath();

    @Value.Parameter
    public abstract Map<String, String> aaiHeaders();

}
