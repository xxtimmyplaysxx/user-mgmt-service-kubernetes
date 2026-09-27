package com.example.jwt.domain.module;

import com.example.jwt.domain.user.UserService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ModuleAssignmentController {
  private final UserService users;
  private final ModuleClient modules;

  public ModuleAssignmentController(UserService users, ModuleClient modules) {
    this.users = users;
    this.modules = modules;
  }

  @PutMapping("/users/{userId}/modules/{moduleId}")
  @PreAuthorize("#userId == authentication.principal.user().getId() or hasAuthority('USER_MODIFY')")
  public ResponseEntity<Void> assign(@PathVariable UUID userId, @PathVariable UUID moduleId) {
    if (!users.existsById(userId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User does not exist");
    }
    modules.assign(userId, moduleId);
    return ResponseEntity.noContent().build();
  }
}
