/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018-2019 Nordix Foundation. All rights reserved.
 * Copyright (C) 2020 Nokia. All rights reserved.
 * ===============================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * ============LICENSE_END========================================================================
 */

package org.onap.dcaegen2.collectors.datafile.ftp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.onap.dcaegen2.collectors.datafile.configuration.ImmutableSftpConfig;
import org.onap.dcaegen2.collectors.datafile.configuration.SftpConfig;

public class SftpClientSettingsTest {

    @Test
    public void shouldUseFtpStrictHostChecking(@TempDir Path tempDir) throws Exception {
        File knowHostsFile = new File(tempDir.toFile(), "known_hosts");
        knowHostsFile.createNewFile();

        SftpConfig config = createSampleSftpConfigWithStrictHostChecking(knowHostsFile.getAbsolutePath());
        SftpClientSettings sftpClient = new SftpClientSettings(config);

        assertThat(sftpClient.shouldUseStrictHostChecking()).isTrue();
    }

    @Test
    public void shouldNotUseFtpStrictHostChecking_whenFileDoesNotExist() {
        SftpConfig config = createSampleSftpConfigWithStrictHostChecking("unknown_file");
        SftpClientSettings sftpClient = new SftpClientSettings(config);

        sftpClient.shouldUseStrictHostChecking();
        assertThat(sftpClient.shouldUseStrictHostChecking()).isFalse();
    }

    @Test
    public void shouldNotUseFtpStrictHostChecking_whenExplicitlySwitchedOff() {
        SftpClientSettings sftpClient = new SftpClientSettings(createSampleSftpConfigNoStrictHostChecking());
        sftpClient.shouldUseStrictHostChecking();
        assertThat(sftpClient.shouldUseStrictHostChecking()).isFalse();
    }

    private SftpConfig createSampleSftpConfigNoStrictHostChecking() {
        return new ImmutableSftpConfig.Builder() //
            .strictHostKeyChecking(false).knownHostsFilePath("N/A").build();
    }

    private SftpConfig createSampleSftpConfigWithStrictHostChecking(String pathToKnownHostsFile) {
        return new ImmutableSftpConfig.Builder() //
            .strictHostKeyChecking(true).knownHostsFilePath(pathToKnownHostsFile).build();
    }
}
