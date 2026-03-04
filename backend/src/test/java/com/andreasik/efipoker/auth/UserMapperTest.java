package com.andreasik.efipoker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.andreasik.efipoker.api.model.UserResponse;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;

@DisplayName("UserMapper")
class UserMapperTest extends BaseUnitTest {

  private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

  @Nested
  @DisplayName("toResponse")
  class ToResponse {

    @Test
    void should_map_all_fields() {
      // Arrange
      UUID id = UUID.randomUUID();
      User user =
          User.builder()
              .id(id)
              .username("john.doe")
              .email("john@example.com")
              .passwordHash("$2a$10$hashedpassword")
              .authProvider("LOCAL")
              .role("ADMIN")
              .createdAt(Instant.now())
              .build();

      // Act
      UserResponse response = mapper.toResponse(user);

      // Assert
      assertAll(
          () -> assertThat(response.getId()).isEqualTo(id),
          () -> assertThat(response.getUsername()).isEqualTo("john.doe"),
          () -> assertThat(response.getEmail()).isEqualTo("john@example.com"),
          () -> assertThat(response.getHasPassword()).isTrue(),
          () -> assertThat(response.getAuthProvider()).isEqualTo("LOCAL"));
    }

    @Test
    void should_map_has_password_false_when_no_password() {
      // Arrange
      User user =
          User.builder()
              .id(UUID.randomUUID())
              .username("auth0user")
              .authProvider("AUTH0")
              .role("USER")
              .createdAt(Instant.now())
              .build();

      // Act
      UserResponse response = mapper.toResponse(user);

      // Assert
      assertAll(
          () -> assertThat(response.getHasPassword()).isFalse(),
          () -> assertThat(response.getAuthProvider()).isEqualTo("AUTH0"));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void should_map_role_to_enum(UserRole entityRole) {
      // Arrange
      User user =
          User.builder()
              .id(UUID.randomUUID())
              .username("test")
              .role(entityRole.name())
              .createdAt(Instant.now())
              .build();

      // Act
      UserResponse response = mapper.toResponse(user);

      // Assert
      assertThat(response.getRole().getValue()).isEqualTo(entityRole.name());
    }

    @Test
    void should_return_null_for_null() {
      // Act
      UserResponse response = mapper.toResponse(null);

      // Assert
      assertThat(response).isNull();
    }
  }
}
