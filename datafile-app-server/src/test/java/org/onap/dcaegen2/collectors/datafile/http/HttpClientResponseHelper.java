/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2020 Nokia. All rights reserved.
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
package org.onap.dcaegen2.collectors.datafile.http;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import reactor.netty.http.client.HttpClientResponse;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.Map;
import java.util.Set;

public class HttpClientResponseHelper {
  public static final HttpClientResponse RESPONSE_OK = new HttpClientResponse() {
    @Override public Map<CharSequence, Set<Cookie>> cookies() {
      return null;
    }

    @Override public boolean isKeepAlive() {
      return false;
    }

    @Override public boolean isWebsocket() {
      return false;
    }

    @Override public HttpMethod method() {
      return null;
    }

    @Override public String fullPath() {
      return null;
    }

    @Override public String uri() {
      return null;
    }

    @Override public HttpVersion version() {
      return null;
    }

    @Override public Context currentContext() {
      return null;
    }

    @Override public ContextView currentContextView() {
      return null;
    }

    @Override public String[] redirectedFrom() {
      return new String[0];
    }

    @Override public HttpHeaders requestHeaders() {
      return null;
    }

    @Override public String resourceUrl() {
      return null;
    }

    @Override public HttpHeaders responseHeaders() {
      return null;
    }

    @Override public HttpResponseStatus status() {
      return HttpResponseStatus.OK;
    }

  };

  public static final HttpClientResponse RESPONSE_ANY_NO_OK = new HttpClientResponse() {
    @Override public Map<CharSequence, Set<Cookie>> cookies() {
      return null;
    }

    @Override public boolean isKeepAlive() {
      return false;
    }

    @Override public boolean isWebsocket() {
      return false;
    }

    @Override public HttpMethod method() {
      return null;
    }

    @Override public String fullPath() {
      return null;
    }

    @Override public String uri() {
      return null;
    }

    @Override public HttpVersion version() {
      return null;
    }

    @Override public Context currentContext() {
      return null;
    }

    @Override public ContextView currentContextView() {
      return null;
    }

    @Override public String[] redirectedFrom() {
      return new String[0];
    }

    @Override public HttpHeaders requestHeaders() {
      return null;
    }

    @Override public String resourceUrl() {
      return null;
    }

    @Override public HttpHeaders responseHeaders() {
      return null;
    }

    @Override public HttpResponseStatus status() {
      return HttpResponseStatus.NOT_IMPLEMENTED;
    }

  };
}
