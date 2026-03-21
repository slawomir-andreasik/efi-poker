package com.andreasik.efipoker.shared.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andreasik.efipoker.estimation.room.RoomEntity;
import com.andreasik.efipoker.estimation.task.TaskEntity;
import com.andreasik.efipoker.participant.ParticipantEntity;
import com.andreasik.efipoker.project.ProjectEntity;
import com.andreasik.efipoker.shared.test.BaseComponentTest;
import com.andreasik.efipoker.shared.test.Fixtures;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/// Security audit regression tests. Each @Nested class maps to one SEC-XXX audit task documenting
/// the secure behavior which the code does not yet implement. Enable after fixing the corresponding
/// SEC task.
@DisplayName("Security Audit Tests")
class SecurityAuditTest extends BaseComponentTest {

  private ProjectEntity project;
  private RoomEntity room;

  @BeforeEach
  void setUp() {
    project = projectRepository.save(Fixtures.projectEntity());
    room = roomRepository.save(Fixtures.roomEntity(project));
  }

  @Nested
  @DisplayName("Analytics endpoints lack access control")
  class AnalyticsAccessControl {

    private RoomEntity revealedRoom;

    @BeforeEach
    void setUpRevealedRoom() {
      revealedRoom = roomRepository.save(Fixtures.revealedRoomEntity(project));
      TaskEntity task = taskRepository.save(Fixtures.taskEntity(revealedRoom));
      ParticipantEntity participant =
          participantRepository.save(Fixtures.participantEntity(project, "Alice"));
      estimateRepository.save(Fixtures.estimateEntity(task, participant, "5"));
    }

    @Test
    void should_reject_room_analytics_without_jwt() throws Exception {
      mockMvc
          .perform(get("/api/v1/rooms/{roomId}/analytics", revealedRoom.getId()))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_reject_project_analytics_without_jwt() throws Exception {
      mockMvc
          .perform(get("/api/v1/projects/{slug}/analytics", project.getSlug()))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Cross-room voting")
  class CrossRoomVoting {

    private RoomEntity roomA;
    private RoomEntity roomB;
    private TaskEntity taskInRoomB;
    private ParticipantEntity participantWithRoomAAccess;

    @BeforeEach
    void setUpTwoRooms() {
      roomA = roomRepository.save(Fixtures.roomEntity(project));
      roomB = roomRepository.save(Fixtures.roomEntity(project));
      taskRepository.save(Fixtures.taskEntity(roomA, "Task in Room A", 0));
      taskInRoomB = taskRepository.save(Fixtures.taskEntity(roomB, "Task in Room B", 0));

      // Create participant with room A access only
      participantWithRoomAAccess =
          participantRepository.save(Fixtures.participantEntity(project, "RoomA-Only"));
      participantRepository.addRoomAccess(participantWithRoomAAccess.getId(), roomA.getId());
    }

    @Test
    void should_reject_estimate_for_room_participant_is_not_invited_to() throws Exception {
      String participantJwt = testJwt.guestParticipantJwt(project, participantWithRoomAAccess);
      String body = "{\"storyPoints\":\"5\"}";

      mockMvc
          .perform(
              post("/api/v1/tasks/{taskId}/estimates", taskInRoomB.getId())
                  .header("Authorization", "Bearer " + participantJwt)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Participant progress leaks IDs")
  class ParticipantProgressLeaksIds {

    @Test
    void should_not_expose_participant_ids_in_progress_response() throws Exception {
      ParticipantEntity participant =
          participantRepository.save(Fixtures.participantEntity(project, "Alice"));
      String participantUuid = participant.getId().toString();

      String responseBody =
          mockMvc
              .perform(get("/api/v1/rooms/{roomId}/participant-progress", room.getId()))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      org.assertj.core.api.Assertions.assertThat(responseBody)
          .as("Participant progress response should not leak participant UUIDs")
          .doesNotContain(participantUuid);
    }
  }

  @Nested
  @DisplayName("CSV export Content-Type")
  class CsvExportContentType {

    @Test
    void should_return_csv_content_type_on_export() throws Exception {
      RoomEntity revealedRoom = roomRepository.save(Fixtures.revealedRoomEntity(project));
      TaskEntity task = taskRepository.save(Fixtures.taskEntity(revealedRoom));
      ParticipantEntity participant =
          participantRepository.save(Fixtures.participantEntity(project, "Alice"));
      estimateRepository.save(Fixtures.estimateEntity(task, participant, "5"));

      String adminJwt = testJwt.guestAdminJwt(project);

      mockMvc
          .perform(
              get("/api/v1/rooms/{roomId}/results/export", revealedRoom.getId())
                  .header("Authorization", "Bearer " + adminJwt))
          .andExpect(status().isOk())
          .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }
  }

  @Nested
  @DisplayName("Error messages reveal internals")
  class ErrorMessageDetail {

    @Test
    void should_return_generic_404_for_unmapped_paths() throws Exception {
      String responseBody =
          mockMvc
              .perform(get("/api/v1/nonexistent-" + UUID.randomUUID()))
              .andExpect(status().isNotFound())
              .andReturn()
              .getResponse()
              .getContentAsString();

      org.assertj.core.api.Assertions.assertThat(responseBody)
          .as("404 response should not reveal Spring internals")
          .doesNotContain("static resource");
    }
  }

  @Nested
  @DisplayName("Login returns 403 instead of 401")
  class LoginStatusCode {

    @Test
    void should_return_401_for_invalid_credentials() throws Exception {
      // language=JSON
      String body =
          """
          {"username":"nonexistent","password":"wrongpassword123"}
          """;

      mockMvc
          .perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isUnauthorized());
    }
  }
}
