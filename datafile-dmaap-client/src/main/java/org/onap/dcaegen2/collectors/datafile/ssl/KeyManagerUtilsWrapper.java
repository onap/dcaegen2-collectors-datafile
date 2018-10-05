/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 Nordix Foundation. All rights reserved.
 * ===============================================================================================
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
 * ============LICENSE_END========================================================================
 */

package org.onap.dcaegen2.collectors.datafile.ssl;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ssl.KeyManager;

import org.apache.commons.net.util.KeyManagerUtils;

/**
 * @author
 *
 */
public class KeyManagerUtilsWrapper implements IKeyManagerUtils {
    private KeyManager keyManager;

    @Override
    public void setCredentials(String keyStorePath, String keyStorePass) throws IOException, GeneralSecurityException {
        keyManager = KeyManagerUtils.createClientKeyManager(new File(keyStorePath), keyStorePass);
    }

    @Override
    public KeyManager getClientKeyManager() {
        return keyManager;
    }
}
