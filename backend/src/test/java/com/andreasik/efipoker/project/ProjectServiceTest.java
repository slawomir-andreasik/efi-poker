package com.andreasik.efipoker.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.andreasik.efipoker.shared.exception.ResourceNotFoundException;
import com.andreasik.efipoker.shared.exception.UnauthorizedException;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

@DisplayName("ProjectService")
class ProjectServiceTest extends BaseUnitTest {

  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectEntityMapper projectEntityMapper;
  @Mock private ApplicationEventPublisher eventPublisher;
  @InjectMocks private ProjectService projectService;

  @Nested
  @DisplayName("validateAdminCode")
  class ValidateAdminCode {

    private final String slug = "test-proj";
    private final String adminCode = "correct-admin-code";

    @Test
    void should_return_project_with_correct_admin_code() {
      // Arrange
      ProjectEntity entity =
          ProjectEntity.builder()
              .id(UUID.randomUUID())
              .adminCode(adminCode)
              .slug(slug)
              .name("Test Project")
              .build();
      Project expectedProject =
          Project.builder()
              .id(entity.getId())
              .name("Test Project")
              .slug(slug)
              .adminCode(adminCode)
              .build();
      given(projectRepository.findBySlug(slug)).willReturn(Optional.of(entity));
      given(projectEntityMapper.toDomain(entity)).willReturn(expectedProject);

      // Act
      Project result = projectService.validateAdminCode(slug, adminCode);

      // Assert
      assertThat(result).isEqualTo(expectedProject);
      assertThat(result.slug()).isEqualTo(slug);
    }

    @Test
    void should_throw_unauthorized_with_wrong_admin_code() {
      // Arrange
      ProjectEntity entity =
          ProjectEntity.builder()
              .id(UUID.randomUUID())
              .adminCode(adminCode)
              .slug(slug)
              .name("Test Project")
              .build();
      given(projectRepository.findBySlug(slug)).willReturn(Optional.of(entity));

      // Act & Assert
      assertThatThrownBy(() -> projectService.validateAdminCode(slug, "wrong-code"))
          .isInstanceOf(UnauthorizedException.class)
          .hasMessageContaining(slug);
    }

    @Test
    void should_throw_not_found_for_nonexistent_project() {
      // Arrange
      given(projectRepository.findBySlug(slug)).willReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> projectService.validateAdminCode(slug, adminCode))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("updateProject")
  class UpdateProject {

    private final UUID projectId = UUID.randomUUID();
    private final String slug = "test-proj";

    @Test
    void should_update_name() {
      // Arrange
      ProjectEntity entity =
          ProjectEntity.builder()
              .id(projectId)
              .adminCode("code")
              .slug(slug)
              .name("Old Name")
              .build();
      Project expectedProject =
          Project.builder().id(projectId).name("New Name").slug(slug).adminCode("code").build();
      given(projectRepository.findById(projectId)).willReturn(Optional.of(entity));
      given(projectRepository.save(entity)).willReturn(entity);
      given(projectEntityMapper.toDomain(entity)).willReturn(expectedProject);

      // Act
      Project result = projectService.updateProject(projectId, "New Name");

      // Assert
      assertThat(result.name()).isEqualTo("New Name");
    }

    @Test
    void should_not_change_name_when_null() {
      // Arrange
      ProjectEntity entity =
          ProjectEntity.builder().id(projectId).adminCode("code").slug(slug).name("Test").build();
      Project expectedProject =
          Project.builder().id(projectId).name("Test").slug(slug).adminCode("code").build();
      given(projectRepository.findById(projectId)).willReturn(Optional.of(entity));
      given(projectRepository.save(entity)).willReturn(entity);
      given(projectEntityMapper.toDomain(entity)).willReturn(expectedProject);

      // Act
      Project result = projectService.updateProject(projectId, null);

      // Assert
      assertThat(result.name()).isEqualTo("Test");
    }

    @Test
    void should_throw_not_found() {
      // Arrange
      UUID unknownId = UUID.randomUUID();
      given(projectRepository.findById(unknownId)).willReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> projectService.updateProject(unknownId, "New"))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("validateAdminCodeForProject")
  class ValidateAdminCodeForProject {

    private final UUID projectId = UUID.randomUUID();
    private final String adminCode = "correct-admin-code";

    @Test
    void should_pass_with_correct_admin_code() {
      // Arrange
      ProjectEntity entity =
          ProjectEntity.builder()
              .id(projectId)
              .adminCode(adminCode)
              .slug("test")
              .name("Test")
              .build();
      given(projectRepository.findById(projectId)).willReturn(Optional.of(entity));

      // Act & Assert
      assertThatCode(() -> projectService.validateAdminCodeForProject(projectId, adminCode))
          .doesNotThrowAnyException();
    }

    @Test
    void should_throw_unauthorized_with_wrong_admin_code() {
      // Arrange
      ProjectEntity entity =
          ProjectEntity.builder()
              .id(projectId)
              .adminCode(adminCode)
              .slug("test")
              .name("Test")
              .build();
      given(projectRepository.findById(projectId)).willReturn(Optional.of(entity));

      // Act & Assert
      assertThatThrownBy(() -> projectService.validateAdminCodeForProject(projectId, "wrong-code"))
          .isInstanceOf(UnauthorizedException.class)
          .hasMessage("Invalid admin code");
    }

    @Test
    void should_throw_not_found_for_nonexistent_project() {
      // Arrange
      given(projectRepository.findById(projectId)).willReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> projectService.validateAdminCodeForProject(projectId, adminCode))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("deleteProject")
  class DeleteProject {

    private final UUID projectId = UUID.randomUUID();

    @Test
    void should_delete_project() {
      // Arrange
      ProjectEntity entity =
          ProjectEntity.builder()
              .id(projectId)
              .adminCode("code")
              .slug("test-slug")
              .name("Test")
              .build();
      given(projectRepository.findById(projectId)).willReturn(Optional.of(entity));

      // Act
      projectService.deleteProject(projectId);

      // Assert
      verify(projectRepository).delete(entity);
    }

    @Test
    void should_throw_not_found_for_unknown_id() {
      // Arrange
      UUID unknownId = UUID.randomUUID();
      given(projectRepository.findById(unknownId)).willReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> projectService.deleteProject(unknownId))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }
}
