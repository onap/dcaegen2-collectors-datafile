package org.onap.dcaegen2.collectors.datafile.service.producer;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.http.client.methods.HttpPut;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublishRedirectStrategyTest {

    private PublishRedirectStrategy redirectStrategy;

    @BeforeEach
    void setUp() {
        redirectStrategy = new PublishRedirectStrategy();
    }

    @Test
    void isRedirectable_shouldReturnTrue() {
        assertTrue(redirectStrategy.isRedirectable(HttpPut.METHOD_NAME));
    }
}