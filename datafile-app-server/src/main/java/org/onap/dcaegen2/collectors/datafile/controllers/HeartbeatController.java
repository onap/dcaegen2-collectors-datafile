/*
 * ============LICENSE_START=======================================================
 * Datafile Collector Service
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

package org.onap.dcaegen2.collectors.datafile.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 4/19/18
 */
@RestController
@Api(value = "HeartbeatController", description = "Check liveness of Datafile service")
public class HeartbeatController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Endpoint for checking that Datafile is alive.
     *
     * @return HTTP Status Code
     */
    @RequestMapping(value = "heartbeat", method = RequestMethod.GET)
    @ApiOperation(value = "Returns liveness of Datafile service")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Datafile sevice is living"),
        @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
        @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
        @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
        }
    )
    public Mono<ResponseEntity<String>> heartbeat() {
        logger.trace("Receiving heartbeat request");
        return Mono.defer(() ->
            Mono.just(new ResponseEntity<>("alive", HttpStatus.OK))
        );
    }
}