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

package org.onap.dcaegen2.collectors.datafile.integration;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.dcaegen2.collectors.datafile.integration.junit5.mockito.MockitoExtension;
import org.onap.dcaegen2.collectors.datafile.tasks.ScheduledTasks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 3/27/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */

@Configuration
@ComponentScan
@ExtendWith({MockitoExtension.class, SpringExtension.class})
@ContextConfiguration(locations = {"classpath:scheduled-context.xml"})
class ScheduledXmlContextITest extends AbstractTestNGSpringContextTests {

    private static final int WAIT_FOR_SCHEDULING = 1;

    @Autowired
    private ScheduledTasks scheduledTask;

    @Test
    void testScheduling() {
        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(this::verifyDmaapConsumerTask, 0, WAIT_FOR_SCHEDULING, TimeUnit.SECONDS);
    }

    private void verifyDmaapConsumerTask() {
        verify(scheduledTask, atLeast(1)).scheduleMainDatafileEventTask();
    }
}
