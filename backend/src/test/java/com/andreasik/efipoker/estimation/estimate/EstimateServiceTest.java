package com.andreasik.efipoker.estimation.estimate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.andreasik.efipoker.estimation.task.Task;
import com.andreasik.efipoker.estimation.task.TaskEntity;
import com.andreasik.efipoker.estimation.task.TaskRepository;
import com.andreasik.efipoker.participant.Participant;
import com.andreasik.efipoker.participant.ParticipantApi;
import com.andreasik.efipoker.participant.ParticipantEntity;
import com.andreasik.efipoker.project.Project;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
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
}
