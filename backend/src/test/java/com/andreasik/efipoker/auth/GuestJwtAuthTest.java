package com.andreasik.efipoker.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andreasik.efipoker.estimation.room.RoomEntity;
import com.andreasik.efipoker.participant.ParticipantEntity;
import com.andreasik.efipoker.project.ProjectEntity;
import com.andreasik.efipoker.shared.test.BaseComponentTest;
import com.andreasik.efipoker.shared.test.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("Guest JWT Auth")
class GuestJwtAuthTest extends BaseComponentTest {

  private ProjectEntity projectA;
  private ProjectEntity projectB;
  private RoomEntity roomA;

  @BeforeEach
  void setUp() {
    projectA = projectRepository.save(Fixtures.projectEntity());
    projectB = projectRepository.save(Fixtures.projectEntity());
    roomA = roomRepository.save(Fixtures.roomEntity(projectA));
  }

  @Nested
  @DisplayName("Cross-project scope validation")
  class CrossProjectScope {

    @Test
    void should_reject_admin_jwt_for_wrong_project() throws Exception {
      String adminJwtForB = testJwt.guestAdminJwt(projectB);

      mockMvc
          .perform(
              get("/api/v1/projects/{slug}/admin", projectA.getSlug())
                  .header("Authorization", "Bearer " + adminJwtForB))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_reject_admin_jwt_for_wrong_project_room() throws Exception {
      String adminJwtForB = testJwt.guestAdminJwt(projectB);

      mockMvc
          .perform(
              post("/api/v1/rooms/{id}/reveal", roomA.getId())
                  .header("Authorization", "Bearer " + adminJwtForB))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Admin code exchange")
  class AdminCodeExchange {

    @Test
    void should_exchange_valid_admin_code_for_jwt() throws Exception {
      // language=JSON
      String body =
          """
          {"slug":"%s","adminCode":"%s"}
          """
              .formatted(projectA.getSlug(), Fixtures.TEST_ADMIN_CODE);

      mockMvc
          .perform(
              post("/api/v1/auth/guest/admin-exchange")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.token").isNotEmpty())
          .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void should_reject_invalid_admin_code() throws Exception {
      // language=JSON
      String body =
          """
          {"slug":"%s","adminCode":"wrong-code"}
          """
              .formatted(projectA.getSlug());

      mockMvc
          .perform(
              post("/api/v1/auth/guest/admin-exchange")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Guest token refresh")
  class GuestTokenRefresh {

    @Test
    void should_refresh_guest_token() throws Exception {
      String adminJwt = testJwt.guestAdminJwt(projectA);

      mockMvc
          .perform(post("/api/v1/auth/guest/refresh").header("Authorization", "Bearer " + adminJwt))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.token").isNotEmpty())
          .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void should_reject_refresh_without_token() throws Exception {
      mockMvc.perform(post("/api/v1/auth/guest/refresh")).andExpect(status().isForbidden());
    }

    @Test
    void should_reject_refresh_with_user_jwt() throws Exception {
      String userJwt = loginAsTestAdmin();

      mockMvc
          .perform(post("/api/v1/auth/guest/refresh").header("Authorization", "Bearer " + userJwt))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Join returns guest token")
  class JoinReturnsToken {

    @Test
    void should_return_guest_token_on_join() throws Exception {
      // language=JSON
      String body =
          """
          {"nickname":"Alice"}
          """;

      mockMvc
          .perform(
              post("/api/v1/projects/{slug}/participants", projectA.getSlug())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.token").isNotEmpty())
          .andExpect(jsonPath("$.tokenExpiresAt").isNotEmpty())
          .andExpect(jsonPath("$.nickname").value("Alice"));
    }

    @Test
    void should_preserve_admin_on_join_with_admin_jwt() throws Exception {
      String adminJwt = testJwt.guestAdminJwt(projectA);
      // language=JSON
      String body =
          """
          {"nickname":"AdminAlice"}
          """;

      mockMvc
          .perform(
              post("/api/v1/projects/{slug}/participants", projectA.getSlug())
                  .header("Authorization", "Bearer " + adminJwt)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.token").isNotEmpty())
          .andExpect(jsonPath("$.nickname").value("AdminAlice"));
    }
  }

  @Nested
  @DisplayName("Create project returns guest admin token")
  class CreateProjectToken {

    @Test
    void should_return_token_and_admin_code_on_create() throws Exception {
      // language=JSON
      String body =
          """
          {"name":"Test Project"}
          """;

      mockMvc
          .perform(post("/api/v1/projects").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.token").isNotEmpty())
          .andExpect(jsonPath("$.tokenExpiresAt").isNotEmpty())
          .andExpect(jsonPath("$.adminCode").isNotEmpty())
          .andExpect(jsonPath("$.slug").isNotEmpty());
    }
  }

  @Nested
  @DisplayName("Logged-in user flows")
  class LoggedInUserFlows {

    @Test
    void should_not_return_guest_token_when_logged_in_creates_project() throws Exception {
      String userJwt = loginAsTestAdmin();

      // language=JSON
      String body =
          """
          {"name":"Logged In Project"}
          """;

      mockMvc
          .perform(
              post("/api/v1/projects")
                  .header("Authorization", "Bearer " + userJwt)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.token").doesNotExist())
          .andExpect(jsonPath("$.adminCode").isNotEmpty())
          .andExpect(jsonPath("$.slug").isNotEmpty());
    }

    @Test
    void should_not_return_guest_token_when_logged_in_joins_project() throws Exception {
      String userJwt = loginAsTestAdmin();

      // language=JSON
      String body =
          """
          {"nickname":"LoggedInUser"}
          """;

      mockMvc
          .perform(
              post("/api/v1/projects/{slug}/participants", projectA.getSlug())
                  .header("Authorization", "Bearer " + userJwt)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.token").doesNotExist());
    }
  }

  @Nested
  @DisplayName("Participant JWT access")
  class ParticipantAccess {

    @Test
    void should_allow_estimate_with_participant_jwt() throws Exception {
      ParticipantEntity participant =
          participantRepository.save(Fixtures.participantEntity(projectA, "Voter"));
      var task = taskRepository.save(Fixtures.taskEntity(roomA));
      String participantJwt = testJwt.guestParticipantJwt(projectA, participant);

      // language=JSON
      String body =
          """
          {"storyPoints":"5"}
          """;

      mockMvc
          .perform(
              post("/api/v1/tasks/{id}/estimates", task.getId())
                  .header("Authorization", "Bearer " + participantJwt)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk());
    }

    @Test
    void should_reject_admin_action_with_participant_jwt() throws Exception {
      ParticipantEntity participant =
          participantRepository.save(Fixtures.participantEntity(projectA, "Voter"));
      String participantJwt = testJwt.guestParticipantJwt(projectA, participant);

      mockMvc
          .perform(
              post("/api/v1/rooms/{id}/reveal", roomA.getId())
                  .header("Authorization", "Bearer " + participantJwt))
          .andExpect(status().isForbidden());
    }
  }
}
