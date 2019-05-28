/*-
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

package org.onap.dcaegen2.collectors.datafile.configuration;

import com.google.common.base.Predicates;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


@EnableSwagger2
@Configuration
@Profile("prod")
public class SwaggerConfig extends WebMvcConfigurationSupport {
    static final String API_TITLE = "DATAFILE app server";
    static final String DESCRIPTION = "This page lists all the rest apis for DATAFILE app server.";
    static final String VERSION = "1.0";
    static final String RESOURCES_PATH = "classpath:/META-INF/resources/";
    static final String WEBJARS_PATH = RESOURCES_PATH + "webjars/";
    static final String SWAGGER_UI = "swagger-ui.html";
    static final String WEBJARS = "/webjars/**";

    /**
     * Gets the API info.
     *
     * @return the API info.
     */
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2) //
                .apiInfo(apiInfo()) //
                .select() //
                .apis(RequestHandlerSelectors.any()) //
                .paths(PathSelectors.any()) //
                .paths(Predicates.not(PathSelectors.regex("/error"))) //
                // this endpoint is not implemented, but was visible for Swagger
                .build();
    }

    private static ApiInfo apiInfo() {
        return new ApiInfoBuilder() //
                .title(API_TITLE) //
                .description(DESCRIPTION) //
                .version(VERSION) //
                .build();
    }

    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(SWAGGER_UI) //
                .addResourceLocations(RESOURCES_PATH);

        registry.addResourceHandler(WEBJARS) //
                .addResourceLocations(WEBJARS_PATH);
    }
}
