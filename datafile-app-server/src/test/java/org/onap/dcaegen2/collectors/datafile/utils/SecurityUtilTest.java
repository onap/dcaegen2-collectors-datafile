/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2021 Nokia. All rights reserved.
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
package org.onap.dcaegen2.collectors.datafile.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.onap.dcaegen2.collectors.datafile.commons.SecurityUtil;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Tests the SecurityUtil.
 *
 * @author <a href="mailto:krzysztof.gajewski@nokia.com">Krzysztof Gajewski</a>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(SecurityUtil.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class SecurityUtilTest {

    @Mock
    Path path;

    private static final String expectedPassword = "password";
    private static final String validPath = "/validPath";
    private static final String invalidPath = "/invalidPath";

    @Test
    public void whenGetKeystorePasswordFromFile_passwordSuccessfullyReturned() throws Exception {
        mockStatic(Paths.class);
        mockStatic(Files.class);
        when(Paths.get(validPath)).thenReturn(path);
        when(Files.readAllBytes(path)).thenReturn("password".getBytes());

        String result = SecurityUtil.getKeystorePasswordFromFile(validPath);
        assertEquals(expectedPassword, result);

    }

    @Test
    public void whenGetKeystorePasswordFromFile_IOExceptionForValidPath() throws Exception {
        mockStatic(Paths.class);
        mockStatic(Files.class);
        when(Paths.get(validPath)).thenReturn(path);
        when(Files.readAllBytes(path)).thenThrow(IOException.class);

        assertEquals("", SecurityUtil.getKeystorePasswordFromFile(validPath));

    }

    @Test
    public void whenGetKeystorePasswordFromFile_InvalidPathExceptionForInvalidPath() {
        mockStatic(Paths.class);
        mockStatic(Files.class);
        when(Paths.get(invalidPath)).thenThrow(InvalidPathException.class);

        assertThrows(InvalidPathException.class, () -> SecurityUtil.getKeystorePasswordFromFile(invalidPath));

    }

    @Test
    public void whenGetTruststorePasswordFromFile_passwordSuccessfullyReturned() throws Exception {
        mockStatic(Paths.class);
        mockStatic(Files.class);
        when(Paths.get(validPath)).thenReturn(path);
        when(Files.readAllBytes(path)).thenReturn("password".getBytes());

        String result = SecurityUtil.getTruststorePasswordFromFile(validPath);
        assertEquals(expectedPassword, result);

    }

    @Test
    public void whenGetTruststorePasswordFromFile_IOExceptionForValidPath() throws Exception {
        mockStatic(Paths.class);
        mockStatic(Files.class);
        when(Paths.get(validPath)).thenReturn(path);
        when(Files.readAllBytes(path)).thenThrow(IOException.class);

        assertEquals("", SecurityUtil.getTruststorePasswordFromFile(validPath));

    }

    @Test
    public void whenGetTruststorePasswordFromFile_InvalidPathExceptionForInvalidPath() {
        mockStatic(Paths.class);
        mockStatic(Files.class);
        when(Paths.get(invalidPath)).thenThrow(InvalidPathException.class);

        assertThrows(InvalidPathException.class, () -> SecurityUtil.getTruststorePasswordFromFile(invalidPath));

    }

}
