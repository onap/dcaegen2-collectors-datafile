/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.tasks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.onap.dcaegen2.collectors.datafile.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.config.ImmutableDmaapPublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.model.ImmutableConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.service.producer.DmaapProducerReactiveHttpClient;
import org.springframework.http.HttpStatus;

import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 5/17/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
class DmaapPublisherTaskImplTest {

    private static ConsumerDmaapModel consumerDmaapModel;
    private static ArrayList<ConsumerDmaapModel> listOfConsumerDmaapModel;
    private static DmaapPublisherTaskImpl dmaapPublisherTask;
    private static DmaapProducerReactiveHttpClient dMaaPProducerReactiveHttpClient;
    private static AppConfig appConfig;
    private static DmaapPublisherConfiguration dmaapPublisherConfiguration;

    @BeforeAll
    public static void setUp() {
        dmaapPublisherConfiguration =
                new ImmutableDmaapPublisherConfiguration.Builder().dmaapContentType("application/json")
                        .dmaapHostName("54.45.33.2").dmaapPortNumber(1234).dmaapProtocol("https").dmaapUserName("DFC")
                        .dmaapUserPassword("DFC").dmaapTopicName("unauthenticated.VES_NOTIFICATION_OUTPUT").build();
        consumerDmaapModel = ImmutableConsumerDmaapModel.builder().location("target/A20161224.1030-1045.bin.gz")
                .compression("gzip").fileFormatType("org.3GPP.32.435#measCollec").fileFormatVersion("V10").build();
        listOfConsumerDmaapModel = new ArrayList<ConsumerDmaapModel>();
        listOfConsumerDmaapModel.add(consumerDmaapModel);
        appConfig = mock(AppConfig.class);
    }

    @Test
    public void whenPassedObjectDoesntFit_ThrowsDatafileTaskException() {
        // given
        when(appConfig.getDmaapPublisherConfiguration()).thenReturn(dmaapPublisherConfiguration);
        dmaapPublisherTask = new DmaapPublisherTaskImpl(appConfig);

        // when
        Executable executableFunction = () -> dmaapPublisherTask.execute(null);

        // then
        Assertions.assertThrows(DatafileTaskException.class, executableFunction,
                "The specified parameter is incorrect");
    }

    @Test
    public void whenPassedObjectFits_ReturnsCorrectStatus() throws DatafileTaskException {
        // given
        prepareMocksForTests(HttpStatus.OK.value());

        // when
        dmaapPublisherTask.execute(Mono.just(listOfConsumerDmaapModel));

        // then
        verify(dMaaPProducerReactiveHttpClient, times(1)).getDmaapProducerResponse(any());
        verifyNoMoreInteractions(dMaaPProducerReactiveHttpClient);
    }

    @Test
    public void whenPassedObjectFits_ReturnsNoContent() throws DatafileTaskException {
        // given
        prepareMocksForTests(HttpStatus.NO_CONTENT.value());

        dmaapPublisherTask.execute(Mono.just(listOfConsumerDmaapModel));

        // then
        verify(dMaaPProducerReactiveHttpClient, times(1)).getDmaapProducerResponse(any());
        verifyNoMoreInteractions(dMaaPProducerReactiveHttpClient);
    }

    private void prepareMocksForTests(Integer httpResponseCode) {
        dMaaPProducerReactiveHttpClient = mock(DmaapProducerReactiveHttpClient.class);
        when(dMaaPProducerReactiveHttpClient.getDmaapProducerResponse(any()))
                .thenReturn(Mono.just(httpResponseCode.toString()));
        when(appConfig.getDmaapPublisherConfiguration()).thenReturn(dmaapPublisherConfiguration);
        dmaapPublisherTask = spy(new DmaapPublisherTaskImpl(appConfig));
        when(dmaapPublisherTask.resolveConfiguration()).thenReturn(dmaapPublisherConfiguration);
        doReturn(dMaaPProducerReactiveHttpClient).when(dmaapPublisherTask).resolveClient();
    }
}
