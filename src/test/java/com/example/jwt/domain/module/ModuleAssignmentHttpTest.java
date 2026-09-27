package com.example.jwt.domain.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.example.jwt.domain.user.User;
import com.example.jwt.domain.user.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

/** A real servlet container is needed to exercise the /error redispatch. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:module-http;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
    "jwt.issuer=http-test", "jwt.expiration-millis=60000",
    "jwt.secret=" + ModuleAssignmentHttpTest.TEST_SECRET
})
class ModuleAssignmentHttpTest {
  // Public test fixture, never used by a deployed application.
  static final String TEST_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
  @Value("${local.server.port}") int port;
  @MockitoBean UserService users;
  @MockitoBean ModuleClient modules;
  private final HttpClient http = HttpClient.newHttpClient();
  private UUID userId;
  private String token;

  @BeforeEach void authenticateTestUser() {
    userId = UUID.randomUUID();
    var user = new User();
    user.setId(userId);
    when(users.findById(userId)).thenReturn(user);
    when(users.existsById(userId)).thenReturn(true);
    token = "Bearer " + Jwts.builder().subject(userId.toString())
        .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET))).compact();
  }

  private int put(String path, String authorization) throws Exception {
    var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
        .timeout(Duration.ofSeconds(10)).PUT(HttpRequest.BodyPublishers.noBody());
    if (authorization != null) request.header("Authorization", authorization);
    return http.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
  }

  private String assignment(UUID id, String module) {
    return "/users/" + id + "/modules/" + module;
  }

  @ParameterizedTest @ValueSource(ints = {404, 502, 503})
  void preservesModuleErrorStatusThroughServletErrorDispatch(int status) throws Exception {
    doThrow(new ResponseStatusException(HttpStatus.valueOf(status), "Module request failed"))
        .when(modules).assign(any(), any());
    assertEquals(status, put(assignment(userId, UUID.randomUUID().toString()), token));
    verify(modules).assign(eq(userId), any());
  }

  @Test void rejectsMalformedModuleIdWithBadRequest() throws Exception {
    assertEquals(400, put(assignment(userId, "invalid"), token));
    verifyNoInteractions(modules);
  }

  @Test void stillRequiresAuthentication() throws Exception {
    assertEquals(403, put(assignment(userId, UUID.randomUUID().toString()), null));
    assertEquals(403, put("/error", null));
    verifyNoInteractions(modules);
  }

  @Test void stillRejectsAnotherUsersAssignment() throws Exception {
    assertEquals(403, put(assignment(UUID.randomUUID(), UUID.randomUUID().toString()), token));
    verifyNoInteractions(modules);
  }

  @Test void validAssignmentRemainsSuccessful() throws Exception {
    assertEquals(204, put(assignment(userId, UUID.randomUUID().toString()), token));
    verify(modules).assign(eq(userId), any());
  }
}
