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

package org.onap.dcaegen2.collectors.datafile.http;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;

/**
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class HttpAsyncClientBuilderWrapper implements IHttpAsyncClientBuilder {
    HttpAsyncClientBuilder builder = HttpAsyncClients.custom();

    @Override
    public IHttpAsyncClientBuilder setRedirectStrategy(RedirectStrategy redirectStrategy) {
        builder.setRedirectStrategy(redirectStrategy);
        return this;
    }

    @Override
    public IHttpAsyncClientBuilder setSSLContext(SSLContext sslcontext) {
        builder.setSSLContext(sslcontext);
        return this;
    }

    @Override
    public IHttpAsyncClientBuilder setSSLHostnameVerifier(HostnameVerifier hostnameVerifier) {
        builder.setSSLHostnameVerifier(hostnameVerifier);
        return this;
    }

    @Override
    public IHttpAsyncClientBuilder setDefaultRequestConfig(RequestConfig config) {
        builder.setDefaultRequestConfig(config);
        return this;
    }

    @Override
    public CloseableHttpAsyncClient build() {
        return builder.build();
    }

}
