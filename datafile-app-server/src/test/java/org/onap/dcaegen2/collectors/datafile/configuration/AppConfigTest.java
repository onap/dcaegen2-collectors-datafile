/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2018, 2020 NOKIA Intellectual Property, 2018-2019 Nordix Foundation. All rights reserved.
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

package org.onap.dcaegen2.collectors.datafile.configuration;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.dcaegen2.collectors.datafile.exceptions.DatafileTaskException;
import org.onap.dcaegen2.collectors.datafile.model.logging.MappedDiagnosticContext;
import org.onap.dcaegen2.collectors.datafile.utils.LoggingUtils;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClient;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.CbsClientConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.MessageRouterSubscribeRequest;
import org.onap.dcaegen2.services.sdk.security.ssl.SecurityKeys;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the AppConfig.
 *
 * @author <a href="mailto:przemyslaw.wasala@nokia.com">Przemysław Wąsala</a> on 4/9/18
 * @author <a href="mailto:henrik.b.andersson@est.tech">Henrik Andersson</a>
 */
public class AppConfigTest {

    public static final String CHANGE_IDENTIFIER = "PM_MEAS_FILES";

    private static final PublisherConfiguration CORRECT_PUBLISHER_CONFIG = //
        ImmutablePublisherConfiguration.builder() //
            .publishUrl("https://localhost:3907/publish/1") //
            .logUrl("https://localhost:3907/feedlog/1") //
            .trustStorePath("src/test/resources/trust.jks") //
            .trustStorePasswordPath("src/test/resources/trust.pass") //
            .keyStorePath("src/test/resources/cert.jks") //
            .keyStorePasswordPath("src/test/resources/jks.pass") //
            .enableDmaapCertAuth(true) //
            .changeIdentifier("PM_MEAS_FILES") //
            .userName("CYE9fl40") //
            .password("izBJD8nLjawq0HMG") //
            .build();

    private static final ImmutableFtpesConfig CORRECT_FTPES_CONFIGURATION = //
        new ImmutableFtpesConfig.Builder() //
            .keyCert("/src/test/resources/dfc.jks") //
            .keyPasswordPath("/src/test/resources/dfc.jks.pass") //
            .trustedCa("/src/test/resources/ftp.jks") //
            .trustedCaPasswordPath("/src/test/resources/ftp.jks.pass") //
            .build();

    private AppConfig appConfigUnderTest;
    private final Map<String, String> context = MappedDiagnosticContext.initializeTraceContext();
    CbsClient cbsClient = mock(CbsClient.class);
    CbsClientConfiguration cbsClientConfiguration = mock(CbsClientConfiguration.class);

    @BeforeEach
    void setUp() {
        appConfigUnderTest = spy(AppConfig.class);
        appConfigUnderTest.systemEnvironment = new Properties();

    }

    @Test
    public void whenTheConfigurationFits() throws IOException, DatafileTaskException {
        // When
        doReturn(getCorrectJson()).when(appConfigUnderTest).createInputStream(any());
        appConfigUnderTest.initialize();

        // Then
        verify(appConfigUnderTest, times(1)).loadConfigurationFromFile();

        ConsumerConfiguration consumerCfg = appConfigUnderTest.getDmaapConsumerConfiguration();
        Assertions.assertNotNull(consumerCfg);
        assertThat(consumerCfg).satisfies(this::checkCorrectConsumerConfiguration);

        PublisherConfiguration publisherCfg = appConfigUnderTest.getPublisherConfiguration(CHANGE_IDENTIFIER);
        Assertions.assertNotNull(publisherCfg);
        assertThat(publisherCfg).isEqualToComparingFieldByField(CORRECT_PUBLISHER_CONFIG);

        FtpesConfig ftpesConfig = appConfigUnderTest.getFtpesConfiguration();
        assertThat(ftpesConfig).isNotNull();
        assertThat(ftpesConfig).isEqualToComparingFieldByField(CORRECT_FTPES_CONFIGURATION);
    }

    @Test
    public void whenTheConfigurationFits_twoProducers() throws IOException, DatafileTaskException {
        // When
        doReturn(getCorrectJsonTwoProducers()).when(appConfigUnderTest).createInputStream(any());
        appConfigUnderTest.loadConfigurationFromFile();

        // Then
        verify(appConfigUnderTest, times(1)).loadConfigurationFromFile();
        Assertions.assertNotNull(appConfigUnderTest.getDmaapConsumerConfiguration());
        Assertions.assertNotNull(appConfigUnderTest.getPublisherConfiguration(CHANGE_IDENTIFIER));
        Assertions.assertNotNull(appConfigUnderTest.getPublisherConfiguration("XX_FILES"));
        Assertions.assertNotNull(appConfigUnderTest.getPublisherConfiguration("YY_FILES"));

        assertThat(appConfigUnderTest.getPublisherConfiguration("XX_FILES").publishUrl())
            .isEqualTo("feed01::publish_url");
        assertThat(appConfigUnderTest.getPublisherConfiguration("YY_FILES").publishUrl())
            .isEqualTo("feed01::publish_url");
    }

    @Test
    public void whenFileIsNotExist_ThrowException() throws DatafileTaskException {
        // Given
        appConfigUnderTest.setFilepath("/temp.json");

        // When
        appConfigUnderTest.loadConfigurationFromFile();

        // Then
        Assertions.assertNull(appConfigUnderTest.getDmaapConsumerConfiguration());
        assertThatThrownBy(() -> appConfigUnderTest.getPublisherConfiguration(CHANGE_IDENTIFIER))
            .hasMessageContaining("No PublishingConfiguration loaded, changeIdentifier: PM_MEAS_FILES");

        Assertions.assertNull(appConfigUnderTest.getFtpesConfiguration());
    }

    @Test
    public void whenFileIsExistsButJsonIsIncorrect() throws IOException, DatafileTaskException {

        // When
        doReturn(getIncorrectJson()).when(appConfigUnderTest).createInputStream(any());
        appConfigUnderTest.loadConfigurationFromFile();

        // Then
        verify(appConfigUnderTest, times(1)).loadConfigurationFromFile();
        Assertions.assertNull(appConfigUnderTest.getDmaapConsumerConfiguration());
        assertThatThrownBy(() -> appConfigUnderTest.getPublisherConfiguration(CHANGE_IDENTIFIER))
            .hasMessageContaining(CHANGE_IDENTIFIER);
        Assertions.assertNull(appConfigUnderTest.getFtpesConfiguration());
    }

    @Test
    public void whenTheConfigurationFits_ButRootElementIsNotAJsonObject() throws IOException, DatafileTaskException {

        // When
        doReturn(getCorrectJson()).when(appConfigUnderTest).createInputStream(any());
        JsonElement jsonElement = mock(JsonElement.class);
        when(jsonElement.isJsonObject()).thenReturn(false);
        doReturn(jsonElement).when(appConfigUnderTest).getJsonElement(any(InputStream.class));
        appConfigUnderTest.loadConfigurationFromFile();

        // Then
        verify(appConfigUnderTest, times(1)).loadConfigurationFromFile();
        Assertions.assertNull(appConfigUnderTest.getDmaapConsumerConfiguration());
        assertThatThrownBy(() -> appConfigUnderTest.getPublisherConfiguration(CHANGE_IDENTIFIER))
            .hasMessageContaining(CHANGE_IDENTIFIER);
        Assertions.assertNull(appConfigUnderTest.getFtpesConfiguration());
    }

    @Test
    public void whenPeriodicConfigRefreshNoEnvironmentVariables() {
        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(AppConfig.class);
        Flux<AppConfig> task = appConfigUnderTest.createRefreshTask(context);

        StepVerifier //
            .create(task) //
            .expectSubscription() //
            .verifyComplete(); //

        assertTrue(logAppender.list.toString().contains("CbsClientConfigurationException"));
    }

    @Test
    public void whenPeriodicConfigRefreshNoConsul() {
        doReturn(Mono.just(cbsClientConfiguration)).when(appConfigUnderTest).createCbsClientConfiguration();
        doReturn(Mono.just(cbsClient)).when(appConfigUnderTest).createCbsClient(cbsClientConfiguration);
        Flux<JsonObject> err = Flux.error(new IOException());
        doReturn(err).when(cbsClient).updates(any(), any(), any());

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(AppConfig.class);
        Flux<AppConfig> task = appConfigUnderTest.createRefreshTask(context);

        StepVerifier //
            .create(task) //
            .expectSubscription() //
            .verifyComplete();

        assertTrue(
            logAppender.list.toString().contains("Could not refresh application configuration java.io.IOException"));
    }

    @Test
    public void whenPeriodicConfigRefreshSuccess() throws JsonIOException, JsonSyntaxException, IOException {
        doReturn(Mono.just(cbsClientConfiguration)).when(appConfigUnderTest).createCbsClientConfiguration();
        doReturn(Mono.just(cbsClient)).when(appConfigUnderTest).createCbsClient(cbsClientConfiguration);

        Flux<JsonObject> json = Flux.just(getJsonRootObject());
        doReturn(json).when(cbsClient).updates(any(), any(), any());

        Flux<AppConfig> task = appConfigUnderTest.createRefreshTask(context);

        StepVerifier //
            .create(task) //
            .expectSubscription() //
            .expectNext(appConfigUnderTest) //
            .verifyComplete();

        Assertions.assertNotNull(appConfigUnderTest.getDmaapConsumerConfiguration());
    }

    @Test
    public void whenPeriodicConfigRefreshSuccess2() throws JsonIOException, JsonSyntaxException, IOException {
        doReturn(Mono.just(cbsClientConfiguration)).when(appConfigUnderTest).createCbsClientConfiguration();
        doReturn(Mono.just(cbsClient)).when(appConfigUnderTest).createCbsClient(cbsClientConfiguration);

        Flux<JsonObject> json = Flux.just(getJsonRootObject());
        Flux<JsonObject> err = Flux.error(new IOException()); // no config entry created by the
        // dmaap plugin

        doReturn(json, err).when(cbsClient).updates(any(), any(), any());

        Flux<AppConfig> task = appConfigUnderTest.createRefreshTask(context);

        StepVerifier //
            .create(task) //
            .expectSubscription() //
            .expectNext(appConfigUnderTest) //
            .verifyComplete();

        Assertions.assertNotNull(appConfigUnderTest.getDmaapConsumerConfiguration());
    }

    private void checkCorrectConsumerConfiguration(ConsumerConfiguration consumerConfiguration) {
        MessageRouterSubscribeRequest messageRouterSubscribeRequest =
                consumerConfiguration.getMessageRouterSubscribeRequest();
        assertThat(messageRouterSubscribeRequest.consumerGroup()).isEqualTo("OpenDcae-c12");
        assertThat(messageRouterSubscribeRequest.consumerId()).isEqualTo("C12");
        assertThat(messageRouterSubscribeRequest.sourceDefinition().topicUrl())
                .isEqualTo("http://localhost:2222/events/unauthenticated.VES_NOTIFICATION_OUTPUT");
        SecurityKeys securityKeys = consumerConfiguration.getMessageRouterSubscriberConfig().securityKeys();
        assertThat(securityKeys.keyStore().path().toString()).isEqualTo("src/test/resources/cert.jks");
        assertThat(securityKeys.trustStore().path().toString()).isEqualTo("src/test/resources/trust.jks");
        assertThat(consumerConfiguration.getMessageRouterSubscriber()).isNotNull();
    }

    private JsonObject getJsonRootObject() throws JsonIOException, JsonSyntaxException, IOException {
        JsonObject rootObject = JsonParser.parseReader(new InputStreamReader(getCorrectJson())).getAsJsonObject();
        return rootObject;
    }

    private static InputStream getCorrectJson() throws IOException {
        URL url = CloudConfigParser.class.getClassLoader().getResource("datafile_endpoints_test.json");
        String string = Resources.toString(url, Charsets.UTF_8);
        return new ByteArrayInputStream((string.getBytes(StandardCharsets.UTF_8)));
    }

    private static InputStream getCorrectJsonTwoProducers() throws IOException {
        URL url = CloudConfigParser.class.getClassLoader().getResource("datafile_endpoints_test_2producers.json");
        String string = Resources.toString(url, Charsets.UTF_8);
        return new ByteArrayInputStream((string.getBytes(StandardCharsets.UTF_8)));
    }

    private static InputStream getIncorrectJson() {
        String string = "{" + //
            "    \"configs\": {" + //
            "        \"dmaap\": {"; //
        return new ByteArrayInputStream((string.getBytes(StandardCharsets.UTF_8)));
    }
}
