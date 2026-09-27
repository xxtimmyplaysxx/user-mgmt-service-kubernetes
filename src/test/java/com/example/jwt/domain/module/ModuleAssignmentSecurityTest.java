package com.example.jwt.domain.module;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.jwt.domain.user.User;
import com.example.jwt.domain.user.UserDetailsImpl;
import com.example.jwt.domain.user.UserService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class ModuleAssignmentSecurityTest {
  @Configuration
  @EnableMethodSecurity
  static class Config {
    @Bean UserService users() { return mock(UserService.class); }
    @Bean ModuleClient modules() { return mock(ModuleClient.class); }
    @Bean ModuleAssignmentController controller(UserService users, ModuleClient modules) {
      return new ModuleAssignmentController(users, modules);
    }
  }

  @Test void onlyOwnerOrUserModifyAuthorityMayAssign() {
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      User user = new User();
      user.setId(UUID.randomUUID());
      UUID other = UUID.randomUUID();
      var users = context.getBean(UserService.class);
      var modules = context.getBean(ModuleClient.class);
      var controller = context.getBean(ModuleAssignmentController.class);
      when(users.existsById(any())).thenReturn(true);
      var principal = new UserDetailsImpl(user);
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(principal, null, List.of()));
      assertEquals(204, controller.assign(user.getId(), UUID.randomUUID()).getStatusCode().value());
      clearInvocations(modules);
      assertThrows(AccessDeniedException.class, () -> controller.assign(other, UUID.randomUUID()));
      verifyNoInteractions(modules);
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(principal, null,
              List.of(new SimpleGrantedAuthority("USER_MODIFY"))));
      assertEquals(204, controller.assign(other, UUID.randomUUID()).getStatusCode().value());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
