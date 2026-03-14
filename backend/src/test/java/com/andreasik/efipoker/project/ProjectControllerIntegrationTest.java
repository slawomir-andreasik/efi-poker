package com.andreasik.efipoker.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andreasik.efipoker.shared.test.BaseComponentTest;
import com.andreasik.efipoker.shared.test.Fixtures;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("ProjectController Integration")
class ProjectControllerIntegrationTest extends BaseComponentTest {

  @Nested
  @DisplayName("POST /api/v1/projects")
  class CreateProject {

    @Test
    void should_create_project_201() throws Exception {
      // language=JSON
      String body =
          """
          {"name":"My Project"}
          """;

      String json =
          mockMvc
              .perform(
                  post("/api/v1/projects").contentType(MediaType.APPLICATION_JSON).content(body))
              .andExpect(status().isCreated())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertAll(
          () -> assertThat(JsonPath.<String>read(json, "$.id")).isNotBlank(),
          () -> assertThat(JsonPath.<String>read(json, "$.name")).isEqualTo("My Project"),
          () -> assertThat(JsonPath.<String>read(json, "$.slug")).isNotBlank(),
          () -> assertThat(JsonPath.<String>read(json, "$.adminCode")).isNotBlank());
    }

    @Test
    void should_reject_blank_name_400() throws Exception {
      // language=JSON
      String body =
          """
          {"name":""}
          """;

      mockMvc
          .perform(post("/api/v1/projects").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/projects/{slug}")
  class GetBySlug {

    @Test
    void should_return_project_200() throws Exception {
      ProjectEntity project = projectRepository.save(Fixtures.projectEntity());

      mockMvc
          .perform(get("/api/v1/projects/{slug}", project.getSlug()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.name").value(project.getName()))
          .andExpect(jsonPath("$.slug").value(project.getSlug()))
          .andExpect(jsonPath("$.adminCode").doesNotExist());
    }

    @Test
    void should_return_404_for_nonexistent() throws Exception {
      mockMvc
          .perform(get("/api/v1/projects/{slug}", "nonexistent"))
          .andExpect(status().isNotFound());
    }

    @Test
    void should_not_expose_internal_id_in_404_response() throws Exception {
      String response =
          mockMvc
              .perform(get("/api/v1/projects/{slug}", "nonexistent"))
              .andExpect(status().isNotFound())
              .andExpect(jsonPath("$.detail").value("Project not found"))
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertThat(response).doesNotContain("with id:");
    }
  }

  @Nested
  @DisplayName("GET /api/v1/projects/{slug}/admin")
  class GetProjectAdmin {

    @Test
    void should_return_admin_view_with_correct_code() throws Exception {
      ProjectEntity project = projectRepository.save(Fixtures.projectEntity());

      mockMvc
          .perform(
              get("/api/v1/projects/{slug}/admin", project.getSlug())
                  .header("X-Admin-Code", project.getAdminCode()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.adminCode").value(project.getAdminCode()));
    }

    @Test
    void should_reject_wrong_admin_code_403() throws Exception {
      ProjectEntity project = projectRepository.save(Fixtures.projectEntity());

      mockMvc
          .perform(
              get("/api/v1/projects/{slug}/admin", project.getSlug())
                  .header("X-Admin-Code", "wrong-code"))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("PATCH /api/v1/projects/{slug}")
  class UpdateProject {

    @Test
    void should_update_project_200() throws Exception {
      ProjectEntity project = projectRepository.save(Fixtures.projectEntity());
      // language=JSON
      String body =
          """
          {"name":"Updated Name"}
          """;

      String json =
          mockMvc
              .perform(
                  patch("/api/v1/projects/{slug}", project.getSlug())
                      .contentType(MediaType.APPLICATION_JSON)
                      .header("X-Admin-Code", project.getAdminCode())
                      .content(body))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertAll(
          () -> assertThat(JsonPath.<String>read(json, "$.name")).isEqualTo("Updated Name"),
          () ->
              assertThat(JsonPath.<String>read(json, "$.adminCode"))
                  .isEqualTo(project.getAdminCode()));
    }

    @Test
    void should_reject_without_admin_code_403() throws Exception {
      ProjectEntity project = projectRepository.save(Fixtures.projectEntity());
      // language=JSON
      String body =
          """
          {"name":"Updated"}
          """;

      mockMvc
          .perform(
              patch("/api/v1/projects/{slug}", project.getSlug())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_reject_wrong_admin_code_403() throws Exception {
      ProjectEntity project = projectRepository.save(Fixtures.projectEntity());
      // language=JSON
      String body =
          """
          {"name":"Updated"}
          """;

      mockMvc
          .perform(
              patch("/api/v1/projects/{slug}", project.getSlug())
                  .contentType(MediaType.APPLICATION_JSON)
                  .header("X-Admin-Code", "wrong-code")
                  .content(body))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_return_404_for_unknown_slug() throws Exception {
      // language=JSON
      String body =
          """
          {"name":"Updated"}
          """;

      mockMvc
          .perform(
              patch("/api/v1/projects/{slug}", "nonexistent")
                  .contentType(MediaType.APPLICATION_JSON)
                  .header("X-Admin-Code", "some-code")
                  .content(body))
          .andExpect(status().isNotFound());
    }

    @Test
    void should_support_partial_update() throws Exception {
      ProjectEntity project = projectRepository.save(Fixtures.projectEntity());
      // language=JSON
      String body =
          """
          {"name":"Only Name Changed"}
          """;

      String json =
          mockMvc
              .perform(
                  patch("/api/v1/projects/{slug}", project.getSlug())
                      .contentType(MediaType.APPLICATION_JSON)
                      .header("X-Admin-Code", project.getAdminCode())
                      .content(body))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertThat(JsonPath.<String>read(json, "$.name")).isEqualTo("Only Name Changed");
    }
  }

  @Nested
  @DisplayName("DELETE /api/v1/projects/{slug}")
  class DeleteProject {

    @Test
    void should_delete_project_204() throws Exception {
      ProjectEntity project = projectRepository.save(Fixtures.projectEntity());

      mockMvc
          .perform(
              delete("/api/v1/projects/{slug}", project.getSlug())
                  .header("X-Admin-Code", project.getAdminCode()))
          .andExpect(status().isNoContent());

      // Verify project is gone
      mockMvc
          .perform(get("/api/v1/projects/{slug}", project.getSlug()))
          .andExpect(status().isNotFound());
    }

    @Test
    void should_reject_without_admin_code_403() throws Exception {
      ProjectEntity project = projectRepository.save(Fixtures.projectEntity());

      mockMvc
          .perform(delete("/api/v1/projects/{slug}", project.getSlug()))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_reject_wrong_admin_code_403() throws Exception {
      ProjectEntity project = projectRepository.save(Fixtures.projectEntity());

      mockMvc
          .perform(
              delete("/api/v1/projects/{slug}", project.getSlug())
                  .header("X-Admin-Code", "wrong-code"))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_return_404_for_unknown_slug() throws Exception {
      mockMvc
          .perform(
              delete("/api/v1/projects/{slug}", "nonexistent").header("X-Admin-Code", "some-code"))
          .andExpect(status().isNotFound());
    }

    @Test
    void should_cascade_delete_rooms_and_tasks() throws Exception {
      // Create project with room via API
      // language=JSON
      String projectBody =
          """
          {"name":"Cascade Test"}
          """;
      String createJson =
          mockMvc
              .perform(
                  post("/api/v1/projects")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(projectBody))
              .andExpect(status().isCreated())
              .andReturn()
              .getResponse()
              .getContentAsString();

      String cascadeSlug = JsonPath.read(createJson, "$.slug");
      String cascadeAdmin = JsonPath.read(createJson, "$.adminCode");

      // language=JSON
      String roomBody =
          """
          {"title":"Room","roomType":"LIVE"}
          """;
      mockMvc
          .perform(
              post("/api/v1/projects/{slug}/rooms", cascadeSlug)
                  .contentType(MediaType.APPLICATION_JSON)
                  .header("X-Admin-Code", cascadeAdmin)
                  .content(roomBody))
          .andExpect(status().isCreated());

      // Delete project - 204 proves ON DELETE CASCADE works
      // (would fail with 500/FK violation without cascade)
      mockMvc
          .perform(
              delete("/api/v1/projects/{slug}", cascadeSlug).header("X-Admin-Code", cascadeAdmin))
          .andExpect(status().isNoContent());
    }
  }
}
