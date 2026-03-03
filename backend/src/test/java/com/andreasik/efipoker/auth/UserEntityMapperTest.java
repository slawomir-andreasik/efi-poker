package com.andreasik.efipoker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("UserEntityMapper")
class UserEntityMapperTest extends BaseUnitTest {

  private final UserEntityMapper mapper = Mappers.getMapper(UserEntityMapper.class);

  @Nested
  @DisplayName("toDomain")
  class ToDomain {

    @Test
    void should_map_all_fields() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      UserEntity entity =
          UserEntity.builder()
              .id(id)
              .username("admin")
              .email("admin@example.com")
              .passwordHash("hashed")
              .authProvider("LOCAL")
              .authProviderId("local-1")
              .role("ADMIN")
              .createdAt(now)
              .lastLoginAt(now)
              .build();

      // Act
      User result = mapper.toDomain(entity);

      // Assert
      assertAll(
          () -> assertThat(result.id()).isEqualTo(id),
          () -> assertThat(result.username()).isEqualTo("admin"),
          () -> assertThat(result.email()).isEqualTo("admin@example.com"),
          () -> assertThat(result.passwordHash()).isEqualTo("hashed"),
          () -> assertThat(result.authProvider()).isEqualTo("LOCAL"),
          () -> assertThat(result.authProviderId()).isEqualTo("local-1"),
          () -> assertThat(result.role()).isEqualTo("ADMIN"),
          () -> assertThat(result.createdAt()).isEqualTo(now),
          () -> assertThat(result.lastLoginAt()).isEqualTo(now));
    }

    @Test
    void should_return_null_for_null() {
      assertThat(mapper.toDomain(null)).isNull();
    }
  }

  @Nested
  @DisplayName("toEntity")
  class ToEntity {

    @Test
    void should_map_all_fields() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      User domain =
          User.builder()
              .id(id)
              .username("admin")
              .email("admin@example.com")
              .passwordHash("hashed")
              .authProvider("LOCAL")
              .role("ADMIN")
              .createdAt(now)
              .build();

      // Act
      UserEntity result = mapper.toEntity(domain);

      // Assert
      assertAll(
          () -> assertThat(result.getId()).isEqualTo(id),
          () -> assertThat(result.getUsername()).isEqualTo("admin"),
          () -> assertThat(result.getEmail()).isEqualTo("admin@example.com"),
          () -> assertThat(result.getRole()).isEqualTo("ADMIN"),
          () -> assertThat(result.getCreatedAt()).isEqualTo(now));
    }

    @Test
    void should_return_null_for_null() {
      assertThat(mapper.toEntity(null)).isNull();
    }
  }

  @Nested
  @DisplayName("toDomainList")
  class ToDomainList {

    @Test
    void should_map_list() {
      // Arrange
      UserEntity entity1 = UserEntity.builder().username("alice").role("USER").build();
      UserEntity entity2 = UserEntity.builder().username("bob").role("ADMIN").build();

      // Act
      List<User> result = mapper.toDomainList(List.of(entity1, entity2));

      // Assert
      assertAll(
          () -> assertThat(result).hasSize(2),
          () -> assertThat(result.get(0).username()).isEqualTo("alice"),
          () -> assertThat(result.get(1).username()).isEqualTo("bob"));
    }

    @Test
    void should_return_null_for_null() {
      assertThat(mapper.toDomainList(null)).isNull();
    }
  }
}
