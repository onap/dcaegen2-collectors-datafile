/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.dcaegen2.collectors.datafile.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.config.DmaapConsumerConfiguration;

public class ConsumerConfigurationTest {
    @Test
    public void toDmaapSuccess() throws DatafileTaskException {
        ConsumerConfiguration configurationUnderTest = ImmutableConsumerConfiguration.builder() //
            .topicUrl(
                "http://admin:admin@message-router.onap.svc.cluster.local:2222/events/unauthenticated.VES_NOTIFICATION_OUTPUT/OpenDcae-c12/C12")
            .trustStorePath("") //
            .trustStorePasswordPath("") //
            .keyStorePath("") //
            .keyStorePasswordPath("") //
            .enableDmaapCertAuth(Boolean.FALSE) //
            .build();

        DmaapConsumerConfiguration dmaapConsumerConfiguration = configurationUnderTest.toDmaap();
        assertEquals("http", dmaapConsumerConfiguration.dmaapProtocol());
        assertEquals("message-router.onap.svc.cluster.local", dmaapConsumerConfiguration.dmaapHostName());
        assertEquals(Integer.valueOf("2222"), dmaapConsumerConfiguration.dmaapPortNumber());
        assertEquals("OpenDcae-c12", dmaapConsumerConfiguration.consumerGroup());
        assertEquals("C12", dmaapConsumerConfiguration.consumerId());
    }

    @Test
    public void toDmaapNoUserInfoSuccess() throws DatafileTaskException {
        ConsumerConfiguration configurationUnderTest = ImmutableConsumerConfiguration.builder() //
            .topicUrl(
                "http://message-router.onap.svc.cluster.local:2222/events/unauthenticated.VES_NOTIFICATION_OUTPUT/OpenDcae-c12/C12")
            .trustStorePath("") //
            .trustStorePasswordPath("") //
            .keyStorePath("") //
            .keyStorePasswordPath("") //
            .enableDmaapCertAuth(Boolean.FALSE) //
            .build();

        DmaapConsumerConfiguration dmaapConsumerConfiguration = configurationUnderTest.toDmaap();
        assertEquals("http", dmaapConsumerConfiguration.dmaapProtocol());
        assertEquals("message-router.onap.svc.cluster.local", dmaapConsumerConfiguration.dmaapHostName());
        assertEquals(Integer.valueOf("2222"), dmaapConsumerConfiguration.dmaapPortNumber());
        assertEquals("OpenDcae-c12", dmaapConsumerConfiguration.consumerGroup());
        assertEquals("C12", dmaapConsumerConfiguration.consumerId());
    }

    @Test
    public void toDmaapWhenInvalidUrlThrowException() throws DatafileTaskException {
        ConsumerConfiguration configurationUnderTest = ImmutableConsumerConfiguration.builder() //
            .topicUrl("//admin:admin@message-router.onap.svc.cluster.local:2222//events/").trustStorePath("") //
            .trustStorePasswordPath("") //
            .keyStorePath("") //
            .keyStorePasswordPath("") //
            .enableDmaapCertAuth(Boolean.FALSE) //
            .build();

        DatafileTaskException exception =
            assertThrows(DatafileTaskException.class, () -> configurationUnderTest.toDmaap());
        assertEquals("Could not parse the URL", exception.getMessage());
    }

    @Test
    public void toDmaapWhenInvalidPathThrowException() throws DatafileTaskException {
        ConsumerConfiguration configurationUnderTest = ImmutableConsumerConfiguration.builder() //
            .topicUrl("http://admin:admin@message-router.onap.svc.cluster.local:2222//events/") //
            .trustStorePath("") //
            .trustStorePasswordPath("") //
            .keyStorePath("") //
            .keyStorePasswordPath("") //
            .enableDmaapCertAuth(Boolean.FALSE) //
            .build();

        DatafileTaskException exception =
            assertThrows(DatafileTaskException.class, () -> configurationUnderTest.toDmaap());
        assertEquals("The path has incorrect syntax: //events/", exception.getMessage());
    }
}
