/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2022 Nokia. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;

class CloudConfigParserTest {

    public static final String CONFIG_TLS_JSON = "src/test/resources/datafile_test_config_tls.json";
    public static final String CONFIG_NO_TLS_JSON = "src/test/resources/datafile_test_config_no_tls.json";
    public static final String INCORRECT_CERT_CONFIG_JSON = "src/test/resources/datafile_test_config_incorrect_cert_config.json";
    public static final String EXPECTED_EXCEPTION_MESSAGE = "Wrong configuration. External certificate enabled but configs are missing: Could not find member: dmaap.certificateConfig.keyCert";

    @Test
    public void shouldCorrectReadCertificateConfigWithTLS () throws IOException, DatafileTaskException {

        CloudConfigParser parser = getCloudConfigParser(CONFIG_TLS_JSON);
        CertificateConfig certificateConfig = parser.getCertificateConfig();

        assertEquals(true, certificateConfig.enableCertAuth());
        assertEquals("/src/test/resources/dfc.jks", certificateConfig.keyCert());
        assertEquals("/src/test/resources/dfc.jks.pass", certificateConfig.keyPasswordPath());
        assertEquals("/src/test/resources/cert.jks", certificateConfig.trustedCa());
        assertEquals("/src/test/resources/cert.jks.pass", certificateConfig.trustedCaPasswordPath());
        assertEquals(true, certificateConfig.httpsHostnameVerify());
    }

    @Test
    public void shouldCorrectReadCertificateConfigWithoutTLS () throws IOException, DatafileTaskException {
        CloudConfigParser parser = getCloudConfigParser(CONFIG_NO_TLS_JSON);
        CertificateConfig certificateConfig = parser.getCertificateConfig();

        assertEquals(false, certificateConfig.enableCertAuth());
        assertEquals("", certificateConfig.keyCert());
        assertEquals("", certificateConfig.keyPasswordPath());
        assertEquals("", certificateConfig.trustedCa());
        assertEquals("", certificateConfig.trustedCaPasswordPath());
    }

    @Test
    public void shouldThrowExceptionWhenCertAuthIsEnabledButPathsPropertyIsMissing () throws IOException {
        CloudConfigParser parser = getCloudConfigParser(INCORRECT_CERT_CONFIG_JSON);

        DatafileTaskException exception = assertThrows(DatafileTaskException.class, parser::getCertificateConfig);
        assertTrue(exception.getMessage().contains(EXPECTED_EXCEPTION_MESSAGE));
    }

    private CloudConfigParser getCloudConfigParser(String configPath) throws IOException {
        String jsonStr = Files.readString(Path.of(configPath));
        JsonObject jsonObject = JsonParser.parseString(jsonStr).getAsJsonObject();

        return new CloudConfigParser(jsonObject,null);
    }
}
