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
}
