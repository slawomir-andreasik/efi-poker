package com.andreasik.efipoker.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.andreasik.efipoker.api.model.ProjectAdminResponse;
import com.andreasik.efipoker.api.model.ProjectResponse;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("ProjectMapper")
class ProjectMapperTest extends BaseUnitTest {

  private final ProjectMapper mapper = Mappers.getMapper(ProjectMapper.class);

  @Nested
  @DisplayName("toResponse")
  class ToResponse {

    @Test
    void should_map_all_fields() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      Project project =
          Project.builder()
              .id(id)
              .name("Sprint 42")
              .slug("sprint-42")
              .adminCode("secret-code")
              .createdBy(null)
              .createdAt(now)
              .build();

      // Act
      ProjectResponse response = mapper.toResponse(project);

      // Assert
      assertAll(
          () -> assertThat(response.getId()).isEqualTo(id),
          () -> assertThat(response.getName()).isEqualTo("Sprint 42"),
          () -> assertThat(response.getSlug()).isEqualTo("sprint-42"),
          () -> assertThat(response.getCreatedAt()).isEqualTo(now));
    }

    @Test
    void should_return_null_for_null() {
      // Act
      ProjectResponse response = mapper.toResponse(null);

      // Assert
      assertThat(response).isNull();
    }
  }

  @Nested
  @DisplayName("toAdminResponse")
  class ToAdminResponse {

    @Test
    void should_include_admin_code() {
      // Arrange
      Project project =
          Project.builder()
              .id(UUID.randomUUID())
              .name("Sprint 42")
              .slug("sprint-42")
              .adminCode("secret-admin-code")
              .createdAt(Instant.now())
              .build();

      // Act
      ProjectAdminResponse response = mapper.toAdminResponse(project);

      // Assert
      assertThat(response.getAdminCode()).isEqualTo("secret-admin-code");
    }
  }

  @Nested
  @DisplayName("toResponseList")
  class ToResponseList {

    @Test
    void should_map_list() {
      // Arrange
      Project project1 =
          Project.builder()
              .id(UUID.randomUUID())
              .name("Sprint 1")
              .slug("s1")
              .adminCode("c1")
              .createdAt(Instant.now())
              .build();
      Project project2 =
          Project.builder()
              .id(UUID.randomUUID())
              .name("Sprint 2")
              .slug("s2")
              .adminCode("c2")
              .createdAt(Instant.now())
              .build();

      // Act
      List<ProjectResponse> responses = mapper.toResponseList(List.of(project1, project2));

      // Assert
      assertAll(
          () -> assertThat(responses).hasSize(2),
          () -> assertThat(responses.get(0).getName()).isEqualTo("Sprint 1"),
          () -> assertThat(responses.get(1).getName()).isEqualTo("Sprint 2"));
    }
  }
}
