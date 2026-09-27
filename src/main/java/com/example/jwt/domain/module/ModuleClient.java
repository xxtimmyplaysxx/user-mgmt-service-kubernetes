package com.example.jwt.domain.module;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Only idempotent GET and PUT calls are retried. No direct MySQL access. */
@Component
public class ModuleClient {
  private final HttpClient http;
  private final String baseUrl;
  private final Duration timeout;
  private final CircuitBreaker breaker;
  private final Retry retry;

  public ModuleClient(@Value("${module-service.base-url}") String baseUrl,
      @Value("${module-service.connect-timeout-ms}") int connectTimeout,
      @Value("${module-service.request-timeout-ms}") int requestTimeout) {
    this.baseUrl = baseUrl.replaceAll("/+$", "");
    this.timeout = Duration.ofMillis(requestTimeout);
    this.http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeout)).build();
    this.breaker = CircuitBreaker.of("module-service", CircuitBreakerConfig.custom()
        .slidingWindowSize(5).minimumNumberOfCalls(5).failureRateThreshold(50)
        .waitDurationInOpenState(Duration.ofSeconds(15)).permittedNumberOfCallsInHalfOpenState(2)
        .recordExceptions(TemporaryFailure.class).ignoreExceptions(ResponseStatusException.class)
        .build());
    this.retry = Retry.of("module-service", RetryConfig.custom().maxAttempts(3)
        .waitDuration(Duration.ofMillis(200)).retryExceptions(TemporaryFailure.class).build());
  }

  public void assign(UUID userId, UUID moduleId) {
    execute("GET", "/api/v1/modules/" + moduleId);
    execute("PUT", "/api/v1/users/" + userId + "/modules/" + moduleId);
  }

  private void execute(String method, String path) {
    // One circuit-breaker sample per logical call, including its retry attempts.
    Supplier<Integer> call = Retry.decorateSupplier(retry, () -> send(method, path));
    try {
      breaker.executeSupplier(call);
    } catch (TemporaryFailure | CallNotPermittedException ex) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Module service temporarily unavailable; retry later");
    }
  }

  private int send(String method, String path) {
    HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
        .timeout(timeout).method(method, HttpRequest.BodyPublishers.noBody()).build();
    try {
      int status = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
      if (status == 404) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Module does not exist");
      }
      if (status >= 500 || status == 429) {
        throw new TemporaryFailure();
      }
      if (status < 200 || status >= 300) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unexpected module service response");
      }
      return status;
    } catch (IOException ex) {
      throw new TemporaryFailure();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Module request interrupted");
    }
  }

  private static class TemporaryFailure extends RuntimeException { }
}
