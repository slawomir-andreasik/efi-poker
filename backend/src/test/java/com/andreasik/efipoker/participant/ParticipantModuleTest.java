package com.andreasik.efipoker.participant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.andreasik.efipoker.project.Project;
import com.andreasik.efipoker.project.ProjectService;
import com.andreasik.efipoker.shared.exception.ResourceNotFoundException;
import com.andreasik.efipoker.shared.test.BaseModuleTest;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;

@DisplayName("ParticipantModuleTest")
@ApplicationModuleTest(mode = BootstrapMode.ALL_DEPENDENCIES)
class ParticipantModuleTest extends BaseModuleTest {

  @Autowired private ParticipantService participantService;
  @Autowired private ProjectService projectService;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private EntityManager entityManager;

  @Test
  void should_join_project_and_list_participants() {
    // Arrange
    Project project = projectService.createProject("Team Poker");

    // Act
    Participant alice = participantService.joinProject(project.id(), "Alice", null, null);
    Participant bob = participantService.joinProject(project.id(), "Bob", null, null);

    // Assert
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
    // Arrange
    Project project = projectService.createProject("Duplicate Test");
    Participant first = participantService.joinProject(project.id(), "Alice", null, null);

    // Act
    Participant second = participantService.joinProject(project.id(), "Alice", null, null);

    // Assert
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
      // Arrange
      Project project = projectService.createProject("Room Scope");
      UUID roomId = insertRoom(project.id(), "room-1");

      // Act
      Participant alice = participantService.joinProject(project.id(), "Alice", null, roomId);

      // Assert
      assertThat(alice.invitedRoomIds()).containsExactly(roomId);
    }

    @Test
    void should_accumulate_room_access() {
      // Arrange
      Project project = projectService.createProject("Accumulate");
      UUID room1 = insertRoom(project.id(), "room-a");
      UUID room2 = insertRoom(project.id(), "room-b");

      // Act
      participantService.joinProject(project.id(), "Alice", null, room1);
      Participant alice = participantService.joinProject(project.id(), "Alice", null, room2);

      // Assert
      assertThat(alice.invitedRoomIds()).containsExactlyInAnyOrder(room1, room2);
    }

    @Test
    void should_clear_room_access_on_project_wide_join() {
      // Arrange
      Project project = projectService.createProject("Clear Access");
      UUID roomId = insertRoom(project.id(), "room-c");
      participantService.joinProject(project.id(), "Alice", null, roomId);

      // Act
      Participant alice = participantService.joinProject(project.id(), "Alice", null, null);

      // Assert
      assertThat(alice.invitedRoomIds()).isEmpty();
    }

    @Test
    void should_reject_invalid_room_id() {
      // Arrange
      Project project = projectService.createProject("Invalid Room");
      UUID fakeRoomId = UUID.randomUUID();

      // Act & Assert
      assertThatThrownBy(
              () -> participantService.joinProject(project.id(), "Alice", null, fakeRoomId))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_enrich_list_with_room_access() {
      // Arrange
      Project project = projectService.createProject("List Enrich");
      UUID roomId = insertRoom(project.id(), "room-d");
      participantService.joinProject(project.id(), "Alice", null, roomId);
      participantService.joinProject(project.id(), "Bob", null, null);

      // Act
      var participants = participantService.listParticipants(project.id());

      // Assert
      Participant alice =
          participants.stream().filter(p -> "Alice".equals(p.nickname())).findFirst().orElseThrow();
      Participant bob =
          participants.stream().filter(p -> "Bob".equals(p.nickname())).findFirst().orElseThrow();
      assertThat(alice.invitedRoomIds()).containsExactly(roomId);
      assertThat(bob.invitedRoomIds()).isEmpty();
    }
  }
}
