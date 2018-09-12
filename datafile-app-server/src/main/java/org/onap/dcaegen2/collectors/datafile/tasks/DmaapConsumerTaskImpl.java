/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property, 2018 Nordix Foundation. All rights reserved.
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

import java.util.ArrayList;

import org.onap.dcaegen2.collectors.datafile.config.DmaapConsumerConfiguration;
import org.onap.dcaegen2.collectors.datafile.configuration.AppConfig;
import org.onap.dcaegen2.collectors.datafile.configuration.Config;
import org.onap.dcaegen2.collectors.datafile.ftp.FileCollector;
import org.onap.dcaegen2.collectors.datafile.model.ConsumerDmaapModel;
import org.onap.dcaegen2.collectors.datafile.service.DmaapConsumerJsonParser;
import org.onap.dcaegen2.collectors.datafile.service.FileData;
import org.onap.dcaegen2.collectors.datafile.service.consumer.DmaapConsumerReactiveHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 3/23/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
@Component
public class DmaapConsumerTaskImpl extends DmaapConsumerTask {

    private static final Logger logger = LoggerFactory.getLogger(DmaapConsumerTaskImpl.class);

    private Config datafileAppConfig;
    private DmaapConsumerJsonParser dmaapConsumerJsonParser;
    private DmaapConsumerReactiveHttpClient dmaaPConsumerReactiveHttpClient;
    FileCollector fileCollector;

    @Autowired
    public DmaapConsumerTaskImpl(AppConfig datafileAppConfig, FileCollector fileCollector) {
        this.datafileAppConfig = datafileAppConfig;
        this.dmaapConsumerJsonParser = new DmaapConsumerJsonParser();
        this.fileCollector = fileCollector;
    }

    protected DmaapConsumerTaskImpl(AppConfig datafileAppConfig,
            DmaapConsumerReactiveHttpClient dmaaPConsumerReactiveHttpClient,
            DmaapConsumerJsonParser dmaapConsumerJsonParser, FileCollector fileCollector) {
        this.datafileAppConfig = datafileAppConfig;
        this.dmaaPConsumerReactiveHttpClient = dmaaPConsumerReactiveHttpClient;
        this.dmaapConsumerJsonParser = dmaapConsumerJsonParser;
        this.fileCollector = fileCollector;
    }

    @Override
    Mono<ArrayList<FileData>> consume(Mono<String> message) {
        logger.trace("Method called with arg {}", message);
        return dmaapConsumerJsonParser.getJsonObject(message);
    }

    private Mono<ArrayList<ConsumerDmaapModel>> getFilesFromSender(ArrayList<FileData> listOfFileData) {
        Mono<ArrayList<ConsumerDmaapModel>> filesFromSender = fileCollector.getFilesFromSender(listOfFileData);
        return filesFromSender;
        // TODO: Refactor for better error handling.
    }

    @Override
    protected Mono<ArrayList<ConsumerDmaapModel>> execute(String object) {
        dmaaPConsumerReactiveHttpClient = resolveClient();
        logger.trace("Method called with arg {}", object);
        Mono<ArrayList<FileData>> consumerResult =
                consume((dmaaPConsumerReactiveHttpClient.getDmaapConsumerResponse()));
        return consumerResult.flatMap(this::getFilesFromSender);
    }

    @Override
    void initConfigs() {
        datafileAppConfig.initFileStreamReader();
    }

    @Override
    protected DmaapConsumerConfiguration resolveConfiguration() {
        return datafileAppConfig.getDmaapConsumerConfiguration();
    }

    @Override
    protected DmaapConsumerReactiveHttpClient resolveClient() {
        return dmaaPConsumerReactiveHttpClient == null
                ? new DmaapConsumerReactiveHttpClient(resolveConfiguration()).createDmaapWebClient(buildWebClient())
                : dmaaPConsumerReactiveHttpClient;
    }
}
