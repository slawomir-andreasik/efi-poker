package com.andreasik.efipoker.estimation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.andreasik.efipoker.estimation.estimate.Estimate;
import com.andreasik.efipoker.estimation.estimate.EstimateService;
import com.andreasik.efipoker.estimation.room.CreateRoomCommand;
import com.andreasik.efipoker.estimation.room.Room;
import com.andreasik.efipoker.estimation.room.RoomService;
import com.andreasik.efipoker.estimation.room.RoomType;
import com.andreasik.efipoker.estimation.task.Task;
import com.andreasik.efipoker.estimation.task.TaskService;
import com.andreasik.efipoker.participant.Participant;
import com.andreasik.efipoker.participant.ParticipantService;
import com.andreasik.efipoker.project.Project;
import com.andreasik.efipoker.project.ProjectService;
import com.andreasik.efipoker.shared.event.EstimateSubmittedEvent;
import com.andreasik.efipoker.shared.event.ProjectCreatedEvent;
import com.andreasik.efipoker.shared.event.RoomCreatedEvent;
import com.andreasik.efipoker.shared.exception.ResourceNotFoundException;
import com.andreasik.efipoker.shared.test.BaseModuleTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.modulith.test.AssertablePublishedEvents;

/**
 * Merged module integration test. All 3 module tests (project, participant, estimation) share one
 * Spring context instead of 3 separate ones. Placed in estimation package because it transitively
 * depends on all other modules (estimation -> project -> auth, estimation -> participant).
 */
@DisplayName("AllModulesIntegrationTest")
@ApplicationModuleTest(mode = BootstrapMode.ALL_DEPENDENCIES)
class AllModulesIntegrationTest extends BaseModuleTest {

  @Autowired private ProjectService projectService;
  @Autowired private ParticipantService participantService;
  @Autowired private RoomService roomService;
  @Autowired private TaskService taskService;
  @Autowired private EstimateService estimateService;
  @Autowired private AssertablePublishedEvents events;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private EntityManager entityManager;

  @Nested
  @DisplayName("Project module")
  class ProjectModule {

    @Test
    void should_create_and_retrieve_project() {
      Project created = projectService.createProject("Sprint Planning");

      Project retrieved = projectService.getProjectBySlug(created.slug());
      assertAll(
          () -> assertThat(retrieved.id()).isEqualTo(created.id()),
          () -> assertThat(retrieved.name()).isEqualTo("Sprint Planning"),
          () -> assertThat(retrieved.slug()).hasSize(8));

      assertThat(events)
          .contains(ProjectCreatedEvent.class)
          .matching(e -> e.projectId().equals(created.id()));
    }
  }

  @Nested
  @DisplayName("Participant module")
  class ParticipantModule {

    @Test
    void should_join_project_and_list_participants() {
      Project project = projectService.createProject("Team Poker");

      Participant alice = participantService.joinProject(project.id(), "Alice", null, null);
      Participant bob = participantService.joinProject(project.id(), "Bob", null, null);

      var participants = participantService.listParticipants(project.id());
      assertThat(participants).hasSize(2);
      assertThat(participants)
          .extracting(Participant::nickname)
          .containsExactlyInAnyOrder("Alice", "Bob");
      assertThat(alice.id()).isNotNull();
      assertThat(bob.id()).isNotNull();
    }

    @Test
    void should_return_existing_participant_on_duplicate_join() {
      Project project = projectService.createProject("Duplicate Test");
      Participant first = participantService.joinProject(project.id(), "Alice", null, null);

      Participant second = participantService.joinProject(project.id(), "Alice", null, null);

      assertThat(second.id()).isEqualTo(first.id());
    }

    @Nested
    @DisplayName("Room-scoped access")
    class RoomScopedAccess {

      private UUID insertRoom(UUID projectId, String slug) {
        entityManager.flush();
        UUID roomId = UUID.randomUUID();
        jdbcTemplate.update(
            """
            INSERT INTO rooms (id, project_id, slug, title, room_type, status, round_number, created_at)
            VALUES (?, ?, ?, ?, 'ASYNC', 'OPEN', 1, NOW())
            """,
            roomId,
            projectId,
            slug,
            "Room " + slug);
        return roomId;
      }

      @Test
      void should_scope_participant_to_room() {
        Project project = projectService.createProject("Room Scope");
        UUID roomId = insertRoom(project.id(), "room-1");

        Participant alice = participantService.joinProject(project.id(), "Alice", null, roomId);

        assertThat(alice.invitedRoomIds()).containsExactly(roomId);
      }

      @Test
      void should_accumulate_room_access() {
        Project project = projectService.createProject("Accumulate");
        UUID room1 = insertRoom(project.id(), "room-a");
        UUID room2 = insertRoom(project.id(), "room-b");

        participantService.joinProject(project.id(), "Alice", null, room1);
        Participant alice = participantService.joinProject(project.id(), "Alice", null, room2);

        assertThat(alice.invitedRoomIds()).containsExactlyInAnyOrder(room1, room2);
      }

      @Test
      void should_clear_room_access_on_project_wide_join() {
        Project project = projectService.createProject("Clear Access");
        UUID roomId = insertRoom(project.id(), "room-c");
        participantService.joinProject(project.id(), "Alice", null, roomId);

        Participant alice = participantService.joinProject(project.id(), "Alice", null, null);

        assertThat(alice.invitedRoomIds()).isEmpty();
      }

      @Test
      void should_reject_invalid_room_id() {
        Project project = projectService.createProject("Invalid Room");
        UUID fakeRoomId = UUID.randomUUID();

        assertThatThrownBy(
                () -> participantService.joinProject(project.id(), "Alice", null, fakeRoomId))
            .isInstanceOf(ResourceNotFoundException.class);
      }

      @Test
      void should_enrich_list_with_room_access() {
        Project project = projectService.createProject("List Enrich");
        UUID roomId = insertRoom(project.id(), "room-d");
        participantService.joinProject(project.id(), "Alice", null, roomId);
        participantService.joinProject(project.id(), "Bob", null, null);

        var participants = participantService.listParticipants(project.id());

        Participant alice =
            participants.stream()
                .filter(p -> "Alice".equals(p.nickname()))
                .findFirst()
                .orElseThrow();
        Participant bob =
            participants.stream().filter(p -> "Bob".equals(p.nickname())).findFirst().orElseThrow();
        assertThat(alice.invitedRoomIds()).containsExactly(roomId);
        assertThat(bob.invitedRoomIds()).isEmpty();
      }
    }
  }

  @Nested
  @DisplayName("Estimation module")
  class EstimationModuleTests {

    @Test
    void should_complete_async_estimation_flow() {
      Project project = projectService.createProject("Estimation Test");
      Participant alice = participantService.joinProject(project.id(), "Alice", null, null);
      Instant deadline = Instant.now().plus(7, ChronoUnit.DAYS);
      Room room =
          roomService.createRoom(
              new CreateRoomCommand(
                  project.id(),
                  "Sprint 1",
                  "Sprint planning",
                  RoomType.ASYNC,
                  deadline,
                  true,
                  null,
                  false));
      Task task = taskService.createTask(room.id(), "Login Page", "Implement login", 0);

      Estimate estimate =
          estimateService.submitEstimate(task.id(), alice.id(), "5", "Seems medium");

      List<Estimate> estimates = estimateService.getEstimatesForTask(task.id());
      assertAll(
          () -> assertThat(estimate.storyPoints()).isEqualTo("5"),
          () -> assertThat(estimate.comment()).isEqualTo("Seems medium"),
          () -> assertThat(estimates).hasSize(1));

      assertThat(events)
          .contains(ProjectCreatedEvent.class)
          .matching(e -> e.projectId().equals(project.id()));
      assertThat(events)
          .contains(RoomCreatedEvent.class)
          .matching(e -> e.roomId().equals(room.id()));
      assertThat(events)
          .contains(EstimateSubmittedEvent.class)
          .matching(e -> e.taskId().equals(task.id()) && e.participantId().equals(alice.id()));
    }

    @Test
    void should_update_existing_estimate() {
      Project project = projectService.createProject("Update Test");
      Participant alice = participantService.joinProject(project.id(), "Alice", null, null);
      Instant deadline = Instant.now().plus(7, ChronoUnit.DAYS);
      Room room =
          roomService.createRoom(
              new CreateRoomCommand(
                  project.id(), "Room", null, RoomType.ASYNC, deadline, true, null, false));
      Task task = taskService.createTask(room.id(), "Task 1", null, 0);
      estimateService.submitEstimate(task.id(), alice.id(), "3", null);

      Estimate updated =
          estimateService.submitEstimate(task.id(), alice.id(), "8", "Changed my mind");

      List<Estimate> estimates = estimateService.getEstimatesForTask(task.id());
      assertAll(
          () -> assertThat(updated.storyPoints()).isEqualTo("8"),
          () -> assertThat(estimates).hasSize(1));
    }
  }
}
