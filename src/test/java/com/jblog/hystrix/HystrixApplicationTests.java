package com.jblog.hystrix;

import com.jblog.hystrix.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.web.client.RestTemplate;

import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 8081)
class HystrixApplicationTests {

    private static final String SERVER_ERROR_NAME = "org.springframework.web.client.HttpServerErrorException$InternalServerError";
    private static final String CIRCUIT_BREAKER_ERROR_NAME = "io.github.resilience4j.circuitbreaker.CallNotPermittedException";

    @Autowired
    private TaskService taskService;
    @SpyBean
    private RestTemplate restTemplate;

    @BeforeEach
    void initWireMock() {
        stubFor(get(urlEqualTo("/task/1")).willReturn(aResponse().withBody("Task 1 details")));
        stubFor(get(urlEqualTo("/task/2")).willReturn(serverError().withBody("Task 2 details failed")));
    }

    @Test
    void testHappyPath() {
        String details = taskService.getTaskDetails(1);
        assertThat(details).isEqualTo("Task 1 details");
    }

    @Test
    void testFailPath() {
        String details = taskService.getTaskDetails(2);
        assertThat(details).isEqualTo("Default fallback with error " + SERVER_ERROR_NAME);
    }

    @Test
    void testCircuitBreaker() {
        IntStream.rangeClosed(1, 5).forEach(i -> {
            String details = taskService.getTaskDetails(2);
            assertThat(details).isEqualTo("Default fallback with error " + SERVER_ERROR_NAME);
        });
        IntStream.rangeClosed(1, 5).forEach(i -> {
            String details = taskService.getTaskDetails(2);
            assertThat(details).isEqualTo("Default fallback with error " + CIRCUIT_BREAKER_ERROR_NAME);
        });
        Mockito.verify(restTemplate, Mockito.times(5)).getForObject("/task/{id}", String.class, 2);
    }

    // one has to call these methods manually

}
