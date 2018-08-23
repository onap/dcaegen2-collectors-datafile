/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018 Nordix Foundation. All rights reserved.
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

import java.util.ArrayList;
import java.util.Optional;

import org.onap.dcaegen2.collectors.datafile.config.DmaapPublisherConfiguration;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.configuration.Config;
import org.onap.dcaegen2.collectors.datafile.exceptions.DmaapNotFoundException;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.service.producer.ExtendedDmaapProducerHttpClientImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 4/13/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
@Component
public class DmaapPublisherTaskImpl extends
    DmaapPublisherTask<ArrayList<ConsumerDmaapModel>, ArrayList<Integer>, DmaapPublisherConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(DmaapPublisherTaskImpl.class);
    private final Config datafileAppConfig;
    private ExtendedDmaapProducerHttpClientImpl extendedDmaapProducerHttpClient;

    @Autowired
    public DmaapPublisherTaskImpl(AppConfig datafileAppConfig) {
        this.datafileAppConfig = datafileAppConfig;
    }

    @Override
    Integer publish(ConsumerDmaapModel consumerDmaapModel) throws DmaapNotFoundException {
        logger.trace("Method called with arg {}", consumerDmaapModel);
        return extendedDmaapProducerHttpClient.getHttpProducerResponse(consumerDmaapModel)
            .filter(response -> response == HttpStatus.OK.value() || response== HttpStatus.NO_CONTENT.value())
            .orElseThrow(() -> new DmaapNotFoundException("Incorrect response from Dmaap"));
    }

    @Override
    public ArrayList<Integer> execute(ArrayList<ConsumerDmaapModel> listOfonsumerDmaapModel) throws DmaapNotFoundException {
    	ArrayList<Integer> res= new ArrayList<Integer>();
    	listOfonsumerDmaapModel = Optional.ofNullable(listOfonsumerDmaapModel)
            .orElseThrow(() -> new DmaapNotFoundException("Invoked null object to Dmaap task"));
        extendedDmaapProducerHttpClient = resolveClient();
        logger.trace("Method called with arg {}", listOfonsumerDmaapModel);
        for(int i=0;i<listOfonsumerDmaapModel.size();i++) {
        	res.add(publish(listOfonsumerDmaapModel.get(i)));
        }
        return res;
    }

    @Override
    DmaapPublisherConfiguration resolveConfiguration() {
        return datafileAppConfig.getDmaapPublisherConfiguration();
    }

    @Override
    ExtendedDmaapProducerHttpClientImpl resolveClient() {
        return Optional.ofNullable(extendedDmaapProducerHttpClient)
            .orElseGet(() -> new ExtendedDmaapProducerHttpClientImpl(resolveConfiguration()));
    }
}