/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018-2019 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.tasks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.service.producer.DmaapProducerReactiveHttpClient;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.ImmutableDmaapPublisherConfiguration;
import org.springframework.http.HttpStatus;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 5/17/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
class DataRouterPublisherTest {
    private static final String PRODUCT_NAME = "NrRadio";
    private static final String VENDOR_NAME = "Ericsson";
    private static final String LAST_EPOCH_MICROSEC = "8745745764578";
    private static final String SOURCE_NAME = "oteNB5309";
    private static final String START_EPOCH_MICROSEC = "8745745764578";
    private static final String TIME_ZONE_OFFSET = "UTC+05:00";
    private static final String PM_FILE_NAME = "A20161224.1030-1045.bin.gz";

    private static ConsumerDmaapModel consumerDmaapModel;
    private static DataRouterPublisher dmaapPublisherTask;
    private static DmaapProducerReactiveHttpClient dMaaPProducerReactiveHttpClient;
    private static AppConfig appConfig;
    private static DmaapPublisherConfiguration dmaapPublisherConfiguration;

    @BeforeAll
    public static void setUp() {
        //@formatter:off
        dmaapPublisherConfiguration = new ImmutableDmaapPublisherConfiguration.Builder()
                .dmaapContentType("application/json")
                .dmaapHostName("54.45.33.2")
                .dmaapPortNumber(1234)
                .dmaapProtocol("https")
                .dmaapUserName("DFC")
                .dmaapUserPassword("DFC")
                .dmaapTopicName("unauthenticated.VES_NOTIFICATION_OUTPUT")
                .trustStorePath("trustStorePath")
                .trustStorePasswordPath("trustStorePasswordPath")
                .keyStorePath("keyStorePath")
                .keyStorePasswordPath("keyStorePasswordPath")
                .enableDmaapCertAuth(true)
                .build();
        consumerDmaapModel = ImmutableConsumerDmaapModel.builder()
                .productName(PRODUCT_NAME)
                .vendorName(VENDOR_NAME)
                .lastEpochMicrosec(LAST_EPOCH_MICROSEC)
                .sourceName(SOURCE_NAME)
                .startEpochMicrosec(START_EPOCH_MICROSEC)
                .timeZoneOffset(TIME_ZONE_OFFSET)
                .name(PM_FILE_NAME)
                .location("ftpes://192.168.0.101:22/ftp/rop/" + PM_FILE_NAME)
                .internalLocation("target/" + PM_FILE_NAME)
                .compression("gzip")
                .fileFormatType("org.3GPP.32.435#measCollec")
                .fileFormatVersion("V10")
                .build();
        appConfig = mock(AppConfig.class);
        //@formatter:on
    }

    @Test
    public void whenPassedObjectFits_ReturnsCorrectStatus() {
        prepareMocksForTests(Flux.just(HttpStatus.OK));

        StepVerifier.create(dmaapPublisherTask.execute(consumerDmaapModel, 1, Duration.ofSeconds(0)))
                .expectNext(consumerDmaapModel).verifyComplete();

        verify(dMaaPProducerReactiveHttpClient, times(1)).getDmaapProducerResponse(any());
        verifyNoMoreInteractions(dMaaPProducerReactiveHttpClient);
    }

    @Test
    public void whenPassedObjectFits_firstFailsThenSucceeds() {
        prepareMocksForTests(Flux.just(HttpStatus.BAD_GATEWAY), Flux.just(HttpStatus.OK));

        StepVerifier.create(dmaapPublisherTask.execute(consumerDmaapModel, 1, Duration.ofSeconds(0)))
                .expectNext(consumerDmaapModel).verifyComplete();

        verify(dMaaPProducerReactiveHttpClient, times(2)).getDmaapProducerResponse(any());
        verifyNoMoreInteractions(dMaaPProducerReactiveHttpClient);
    }

    @Test
    public void whenPassedObjectFits_firstFailsThenFails() {
        prepareMocksForTests(Flux.just(HttpStatus.BAD_GATEWAY), Flux.just(HttpStatus.BAD_GATEWAY));

        StepVerifier.create(dmaapPublisherTask.execute(consumerDmaapModel, 1, Duration.ofSeconds(0)))
                .expectErrorMessage("Retries exhausted: 1/1").verify();

        verify(dMaaPProducerReactiveHttpClient, times(2)).getDmaapProducerResponse(any());
        verifyNoMoreInteractions(dMaaPProducerReactiveHttpClient);
    }

    @SafeVarargs
    final void prepareMocksForTests(Flux<HttpStatus> firstResponse, Flux<HttpStatus>... nextHttpResponses) {
        dMaaPProducerReactiveHttpClient = mock(DmaapProducerReactiveHttpClient.class);
        when(dMaaPProducerReactiveHttpClient.getDmaapProducerResponse(any())).thenReturn(firstResponse,
                nextHttpResponses);
        when(appConfig.getDmaapPublisherConfiguration()).thenReturn(dmaapPublisherConfiguration);
        dmaapPublisherTask = spy(new DataRouterPublisher(appConfig));
        when(dmaapPublisherTask.resolveConfiguration()).thenReturn(dmaapPublisherConfiguration);
        doReturn(dMaaPProducerReactiveHttpClient).when(dmaapPublisherTask).resolveClient();
    }
}
