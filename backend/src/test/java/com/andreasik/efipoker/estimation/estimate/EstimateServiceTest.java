package com.andreasik.efipoker.estimation.estimate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.andreasik.efipoker.estimation.room.RoomEntity;
import com.andreasik.efipoker.estimation.task.Task;
import com.andreasik.efipoker.estimation.task.TaskEntity;
import com.andreasik.efipoker.estimation.task.TaskRepository;
import com.andreasik.efipoker.participant.Participant;
import com.andreasik.efipoker.participant.ParticipantApi;
import com.andreasik.efipoker.participant.ParticipantEntity;
import com.andreasik.efipoker.project.Project;
import com.andreasik.efipoker.project.ProjectEntity;
import com.andreasik.efipoker.shared.event.EstimateSubmittedEvent;
import com.andreasik.efipoker.shared.exception.UnauthorizedException;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

@DisplayName("EstimateService")
class EstimateServiceTest extends BaseUnitTest {

  @Mock private EstimateRepository estimateRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private ParticipantApi participantApi;
  @Mock private EntityManager entityManager;
  @Mock private EstimateEntityMapper estimateEntityMapper;
  @Mock private ApplicationEventPublisher eventPublisher;
  @InjectMocks private EstimateService estimateService;

  @Nested
  @DisplayName("getEstimatesByTaskIds")
  class GetEstimatesByTaskIds {

    @Test
    void should_return_empty_map_for_empty_input() {
      // Act
      Map<UUID, List<Estimate>> result = estimateService.getEstimatesByTaskIds(List.of());

      // Assert
      assertThat(result).isEmpty();
      then(estimateRepository).should(never()).findByTaskIds(ArgumentMatchers.anyCollection());
    }

    @Test
    void should_group_estimates_by_task_id() {
      // Arrange
      UUID taskId1 = UUID.randomUUID();
      UUID taskId2 = UUID.randomUUID();
      List<UUID> taskIds = List.of(taskId1, taskId2);

      TaskEntity taskEntity1 = TaskEntity.builder().id(taskId1).title("Login page").build();
      TaskEntity taskEntity2 = TaskEntity.builder().id(taskId2).title("Dashboard").build();
      ParticipantEntity participantEntity =
          ParticipantEntity.builder().id(UUID.randomUUID()).nickname("Alice").build();

      EstimateEntity entity1 =
          EstimateEntity.builder()
              .id(UUID.randomUUID())
              .task(taskEntity1)
              .participant(participantEntity)
              .storyPoints("5")
              .build();
      EstimateEntity entity2 =
          EstimateEntity.builder()
              .id(UUID.randomUUID())
              .task(taskEntity2)
              .participant(participantEntity)
              .storyPoints("8")
              .build();

      Project project = Project.builder().id(UUID.randomUUID()).name("Test").slug("test").build();
      Participant participant =
          Participant.builder()
              .id(participantEntity.getId())
              .nickname("Alice")
              .project(project)
              .build();
      Task task1 = Task.builder().id(taskId1).title("Login page").build();
      Task task2 = Task.builder().id(taskId2).title("Dashboard").build();

      Estimate estimate1 =
          Estimate.builder()
              .id(entity1.getId())
              .task(task1)
              .participant(participant)
              .storyPoints("5")
              .build();
      Estimate estimate2 =
          Estimate.builder()
              .id(entity2.getId())
              .task(task2)
              .participant(participant)
              .storyPoints("8")
              .build();

      given(estimateRepository.findByTaskIds(taskIds)).willReturn(List.of(entity1, entity2));
      given(estimateEntityMapper.toDomainList(List.of(entity1, entity2)))
          .willReturn(List.of(estimate1, estimate2));

      // Act
      Map<UUID, List<Estimate>> result = estimateService.getEstimatesByTaskIds(taskIds);

      // Assert
      assertThat(result).hasSize(2);
      assertThat(result.get(taskId1)).containsExactly(estimate1);
      assertThat(result.get(taskId2)).containsExactly(estimate2);
    }

    @Test
    void should_return_empty_map_when_no_estimates_found() {
      // Arrange
      UUID taskId = UUID.randomUUID();
      List<UUID> taskIds = List.of(taskId);

      given(estimateRepository.findByTaskIds(taskIds)).willReturn(List.of());
      given(estimateEntityMapper.toDomainList(List.of())).willReturn(List.of());

      // Act
      Map<UUID, List<Estimate>> result = estimateService.getEstimatesByTaskIds(taskIds);

      // Assert
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("submitEstimate - cross-project validation")
  class SubmitEstimateCrossProject {

    @Test
    void should_reject_estimate_from_participant_in_different_project() {
      // Arrange
      UUID taskId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();
      UUID projectId = UUID.randomUUID();

      ProjectEntity project = ProjectEntity.builder().id(projectId).build();
      RoomEntity room = RoomEntity.builder().id(UUID.randomUUID()).project(project).build();
      TaskEntity task = TaskEntity.builder().id(taskId).room(room).build();

      given(estimateRepository.findByTaskAndParticipant(taskId, participantId))
          .willReturn(Optional.empty());
      given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
      willThrow(new UnauthorizedException("Participant does not belong to this project"))
          .given(participantApi)
          .validateParticipantBelongsToProject(participantId, projectId);

      // Act & Assert
      assertThatThrownBy(() -> estimateService.submitEstimate(taskId, participantId, "5", null))
          .isInstanceOf(UnauthorizedException.class)
          .hasMessageContaining("does not belong to this project");
    }
  }

  @Nested
  @DisplayName("submitEstimate - comment required")
  class SubmitEstimateCommentRequired {

    @Test
    void should_reject_new_estimate_without_comment_when_required() {
      // Arrange
      UUID taskId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();
      UUID projectId = UUID.randomUUID();

      ProjectEntity project = ProjectEntity.builder().id(projectId).build();
      RoomEntity room =
          RoomEntity.builder().id(UUID.randomUUID()).project(project).commentRequired(true).build();
      TaskEntity task = TaskEntity.builder().id(taskId).room(room).build();

      given(estimateRepository.findByTaskAndParticipant(taskId, participantId))
          .willReturn(Optional.empty());
      given(taskRepository.findById(taskId)).willReturn(Optional.of(task));

      // Act & Assert
      assertThatThrownBy(() -> estimateService.submitEstimate(taskId, participantId, "5", null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Comment is required");
    }

    @Test
    void should_reject_new_estimate_with_blank_comment_when_required() {
      // Arrange
      UUID taskId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();
      UUID projectId = UUID.randomUUID();

      ProjectEntity project = ProjectEntity.builder().id(projectId).build();
      RoomEntity room =
          RoomEntity.builder().id(UUID.randomUUID()).project(project).commentRequired(true).build();
      TaskEntity task = TaskEntity.builder().id(taskId).room(room).build();

      given(estimateRepository.findByTaskAndParticipant(taskId, participantId))
          .willReturn(Optional.empty());
      given(taskRepository.findById(taskId)).willReturn(Optional.of(task));

      // Act & Assert
      assertThatThrownBy(() -> estimateService.submitEstimate(taskId, participantId, "5", "  "))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Comment is required");
    }

    @Test
    void should_reject_existing_estimate_update_without_comment_when_required() {
      // Arrange
      UUID taskId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();

      RoomEntity room = RoomEntity.builder().id(UUID.randomUUID()).commentRequired(true).build();
      TaskEntity task = TaskEntity.builder().id(taskId).room(room).build();
      EstimateEntity existing =
          EstimateEntity.builder()
              .id(UUID.randomUUID())
              .task(task)
              .storyPoints("3")
              .comment("old comment")
              .build();

      given(estimateRepository.findByTaskAndParticipant(taskId, participantId))
          .willReturn(Optional.of(existing));

      // Act & Assert
      assertThatThrownBy(() -> estimateService.submitEstimate(taskId, participantId, "5", null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Comment is required");

      then(estimateRepository).should(never()).save(ArgumentMatchers.any());
    }

    @Test
    void should_allow_estimate_without_comment_when_not_required() {
      // Arrange
      UUID taskId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();
      UUID projectId = UUID.randomUUID();

      ProjectEntity project = ProjectEntity.builder().id(projectId).build();
      RoomEntity room =
          RoomEntity.builder()
              .id(UUID.randomUUID())
              .project(project)
              .commentRequired(false)
              .build();
      TaskEntity task = TaskEntity.builder().id(taskId).room(room).build();
      ParticipantEntity participant =
          ParticipantEntity.builder().id(participantId).nickname("Bob").build();

      EstimateEntity saved =
          EstimateEntity.builder()
              .id(UUID.randomUUID())
              .task(task)
              .participant(participant)
              .storyPoints("5")
              .build();
      Estimate domain = Estimate.builder().id(saved.getId()).storyPoints("5").build();

      given(estimateRepository.findByTaskAndParticipant(taskId, participantId))
          .willReturn(Optional.empty());
      given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
      given(entityManager.getReference(ParticipantEntity.class, participantId))
          .willReturn(participant);
      given(estimateRepository.save(ArgumentMatchers.any())).willReturn(saved);
      given(estimateEntityMapper.toDomain(saved)).willReturn(domain);

      // Act
      Estimate result = estimateService.submitEstimate(taskId, participantId, "5", null);

      // Assert
      assertThat(result.storyPoints()).isEqualTo("5");
      then(participantApi).should().validateParticipantExists(participantId);
      then(eventPublisher).should().publishEvent(new EstimateSubmittedEvent(taskId, participantId));
    }
  }

  @Nested
  @DisplayName("submitEstimate - event publishing")
  class SubmitEstimateEventPublishing {

    @Test
    void should_publish_event_when_creating_new_estimate() {
      // Arrange
      UUID taskId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();

      ProjectEntity project = ProjectEntity.builder().id(UUID.randomUUID()).build();
      RoomEntity room =
          RoomEntity.builder()
              .id(UUID.randomUUID())
              .project(project)
              .commentRequired(false)
              .build();
      TaskEntity task = TaskEntity.builder().id(taskId).room(room).build();
      ParticipantEntity participant =
          ParticipantEntity.builder().id(participantId).nickname("Alice").build();
      EstimateEntity saved =
          EstimateEntity.builder()
              .id(UUID.randomUUID())
              .task(task)
              .participant(participant)
              .storyPoints("8")
              .build();
      Estimate domain = Estimate.builder().id(saved.getId()).storyPoints("8").build();

      given(estimateRepository.findByTaskAndParticipant(taskId, participantId))
          .willReturn(Optional.empty());
      given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
      given(entityManager.getReference(ParticipantEntity.class, participantId))
          .willReturn(participant);
      given(estimateRepository.save(ArgumentMatchers.any())).willReturn(saved);
      given(estimateEntityMapper.toDomain(saved)).willReturn(domain);

      // Act
      estimateService.submitEstimate(taskId, participantId, "8", null);

      // Assert
      then(eventPublisher).should().publishEvent(new EstimateSubmittedEvent(taskId, participantId));
    }

    @Test
    void should_publish_event_when_updating_existing_estimate() {
      // Arrange
      UUID taskId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();

      RoomEntity room = RoomEntity.builder().id(UUID.randomUUID()).commentRequired(false).build();
      TaskEntity task = TaskEntity.builder().id(taskId).room(room).build();
      EstimateEntity existing =
          EstimateEntity.builder().id(UUID.randomUUID()).task(task).storyPoints("3").build();
      Estimate domain = Estimate.builder().id(existing.getId()).storyPoints("5").build();

      given(estimateRepository.findByTaskAndParticipant(taskId, participantId))
          .willReturn(Optional.of(existing));
      given(estimateRepository.save(existing)).willReturn(existing);
      given(estimateEntityMapper.toDomain(existing)).willReturn(domain);

      // Act
      estimateService.submitEstimate(taskId, participantId, "5", null);

      // Assert
      then(eventPublisher).should().publishEvent(new EstimateSubmittedEvent(taskId, participantId));
    }
  }

  @Nested
  @DisplayName("submitEstimate - validateParticipantExists")
  class SubmitEstimateParticipantValidation {

    @Test
    void should_not_validate_participant_exists_when_updating_existing_estimate() {
      // Arrange
      UUID taskId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();

      RoomEntity room = RoomEntity.builder().id(UUID.randomUUID()).commentRequired(false).build();
      TaskEntity task = TaskEntity.builder().id(taskId).room(room).build();
      EstimateEntity existing =
          EstimateEntity.builder().id(UUID.randomUUID()).task(task).storyPoints("3").build();
      Estimate domain = Estimate.builder().id(existing.getId()).storyPoints("5").build();

      given(estimateRepository.findByTaskAndParticipant(taskId, participantId))
          .willReturn(Optional.of(existing));
      given(estimateRepository.save(existing)).willReturn(existing);
      given(estimateEntityMapper.toDomain(existing)).willReturn(domain);

      // Act
      estimateService.submitEstimate(taskId, participantId, "5", null);

      // Assert - update path skips participant existence check
      then(participantApi).should(never()).validateParticipantExists(participantId);
    }
  }

  @Nested
  @DisplayName("validateCommentIfRequired")
  class ValidateCommentIfRequired {

    @Test
    void should_not_throw_when_comment_not_required_and_comment_absent() {
      // Arrange
      UUID taskId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();

      ProjectEntity project = ProjectEntity.builder().id(UUID.randomUUID()).build();
      RoomEntity room =
          RoomEntity.builder()
              .id(UUID.randomUUID())
              .project(project)
              .commentRequired(false)
              .build();
      TaskEntity task = TaskEntity.builder().id(taskId).room(room).build();
      ParticipantEntity participant =
          ParticipantEntity.builder().id(participantId).nickname("Dave").build();
      EstimateEntity saved =
          EstimateEntity.builder()
              .id(UUID.randomUUID())
              .task(task)
              .participant(participant)
              .storyPoints("5")
              .build();
      Estimate domain = Estimate.builder().id(saved.getId()).storyPoints("5").build();

      given(estimateRepository.findByTaskAndParticipant(taskId, participantId))
          .willReturn(Optional.empty());
      given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
      given(entityManager.getReference(ParticipantEntity.class, participantId))
          .willReturn(participant);
      given(estimateRepository.save(ArgumentMatchers.any())).willReturn(saved);
      given(estimateEntityMapper.toDomain(saved)).willReturn(domain);

      // Act & Assert - must not throw
      assertThat(estimateService.submitEstimate(taskId, participantId, "5", null)).isNotNull();
    }

    @Test
    void should_not_throw_when_comment_required_and_comment_provided() {
      // Arrange
      UUID taskId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();

      ProjectEntity project = ProjectEntity.builder().id(UUID.randomUUID()).build();
      RoomEntity room =
          RoomEntity.builder().id(UUID.randomUUID()).project(project).commentRequired(true).build();
      TaskEntity task = TaskEntity.builder().id(taskId).room(room).build();
      ParticipantEntity participant =
          ParticipantEntity.builder().id(participantId).nickname("Eve").build();
      EstimateEntity saved =
          EstimateEntity.builder()
              .id(UUID.randomUUID())
              .task(task)
              .participant(participant)
              .storyPoints("3")
              .build();
      Estimate domain = Estimate.builder().id(saved.getId()).storyPoints("3").build();

      given(estimateRepository.findByTaskAndParticipant(taskId, participantId))
          .willReturn(Optional.empty());
      given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
      given(entityManager.getReference(ParticipantEntity.class, participantId))
          .willReturn(participant);
      given(estimateRepository.save(ArgumentMatchers.any())).willReturn(saved);
      given(estimateEntityMapper.toDomain(saved)).willReturn(domain);

      // Act & Assert - must not throw when comment is provided
      assertThat(estimateService.submitEstimate(taskId, participantId, "3", "looks like 3 points"))
          .isNotNull();
    }
  }

  @Nested
  @DisplayName("deleteEstimate - cross-project validation")
  class DeleteEstimateCrossProject {

    @Test
    void should_reject_delete_from_participant_in_different_project() {
      // Arrange
      UUID taskId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();
      UUID projectId = UUID.randomUUID();

      ProjectEntity project = ProjectEntity.builder().id(projectId).build();
      RoomEntity room = RoomEntity.builder().id(UUID.randomUUID()).project(project).build();
      TaskEntity task = TaskEntity.builder().id(taskId).room(room).build();

      given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
      willThrow(new UnauthorizedException("Participant does not belong to this project"))
          .given(participantApi)
          .validateParticipantBelongsToProject(participantId, projectId);

      // Act & Assert
      assertThatThrownBy(() -> estimateService.deleteEstimate(taskId, participantId))
          .isInstanceOf(UnauthorizedException.class)
          .hasMessageContaining("does not belong to this project");

      then(estimateRepository)
          .should(never())
          .deleteByTaskIdAndParticipantId(taskId, participantId);
    }
  }

  @Nested
  @DisplayName("submitEstimate - storyPoints validation")
  class SubmitEstimateStoryPointsValidation {

    @Test
    void should_throw_for_invalid_story_points_value() {
      // Arrange
      UUID taskId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();

      // No repository stub needed - validation fires before any repo call

      // Act & Assert
      assertThatThrownBy(() -> estimateService.submitEstimate(taskId, participantId, "999", null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid story points value");

      then(estimateRepository).should(never()).save(ArgumentMatchers.any());
    }

    @Test
    void should_not_throw_for_null_story_points() {
      // Arrange - null storyPoints means draft vote; validation must be skipped
      UUID taskId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();

      ProjectEntity project = ProjectEntity.builder().id(UUID.randomUUID()).build();
      RoomEntity room =
          RoomEntity.builder()
              .id(UUID.randomUUID())
              .project(project)
              .commentRequired(false)
              .build();
      TaskEntity task = TaskEntity.builder().id(taskId).room(room).build();
      ParticipantEntity participant =
          ParticipantEntity.builder().id(participantId).nickname("Carol").build();
      EstimateEntity saved =
          EstimateEntity.builder()
              .id(UUID.randomUUID())
              .task(task)
              .participant(participant)
              .storyPoints(null)
              .build();
      Estimate domain = Estimate.builder().id(saved.getId()).storyPoints(null).build();

      given(estimateRepository.findByTaskAndParticipant(taskId, participantId))
          .willReturn(Optional.empty());
      given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
      given(entityManager.getReference(ParticipantEntity.class, participantId))
          .willReturn(participant);
      given(estimateRepository.save(ArgumentMatchers.any())).willReturn(saved);
      given(estimateEntityMapper.toDomain(saved)).willReturn(domain);

      // Act
      Estimate result = estimateService.submitEstimate(taskId, participantId, null, null);

      // Assert - null storyPoints is a draft vote, must succeed and return the saved estimate
      assertThat(result).isNotNull();
      assertThat(result.storyPoints()).isNull();
    }
  }

  @Nested
  @DisplayName("submitEstimate - update existing estimate")
  class SubmitEstimateUpdateExisting {

    @Test
    void should_set_new_story_points_and_comment_on_update() {
      // Arrange
      UUID taskId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();

      RoomEntity room = RoomEntity.builder().id(UUID.randomUUID()).commentRequired(false).build();
      TaskEntity task = TaskEntity.builder().id(taskId).room(room).build();
      EstimateEntity existing =
          EstimateEntity.builder()
              .id(UUID.randomUUID())
              .task(task)
              .storyPoints("3")
              .comment("old comment")
              .build();
      Estimate domain =
          Estimate.builder().id(existing.getId()).storyPoints("8").comment("new comment").build();

      given(estimateRepository.findByTaskAndParticipant(taskId, participantId))
          .willReturn(Optional.of(existing));
      given(estimateRepository.save(existing)).willReturn(existing);
      given(estimateEntityMapper.toDomain(existing)).willReturn(domain);

      // Act
      Estimate result = estimateService.submitEstimate(taskId, participantId, "8", "new comment");

      // Assert - entity must have the new values applied before save
      assertThat(existing.getStoryPoints()).isEqualTo("8");
      assertThat(existing.getComment()).isEqualTo("new comment");
      assertThat(result).isNotNull();
      assertThat(result.storyPoints()).isEqualTo("8");
    }

    @Test
    void should_clear_story_points_and_comment_when_updating_to_null() {
      // Arrange
      UUID taskId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();

      RoomEntity room = RoomEntity.builder().id(UUID.randomUUID()).commentRequired(false).build();
      TaskEntity task = TaskEntity.builder().id(taskId).room(room).build();
      EstimateEntity existing =
          EstimateEntity.builder()
              .id(UUID.randomUUID())
              .task(task)
              .storyPoints("5")
              .comment("some comment")
              .build();
      Estimate domain =
          Estimate.builder().id(existing.getId()).storyPoints(null).comment(null).build();

      given(estimateRepository.findByTaskAndParticipant(taskId, participantId))
          .willReturn(Optional.of(existing));
      given(estimateRepository.save(existing)).willReturn(existing);
      given(estimateEntityMapper.toDomain(existing)).willReturn(domain);

      // Act
      Estimate result = estimateService.submitEstimate(taskId, participantId, null, null);

      // Assert - null storyPoints (draft) must clear both fields
      assertThat(existing.getStoryPoints()).isNull();
      assertThat(existing.getComment()).isNull();
      assertThat(result).isNotNull();
    }

    @Test
    void should_return_non_null_estimate_after_update() {
      // Mutant #3: replaced return value with null
      // Arrange
      UUID taskId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();

      RoomEntity room = RoomEntity.builder().id(UUID.randomUUID()).commentRequired(false).build();
      TaskEntity task = TaskEntity.builder().id(taskId).room(room).build();
      EstimateEntity existing =
          EstimateEntity.builder().id(UUID.randomUUID()).task(task).storyPoints("3").build();
      Estimate domain = Estimate.builder().id(existing.getId()).storyPoints("5").build();

      given(estimateRepository.findByTaskAndParticipant(taskId, participantId))
          .willReturn(Optional.of(existing));
      given(estimateRepository.save(existing)).willReturn(existing);
      given(estimateEntityMapper.toDomain(existing)).willReturn(domain);

      // Act
      Estimate result = estimateService.submitEstimate(taskId, participantId, "5", null);

      // Assert - return value must not be null
      assertThat(result).isNotNull();
      assertThat(result.storyPoints()).isEqualTo("5");
    }
  }

  @Nested
  @DisplayName("Estimate.hasVoted")
  class EstimateHasVoted {

    @Test
    void should_return_false_when_story_points_is_null() {
      // Mutant: replaced boolean return with true
      Estimate estimate = Estimate.builder().id(UUID.randomUUID()).storyPoints(null).build();
      assertThat(estimate.hasVoted()).isFalse();
    }

    @Test
    void should_return_true_when_story_points_is_set() {
      Estimate estimate = Estimate.builder().id(UUID.randomUUID()).storyPoints("5").build();
      assertThat(estimate.hasVoted()).isTrue();
    }
  }
}
