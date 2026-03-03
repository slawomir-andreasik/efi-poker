package com.andreasik.efipoker.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.andreasik.efipoker.auth.UserEntity;
import com.andreasik.efipoker.auth.UserEntityMapper;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("ProjectEntityMapper")
class ProjectEntityMapperTest extends BaseUnitTest {

  private final ProjectEntityMapper mapper =
      new ProjectEntityMapperImpl(Mappers.getMapper(UserEntityMapper.class));

  @Nested
  @DisplayName("toDomain")
  class ToDomain {

    @Test
    void should_map_all_fields() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      UserEntity createdBy = UserEntity.builder().id(UUID.randomUUID()).username("admin").build();
      ProjectEntity entity =
          ProjectEntity.builder()
              .id(id)
              .name("Sprint 42")
              .slug("sprint-42")
              .adminCode("secret")
              .createdBy(createdBy)
              .createdAt(now)
              .build();

      // Act
      Project result = mapper.toDomain(entity);

      // Assert
      assertAll(
          () -> assertThat(result.id()).isEqualTo(id),
          () -> assertThat(result.name()).isEqualTo("Sprint 42"),
          () -> assertThat(result.slug()).isEqualTo("sprint-42"),
          () -> assertThat(result.adminCode()).isEqualTo("secret"),
          () -> assertThat(result.createdBy()).isNotNull(),
          () -> assertThat(result.createdBy().username()).isEqualTo("admin"),
          () -> assertThat(result.createdAt()).isEqualTo(now));
    }

    @Test
    void should_map_with_null_created_by() {
      // Arrange
      ProjectEntity entity =
          ProjectEntity.builder().id(UUID.randomUUID()).name("Project").slug("project").build();

      // Act
      Project result = mapper.toDomain(entity);

      // Assert
      assertThat(result.createdBy()).isNull();
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
      Project domain =
          Project.builder()
              .id(id)
              .name("Sprint 42")
              .slug("sprint-42")
              .adminCode("secret")
              .createdAt(now)
              .build();

      // Act
      ProjectEntity result = mapper.toEntity(domain);

      // Assert
      assertAll(
          () -> assertThat(result.getId()).isEqualTo(id),
          () -> assertThat(result.getName()).isEqualTo("Sprint 42"),
          () -> assertThat(result.getSlug()).isEqualTo("sprint-42"),
          () -> assertThat(result.getAdminCode()).isEqualTo("secret"),
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
      ProjectEntity entity1 =
          ProjectEntity.builder().id(UUID.randomUUID()).name("Project 1").slug("p1").build();
      ProjectEntity entity2 =
          ProjectEntity.builder().id(UUID.randomUUID()).name("Project 2").slug("p2").build();

      // Act
      List<Project> result = mapper.toDomainList(List.of(entity1, entity2));

      // Assert
      assertAll(
          () -> assertThat(result).hasSize(2),
          () -> assertThat(result.get(0).name()).isEqualTo("Project 1"),
          () -> assertThat(result.get(1).name()).isEqualTo("Project 2"));
    }

    @Test
    void should_return_null_for_null() {
      assertThat(mapper.toDomainList(null)).isNull();
    }
  }
}
