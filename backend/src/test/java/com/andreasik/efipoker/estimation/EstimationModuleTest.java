package com.andreasik.efipoker.estimation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.andreasik.efipoker.estimation.estimate.Estimate;
import com.andreasik.efipoker.estimation.estimate.EstimateService;
import com.andreasik.efipoker.estimation.room.Room;
import com.andreasik.efipoker.estimation.room.RoomService;
import com.andreasik.efipoker.estimation.task.Task;
import com.andreasik.efipoker.estimation.task.TaskService;
import com.andreasik.efipoker.participant.Participant;
import com.andreasik.efipoker.participant.ParticipantService;
import com.andreasik.efipoker.project.Project;
import com.andreasik.efipoker.project.ProjectService;
import com.andreasik.efipoker.shared.event.EstimateSubmittedEvent;
import com.andreasik.efipoker.shared.event.ProjectCreatedEvent;
import com.andreasik.efipoker.shared.event.RoomCreatedEvent;
import com.andreasik.efipoker.shared.test.BaseModuleTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.modulith.test.AssertablePublishedEvents;

@DisplayName("EstimationModuleTest")
@ApplicationModuleTest(mode = BootstrapMode.ALL_DEPENDENCIES)
class EstimationModuleTest extends BaseModuleTest {

  @Autowired private ProjectService projectService;
  @Autowired private ParticipantService participantService;
  @Autowired private RoomService roomService;
  @Autowired private TaskService taskService;
  @Autowired private EstimateService estimateService;
  @Autowired private AssertablePublishedEvents events;

  @Test
  void should_complete_async_estimation_flow() {
    // Arrange
    Project project = projectService.createProject("Estimation Test");
    Participant alice = participantService.joinProject(project.id(), "Alice", null, null);
    Instant deadline = Instant.now().plus(7, ChronoUnit.DAYS);
    Room room =
        roomService.createRoom(
            project.id(), "Sprint 1", "Sprint planning", "ASYNC", deadline, true, null, false);
    Task task = taskService.createTask(room.id(), "Login Page", "Implement login", 0);

    // Act
    Estimate estimate = estimateService.submitEstimate(task.id(), alice.id(), "5", "Seems medium");

    // Assert
    List<Estimate> estimates = estimateService.getEstimatesForTask(task.id());
    assertAll(
        () -> assertThat(estimate.storyPoints()).isEqualTo("5"),
        () -> assertThat(estimate.comment()).isEqualTo("Seems medium"),
        () -> assertThat(estimates).hasSize(1));

    assertThat(events)
        .contains(ProjectCreatedEvent.class)
        .matching(e -> e.projectId().equals(project.id()));
    assertThat(events).contains(RoomCreatedEvent.class).matching(e -> e.roomId().equals(room.id()));
    assertThat(events)
        .contains(EstimateSubmittedEvent.class)
        .matching(e -> e.taskId().equals(task.id()) && e.participantId().equals(alice.id()));
  }

  @Test
  void should_update_existing_estimate() {
    // Arrange
    Project project = projectService.createProject("Update Test");
    Participant alice = participantService.joinProject(project.id(), "Alice", null, null);
    Instant deadline = Instant.now().plus(7, ChronoUnit.DAYS);
    Room room =
        roomService.createRoom(project.id(), "Room", null, "ASYNC", deadline, true, null, false);
    Task task = taskService.createTask(room.id(), "Task 1", null, 0);
    estimateService.submitEstimate(task.id(), alice.id(), "3", null);

    // Act
    Estimate updated =
        estimateService.submitEstimate(task.id(), alice.id(), "8", "Changed my mind");

    // Assert
    List<Estimate> estimates = estimateService.getEstimatesForTask(task.id());
    assertAll(
        () -> assertThat(updated.storyPoints()).isEqualTo("8"),
        () -> assertThat(estimates).hasSize(1));
  }
}
