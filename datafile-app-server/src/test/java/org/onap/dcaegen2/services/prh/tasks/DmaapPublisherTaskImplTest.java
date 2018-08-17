/*
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property. All rights reserved.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.dcaegen2.services.prh.tasks;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.onap.dcaegen2.services.prh.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.services.prh.config.ImmutableDmaapPublisherConfiguration;
import org.onap.dcaegen2.services.prh.configuration.AppConfig;
import org.onap.dcaegen2.services.prh.exceptions.DmaapNotFoundException;
import org.onap.dcaegen2.services.prh.exceptions.PrhTaskException;
import org.onap.dcaegen2.services.prh.model.ConsumerDmaapModel;
import org.onap.dcaegen2.services.prh.model.ImmutableConsumerDmaapModel;
import org.onap.dcaegen2.services.prh.service.producer.DMaaPProducerReactiveHttpClient;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 5/17/18
 */
class DmaapPublisherTaskImplTest {

    private static ConsumerDmaapModel consumerDmaapModel;
    private static DmaapPublisherTaskImpl dmaapPublisherTask;
    private static DMaaPProducerReactiveHttpClient dMaaPProducerReactiveHttpClient;
    private static AppConfig appConfig;
    private static DmaapPublisherConfiguration dmaapPublisherConfiguration;

    @BeforeAll
    static void setUp() {
        dmaapPublisherConfiguration = new ImmutableDmaapPublisherConfiguration.Builder()
            .dmaapContentType("application/json").dmaapHostName("54.45.33.2").dmaapPortNumber(1234)
            .dmaapProtocol("https").dmaapUserName("PRH").dmaapUserPassword("PRH")
            .dmaapTopicName("unauthenticated.SEC_OTHER_OUTPUT").build();
        consumerDmaapModel = ImmutableConsumerDmaapModel.builder().ipv4("10.16.123.234")
            .ipv6("0:0:0:0:0:FFFF:0A10:7BEA")
            .pnfName("NOKQTFCOC540002E").build();
        appConfig = mock(AppConfig.class);
    }

    @Test
    void whenPassedObjectDoesntFit_ThrowsPrhTaskException() {
        //given
        when(appConfig.getDmaapPublisherConfiguration()).thenReturn(dmaapPublisherConfiguration);
        dmaapPublisherTask = new DmaapPublisherTaskImpl(appConfig);

        //when
        Executable executableFunction = () -> dmaapPublisherTask.execute(null);

        //then
        assertThrows(PrhTaskException.class, executableFunction, "The specified parameter is incorrect");
    }

    @Test
    void whenPassedObjectFits_ReturnsCorrectStatus() throws PrhTaskException {
        //given
        prepareMocksForTests(HttpStatus.OK.value());

        //when
        StepVerifier.create(dmaapPublisherTask.execute(Mono.just(consumerDmaapModel))).expectSubscription()
            .expectNext(HttpStatus.OK.toString()).verifyComplete();

        //then
        verify(dMaaPProducerReactiveHttpClient, times(1))
            .getDMaaPProducerResponse(any(Mono.class));
        verifyNoMoreInteractions(dMaaPProducerReactiveHttpClient);
    }


    @Test
    void whenPassedObjectFits_butIncorrectResponseReturns() throws DmaapNotFoundException {
        //given
        prepareMocksForTests(HttpStatus.UNAUTHORIZED.value());

        //when
        StepVerifier.create(dmaapPublisherTask.execute(Mono.just(consumerDmaapModel))).expectSubscription()
            .expectNext(String.valueOf(HttpStatus.UNAUTHORIZED.value())).verifyComplete();

        //then
        verify(dMaaPProducerReactiveHttpClient, times(1)).getDMaaPProducerResponse(any(Mono.class));
        verifyNoMoreInteractions(dMaaPProducerReactiveHttpClient);
    }


    private void prepareMocksForTests(Integer httpResponseCode) {
        dMaaPProducerReactiveHttpClient = mock(DMaaPProducerReactiveHttpClient.class);
        when(dMaaPProducerReactiveHttpClient.getDMaaPProducerResponse(any(Mono.class)))
            .thenReturn(Mono.just(httpResponseCode.toString()));
        dmaapPublisherTask = spy(new DmaapPublisherTaskImpl(appConfig));
        when(dmaapPublisherTask.resolveConfiguration()).thenReturn(dmaapPublisherConfiguration);
        doReturn(dMaaPProducerReactiveHttpClient).when(dmaapPublisherTask).resolveClient();
    }
}