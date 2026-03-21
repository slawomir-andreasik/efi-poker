package com.andreasik.efipoker.shared.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andreasik.efipoker.estimation.room.RoomEntity;
import com.andreasik.efipoker.participant.ParticipantEntity;
import com.andreasik.efipoker.project.ProjectEntity;
import com.andreasik.efipoker.shared.test.BaseComponentTest;
import com.andreasik.efipoker.shared.test.Fixtures;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("Admin Auth Enforcement")
class AdminAuthEnforcementTest extends BaseComponentTest {

  private ProjectEntity project;
  private RoomEntity room;

  @BeforeEach
  void setUp() {
    project = projectRepository.save(Fixtures.projectEntity());
    room = roomRepository.save(Fixtures.roomEntity(project));
  }

  @Nested
  @DisplayName("Missing JWT")
  class MissingJwt {

    @Test
    void should_reject_get_project_admin() throws Exception {
      mockMvc
          .perform(get("/api/v1/projects/{slug}/admin", project.getSlug()))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_reject_post_room() throws Exception {
      String body =
          "{\"title\":\"Sprint\",\"roomType\":\"ASYNC\",\"deadline\":\""
              + Instant.now().plus(7, ChronoUnit.DAYS)
              + "\"}";

      mockMvc
          .perform(
              post("/api/v1/projects/{slug}/rooms", project.getSlug())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_reject_post_reveal() throws Exception {
      mockMvc
          .perform(post("/api/v1/rooms/{id}/reveal", room.getId()))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_reject_get_admin_view() throws Exception {
      mockMvc
          .perform(get("/api/v1/rooms/{id}/admin", room.getId()))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_reject_patch_room() throws Exception {
      // language=JSON
      String body =
          """
          {"title":"Updated"}
          """;

      mockMvc
          .perform(
              patch("/api/v1/rooms/{id}", room.getId())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_reject_room_analytics() throws Exception {
      RoomEntity revealedRoom = roomRepository.save(Fixtures.revealedRoomEntity(project));
      mockMvc
          .perform(get("/api/v1/rooms/{id}/analytics", revealedRoom.getId()))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_reject_project_analytics() throws Exception {
      mockMvc
          .perform(get("/api/v1/projects/{slug}/analytics", project.getSlug()))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Non-admin JWT (participant only)")
  class NonAdminJwt {

    @Test
    void should_reject_get_project_admin() throws Exception {
      ParticipantEntity participant =
          participantRepository.save(Fixtures.participantEntity(project, "Alice"));
      String participantJwt = testJwt.guestParticipantJwt(project, participant);

      mockMvc
          .perform(
              get("/api/v1/projects/{slug}/admin", project.getSlug())
                  .header("Authorization", "Bearer " + participantJwt))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_reject_post_reveal() throws Exception {
      ParticipantEntity participant =
          participantRepository.save(Fixtures.participantEntity(project, "Alice"));
      String participantJwt = testJwt.guestParticipantJwt(project, participant);

      mockMvc
          .perform(
              post("/api/v1/rooms/{id}/reveal", room.getId())
                  .header("Authorization", "Bearer " + participantJwt))
          .andExpect(status().isForbidden());
    }
  }
}
