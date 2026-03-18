package com.andreasik.efipoker.participant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andreasik.efipoker.project.ProjectEntity;
import com.andreasik.efipoker.shared.test.BaseComponentTest;
import com.andreasik.efipoker.shared.test.Fixtures;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("ParticipantController Integration")
class ParticipantControllerIntegrationTest extends BaseComponentTest {

  private ProjectEntity project;

  @BeforeEach
  void setUp() {
    project = projectRepository.save(Fixtures.projectEntity());
  }

  @Nested
  @DisplayName("POST /api/v1/projects/{slug}/participants")
  class JoinProject {

    @Test
    void should_join_project_200() throws Exception {
      // language=JSON
      String body =
          """
          {"nickname":"Alice"}
          """;

      mockMvc
          .perform(
              post("/api/v1/projects/{slug}/participants", project.getSlug())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").isNotEmpty())
          .andExpect(jsonPath("$.nickname").value("Alice"));
    }

    @Test
    void should_return_same_participant_for_same_nickname() throws Exception {
      // language=JSON
      String body =
          """
          {"nickname":"Bob"}
          """;

      String json1 =
          mockMvc
              .perform(
                  post("/api/v1/projects/{slug}/participants", project.getSlug())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(body))
              .andReturn()
              .getResponse()
              .getContentAsString();

      String json2 =
          mockMvc
              .perform(
                  post("/api/v1/projects/{slug}/participants", project.getSlug())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(body))
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertThat(JsonPath.<String>read(json1, "$.id"))
          .isEqualTo(JsonPath.<String>read(json2, "$.id"));
    }

    @Test
    void should_return_404_for_nonexistent_project() throws Exception {
      // language=JSON
      String body =
          """
          {"nickname":"Alice"}
          """;

      mockMvc
          .perform(
              post("/api/v1/projects/{slug}/participants", "nonexistent")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isNotFound());
    }

    @Test
    void should_reject_blank_nickname_400() throws Exception {
      // language=JSON
      String body =
          """
          {"nickname":""}
          """;

      mockMvc
          .perform(
              post("/api/v1/projects/{slug}/participants", project.getSlug())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/projects/{slug}/participants")
  class ListProjectParticipants {

    @Test
    void should_list_participants_200() throws Exception {
      participantRepository.save(Fixtures.participantEntity(project, "Alice"));
      participantRepository.save(Fixtures.participantEntity(project, "Bob"));

      mockMvc
          .perform(
              get("/api/v1/projects/{slug}/participants", project.getSlug())
                  .header("X-Admin-Code", Fixtures.TEST_ADMIN_CODE))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].nickname").isNotEmpty())
          .andExpect(jsonPath("$[1].nickname").isNotEmpty());
    }

    @Test
    void should_return_empty_list_200() throws Exception {
      mockMvc
          .perform(
              get("/api/v1/projects/{slug}/participants", project.getSlug())
                  .header("X-Admin-Code", Fixtures.TEST_ADMIN_CODE))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void should_reject_without_admin_code_403() throws Exception {
      mockMvc
          .perform(get("/api/v1/projects/{slug}/participants", project.getSlug()))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_reject_invalid_admin_code_403() throws Exception {
      mockMvc
          .perform(
              get("/api/v1/projects/{slug}/participants", project.getSlug())
                  .header("X-Admin-Code", "wrong-code"))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_return_404_for_nonexistent_project() throws Exception {
      mockMvc
          .perform(
              get("/api/v1/projects/{slug}/participants", "nonexistent")
                  .header("X-Admin-Code", Fixtures.TEST_ADMIN_CODE))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/projects/{slug}/participants/{participantId}")
  class GetParticipant {

    @Test
    void should_get_participant_200() throws Exception {
      ParticipantEntity participant =
          participantRepository.save(Fixtures.participantEntity(project, "Alice"));

      mockMvc
          .perform(
              get(
                  "/api/v1/projects/{slug}/participants/{participantId}",
                  project.getSlug(),
                  participant.getId()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(participant.getId().toString()))
          .andExpect(jsonPath("$.nickname").value("Alice"));
    }

    @Test
    void should_return_404_for_nonexistent_participant() throws Exception {
      mockMvc
          .perform(
              get(
                  "/api/v1/projects/{slug}/participants/{participantId}",
                  project.getSlug(),
                  UUID.randomUUID()))
          .andExpect(status().isNotFound());
    }

    @Test
    void should_not_expose_internal_id_in_404_response() throws Exception {
      UUID randomId = UUID.randomUUID();
      String response =
          mockMvc
              .perform(
                  get(
                      "/api/v1/projects/{slug}/participants/{participantId}",
                      project.getSlug(),
                      randomId))
              .andExpect(status().isNotFound())
              .andExpect(jsonPath("$.detail").value("Participant not found"))
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertThat(response).doesNotContain("with id:");
    }

    @Test
    void should_return_404_for_participant_in_different_project() throws Exception {
      ProjectEntity otherProject =
          projectRepository.save(Fixtures.projectEntity("Other Project", "other123"));
      ParticipantEntity participant =
          participantRepository.save(Fixtures.participantEntity(otherProject, "Alice"));

      mockMvc
          .perform(
              get(
                  "/api/v1/projects/{slug}/participants/{participantId}",
                  project.getSlug(),
                  participant.getId()))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("PATCH /api/v1/projects/{slug}/participants/{participantId}")
  class UpdateParticipant {

    @Test
    void should_update_nickname_200() throws Exception {
      ParticipantEntity participant =
          participantRepository.save(Fixtures.participantEntity(project, "Alice"));
      // language=JSON
      String body =
          """
          {"nickname":"AliceNew"}
          """;

      mockMvc
          .perform(
              patch(
                      "/api/v1/projects/{slug}/participants/{participantId}",
                      project.getSlug(),
                      participant.getId())
                  .header("X-Participant-Id", participant.getId())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(participant.getId().toString()))
          .andExpect(jsonPath("$.nickname").value("AliceNew"));

      assertThat(participantRepository.findById(participant.getId()))
          .isPresent()
          .get()
          .extracting(ParticipantEntity::getNickname)
          .isEqualTo("AliceNew");
    }

    @Test
    void should_return_same_when_nickname_unchanged() throws Exception {
      ParticipantEntity participant =
          participantRepository.save(Fixtures.participantEntity(project, "Alice"));
      // language=JSON
      String body =
          """
          {"nickname":"Alice"}
          """;

      mockMvc
          .perform(
              patch(
                      "/api/v1/projects/{slug}/participants/{participantId}",
                      project.getSlug(),
                      participant.getId())
                  .header("X-Participant-Id", participant.getId())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.nickname").value("Alice"));
    }

    @Test
    void should_reject_when_nickname_taken_409() throws Exception {
      ParticipantEntity alice =
          participantRepository.save(Fixtures.participantEntity(project, "Alice"));
      participantRepository.save(Fixtures.participantEntity(project, "Bob"));
      // language=JSON
      String body =
          """
          {"nickname":"Bob"}
          """;

      mockMvc
          .perform(
              patch(
                      "/api/v1/projects/{slug}/participants/{participantId}",
                      project.getSlug(),
                      alice.getId())
                  .header("X-Participant-Id", alice.getId())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isConflict());
    }

    @Test
    void should_reject_without_participant_header_403() throws Exception {
      ParticipantEntity participant =
          participantRepository.save(Fixtures.participantEntity(project, "Alice"));
      // language=JSON
      String body =
          """
          {"nickname":"AliceNew"}
          """;

      mockMvc
          .perform(
              patch(
                      "/api/v1/projects/{slug}/participants/{participantId}",
                      project.getSlug(),
                      participant.getId())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_reject_mismatched_participant_id_403() throws Exception {
      ParticipantEntity participant =
          participantRepository.save(Fixtures.participantEntity(project, "Alice"));
      // language=JSON
      String body =
          """
          {"nickname":"AliceNew"}
          """;

      mockMvc
          .perform(
              patch(
                      "/api/v1/projects/{slug}/participants/{participantId}",
                      project.getSlug(),
                      participant.getId())
                  .header("X-Participant-Id", UUID.randomUUID())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_return_404_for_nonexistent_participant() throws Exception {
      UUID fakeId = UUID.randomUUID();
      // language=JSON
      String body =
          """
          {"nickname":"AliceNew"}
          """;

      mockMvc
          .perform(
              patch(
                      "/api/v1/projects/{slug}/participants/{participantId}",
                      project.getSlug(),
                      fakeId)
                  .header("X-Participant-Id", fakeId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("DELETE /api/v1/projects/{slug}/participants/{participantId}")
  class DeleteParticipant {

    @Test
    void should_delete_participant_204() throws Exception {
      ParticipantEntity participant =
          participantRepository.save(Fixtures.participantEntity(project, "Alice"));

      mockMvc
          .perform(
              delete(
                      "/api/v1/projects/{slug}/participants/{participantId}",
                      project.getSlug(),
                      participant.getId())
                  .header("X-Admin-Code", Fixtures.TEST_ADMIN_CODE))
          .andExpect(status().isNoContent());

      assertThat(participantRepository.findByIdAndProjectId(participant.getId(), project.getId()))
          .isEmpty();
    }

    @Test
    void should_reject_without_admin_code_403() throws Exception {
      ParticipantEntity participant =
          participantRepository.save(Fixtures.participantEntity(project, "Alice"));

      mockMvc
          .perform(
              delete(
                  "/api/v1/projects/{slug}/participants/{participantId}",
                  project.getSlug(),
                  participant.getId()))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_reject_invalid_admin_code_403() throws Exception {
      ParticipantEntity participant =
          participantRepository.save(Fixtures.participantEntity(project, "Alice"));

      mockMvc
          .perform(
              delete(
                      "/api/v1/projects/{slug}/participants/{participantId}",
                      project.getSlug(),
                      participant.getId())
                  .header("X-Admin-Code", "wrong-code"))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_return_404_for_nonexistent_participant() throws Exception {
      mockMvc
          .perform(
              delete(
                      "/api/v1/projects/{slug}/participants/{participantId}",
                      project.getSlug(),
                      UUID.randomUUID())
                  .header("X-Admin-Code", Fixtures.TEST_ADMIN_CODE))
          .andExpect(status().isNotFound());
    }
  }
}
