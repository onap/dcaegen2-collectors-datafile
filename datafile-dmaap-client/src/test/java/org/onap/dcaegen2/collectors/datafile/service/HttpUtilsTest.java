package org.onap.dcaegen2.collectors.datafile.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpUtilsTest {

    @Test
    public void shouldReturnSuccessfulResponse() {
        assertTrue(HttpUtils.isSuccessfulResponseCode(200));
    }

    @Test
    public void shouldReturnBadResponse() {
        assertFalse(HttpUtils.isSuccessfulResponseCode(404));

    }
}