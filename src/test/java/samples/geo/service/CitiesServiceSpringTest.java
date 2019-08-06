package samples.geo.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;
import samples.geo.domain.City;

import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = CitiesServiceSpringTest.SpringConfig.class)
@ContextConfiguration(initializers = {CitiesServiceSpringTest.PropertiesInitializer.class})
public class CitiesServiceSpringTest {
    private static final WireMockServer WIREMOCK_SERVER =
            new WireMockServer(wireMockConfig().dynamicPort());

    @Autowired
    private CitiesService citiesService;

    @Test
    public void testGetCitiesCleanFlow() throws Exception {
        String resultJson = StreamUtils.copyToString(
                this.getClass().getResourceAsStream("/sample-cities.json"),
                StandardCharsets.UTF_8);

        WIREMOCK_SERVER.stubFor(get(urlEqualTo("/cities"))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(resultJson)));

        StepVerifier
                .create(citiesService.getCities())
                .expectNext(new City(1L, "Portland", "USA", 1_600_000L))
                .verifyComplete();
    }

    @Test
    public void testGetCitiesWithServerError() {
        WIREMOCK_SERVER.stubFor(get(urlEqualTo("/cities"))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Some Internal Error")));

        StepVerifier
                .create(citiesService.getCities())
                .verifyError();
    }

    @BeforeAll
    public static void beforeAll() {
        WIREMOCK_SERVER.start();
    }

    @AfterAll
    public static void afterAll() {
        WIREMOCK_SERVER.stop();
    }



    static class PropertiesInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of(
                    "cityservice.url=" + "http://localhost:" + WIREMOCK_SERVER.port()
            ).applyTo(applicationContext.getEnvironment());
        }
    }

    @SpringBootApplication(scanBasePackageClasses = CitiesService.class)
    static class SpringConfig {
    }
}
