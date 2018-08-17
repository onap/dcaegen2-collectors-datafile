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

package org.onap.dcaegen2.collectors.datafile.tasks;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.onap.dcaegen2.collectors.datafile.config.AaiClientConfiguration;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.tasks.AaiConsumerTask;
import org.onap.dcaegen2.collectors.datafile.tasks.AaiConsumerTaskImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AaiConsumerTaskSpy {

    /**
     * Mocking bean for tests.
     *
     * @return A&AI ConsumerTask spy
     */
    @Bean
    @Primary
    public AaiConsumerTask registerSimpleAaiPublisherTask() {
        AppConfig appConfig = mock(AppConfig.class);
        when(appConfig.getAaiClientConfiguration()).thenReturn(mock(AaiClientConfiguration.class));
        return spy(new AaiConsumerTaskImpl(appConfig));
    }
}