package com.example.jwt.domain.module;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.jwt.domain.user.UserService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ModuleAssignmentControllerTest {
  @Test void missingUserDoesNotCallModuleService() {
    UserService users = mock(UserService.class);
    ModuleClient client = mock(ModuleClient.class);
    var controller = new ModuleAssignmentController(users, client);
    var error = assertThrows(ResponseStatusException.class,
        () -> controller.assign(UUID.randomUUID(), UUID.randomUUID()));
    assertEquals(404, error.getStatusCode().value());
    verifyNoInteractions(client);
  }
  @Test void existingUserIsAssignedAndReturnsNoContent() {
    UserService users = mock(UserService.class);
    ModuleClient client = mock(ModuleClient.class);
    UUID user = UUID.randomUUID(), module = UUID.randomUUID();
    when(users.existsById(user)).thenReturn(true);
    var result = new ModuleAssignmentController(users, client).assign(user, module);
    assertEquals(204, result.getStatusCode().value());
    verify(client).assign(user, module);
  }
}
