package org.onap.dcaegen2.collectors.datafile.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.onap.dcaegen2.collectors.datafile.config.DmaapConsumerConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

class DmaapReactiveWebClientTest {

    @Mock
    private DmaapConsumerConfiguration dmaapConsumerConfiguration;

    @BeforeEach
    void setUp() {
        dmaapConsumerConfiguration = mock(DmaapConsumerConfiguration.class);
    }


    @Test
    void buildsDMaaPReactiveWebClientProperly() {
        when(dmaapConsumerConfiguration.dmaapContentType()).thenReturn("*/*");
        WebClient dmaapReactiveWebClient = new DmaapReactiveWebClient()
            .fromConfiguration(dmaapConsumerConfiguration)
            .build();

        verify(dmaapConsumerConfiguration, times(1)).dmaapContentType();
        assertNotNull(dmaapReactiveWebClient);
    }
}