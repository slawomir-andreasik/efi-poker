package com.andreasik.efipoker.estimation.estimate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andreasik.efipoker.auth.UserEntity;
import com.andreasik.efipoker.estimation.room.RoomEntity;
import com.andreasik.efipoker.estimation.task.TaskEntity;
import com.andreasik.efipoker.participant.ParticipantEntity;
import com.andreasik.efipoker.project.ProjectEntity;
import com.andreasik.efipoker.shared.test.BaseComponentTest;
import com.andreasik.efipoker.shared.test.Fixtures;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("EstimateController Integration")
class EstimateControllerIntegrationTest extends BaseComponentTest {

  private ProjectEntity project;
  private TaskEntity task;
  private ParticipantEntity participant;
  private String participantJwt;

  @BeforeEach
  void setUp() {
    project = projectRepository.save(Fixtures.projectEntity());
    RoomEntity room = roomRepository.save(Fixtures.roomEntity(project));
    task = taskRepository.save(Fixtures.taskEntity(room));
    participant = participantRepository.save(Fixtures.participantEntity(project, "Alice"));
    participantJwt = testJwt.guestParticipantJwt(project, participant);
  }

  @Nested
  @DisplayName("POST /api/v1/tasks/{id}/estimates")
  class SubmitEstimate {

    @Test
    void should_submit_estimate_200() throws Exception {
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
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.storyPoints").value("5"));
    }

    @Test
    void should_update_estimate_on_resubmit() throws Exception {
      // language=JSON
      String body1 =
          """
          {"storyPoints":"5"}
          """;
      // language=JSON
      String body2 =
          """
          {"storyPoints":"8"}
          """;

      mockMvc
          .perform(
              post("/api/v1/tasks/{id}/estimates", task.getId())
                  .header("Authorization", "Bearer " + participantJwt)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body1))
          .andExpect(status().isOk());

      mockMvc
          .perform(
              post("/api/v1/tasks/{id}/estimates", task.getId())
                  .header("Authorization", "Bearer " + participantJwt)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body2))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.storyPoints").value("8"));
    }

    @Test
    void should_reject_missing_jwt_403() throws Exception {
      // language=JSON
      String body =
          """
          {"storyPoints":"5"}
          """;

      mockMvc
          .perform(
              post("/api/v1/tasks/{id}/estimates", task.getId())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_reject_invalid_story_points_400() throws Exception {
      // language=JSON
      String body =
          """
          {"storyPoints":"99"}
          """;

      mockMvc
          .perform(
              post("/api/v1/tasks/{id}/estimates", task.getId())
                  .header("Authorization", "Bearer " + participantJwt)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isBadRequest());
    }

    @Test
    void should_accept_zero_story_points() throws Exception {
      // language=JSON
      String body =
          """
          {"storyPoints":"0"}
          """;

      mockMvc
          .perform(
              post("/api/v1/tasks/{id}/estimates", task.getId())
                  .header("Authorization", "Bearer " + participantJwt)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.storyPoints").value("0"));
    }

    @Test
    void should_accept_half_story_points() throws Exception {
      // language=JSON
      String body =
          """
          {"storyPoints":"0.5"}
          """;

      mockMvc
          .perform(
              post("/api/v1/tasks/{id}/estimates", task.getId())
                  .header("Authorization", "Bearer " + participantJwt)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.storyPoints").value("0.5"));
    }

    @Test
    void should_accept_question_mark() throws Exception {
      // language=JSON
      String body =
          """
          {"storyPoints":"?"}
          """;

      mockMvc
          .perform(
              post("/api/v1/tasks/{id}/estimates", task.getId())
                  .header("Authorization", "Bearer " + participantJwt)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.storyPoints").value("?"));
    }
  }

  @Nested
  @DisplayName("DELETE /api/v1/tasks/{id}/estimates")
  class DeleteEstimate {

    @Test
    void should_delete_estimate_204() throws Exception {
      // Arrange - submit estimate first
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

      // Act
      mockMvc
          .perform(
              delete("/api/v1/tasks/{id}/estimates", task.getId())
                  .header("Authorization", "Bearer " + participantJwt))
          .andExpect(status().isNoContent());

      // Assert
      assertThat(estimateRepository.findByTaskAndParticipant(task.getId(), participant.getId()))
          .isEmpty();
    }

    @Test
    void should_return_204_even_without_existing_estimate() throws Exception {
      mockMvc
          .perform(
              delete("/api/v1/tasks/{id}/estimates", task.getId())
                  .header("Authorization", "Bearer " + participantJwt))
          .andExpect(status().isNoContent());
    }

    @Test
    void should_reject_missing_jwt_403() throws Exception {
      mockMvc
          .perform(delete("/api/v1/tasks/{id}/estimates", task.getId()))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("User JWT estimates")
  class UserJwtEstimate {

    private String userJwt;
    private ProjectEntity userProject;
    private TaskEntity userTask;

    @BeforeEach
    void setUp() throws Exception {
      userJwt = loginAsTestAdmin();
      UserEntity user =
          userRepository
              .findByUsername("testadmin")
              .orElseThrow(() -> new AssertionError("testadmin user not found"));
      userProject = projectRepository.save(Fixtures.projectEntity());
      RoomEntity room = roomRepository.save(Fixtures.roomEntity(userProject));
      userTask = taskRepository.save(Fixtures.taskEntity(room));
      participantRepository.save(Fixtures.participantEntity(userProject, "TestAdmin", user));
    }

    @Test
    void should_submit_estimate_with_user_jwt_200() throws Exception {
      // language=JSON
      String body =
          """
          {"storyPoints":"5"}
          """;

      mockMvc
          .perform(
              post("/api/v1/tasks/{id}/estimates", userTask.getId())
                  .header("Authorization", "Bearer " + userJwt)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.storyPoints").value("5"));
    }

    @Test
    void should_delete_estimate_with_user_jwt_204() throws Exception {
      // language=JSON
      String body =
          """
          {"storyPoints":"5"}
          """;
      mockMvc
          .perform(
              post("/api/v1/tasks/{id}/estimates", userTask.getId())
                  .header("Authorization", "Bearer " + userJwt)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk());

      mockMvc
          .perform(
              delete("/api/v1/tasks/{id}/estimates", userTask.getId())
                  .header("Authorization", "Bearer " + userJwt))
          .andExpect(status().isNoContent());
    }

    @Test
    void should_reject_user_jwt_without_participant_403() throws Exception {
      ProjectEntity otherProject = projectRepository.save(Fixtures.projectEntity());
      RoomEntity otherRoom = roomRepository.save(Fixtures.roomEntity(otherProject));
      TaskEntity otherTask = taskRepository.save(Fixtures.taskEntity(otherRoom));

      // language=JSON
      String body =
          """
          {"storyPoints":"5"}
          """;

      mockMvc
          .perform(
              post("/api/v1/tasks/{id}/estimates", otherTask.getId())
                  .header("Authorization", "Bearer " + userJwt)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Archived participant voting")
  class ArchivedParticipantEstimate {

    @Test
    void should_reject_estimate_from_archived_participant_403() throws Exception {
      ParticipantEntity archivedParticipant =
          participantRepository.save(Fixtures.participantEntity(project, "Archived"));
      archivedParticipant.setArchived(true);
      archivedParticipant.setArchivedAt(Instant.now());
      participantRepository.save(archivedParticipant);
      String archivedJwt = testJwt.guestParticipantJwt(project, archivedParticipant);

      // language=JSON
      String body =
          """
          {"storyPoints":"5"}
          """;

      mockMvc
          .perform(
              post("/api/v1/tasks/{id}/estimates", task.getId())
                  .header("Authorization", "Bearer " + archivedJwt)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isForbidden());
    }
  }
}
