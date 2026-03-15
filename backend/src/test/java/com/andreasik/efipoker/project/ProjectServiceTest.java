package com.andreasik.efipoker.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.andreasik.efipoker.auth.UserEntity;
import com.andreasik.efipoker.shared.exception.ResourceNotFoundException;
import com.andreasik.efipoker.shared.exception.UnauthorizedException;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("ProjectService")
class ProjectServiceTest extends BaseUnitTest {

  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectEntityMapper projectEntityMapper;
  @Mock private ApplicationEventPublisher eventPublisher;
  @InjectMocks private ProjectService projectService;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private void setSiteAdmin() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("admin", null, "ROLE_ADMIN"));
  }

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

    @Test
    void should_return_project_when_site_admin_without_admin_code() {
      // Arrange
      setSiteAdmin();
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
      Project result = projectService.validateAdminCode(slug, "wrong-code");

      // Assert
      assertThat(result).isEqualTo(expectedProject);
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

      // Assert - verify the name was actually set on the entity (guards against removed setName
      // call)
      assertThat(entity.getName()).isEqualTo("New Name");
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

      // Assert - name on entity must be unchanged (guards against removed null-check)
      assertThat(entity.getName()).isEqualTo("Test");
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
  @DisplayName("isOwner (via validateAdminAccess)")
  class IsOwner {

    private final String slug = "owner-proj";
    private final String adminCode = "admin-code";

    @Test
    void should_grant_access_when_user_is_owner() {
      // Arrange
      UUID ownerId = UUID.randomUUID();
      UserEntity owner = UserEntity.builder().id(ownerId).username("owner").build();
      ProjectEntity entity =
          ProjectEntity.builder()
              .id(UUID.randomUUID())
              .adminCode(adminCode)
              .slug(slug)
              .name("Owned Project")
              .createdBy(owner)
              .build();
      Project expectedProject =
          Project.builder().id(entity.getId()).name("Owned Project").slug(slug).build();
      given(projectRepository.findBySlug(slug)).willReturn(Optional.of(entity));
      given(projectEntityMapper.toDomain(entity)).willReturn(expectedProject);

      // Act - wrong admin code, but owner ID matches -> should NOT throw
      Project result = projectService.validateAdminAccess(slug, "wrong-code", ownerId);

      // Assert - owner path returned a project (isOwner returned true)
      assertThat(result).isEqualTo(expectedProject);
    }

    @Test
    void should_deny_access_when_user_is_not_owner() {
      // Arrange
      UUID actualOwnerId = UUID.randomUUID();
      UUID differentUserId = UUID.randomUUID();
      UserEntity owner = UserEntity.builder().id(actualOwnerId).username("owner").build();
      ProjectEntity entity =
          ProjectEntity.builder()
              .id(UUID.randomUUID())
              .adminCode(adminCode)
              .slug(slug)
              .name("Owned Project")
              .createdBy(owner)
              .build();
      given(projectRepository.findBySlug(slug)).willReturn(Optional.of(entity));

      // Act & Assert - different user ID, wrong admin code -> isOwner must return false
      assertThatThrownBy(
              () -> projectService.validateAdminAccess(slug, "wrong-code", differentUserId))
          .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void should_deny_access_when_user_id_is_null() {
      // Arrange - userId null means isOwner must return false regardless of entity
      UUID actualOwnerId = UUID.randomUUID();
      UserEntity owner = UserEntity.builder().id(actualOwnerId).username("owner").build();
      ProjectEntity entity =
          ProjectEntity.builder()
              .id(UUID.randomUUID())
              .adminCode(adminCode)
              .slug(slug)
              .name("Project")
              .createdBy(owner)
              .build();
      given(projectRepository.findBySlug(slug)).willReturn(Optional.of(entity));

      // Act & Assert
      assertThatThrownBy(() -> projectService.validateAdminAccess(slug, "wrong-code", null))
          .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void should_deny_access_when_project_has_no_owner() {
      // Arrange - createdBy null means isOwner must return false
      UUID userId = UUID.randomUUID();
      ProjectEntity entity =
          ProjectEntity.builder()
              .id(UUID.randomUUID())
              .adminCode(adminCode)
              .slug(slug)
              .name("Anonymous Project")
              .build(); // no createdBy
      given(projectRepository.findBySlug(slug)).willReturn(Optional.of(entity));

      // Act & Assert
      assertThatThrownBy(() -> projectService.validateAdminAccess(slug, "wrong-code", userId))
          .isInstanceOf(UnauthorizedException.class);
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

    @Test
    void should_pass_when_site_admin_without_admin_code() {
      // Arrange
      setSiteAdmin();
      ProjectEntity entity =
          ProjectEntity.builder()
              .id(projectId)
              .adminCode(adminCode)
              .slug("test")
              .name("Test")
              .build();
      given(projectRepository.findById(projectId)).willReturn(Optional.of(entity));

      // Act & Assert
      assertThatCode(() -> projectService.validateAdminCodeForProject(projectId, "wrong-code"))
          .doesNotThrowAnyException();
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
