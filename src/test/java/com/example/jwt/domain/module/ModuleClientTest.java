package com.example.jwt.domain.module;

import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ModuleClientTest {
  private HttpServer server;
  private final AtomicInteger calls = new AtomicInteger();
  private final AtomicInteger status = new AtomicInteger(204);
  private ModuleClient client;

  @BeforeEach void setup() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      calls.incrementAndGet();
      exchange.sendResponseHeaders(status.get(), -1);
      exchange.close();
    });
    server.start();
    client = new ModuleClient("http://127.0.0.1:" + server.getAddress().getPort(), 200, 200);
  }
  @AfterEach void cleanup() { server.stop(0); }
  private void assign() { client.assign(UUID.randomUUID(), UUID.randomUUID()); }

  @Test void verifiesThenAssigns() { assign(); assertEquals(2, calls.get()); }
  @Test void missingModuleIsNotRetriedOrAssigned() {
    status.set(404);
    assertEquals(404, assertThrows(ResponseStatusException.class, this::assign).getStatusCode().value());
    assertEquals(1, calls.get());
  }
  @Test void temporaryFailuresRetryAndThenOpenCircuit() {
    status.set(503);
    for (int i = 0; i < 5; i++) {
      assertEquals(503, assertThrows(ResponseStatusException.class, this::assign).getStatusCode().value());
    }
    assertEquals(15, calls.get());
    assertThrows(ResponseStatusException.class, this::assign);
    assertEquals(15, calls.get(), "Open circuit must not call downstream");
  }
  @Test void recoversFromTransientFailure() {
    server.removeContext("/");
    server.createContext("/", exchange -> {
      int response = calls.incrementAndGet() == 1 ? 503 : 204;
      exchange.sendResponseHeaders(response, -1);
      exchange.close();
    });
    assign();
    assertEquals(3, calls.get());
  }
  @Test void timeoutIsBoundedAndBecomesServiceUnavailable() {
    server.removeContext("/");
    server.createContext("/", exchange -> {
      try { Thread.sleep(1200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
      exchange.close();
    });
    long start = System.nanoTime();
    assertEquals(503, assertThrows(ResponseStatusException.class, this::assign).getStatusCode().value());
    assertTrue((System.nanoTime() - start) / 1_000_000 < 3000);
  }
}
