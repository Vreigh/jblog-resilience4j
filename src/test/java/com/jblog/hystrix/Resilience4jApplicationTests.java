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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 8081)
class Resilience4jApplicationTests {

    private static final String SERVER_ERROR_NAME = "org.springframework.web.client.HttpServerErrorException$InternalServerError";
    private static final String CIRCUIT_BREAKER_ERROR_NAME = "io.github.resilience4j.circuitbreaker.CallNotPermittedException";
    private static final String BULKHEAD_ERROR_NAME = "io.github.resilience4j.bulkhead.BulkheadFullException";
    private static final String RATELIMITER_ERROR_NAME = "io.github.resilience4j.ratelimiter.RequestNotPermitted";

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
        String details = taskService.getTaskDetailsCircuitBreaker(1);
        assertThat(details).isEqualTo("Task 1 details");
    }

    @Test
    void testFailPath() {
        String details = taskService.getTaskDetailsCircuitBreaker(2);
        assertThat(details).isEqualTo("Default fallback with error " + SERVER_ERROR_NAME);
    }

    @Test
    void testCircuitBreaker() {
        IntStream.rangeClosed(1, 5).forEach(i -> {
            String details = taskService.getTaskDetailsCircuitBreaker(2);
            assertThat(details).isEqualTo("Default fallback with error " + SERVER_ERROR_NAME);
        });
        IntStream.rangeClosed(1, 5).forEach(i -> {
            String details = taskService.getTaskDetailsCircuitBreaker(2);
            assertThat(details).isEqualTo("Default fallback with error " + CIRCUIT_BREAKER_ERROR_NAME);
        });
        Mockito.verify(restTemplate, Mockito.times(5)).getForObject("/task/{id}", String.class, 2);
    }

    @Test
    public void testRetry() {
        String result1 = taskService.getTaskDetailsRetry(1);
        assertThat(result1).isEqualTo("Task 1 details");
        Mockito.verify(restTemplate, Mockito.times(1)).getForObject("/task/{id}", String.class, 1);

        String result2 = taskService.getTaskDetailsRetry(2);
        assertThat(result2).isEqualTo("Default fallback with error " + SERVER_ERROR_NAME);
        Mockito.verify(restTemplate, Mockito.times(3)).getForObject("/task/{id}", String.class, 2);
    }

    @Test
    public void testBulkhead() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger failCounter = new AtomicInteger(0);

        IntStream.rangeClosed(1, 5)
                .forEach(i -> executorService.execute(() -> {
                    String result = taskService.getTaskDetailsBulkhead(1);
                    if (result.equals("Default fallback with error " + BULKHEAD_ERROR_NAME)) {
                        failCounter.incrementAndGet();
                    } else if (result.equals("Task 1 details")) {
                        successCounter.incrementAndGet();
                    }
                    latch.countDown();
                }));
        latch.await();
        executorService.shutdown();

        assertThat(successCounter.get()).isEqualTo(3);
        assertThat(failCounter.get()).isEqualTo(2);
        Mockito.verify(restTemplate, Mockito.times(3)).getForObject("/task/{id}", String.class, 1);
    }

    @Test
    public void testRateLimiter() {
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger failCounter = new AtomicInteger(0);

        IntStream.rangeClosed(1, 10)
                .parallel()
                .forEach(i -> {
                    String result = taskService.getTaskDetailsRatelimiter(1);
                    if (result.equals("Default fallback with error " + RATELIMITER_ERROR_NAME)) {
                        failCounter.incrementAndGet();
                    } else if (result.equals("Task 1 details")) {
                        successCounter.incrementAndGet();
                    }
                });

        assertThat(successCounter.get()).isEqualTo(5);
        assertThat(failCounter.get()).isEqualTo(5);
        Mockito.verify(restTemplate, Mockito.times(5)).getForObject("/task/{id}", String.class, 1);
    }

}
