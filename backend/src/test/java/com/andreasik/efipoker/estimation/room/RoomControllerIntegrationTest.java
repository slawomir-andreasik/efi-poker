package com.andreasik.efipoker.estimation.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@DisplayName("RoomController Integration")
class RoomControllerIntegrationTest extends BaseComponentTest {

  private ProjectEntity project;

  @BeforeEach
  void setUp() {
    project = projectRepository.save(Fixtures.projectEntity());
  }

  @Nested
  @DisplayName("POST /api/v1/projects/{slug}/rooms")
  class CreateRoom {

    @Test
    void should_create_async_room_201() throws Exception {
      // language=JSON
      String body =
          """
          {"title":"Sprint 1","roomType":"ASYNC","deadline":"2030-01-01T00:00:00Z"}
          """;

      mockMvc
          .perform(
              post("/api/v1/projects/{slug}/rooms", project.getSlug())
                  .header("X-Admin-Code", project.getAdminCode())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.title").value("Sprint 1"))
          .andExpect(jsonPath("$.slug").isNotEmpty())
          .andExpect(jsonPath("$.roomType").value("ASYNC"))
          .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void should_create_live_room_201() throws Exception {
      // language=JSON
      String body =
          """
          {"title":"Live Planning","roomType":"LIVE"}
          """;

      mockMvc
          .perform(
              post("/api/v1/projects/{slug}/rooms", project.getSlug())
                  .header("X-Admin-Code", project.getAdminCode())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.title").value("Live Planning"))
          .andExpect(jsonPath("$.slug").isNotEmpty())
          .andExpect(jsonPath("$.roomType").value("LIVE"));
    }

    @Test
    void should_reject_wrong_admin_code_403() throws Exception {
      // language=JSON
      String body =
          """
          {"title":"Sprint 1","roomType":"ASYNC","deadline":"2030-01-01T00:00:00Z"}
          """;

      mockMvc
          .perform(
              post("/api/v1/projects/{slug}/rooms", project.getSlug())
                  .header("X-Admin-Code", "wrong-code")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_create_room_with_auto_reveal_false() throws Exception {
      // language=JSON
      String body =
          """
          {"title":"Sprint 1","roomType":"ASYNC","deadline":"2030-01-01T00:00:00Z","autoRevealOnDeadline":false}
          """;

      mockMvc
          .perform(
              post("/api/v1/projects/{slug}/rooms", project.getSlug())
                  .header("X-Admin-Code", project.getAdminCode())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.autoRevealOnDeadline").value(false));
    }

    @Test
    void should_default_auto_reveal_to_true() throws Exception {
      // language=JSON
      String body =
          """
          {"title":"Sprint 2","roomType":"ASYNC","deadline":"2030-01-01T00:00:00Z"}
          """;

      mockMvc
          .perform(
              post("/api/v1/projects/{slug}/rooms", project.getSlug())
                  .header("X-Admin-Code", project.getAdminCode())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.autoRevealOnDeadline").value(true));
    }

    @Test
    void should_reject_too_long_description_400() throws Exception {
      String longDescription = "a".repeat(2001);
      String body =
          "{\"title\":\"Sprint 1\",\"roomType\":\"ASYNC\","
              + "\"deadline\":\"2030-01-01T00:00:00Z\","
              + "\"description\":\""
              + longDescription
              + "\"}";

      mockMvc
          .perform(
              post("/api/v1/projects/{slug}/rooms", project.getSlug())
                  .header("X-Admin-Code", project.getAdminCode())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/projects/{slug}/rooms")
  class ListRooms {

    @Test
    void should_list_rooms_200() throws Exception {
      roomRepository.save(Fixtures.roomEntity(project));
      roomRepository.save(Fixtures.roomEntity(project, "LIVE"));

      mockMvc
          .perform(get("/api/v1/projects/{slug}/rooms", project.getSlug()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void should_return_empty_list_200() throws Exception {
      mockMvc
          .perform(get("/api/v1/projects/{slug}/rooms", project.getSlug()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }
  }

  @Nested
  @DisplayName("GET /api/v1/rooms/{roomId}")
  class GetRoom {

    @Test
    void should_return_room_detail_200() throws Exception {
      RoomEntity room = roomRepository.save(Fixtures.roomEntity(project));
      ParticipantEntity participant =
          participantRepository.save(Fixtures.participantEntity(project, "Alice"));

      mockMvc
          .perform(
              get("/api/v1/rooms/{roomId}", room.getId())
                  .header("X-Participant-Id", participant.getId().toString()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.title").value(room.getTitle()))
          .andExpect(jsonPath("$.roomType").value("ASYNC"))
          .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void should_return_404_for_invalid_room_id() throws Exception {
      String response =
          mockMvc
              .perform(get("/api/v1/rooms/{roomId}", UUID.randomUUID()))
              .andExpect(status().isNotFound())
              .andExpect(jsonPath("$.detail").value("Room not found"))
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertThat(response).doesNotContain("with id:");
    }
  }

  @Nested
  @DisplayName("GET /api/v1/rooms/{roomId}/admin")
  class GetRoomAdmin {

    @Test
    void should_return_admin_view_200() throws Exception {
      RoomEntity room = roomRepository.save(Fixtures.roomEntity(project));

      mockMvc
          .perform(
              get("/api/v1/rooms/{roomId}/admin", room.getId())
                  .header("X-Admin-Code", project.getAdminCode()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.title").value(room.getTitle()))
          .andExpect(jsonPath("$.tasks").isArray())
          .andExpect(jsonPath("$.participants").isArray());
    }

    @Test
    void should_reject_wrong_admin_code_403() throws Exception {
      RoomEntity room = roomRepository.save(Fixtures.roomEntity(project));

      mockMvc
          .perform(
              get("/api/v1/rooms/{roomId}/admin", room.getId())
                  .header("X-Admin-Code", "wrong-code"))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("PATCH /api/v1/rooms/{roomId}")
  class UpdateRoom {

    @Test
    void should_update_room_200() throws Exception {
      RoomEntity room = roomRepository.save(Fixtures.roomEntity(project));
      // language=JSON
      String body =
          """
          {"title":"Updated Title"}
          """;

      mockMvc
          .perform(
              patch("/api/v1/rooms/{roomId}", room.getId())
                  .header("X-Admin-Code", project.getAdminCode())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    void should_reject_wrong_admin_code_403() throws Exception {
      RoomEntity room = roomRepository.save(Fixtures.roomEntity(project));
      // language=JSON
      String body =
          """
          {"title":"Updated"}
          """;

      mockMvc
          .perform(
              patch("/api/v1/rooms/{roomId}", room.getId())
                  .header("X-Admin-Code", "wrong-code")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("POST /api/v1/rooms/{roomId}/reveal")
  class RevealEstimates {

    @Test
    void should_reveal_room_200() throws Exception {
      RoomEntity room = roomRepository.save(Fixtures.roomEntity(project));

      mockMvc
          .perform(
              post("/api/v1/rooms/{roomId}/reveal", room.getId())
                  .header("X-Admin-Code", project.getAdminCode()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("REVEALED"));
    }

    @Test
    void should_reject_wrong_admin_code_403() throws Exception {
      RoomEntity room = roomRepository.save(Fixtures.roomEntity(project));

      mockMvc
          .perform(
              post("/api/v1/rooms/{roomId}/reveal", room.getId())
                  .header("X-Admin-Code", "wrong-code"))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("POST /api/v1/rooms/{roomId}/reopen")
  class ReopenRoom {

    @Test
    void should_reopen_room_200() throws Exception {
      RoomEntity room = roomRepository.save(Fixtures.revealedRoomEntity(project));

      mockMvc
          .perform(
              post("/api/v1/rooms/{roomId}/reopen", room.getId())
                  .header("X-Admin-Code", project.getAdminCode()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("OPEN"));
    }
  }

  @Nested
  @DisplayName("GET /api/v1/rooms/{roomId}/progress")
  class GetRoomProgress {

    @Test
    void should_return_progress_200() throws Exception {
      RoomEntity room = roomRepository.save(Fixtures.roomEntity(project));
      taskRepository.save(Fixtures.taskEntity(room));

      mockMvc
          .perform(get("/api/v1/rooms/{roomId}/progress", room.getId()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.roomId").value(room.getId().toString()))
          .andExpect(jsonPath("$.tasks").isArray());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/rooms/{roomId}/results")
  class GetRoomResults {

    @Test
    void should_return_results_when_revealed() throws Exception {
      RoomEntity room = roomRepository.save(Fixtures.revealedRoomEntity(project));
      taskRepository.save(Fixtures.taskEntity(room));

      mockMvc
          .perform(get("/api/v1/rooms/{roomId}/results", room.getId()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.roomId").value(room.getId().toString()))
          .andExpect(jsonPath("$.tasks").isArray());
    }

    @Test
    void should_reject_not_revealed_403() throws Exception {
      RoomEntity room = roomRepository.save(Fixtures.roomEntity(project));

      mockMvc
          .perform(get("/api/v1/rooms/{roomId}/results", room.getId()))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/rooms/{roomId}/results/export")
  class ExportResults {

    @Test
    void should_export_csv_when_revealed() throws Exception {
      RoomEntity room = roomRepository.save(Fixtures.revealedRoomEntity(project));
      ParticipantEntity participant =
          participantRepository.save(Fixtures.participantEntity(project, "Alice"));
      TaskEntity task = taskRepository.save(Fixtures.taskEntity(room));
      estimateRepository.save(Fixtures.estimateEntity(task, participant, "5"));

      String csv =
          mockMvc
              .perform(
                  get("/api/v1/rooms/{roomId}/results/export", room.getId())
                      .header("X-Admin-Code", project.getAdminCode()))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertAll(
          () -> assertThat(csv).contains("Task"),
          () -> assertThat(csv).contains("Alice"),
          () -> assertThat(csv).contains("Final SP"));
    }

    @Test
    void should_reject_not_revealed_403() throws Exception {
      RoomEntity room = roomRepository.save(Fixtures.roomEntity(project));

      mockMvc
          .perform(
              get("/api/v1/rooms/{roomId}/results/export", room.getId())
                  .header("X-Admin-Code", project.getAdminCode()))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("POST /api/v1/projects/{slug}/rooms - LIVE creates phantom task")
  class CreateLiveRoom {

    @Test
    void should_create_live_room_with_phantom_task() throws Exception {
      // language=JSON
      String body =
          """
          {"title":"Live Planning","roomType":"LIVE"}
          """;

      String response =
          mockMvc
              .perform(
                  post("/api/v1/projects/{slug}/rooms", project.getSlug())
                      .header("X-Admin-Code", project.getAdminCode())
                      .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                      .content(body))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("$.roomType").value("LIVE"))
              .andReturn()
              .getResponse()
              .getContentAsString();

      UUID roomId = UUID.fromString(objectMapper.readTree(response).get("id").asText());

      assertThat(taskRepository.findByRoomIdAndTitle(roomId, "__live__")).isPresent();
    }
  }

  @Nested
  @DisplayName("GET /api/v1/rooms/{roomId}/live")
  class GetLiveRoomState {

    @Test
    void should_return_live_state_200() throws Exception {
      // Arrange - create LIVE room via API so phantom task is auto-created
      // language=JSON
      String body =
          """
          {"title":"Live Planning","roomType":"LIVE"}
          """;
      String createResponse =
          mockMvc
              .perform(
                  post("/api/v1/projects/{slug}/rooms", project.getSlug())
                      .header("X-Admin-Code", project.getAdminCode())
                      .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                      .content(body))
              .andExpect(status().isCreated())
              .andReturn()
              .getResponse()
              .getContentAsString();

      String roomId = objectMapper.readTree(createResponse).get("id").asText();

      mockMvc
          .perform(get("/api/v1/rooms/{roomId}/live", roomId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.roomId").value(roomId))
          .andExpect(jsonPath("$.status").value("OPEN"))
          .andExpect(jsonPath("$.roundNumber").value(1))
          .andExpect(jsonPath("$.participants").isArray())
          .andExpect(jsonPath("$.taskId").isNotEmpty());
    }

    @Test
    void should_reject_live_state_on_async_room_409() throws Exception {
      RoomEntity asyncRoom = roomRepository.save(Fixtures.roomEntity(project, "ASYNC"));

      mockMvc
          .perform(get("/api/v1/rooms/{roomId}/live", asyncRoom.getId()))
          .andExpect(status().isConflict());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/rooms/by-slug/{roomSlug}")
  class GetRoomBySlug {

    @Test
    void should_return_slug_response_200() throws Exception {
      RoomEntity room = roomRepository.save(Fixtures.roomEntity(project));

      mockMvc
          .perform(get("/api/v1/rooms/by-slug/{roomSlug}", room.getSlug()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.roomId").value(room.getId().toString()))
          .andExpect(jsonPath("$.roomTitle").value(room.getTitle()))
          .andExpect(jsonPath("$.roomSlug").value(room.getSlug()))
          .andExpect(jsonPath("$.projectSlug").value(project.getSlug()))
          .andExpect(jsonPath("$.projectName").value(project.getName()));
    }

    @Test
    void should_return_404_for_unknown_slug() throws Exception {
      mockMvc
          .perform(get("/api/v1/rooms/by-slug/{roomSlug}", "A3X-K7B"))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("DELETE /api/v1/rooms/{roomId}")
  class DeleteRoom {

    @Test
    void should_delete_room_204() throws Exception {
      RoomEntity room = roomRepository.save(Fixtures.roomEntity(project));

      mockMvc
          .perform(
              delete("/api/v1/rooms/{roomId}", room.getId())
                  .header("X-Admin-Code", project.getAdminCode()))
          .andExpect(status().isNoContent());

      assertThat(roomRepository.findById(room.getId())).isEmpty();
    }

    @Test
    void should_reject_wrong_admin_code_403() throws Exception {
      RoomEntity room = roomRepository.save(Fixtures.roomEntity(project));

      mockMvc
          .perform(
              delete("/api/v1/rooms/{roomId}", room.getId()).header("X-Admin-Code", "wrong-code"))
          .andExpect(status().isForbidden());

      assertThat(roomRepository.findById(room.getId())).isPresent();
    }

    @Test
    void should_return_404_for_unknown_room() throws Exception {
      mockMvc
          .perform(
              delete("/api/v1/rooms/{roomId}", UUID.randomUUID())
                  .header("X-Admin-Code", project.getAdminCode()))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("POST /api/v1/rooms/{roomId}/new-round")
  class NewRound {

    @Test
    void should_start_new_round_200() throws Exception {
      // Arrange - create LIVE room via API so phantom task is auto-created
      // language=JSON
      String body =
          """
          {"title":"Live Planning","roomType":"LIVE"}
          """;
      String createResponse =
          mockMvc
              .perform(
                  post("/api/v1/projects/{slug}/rooms", project.getSlug())
                      .header("X-Admin-Code", project.getAdminCode())
                      .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                      .content(body))
              .andExpect(status().isCreated())
              .andReturn()
              .getResponse()
              .getContentAsString();

      String roomId = objectMapper.readTree(createResponse).get("id").asText();

      // language=JSON
      String newRoundBody =
          """
          {"topic":"Story EP-42"}
          """;

      mockMvc
          .perform(
              post("/api/v1/rooms/{roomId}/new-round", roomId)
                  .header("X-Admin-Code", project.getAdminCode())
                  .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                  .content(newRoundBody))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.roundNumber").value(2))
          .andExpect(jsonPath("$.topic").value("Story EP-42"))
          .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void should_reject_new_round_on_async_room_409() throws Exception {
      RoomEntity asyncRoom = roomRepository.save(Fixtures.roomEntity(project, "ASYNC"));

      mockMvc
          .perform(
              post("/api/v1/rooms/{roomId}/new-round", asyncRoom.getId())
                  .header("X-Admin-Code", project.getAdminCode())
                  .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
          .andExpect(status().isConflict());
    }
  }
}
