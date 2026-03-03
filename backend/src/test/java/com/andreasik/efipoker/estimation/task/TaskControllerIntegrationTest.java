package com.andreasik.efipoker.estimation.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andreasik.efipoker.estimation.room.RoomEntity;
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

@DisplayName("TaskController Integration")
class TaskControllerIntegrationTest extends BaseComponentTest {

  private ProjectEntity project;
  private RoomEntity room;

  @BeforeEach
  void setUp() {
    project = projectRepository.save(Fixtures.projectEntity());
    room = roomRepository.save(Fixtures.roomEntity(project));
  }

  @Nested
  @DisplayName("POST /api/v1/rooms/{id}/tasks")
  class CreateTask {

    @Test
    void should_create_task_201() throws Exception {
      String body = "{\"title\":\"Implement login\",\"sortOrder\":0}";

      mockMvc
          .perform(
              post("/api/v1/rooms/{id}/tasks", room.getId())
                  .header("X-Admin-Code", project.getAdminCode())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.title").value("Implement login"))
          .andExpect(jsonPath("$.sortOrder").value(0));
    }

    @Test
    void should_reject_wrong_admin_code_403() throws Exception {
      String body = "{\"title\":\"Some task\",\"sortOrder\":0}";

      mockMvc
          .perform(
              post("/api/v1/rooms/{id}/tasks", room.getId())
                  .header("X-Admin-Code", "wrong-code")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_reject_blank_title_400() throws Exception {
      String body = "{\"title\":\"\",\"sortOrder\":0}";

      mockMvc
          .perform(
              post("/api/v1/rooms/{id}/tasks", room.getId())
                  .header("X-Admin-Code", project.getAdminCode())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("POST /api/v1/rooms/{id}/tasks/import")
  class ImportTasks {

    @Test
    void should_import_tasks_201() throws Exception {
      String body = "{\"titles\":[\"Task A\",\"Task B\",\"Task C\"]}";

      String json =
          mockMvc
              .perform(
                  post("/api/v1/rooms/{id}/tasks/import", room.getId())
                      .header("X-Admin-Code", project.getAdminCode())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(body))
              .andExpect(status().isCreated())
              .andReturn()
              .getResponse()
              .getContentAsString();

      int count = JsonPath.<Integer>read(json, "$.length()");
      assertAll(
          () -> assertThat(count).isEqualTo(3),
          () -> assertThat(JsonPath.<String>read(json, "$[0].title")).isEqualTo("Task A"),
          () -> assertThat(JsonPath.<Integer>read(json, "$[0].sortOrder")).isZero(),
          () -> assertThat(JsonPath.<Integer>read(json, "$[2].sortOrder")).isEqualTo(2));
    }

    @Test
    void should_reject_wrong_admin_code_403() throws Exception {
      String body = "{\"titles\":[\"Task A\"]}";

      mockMvc
          .perform(
              post("/api/v1/rooms/{id}/tasks/import", room.getId())
                  .header("X-Admin-Code", "wrong-code")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("PATCH /api/v1/tasks/{id}")
  class UpdateTask {

    private TaskEntity task;

    @BeforeEach
    void setUp() {
      task = taskRepository.save(Fixtures.taskEntity(room));
    }

    @Test
    void should_update_title_200() throws Exception {
      String body = "{\"title\":\"Updated title\"}";

      mockMvc
          .perform(
              patch("/api/v1/tasks/{id}", task.getId())
                  .header("X-Admin-Code", project.getAdminCode())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.title").value("Updated title"));
    }

    @Test
    void should_reject_wrong_admin_code_403() throws Exception {
      String body = "{\"title\":\"Updated\"}";

      mockMvc
          .perform(
              patch("/api/v1/tasks/{id}", task.getId())
                  .header("X-Admin-Code", "wrong-code")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_return_404_for_nonexistent_task() throws Exception {
      String body = "{\"title\":\"Updated\"}";

      mockMvc
          .perform(
              patch("/api/v1/tasks/{id}", UUID.randomUUID())
                  .header("X-Admin-Code", project.getAdminCode())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isNotFound());
    }

    @Test
    void should_not_expose_internal_id_in_404_response() throws Exception {
      UUID randomId = UUID.randomUUID();
      String body = "{\"title\":\"Updated\"}";

      String response =
          mockMvc
              .perform(
                  patch("/api/v1/tasks/{id}", randomId)
                      .header("X-Admin-Code", project.getAdminCode())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(body))
              .andExpect(status().isNotFound())
              .andExpect(jsonPath("$.detail").value("Task not found"))
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertThat(response).doesNotContain("with id:");
    }
  }

  @Nested
  @DisplayName("PUT /api/v1/tasks/{id}/final-estimate")
  class SetFinalEstimate {

    private RoomEntity revealedRoom;
    private TaskEntity task;

    @BeforeEach
    void setUp() {
      revealedRoom = roomRepository.save(Fixtures.revealedRoomEntity(project));
      task = taskRepository.save(Fixtures.taskEntity(revealedRoom));
    }

    @Test
    void should_set_final_estimate_200() throws Exception {
      String body = "{\"storyPoints\":\"8\"}";

      mockMvc
          .perform(
              put("/api/v1/tasks/{id}/final-estimate", task.getId())
                  .header("X-Admin-Code", project.getAdminCode())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.finalEstimate").value("8"));

      TaskEntity updated = taskRepository.findById(task.getId()).orElseThrow();
      assertThat(updated.getFinalEstimate()).isEqualTo("8");
    }

    @Test
    void should_overwrite_final_estimate_200() throws Exception {
      // Arrange
      task.setFinalEstimate("5");
      taskRepository.save(task);

      // Act
      String body = "{\"storyPoints\":\"13\"}";
      mockMvc
          .perform(
              put("/api/v1/tasks/{id}/final-estimate", task.getId())
                  .header("X-Admin-Code", project.getAdminCode())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.finalEstimate").value("13"));
    }

    @Test
    void should_reject_not_revealed_403() throws Exception {
      // Arrange - use the OPEN room from parent setUp
      TaskEntity openTask = taskRepository.save(Fixtures.taskEntity(room));

      // Act
      String body = "{\"storyPoints\":\"5\"}";
      mockMvc
          .perform(
              put("/api/v1/tasks/{id}/final-estimate", openTask.getId())
                  .header("X-Admin-Code", project.getAdminCode())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_reject_wrong_admin_code_403() throws Exception {
      String body = "{\"storyPoints\":\"5\"}";

      mockMvc
          .perform(
              put("/api/v1/tasks/{id}/final-estimate", task.getId())
                  .header("X-Admin-Code", "wrong-code")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_reject_invalid_story_points_400() throws Exception {
      String body = "{\"storyPoints\":\"99\"}";

      mockMvc
          .perform(
              put("/api/v1/tasks/{id}/final-estimate", task.getId())
                  .header("X-Admin-Code", project.getAdminCode())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_404_for_nonexistent_task() throws Exception {
      String body = "{\"storyPoints\":\"5\"}";

      mockMvc
          .perform(
              put("/api/v1/tasks/{id}/final-estimate", UUID.randomUUID())
                  .header("X-Admin-Code", project.getAdminCode())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("DELETE /api/v1/tasks/{id}")
  class DeleteTask {

    private TaskEntity task;

    @BeforeEach
    void setUp() {
      task = taskRepository.save(Fixtures.taskEntity(room));
    }

    @Test
    void should_delete_task_204() throws Exception {
      mockMvc
          .perform(
              delete("/api/v1/tasks/{id}", task.getId())
                  .header("X-Admin-Code", project.getAdminCode()))
          .andExpect(status().isNoContent());

      assertThat(taskRepository.findById(task.getId())).isEmpty();
    }

    @Test
    void should_reject_wrong_admin_code_403() throws Exception {
      mockMvc
          .perform(delete("/api/v1/tasks/{id}", task.getId()).header("X-Admin-Code", "wrong-code"))
          .andExpect(status().isForbidden());
    }
  }
}
