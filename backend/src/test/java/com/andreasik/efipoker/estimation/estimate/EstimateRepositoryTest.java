package com.andreasik.efipoker.estimation.estimate;

import static org.assertj.core.api.Assertions.assertThat;

import com.andreasik.efipoker.estimation.room.RoomEntity;
import com.andreasik.efipoker.estimation.task.TaskEntity;
import com.andreasik.efipoker.participant.ParticipantEntity;
import com.andreasik.efipoker.project.ProjectEntity;
import com.andreasik.efipoker.shared.test.BaseComponentTest;
import com.andreasik.efipoker.shared.test.Fixtures;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EstimateRepository")
class EstimateRepositoryTest extends BaseComponentTest {

  @Nested
  @DisplayName("findByTaskIds")
  class FindByTaskIdIn {

    @Test
    void should_return_estimates_for_multiple_tasks() {
      // Arrange
      ProjectEntity project = projectRepository.save(Fixtures.projectEntity());
      RoomEntity room = roomRepository.save(Fixtures.roomEntity(project));
      TaskEntity task1 = taskRepository.save(Fixtures.taskEntity(room, "Login page", 0));
      TaskEntity task2 = taskRepository.save(Fixtures.taskEntity(room, "Dashboard", 1));
      ParticipantEntity participant =
          participantRepository.save(Fixtures.participantEntity(project, "Alice"));

      estimateRepository.save(Fixtures.estimateEntity(task1, participant, "5"));
      estimateRepository.save(Fixtures.estimateEntity(task2, participant, "8"));

      // Act
      List<EstimateEntity> results =
          estimateRepository.findByTaskIds(List.of(task1.getId(), task2.getId()));

      // Assert
      assertThat(results).hasSize(2);
      assertThat(results)
          .extracting(e -> e.getTask().getId())
          .containsExactlyInAnyOrder(task1.getId(), task2.getId());
    }

    @Test
    void should_return_empty_for_empty_task_ids() {
      // Act
      List<EstimateEntity> results = estimateRepository.findByTaskIds(List.of());

      // Assert
      assertThat(results).isEmpty();
    }

    @Test
    void should_return_empty_when_no_estimates_exist() {
      // Arrange
      UUID nonExistentTaskId = UUID.randomUUID();

      // Act
      List<EstimateEntity> results = estimateRepository.findByTaskIds(List.of(nonExistentTaskId));

      // Assert
      assertThat(results).isEmpty();
    }

    @Test
    void should_not_return_estimates_for_other_tasks() {
      // Arrange
      ProjectEntity project = projectRepository.save(Fixtures.projectEntity());
      RoomEntity room = roomRepository.save(Fixtures.roomEntity(project));
      TaskEntity task1 = taskRepository.save(Fixtures.taskEntity(room, "Login page", 0));
      TaskEntity task2 = taskRepository.save(Fixtures.taskEntity(room, "Dashboard", 1));
      TaskEntity otherTask = taskRepository.save(Fixtures.taskEntity(room, "Settings page", 2));
      ParticipantEntity participant =
          participantRepository.save(Fixtures.participantEntity(project, "Bob"));

      estimateRepository.save(Fixtures.estimateEntity(task1, participant, "3"));
      estimateRepository.save(Fixtures.estimateEntity(task2, participant, "5"));
      estimateRepository.save(Fixtures.estimateEntity(otherTask, participant, "13"));

      // Act - only query task1 and task2
      List<EstimateEntity> results =
          estimateRepository.findByTaskIds(List.of(task1.getId(), task2.getId()));

      // Assert - should NOT include otherTask's estimate
      assertThat(results).hasSize(2);
      assertThat(results)
          .extracting(e -> e.getTask().getId())
          .containsExactlyInAnyOrder(task1.getId(), task2.getId())
          .doesNotContain(otherTask.getId());
    }
  }
}
