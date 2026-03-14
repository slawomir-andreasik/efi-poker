package com.andreasik.efipoker.estimation.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.BDDMockito.given;

import com.andreasik.efipoker.api.model.RoomAnalyticsResponse;
import com.andreasik.efipoker.estimation.estimate.Estimate;
import com.andreasik.efipoker.estimation.estimate.EstimateService;
import com.andreasik.efipoker.estimation.task.Task;
import com.andreasik.efipoker.estimation.task.TaskService;
import com.andreasik.efipoker.participant.Participant;
import com.andreasik.efipoker.participant.ParticipantService;
import com.andreasik.efipoker.project.Project;
import com.andreasik.efipoker.project.ProjectService;
import com.andreasik.efipoker.shared.exception.UnauthorizedException;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("AnalyticsService")
class AnalyticsServiceTest extends BaseUnitTest {

  @Mock private RoomService roomService;
  @Mock private EstimateService estimateService;
  @Mock private ProjectService projectService;
  @Mock private ParticipantService participantService;
  @Mock private TaskService taskService;
  @InjectMocks private AnalyticsService analyticsService;

  @Nested
  @DisplayName("computeRoomAnalytics")
  class ComputeRoomAnalytics {

    @Test
    void should_throw_unauthorized_when_room_not_revealed() {
      // Arrange
      UUID roomId = UUID.randomUUID();
      UUID projectId = UUID.randomUUID();
      Project project = Project.builder().id(projectId).name("P").slug("p").build();
      Room room =
          Room.builder()
              .id(roomId)
              .project(project)
              .slug("ABC-123")
              .title("Sprint 1")
              .status(RoomStatus.OPEN.name())
              .build();
      given(roomService.getRoom(roomId)).willReturn(room);

      // Act & Assert
      assertThatThrownBy(() -> analyticsService.computeRoomAnalytics(roomId))
          .isInstanceOf(UnauthorizedException.class)
          .hasMessageContaining(roomId.toString());
    }

    @Test
    void should_compute_analytics_for_revealed_room() {
      // Arrange
      UUID roomId = UUID.randomUUID();
      UUID projectId = UUID.randomUUID();
      UUID taskId1 = UUID.randomUUID();
      UUID taskId2 = UUID.randomUUID();
      UUID participantId1 = UUID.randomUUID();
      UUID participantId2 = UUID.randomUUID();

      Project project = Project.builder().id(projectId).name("Project").slug("proj").build();
      Room room =
          Room.builder()
              .id(roomId)
              .project(project)
              .slug("ABC-DEF")
              .title("Sprint 1")
              .status(RoomStatus.REVEALED.name())
              .build();

      Task task1 = Task.builder().id(taskId1).title("Login").room(room).build();
      Task task2 = Task.builder().id(taskId2).title("Logout").room(room).build();

      Participant p1 =
          Participant.builder().id(participantId1).nickname("Alice").project(project).build();
      Participant p2 =
          Participant.builder().id(participantId2).nickname("Bob").project(project).build();

      Estimate e1 =
          Estimate.builder()
              .id(UUID.randomUUID())
              .task(task1)
              .participant(p1)
              .storyPoints("5")
              .build();
      Estimate e2 =
          Estimate.builder()
              .id(UUID.randomUUID())
              .task(task1)
              .participant(p2)
              .storyPoints("5")
              .build();
      Estimate e3 =
          Estimate.builder()
              .id(UUID.randomUUID())
              .task(task2)
              .participant(p1)
              .storyPoints("3")
              .build();
      Estimate e4 =
          Estimate.builder()
              .id(UUID.randomUUID())
              .task(task2)
              .participant(p2)
              .storyPoints("8")
              .build();

      given(roomService.getRoom(roomId)).willReturn(room);
      given(taskService.listByRoom(roomId)).willReturn(List.of(task1, task2));
      given(participantService.listParticipants(projectId)).willReturn(List.of(p1, p2));
      given(estimateService.getEstimatesByTaskIds(List.of(taskId1, taskId2)))
          .willReturn(Map.of(taskId1, List.of(e1, e2), taskId2, List.of(e3, e4)));

      // Act
      RoomAnalyticsResponse result = analyticsService.computeRoomAnalytics(roomId);

      // Assert
      assertThat(result.getRoomId()).isEqualTo(roomId);
      assertThat(result.getTitle()).isEqualTo("Sprint 1");
      assertThat(result.getTaskAnalytics()).hasSize(2);
      assertThat(result.getSummary().getTotalTasks()).isEqualTo(2);
      assertThat(result.getSummary().getTotalParticipants()).isEqualTo(2);
      assertThat(result.getSummary().getParticipationRate()).isCloseTo(100.0, within(0.1));
      // task1 has consensus (both voted 5)
      assertThat(result.getSummary().getConsensusCount()).isEqualTo(1);
    }

    @Test
    void should_exclude_non_numeric_votes_from_calculations() {
      // Arrange
      UUID roomId = UUID.randomUUID();
      UUID projectId = UUID.randomUUID();
      UUID taskId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();

      Project project = Project.builder().id(projectId).name("P").slug("p").build();
      Room room =
          Room.builder()
              .id(roomId)
              .project(project)
              .slug("XYZ-ABC")
              .title("Room")
              .status(RoomStatus.REVEALED.name())
              .build();

      Task task = Task.builder().id(taskId).title("Task").room(room).build();
      Participant participant =
          Participant.builder().id(participantId).nickname("Alice").project(project).build();

      Estimate e1 =
          Estimate.builder()
              .id(UUID.randomUUID())
              .task(task)
              .participant(participant)
              .storyPoints("?")
              .build();

      given(roomService.getRoom(roomId)).willReturn(room);
      given(taskService.listByRoom(roomId)).willReturn(List.of(task));
      given(participantService.listParticipants(projectId)).willReturn(List.of(participant));
      given(estimateService.getEstimatesByTaskIds(List.of(taskId)))
          .willReturn(Map.of(taskId, List.of(e1)));

      // Act
      RoomAnalyticsResponse result = analyticsService.computeRoomAnalytics(roomId);

      // Assert - no consensus since "?" is non-numeric and < 2 numeric votes
      assertThat(result.getSummary().getConsensusCount()).isEqualTo(0);
      assertThat(result.getTaskAnalytics().get(0).getAveragePoints()).isNull();
      assertThat(result.getTaskAnalytics().get(0).getSpread()).isNull();
    }

    @Test
    void should_detect_consensus_when_all_numeric_votes_same() {
      // Arrange
      UUID roomId = UUID.randomUUID();
      UUID projectId = UUID.randomUUID();
      UUID taskId = UUID.randomUUID();
      UUID p1Id = UUID.randomUUID();
      UUID p2Id = UUID.randomUUID();

      Project project = Project.builder().id(projectId).name("P").slug("p").build();
      Room room =
          Room.builder()
              .id(roomId)
              .project(project)
              .slug("CON-SEN")
              .title("Room")
              .status(RoomStatus.REVEALED.name())
              .build();

      Task task = Task.builder().id(taskId).title("Task").room(room).build();
      Participant participant1 =
          Participant.builder().id(p1Id).nickname("Alice").project(project).build();
      Participant participant2 =
          Participant.builder().id(p2Id).nickname("Bob").project(project).build();

      Estimate e1 =
          Estimate.builder()
              .id(UUID.randomUUID())
              .task(task)
              .participant(participant1)
              .storyPoints("8")
              .build();
      Estimate e2 =
          Estimate.builder()
              .id(UUID.randomUUID())
              .task(task)
              .participant(participant2)
              .storyPoints("8")
              .build();

      given(roomService.getRoom(roomId)).willReturn(room);
      given(taskService.listByRoom(roomId)).willReturn(List.of(task));
      given(participantService.listParticipants(projectId))
          .willReturn(List.of(participant1, participant2));
      given(estimateService.getEstimatesByTaskIds(List.of(taskId)))
          .willReturn(Map.of(taskId, List.of(e1, e2)));

      // Act
      RoomAnalyticsResponse result = analyticsService.computeRoomAnalytics(roomId);

      // Assert
      assertThat(result.getSummary().getConsensusCount()).isEqualTo(1);
      assertThat(result.getTaskAnalytics().get(0).getSpread()).isCloseTo(0.0, within(0.001));
    }

    @Test
    void should_return_empty_analytics_for_room_with_no_tasks() {
      // Arrange
      UUID roomId = UUID.randomUUID();
      UUID projectId = UUID.randomUUID();

      Project project = Project.builder().id(projectId).name("P").slug("p").build();
      Room room =
          Room.builder()
              .id(roomId)
              .project(project)
              .slug("EMP-TY1")
              .title("Empty Room")
              .status(RoomStatus.CLOSED.name())
              .build();

      given(roomService.getRoom(roomId)).willReturn(room);
      given(taskService.listByRoom(roomId)).willReturn(List.of());
      given(participantService.listParticipants(projectId)).willReturn(List.of());
      given(estimateService.getEstimatesByTaskIds(List.of())).willReturn(Map.of());

      // Act
      RoomAnalyticsResponse result = analyticsService.computeRoomAnalytics(roomId);

      // Assert
      assertThat(result.getTaskAnalytics()).isEmpty();
      assertThat(result.getSummary().getTotalTasks()).isEqualTo(0);
      assertThat(result.getSummary().getConsensusCount()).isEqualTo(0);
      assertThat(result.getSummary().getTotalStoryPoints()).isCloseTo(0.0, within(0.001));
    }
  }
}
