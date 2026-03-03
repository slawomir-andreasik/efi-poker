package com.andreasik.efipoker.estimation.room;

import com.andreasik.efipoker.api.RoomsApi;
import com.andreasik.efipoker.api.model.CreateRoomRequest;
import com.andreasik.efipoker.api.model.EstimateResponse;
import com.andreasik.efipoker.api.model.LiveParticipantStatus;
import com.andreasik.efipoker.api.model.LiveRoomResults;
import com.andreasik.efipoker.api.model.LiveRoomStateResponse;
import com.andreasik.efipoker.api.model.NewRoundRequest;
import com.andreasik.efipoker.api.model.RoomAdminResponse;
import com.andreasik.efipoker.api.model.RoomDetailResponse;
import com.andreasik.efipoker.api.model.RoomProgressResponse;
import com.andreasik.efipoker.api.model.RoomResponse;
import com.andreasik.efipoker.api.model.RoomResultsResponse;
import com.andreasik.efipoker.api.model.RoomSlugResponse;
import com.andreasik.efipoker.api.model.RoundHistoryEntry;
import com.andreasik.efipoker.api.model.TaskProgressResponse;
import com.andreasik.efipoker.api.model.TaskResultResponse;
import com.andreasik.efipoker.api.model.TaskWithAllEstimatesResponse;
import com.andreasik.efipoker.api.model.TaskWithEstimateResponse;
import com.andreasik.efipoker.api.model.UpdateRoomRequest;
import com.andreasik.efipoker.estimation.EstimationStats;
import com.andreasik.efipoker.estimation.estimate.Estimate;
import com.andreasik.efipoker.estimation.estimate.EstimateMapper;
import com.andreasik.efipoker.estimation.estimate.EstimateService;
import com.andreasik.efipoker.estimation.task.Task;
import com.andreasik.efipoker.estimation.task.TaskService;
import com.andreasik.efipoker.participant.Participant;
import com.andreasik.efipoker.participant.ParticipantMapper;
import com.andreasik.efipoker.participant.ParticipantService;
import com.andreasik.efipoker.project.Project;
import com.andreasik.efipoker.project.ProjectService;
import com.andreasik.efipoker.shared.exception.UnauthorizedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RoomController implements RoomsApi {

  private final RoomService roomService;
  private final ProjectService projectService;
  private final EstimateService estimateService;
  private final TaskService taskService;
  private final ParticipantService participantService;
  private final RoomMapper roomMapper;
  private final EstimateMapper estimateMapper;
  private final ParticipantMapper participantMapper;
  private final RoundHistoryService roundHistoryService;

  @Override
  public ResponseEntity<RoomResponse> createRoom(
      String slug, CreateRoomRequest createRoomRequest, String xAdminCode) {
    log.debug("POST /projects/{}/rooms type={}", slug, createRoomRequest.getRoomType());
    Project project = projectService.validateAdminCode(slug, xAdminCode);

    String roomType = createRoomRequest.getRoomType().getValue();
    Room room =
        roomService.createRoom(
            project.id(),
            createRoomRequest.getTitle(),
            createRoomRequest.getDescription(),
            roomType,
            createRoomRequest.getDeadline());
    return ResponseEntity.status(HttpStatus.CREATED).body(roomMapper.toResponse(room));
  }

  @Override
  public ResponseEntity<List<RoomResponse>> listRooms(String slug, UUID xParticipantId) {
    log.debug("GET /projects/{}/rooms participantId={}", slug, xParticipantId);
    Project project = projectService.getProjectBySlug(slug);
    List<Room> rooms = roomService.listByProject(project.id());
    if (xParticipantId != null) {
      Participant participant = participantService.getParticipant(project.id(), xParticipantId);
      Set<UUID> invitedRoomIds = participant.invitedRoomIds();
      if (invitedRoomIds != null && !invitedRoomIds.isEmpty()) {
        rooms = rooms.stream().filter(r -> invitedRoomIds.contains(r.id())).toList();
      }
    }
    return ResponseEntity.ok(roomMapper.toResponseList(rooms));
  }

  @Override
  public ResponseEntity<RoomDetailResponse> getRoom(UUID roomId, UUID xParticipantId) {
    log.debug("GET /rooms/{}", roomId);
    Room room = roomService.getRoom(roomId);
    return ResponseEntity.ok(buildDetailResponse(room, xParticipantId));
  }

  @Override
  public ResponseEntity<RoomSlugResponse> getRoomBySlug(String roomSlug) {
    log.debug("GET /rooms/by-slug/{}", roomSlug);
    Room room = roomService.getRoomBySlug(roomSlug);
    return ResponseEntity.ok(roomMapper.toSlugResponse(room));
  }

  @Override
  public ResponseEntity<RoomAdminResponse> getRoomAdmin(UUID roomId, String xAdminCode) {
    log.debug("GET /rooms/{}/admin", roomId);
    Room room = roomService.getRoom(roomId);
    projectService.validateAdminCodeForProject(room.project().id(), xAdminCode);
    List<Task> tasks = taskService.listByRoom(roomId);
    List<Participant> participants = participantService.listParticipants(room.project().id());

    List<UUID> taskIds = tasks.stream().map(Task::id).toList();
    Map<UUID, List<Estimate>> estimatesByTask = estimateService.getEstimatesByTaskIds(taskIds);

    List<TaskWithAllEstimatesResponse> taskResponses = new ArrayList<>();
    for (Task task : tasks) {
      List<Estimate> estimates = estimatesByTask.getOrDefault(task.id(), List.of());
      List<EstimateResponse> estimateResponses =
          estimates.stream().map(estimateMapper::toResponse).collect(Collectors.toList());

      taskResponses.add(
          new TaskWithAllEstimatesResponse()
              .id(task.id())
              .title(task.title())
              .description(task.description())
              .sortOrder(task.sortOrder())
              .finalEstimate(task.finalEstimate())
              .revealed(task.revealed())
              .active(task.active())
              .estimates(estimateResponses));
    }

    RoomAdminResponse response =
        new RoomAdminResponse()
            .id(room.id())
            .slug(room.slug())
            .title(room.title())
            .description(room.description())
            .roomType(roomMapper.mapRoomType(room.roomType()))
            .deadline(room.deadline())
            .status(roomMapper.mapStatus(room.status()))
            .topic(room.topic())
            .roundNumber(room.roundNumber())
            .tasks(taskResponses)
            .participants(participantMapper.toResponseList(participants));

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<RoomResponse> updateRoom(
      UUID roomId, UpdateRoomRequest updateRoomRequest, String xAdminCode) {
    log.debug("PATCH /rooms/{}", roomId);
    Room room = roomService.getRoom(roomId);
    projectService.validateAdminCodeForProject(room.project().id(), xAdminCode);

    Room updated =
        roomService.updateRoom(
            roomId,
            updateRoomRequest.getTitle(),
            updateRoomRequest.getDescription(),
            updateRoomRequest.getDeadline(),
            updateRoomRequest.getTopic());
    return ResponseEntity.ok(roomMapper.toResponse(updated));
  }

  @Override
  public ResponseEntity<RoomDetailResponse> revealEstimates(UUID roomId, String xAdminCode) {
    log.debug("POST /rooms/{}/reveal", roomId);
    Room room = roomService.getRoom(roomId);
    projectService.validateAdminCodeForProject(room.project().id(), xAdminCode);
    Room revealed = roomService.revealRoom(roomId);
    return ResponseEntity.ok(buildDetailResponse(revealed, null));
  }

  @Override
  public ResponseEntity<RoomDetailResponse> reopenRoom(UUID roomId, String xAdminCode) {
    log.debug("POST /rooms/{}/reopen", roomId);
    Room room = roomService.getRoom(roomId);
    projectService.validateAdminCodeForProject(room.project().id(), xAdminCode);
    Room reopened = roomService.reopenRoom(roomId);
    return ResponseEntity.ok(buildDetailResponse(reopened, null));
  }

  @Override
  public ResponseEntity<RoomProgressResponse> getRoomProgress(UUID roomId) {
    log.debug("GET /rooms/{}/progress", roomId);
    Room room = roomService.getRoom(roomId);
    List<Task> tasks = taskService.listByRoom(roomId);
    long totalParticipants = participantService.countByProject(room.project().id());

    List<UUID> taskIds = tasks.stream().map(Task::id).toList();
    Map<UUID, List<Estimate>> estimatesByTask = estimateService.getEstimatesByTaskIds(taskIds);

    List<TaskProgressResponse> taskProgress = new ArrayList<>();
    for (Task task : tasks) {
      List<Estimate> estimates = estimatesByTask.getOrDefault(task.id(), List.of());
      long questionVotesCount = estimates.stream().filter(e -> "?".equals(e.storyPoints())).count();

      taskProgress.add(
          new TaskProgressResponse()
              .taskId(task.id())
              .title(task.title())
              .votedCount(estimates.size())
              .questionVotesCount((int) questionVotesCount)
              .totalParticipants((int) totalParticipants));
    }

    RoomProgressResponse response =
        new RoomProgressResponse()
            .roomId(roomId)
            .slug(room.slug())
            .status(roomMapper.mapStatus(room.status()))
            .totalParticipants((int) totalParticipants)
            .tasks(taskProgress);

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<RoomResultsResponse> getRoomResults(UUID roomId) {
    log.debug("GET /rooms/{}/results", roomId);
    Room room = roomService.getRoom(roomId);
    if (!RoomService.isRevealedStatus(room.status())) {
      throw new UnauthorizedException("Results are not available until estimates are revealed");
    }

    List<Task> tasks =
        taskService.listByRoom(roomId).stream()
            .filter(t -> !RoomService.PHANTOM_TASK_TITLE.equals(t.title()))
            .toList();

    List<UUID> taskIds = tasks.stream().map(Task::id).toList();
    Map<UUID, List<Estimate>> estimatesByTask = estimateService.getEstimatesByTaskIds(taskIds);

    List<TaskResultResponse> taskResults = new ArrayList<>();
    for (Task task : tasks) {
      List<Estimate> estimates = estimatesByTask.getOrDefault(task.id(), List.of());
      List<EstimateResponse> estimateResponses =
          estimates.stream().map(estimateMapper::toResponse).collect(Collectors.toList());

      List<String> points = estimates.stream().map(Estimate::storyPoints).toList();
      taskResults.add(
          new TaskResultResponse()
              .taskId(task.id())
              .title(task.title())
              .estimates(estimateResponses)
              .averagePoints(EstimationStats.computeAverage(points))
              .medianPoints(EstimationStats.computeMedian(points))
              .finalEstimate(task.finalEstimate()));
    }

    RoomResultsResponse response =
        new RoomResultsResponse()
            .roomId(roomId)
            .slug(room.slug())
            .title(room.title())
            .status(roomMapper.mapStatus(room.status()))
            .tasks(taskResults);

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<String> exportResults(UUID roomId, String xAdminCode) {
    log.debug("GET /rooms/{}/export", roomId);
    Room room = roomService.getRoom(roomId);
    projectService.validateAdminCodeForProject(room.project().id(), xAdminCode);

    if (!RoomService.isRevealedStatus(room.status())) {
      throw new UnauthorizedException("Results are not available until estimates are revealed");
    }
    List<Task> tasks =
        taskService.listByRoom(roomId).stream()
            .filter(t -> !RoomService.PHANTOM_TASK_TITLE.equals(t.title()))
            .toList();
    List<Participant> participants = participantService.listParticipants(room.project().id());

    List<UUID> taskIds = tasks.stream().map(Task::id).toList();
    Map<UUID, List<Estimate>> estimatesByTask = estimateService.getEstimatesByTaskIds(taskIds);

    StringBuilder csv = new StringBuilder();
    csv.append("Task");
    for (Participant p : participants) {
      csv.append(",").append(escapeCsv(p.nickname()));
    }
    csv.append(",Final SP");
    csv.append("\n");

    for (Task task : tasks) {
      csv.append(escapeCsv(task.title()));
      List<Estimate> estimates = estimatesByTask.getOrDefault(task.id(), List.of());
      for (Participant p : participants) {
        String sp =
            estimates.stream()
                .filter(e -> e.participant().id().equals(p.id()))
                .findFirst()
                .map(Estimate::storyPoints)
                .orElse("");
        csv.append(",").append(sp);
      }
      csv.append(",").append(task.finalEstimate() != null ? task.finalEstimate() : "");
      csv.append("\n");
    }

    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=\"room-" + roomId + ".csv\"")
        .body(csv.toString());
  }

  @Override
  public ResponseEntity<LiveRoomStateResponse> getLiveRoomState(UUID roomId, UUID xParticipantId) {
    log.debug("GET /rooms/{}/live", roomId);
    Room room = roomService.getRoom(roomId);
    if (!"LIVE".equals(room.roomType())) {
      throw new IllegalStateException("getLiveRoomState is only available for LIVE rooms");
    }
    return ResponseEntity.ok(buildLiveRoomStateResponse(room, xParticipantId));
  }

  @Override
  public ResponseEntity<LiveRoomStateResponse> newRound(
      UUID roomId, String xAdminCode, NewRoundRequest newRoundRequest) {
    log.debug("POST /rooms/{}/new-round", roomId);
    Room room = roomService.getRoom(roomId);
    projectService.validateAdminCodeForProject(room.project().id(), xAdminCode);
    String topic = newRoundRequest != null ? newRoundRequest.getTopic() : null;
    Room updated = roomService.newRound(roomId, topic);
    return ResponseEntity.ok(buildLiveRoomStateResponse(updated, null));
  }

  @Override
  public ResponseEntity<List<RoundHistoryEntry>> getRoundHistory(UUID roomId) {
    log.debug("GET /rooms/{}/history", roomId);
    Room room = roomService.getRoom(roomId);
    if (!"LIVE".equals(room.roomType())) {
      throw new IllegalStateException("getRoundHistory is only available for LIVE rooms");
    }
    return ResponseEntity.ok(roundHistoryService.getHistory(roomId));
  }

  private LiveRoomStateResponse buildLiveRoomStateResponse(Room room, UUID participantId) {
    Task phantom = taskService.getPhantomTask(room.id());
    List<Participant> participants = participantService.listParticipants(room.project().id());
    boolean roomRevealed = RoomService.isRevealedStatus(room.status());

    List<Estimate> estimates = estimateService.getEstimatesForTask(phantom.id());
    Set<UUID> votedParticipantIds =
        estimates.stream().map(e -> e.participant().id()).collect(Collectors.toSet());

    EstimateResponse myEstimate = null;
    if (participantId != null) {
      myEstimate =
          estimates.stream()
              .filter(e -> e.participant().id().equals(participantId))
              .findFirst()
              .map(estimateMapper::toResponse)
              .orElse(null);
    }

    List<LiveParticipantStatus> participantStatuses = new ArrayList<>();
    for (Participant participant : participants) {
      participantStatuses.add(
          new LiveParticipantStatus()
              .participantId(participant.id())
              .nickname(participant.nickname())
              .hasVoted(votedParticipantIds.contains(participant.id())));
    }

    LiveRoomResults results = null;
    if (roomRevealed) {
      List<EstimateResponse> estimateResponses =
          estimates.stream().map(estimateMapper::toResponse).collect(Collectors.toList());
      List<String> points = estimates.stream().map(Estimate::storyPoints).toList();
      results =
          new LiveRoomResults()
              .estimates(estimateResponses)
              .averagePoints(EstimationStats.computeAverage(points))
              .medianPoints(EstimationStats.computeMedian(points));
    }

    long questionVotesCount = estimates.stream().filter(e -> "?".equals(e.storyPoints())).count();

    return new LiveRoomStateResponse()
        .roomId(room.id())
        .slug(room.slug())
        .title(room.title())
        .topic(room.topic())
        .status(roomMapper.mapStatus(room.status()))
        .roundNumber(room.roundNumber())
        .questionVotesCount((int) questionVotesCount)
        .taskId(phantom.id())
        .myEstimate(myEstimate)
        .participants(participantStatuses)
        .results(results);
  }

  private RoomDetailResponse buildDetailResponse(Room room, UUID participantId) {
    List<Task> tasks = taskService.listByRoom(room.id());
    long totalParticipants = participantService.countByProject(room.project().id());
    boolean roomRevealed = RoomService.isRevealedStatus(room.status());

    List<UUID> taskIds = tasks.stream().map(Task::id).toList();
    Map<UUID, List<Estimate>> estimatesByTask = estimateService.getEstimatesByTaskIds(taskIds);

    List<TaskWithEstimateResponse> taskResponses = new ArrayList<>();
    for (Task task : tasks) {
      List<Estimate> estimates = estimatesByTask.getOrDefault(task.id(), List.of());

      EstimateResponse myEstimate = null;
      if (participantId != null) {
        myEstimate =
            estimates.stream()
                .filter(e -> e.participant().id().equals(participantId))
                .findFirst()
                .map(estimateMapper::toResponse)
                .orElse(null);
      }

      List<EstimateResponse> allEstimates = null;
      if (roomRevealed) {
        allEstimates =
            estimates.stream().map(estimateMapper::toResponse).collect(Collectors.toList());
      }

      long questionVotesCount = estimates.stream().filter(e -> "?".equals(e.storyPoints())).count();

      TaskWithEstimateResponse taskResponse =
          new TaskWithEstimateResponse()
              .id(task.id())
              .title(task.title())
              .description(task.description())
              .sortOrder(task.sortOrder())
              .myEstimate(myEstimate)
              .allEstimates(allEstimates)
              .votedCount(estimates.size())
              .totalParticipants((int) totalParticipants)
              .finalEstimate(task.finalEstimate())
              .revealed(task.revealed())
              .active(task.active());

      List<String> points = estimates.stream().map(Estimate::storyPoints).toList();
      taskResponse
          .averagePoints(EstimationStats.computeAverage(points))
          .medianPoints(EstimationStats.computeMedian(points));

      taskResponses.add(taskResponse);
    }

    return new RoomDetailResponse()
        .id(room.id())
        .slug(room.slug())
        .title(room.title())
        .description(room.description())
        .roomType(roomMapper.mapRoomType(room.roomType()))
        .deadline(room.deadline())
        .status(roomMapper.mapStatus(room.status()))
        .topic(room.topic())
        .roundNumber(room.roundNumber())
        .tasks(taskResponses);
  }

  private String escapeCsv(String value) {
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
