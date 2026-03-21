package com.andreasik.efipoker.estimation.room;

import com.andreasik.efipoker.api.RoomsApi;
import com.andreasik.efipoker.api.model.AutoAssignedEstimate;
import com.andreasik.efipoker.api.model.CreateRoomRequest;
import com.andreasik.efipoker.api.model.EstimateResponse;
import com.andreasik.efipoker.api.model.FinishSessionResponse;
import com.andreasik.efipoker.api.model.LiveParticipantStatus;
import com.andreasik.efipoker.api.model.LiveRoomResults;
import com.andreasik.efipoker.api.model.LiveRoomStateResponse;
import com.andreasik.efipoker.api.model.NewRoundRequest;
import com.andreasik.efipoker.api.model.ParticipantProgressEntry;
import com.andreasik.efipoker.api.model.ParticipantProgressResponse;
import com.andreasik.efipoker.api.model.RoomAdminResponse;
import com.andreasik.efipoker.api.model.RoomDetailResponse;
import com.andreasik.efipoker.api.model.RoomProgressResponse;
import com.andreasik.efipoker.api.model.RoomResponse;
import com.andreasik.efipoker.api.model.RoomResultsResponse;
import com.andreasik.efipoker.api.model.RoomSlugResponse;
import com.andreasik.efipoker.api.model.RoundHistoryEntry;
import com.andreasik.efipoker.api.model.StoryPoints;
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
import com.andreasik.efipoker.shared.security.SecurityUtils;
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
  public ResponseEntity<RoomResponse> createRoom(String slug, CreateRoomRequest createRoomRequest) {
    log.debug("POST /projects/{}/rooms type={}", slug, createRoomRequest.getRoomType());
    Project project = projectService.validateAdminAccessBySlug(slug);

    RoomType roomType = RoomType.valueOf(createRoomRequest.getRoomType().getValue());
    boolean autoReveal =
        createRoomRequest.getAutoRevealOnDeadline() == null
            || createRoomRequest.getAutoRevealOnDeadline();
    boolean commentRequired = Boolean.TRUE.equals(createRoomRequest.getCommentRequired());
    Room room =
        roomService.createRoom(
            new CreateRoomCommand(
                project.id(),
                createRoomRequest.getTitle(),
                createRoomRequest.getDescription(),
                roomType,
                createRoomRequest.getDeadline(),
                autoReveal,
                createRoomRequest.getCommentTemplate(),
                commentRequired));
    return ResponseEntity.status(HttpStatus.CREATED).body(roomMapper.toResponse(room));
  }

  @Override
  public ResponseEntity<List<RoomResponse>> listRooms(String slug) {
    log.debug("GET /projects/{}/rooms", slug);
    Project project = projectService.getProjectBySlug(slug);
    List<Room> rooms = roomService.listByProject(project.id());
    UUID participantId = SecurityUtils.getCurrentParticipantId();
    if (participantId != null) {
      Participant participant = participantService.getParticipant(project.id(), participantId);
      Set<UUID> invitedRoomIds = participant.invitedRoomIds();
      if (invitedRoomIds != null && !invitedRoomIds.isEmpty()) {
        rooms = rooms.stream().filter(r -> invitedRoomIds.contains(r.id())).toList();
      }
    }
    return ResponseEntity.ok(roomMapper.toResponseList(rooms));
  }

  @Override
  public ResponseEntity<RoomDetailResponse> getRoom(UUID roomId) {
    log.debug("GET /rooms/{}", roomId);
    Room room = roomService.getRoom(roomId);
    UUID participantId = SecurityUtils.getCurrentParticipantId();
    return ResponseEntity.ok(buildDetailResponse(room, participantId));
  }

  @Override
  public ResponseEntity<RoomSlugResponse> getRoomBySlug(String roomSlug) {
    log.debug("GET /rooms/by-slug/{}", roomSlug);
    Room room = roomService.getRoomBySlug(roomSlug);
    return ResponseEntity.ok(roomMapper.toSlugResponse(room));
  }

  @Override
  public ResponseEntity<RoomAdminResponse> getRoomAdmin(UUID roomId) {
    log.debug("GET /rooms/{}/admin", roomId);
    Room room = roomService.validateAdminAndGetRoom(roomId);
    List<Task> tasks = taskService.listByRoom(roomId);
    List<Participant> participants = participantService.listParticipants(room.project().id());

    List<UUID> taskIds = tasks.stream().map(Task::id).toList();
    Map<UUID, List<Estimate>> estimatesByTask = estimateService.getEstimatesByTaskIds(taskIds);

    List<TaskWithAllEstimatesResponse> taskResponses = new ArrayList<>();
    for (Task task : tasks) {
      List<Estimate> estimates = estimatesByTask.getOrDefault(task.id(), List.of());
      List<EstimateResponse> estimateResponses =
          estimates.stream().map(estimateMapper::toResponse).toList();

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
            .autoRevealOnDeadline(room.autoRevealOnDeadline())
            .commentTemplate(room.commentTemplate())
            .commentRequired(room.commentRequired())
            .tasks(taskResponses)
            .participants(participantMapper.toResponseList(participants));

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<RoomResponse> updateRoom(UUID roomId, UpdateRoomRequest updateRoomRequest) {
    log.debug("PATCH /rooms/{}", roomId);
    roomService.validateAdminAndGetRoom(roomId);

    Room updated =
        roomService.updateRoom(
            new UpdateRoomCommand(
                roomId,
                updateRoomRequest.getTitle(),
                updateRoomRequest.getDescription(),
                updateRoomRequest.getDeadline(),
                updateRoomRequest.getTopic(),
                updateRoomRequest.getCommentTemplate(),
                updateRoomRequest.getCommentRequired(),
                updateRoomRequest.getAutoRevealOnDeadline()));
    return ResponseEntity.ok(roomMapper.toResponse(updated));
  }

  @Override
  public ResponseEntity<Void> deleteRoom(UUID roomId) {
    log.debug("DELETE /rooms/{}", roomId);
    Room room = roomService.validateAdminAndGetRoom(roomId);
    roomService.deleteRoom(room);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<RoomDetailResponse> revealEstimates(UUID roomId) {
    log.debug("POST /rooms/{}/reveal", roomId);
    roomService.validateAdminAndGetRoom(roomId);
    Room revealed = roomService.revealRoom(roomId);
    return ResponseEntity.ok(buildDetailResponse(revealed, null));
  }

  @Override
  public ResponseEntity<RoomDetailResponse> reopenRoom(UUID roomId) {
    log.debug("POST /rooms/{}/reopen", roomId);
    roomService.validateAdminAndGetRoom(roomId);
    Room reopened = roomService.reopenRoom(roomId);
    return ResponseEntity.ok(buildDetailResponse(reopened, null));
  }

  @Override
  public ResponseEntity<FinishSessionResponse> finishSession(UUID roomId, Boolean revealVotes) {
    log.debug("POST /rooms/{}/finish revealVotes={}", roomId, revealVotes);
    roomService.validateAdminAndGetRoom(roomId);
    boolean reveal = revealVotes == null || revealVotes;
    RoomService.FinishSessionResult result = roomService.finishSession(roomId, reveal);

    List<AutoAssignedEstimate> autoAssigned =
        result.autoAssigned().stream()
            .map(
                a ->
                    new AutoAssignedEstimate()
                        .taskId(a.taskId())
                        .taskTitle(a.taskTitle())
                        .finalEstimate(a.finalEstimate()))
            .toList();

    FinishSessionResponse response =
        new FinishSessionResponse()
            .status(roomMapper.mapStatus(result.room().status()))
            .autoAssignedEstimates(autoAssigned);

    return ResponseEntity.ok(response);
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
      List<String> points = estimates.stream().map(Estimate::storyPoints).toList();
      long questionVotesCount = EstimationStats.countQuestionVotes(points);
      long voted = estimates.stream().filter(Estimate::hasVoted).count();

      taskProgress.add(
          new TaskProgressResponse()
              .taskId(task.id())
              .title(task.title())
              .votedCount((int) voted)
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
  public ResponseEntity<ParticipantProgressResponse> getParticipantProgress(UUID roomId) {
    log.debug("GET /rooms/{}/participant-progress", roomId);
    Room room = roomService.getRoom(roomId);
    List<Task> tasks =
        taskService.listByRoom(roomId).stream()
            .filter(t -> !RoomService.PHANTOM_TASK_TITLE.equals(t.title()))
            .toList();
    List<Participant> participants = participantService.listParticipants(room.project().id());

    List<UUID> taskIds = tasks.stream().map(Task::id).toList();
    Map<UUID, List<Estimate>> estimatesByTask = estimateService.getEstimatesByTaskIds(taskIds);

    // Flatten all estimates and group by participant
    Map<UUID, List<Estimate>> estimatesByParticipant =
        estimatesByTask.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.groupingBy(e -> e.participant().id()));

    int totalTasks = tasks.size();

    List<ParticipantProgressEntry> entries = new ArrayList<>();
    for (Participant participant : participants) {
      List<Estimate> participantEstimates =
          estimatesByParticipant.getOrDefault(participant.id(), List.of());
      int votedCount = (int) participantEstimates.stream().filter(Estimate::hasVoted).count();
      boolean hasCommentedAll =
          votedCount > 0
              && participantEstimates.stream()
                  .filter(Estimate::hasVoted)
                  .allMatch(e -> e.comment() != null && !e.comment().isBlank());

      entries.add(
          new ParticipantProgressEntry()
              .nickname(participant.nickname())
              .votedCount(votedCount)
              .totalTasks(totalTasks)
              .hasCommentedAll(hasCommentedAll));
    }

    ParticipantProgressResponse response =
        new ParticipantProgressResponse()
            .roomId(roomId)
            .slug(room.slug())
            .totalTasks(totalTasks)
            .participants(entries);

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<RoomResultsResponse> getRoomResults(UUID roomId) {
    log.debug("GET /rooms/{}/results", roomId);
    Room room = roomService.getRoom(roomId);
    if (!RoomService.isRevealedStatus(room.status())) {
      throw new UnauthorizedException("Results are not available until estimates are revealed");
    }

    TasksWithEstimates data = loadTasksAndEstimates(roomId);

    List<TaskResultResponse> taskResults = new ArrayList<>();
    for (Task task : data.tasks()) {
      List<Estimate> voted =
          data.estimatesByTask().getOrDefault(task.id(), List.of()).stream()
              .filter(Estimate::hasVoted)
              .toList();
      List<EstimateResponse> estimateResponses =
          voted.stream().map(estimateMapper::toResponse).toList();

      List<String> points = voted.stream().map(Estimate::storyPoints).toList();
      taskResults.add(
          new TaskResultResponse()
              .taskId(task.id())
              .title(task.title())
              .estimates(estimateResponses)
              .averagePoints(EstimationStats.computeAverage(points))
              .medianPoints(EstimationStats.computeMedian(points))
              .finalEstimate(
                  task.finalEstimate() != null
                      ? StoryPoints.fromValue(task.finalEstimate())
                      : null));
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
  public ResponseEntity<String> exportResults(UUID roomId) {
    log.debug("GET /rooms/{}/export", roomId);
    Room room = roomService.validateAdminAndGetRoom(roomId);

    if (!RoomService.isRevealedStatus(room.status())) {
      throw new UnauthorizedException("Results are not available until estimates are revealed");
    }

    TasksWithEstimates data = loadTasksAndEstimates(roomId);
    List<Participant> participants = participantService.listParticipants(room.project().id());

    boolean hasComments =
        data.estimatesByTask().values().stream()
            .flatMap(List::stream)
            .anyMatch(e -> e.comment() != null && !e.comment().isBlank());

    StringBuilder csv = new StringBuilder();
    csv.append("Task");
    for (Participant p : participants) {
      csv.append(",").append(escapeCsv(p.nickname()));
      if (hasComments) {
        csv.append(",").append(escapeCsv(p.nickname() + " Comment"));
      }
    }
    csv.append(",Final SP");
    csv.append("\n");

    for (Task task : data.tasks()) {
      csv.append(escapeCsv(task.title()));
      List<Estimate> estimates = data.estimatesByTask().getOrDefault(task.id(), List.of());
      Map<UUID, Estimate> estimateByParticipant =
          estimates.stream().collect(Collectors.toMap(e -> e.participant().id(), e -> e));
      for (Participant p : participants) {
        Estimate est = estimateByParticipant.get(p.id());
        csv.append(",").append(est != null && est.storyPoints() != null ? est.storyPoints() : "");
        if (hasComments) {
          csv.append(",")
              .append(est != null && est.comment() != null ? escapeCsv(est.comment()) : "");
        }
      }
      csv.append(",").append(task.finalEstimate() != null ? task.finalEstimate() : "");
      csv.append("\n");
    }

    return ResponseEntity.ok()
        .header("Content-Type", "text/csv; charset=UTF-8")
        .header("Content-Disposition", "attachment; filename=\"room-" + roomId + ".csv\"")
        .body(csv.toString());
  }

  @Override
  public ResponseEntity<LiveRoomStateResponse> getLiveRoomState(UUID roomId) {
    log.debug("GET /rooms/{}/live", roomId);
    Room room = roomService.getRoom(roomId);
    if (RoomType.LIVE != room.roomType()) {
      throw new IllegalStateException("getLiveRoomState is only available for LIVE rooms");
    }
    UUID participantId = SecurityUtils.getCurrentParticipantId();
    return ResponseEntity.ok(buildLiveRoomStateResponse(room, participantId));
  }

  @Override
  public ResponseEntity<LiveRoomStateResponse> newRound(
      UUID roomId, NewRoundRequest newRoundRequest) {
    log.debug("POST /rooms/{}/new-round", roomId);
    roomService.validateAdminAndGetRoom(roomId);
    String topic = newRoundRequest != null ? newRoundRequest.getTopic() : null;
    Room updated = roomService.newRound(roomId, topic);
    return ResponseEntity.ok(buildLiveRoomStateResponse(updated, null));
  }

  @Override
  public ResponseEntity<List<RoundHistoryEntry>> getRoundHistory(UUID roomId) {
    log.debug("GET /rooms/{}/history", roomId);
    Room room = roomService.getRoom(roomId);
    if (RoomType.LIVE != room.roomType()) {
      throw new IllegalStateException("getRoundHistory is only available for LIVE rooms");
    }
    return ResponseEntity.ok(roundHistoryService.getHistory(roomId));
  }

  private record TasksWithEstimates(List<Task> tasks, Map<UUID, List<Estimate>> estimatesByTask) {}

  private TasksWithEstimates loadTasksAndEstimates(UUID roomId) {
    List<Task> tasks =
        taskService.listByRoom(roomId).stream()
            .filter(t -> !RoomService.PHANTOM_TASK_TITLE.equals(t.title()))
            .toList();
    List<UUID> taskIds = tasks.stream().map(Task::id).toList();
    Map<UUID, List<Estimate>> estimatesByTask = estimateService.getEstimatesByTaskIds(taskIds);
    return new TasksWithEstimates(tasks, estimatesByTask);
  }

  private LiveRoomStateResponse buildLiveRoomStateResponse(Room room, UUID participantId) {
    Task phantom = taskService.getPhantomTask(room.id());
    List<Participant> participants = participantService.listParticipants(room.project().id());
    boolean roomRevealed = RoomService.isRevealedStatus(room.status());

    List<Estimate> estimates = estimateService.getEstimatesForTask(phantom.id());
    Set<UUID> votedParticipantIds =
        estimates.stream()
            .filter(Estimate::hasVoted)
            .map(e -> e.participant().id())
            .collect(Collectors.toSet());

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
      List<Estimate> voted = estimates.stream().filter(Estimate::hasVoted).toList();
      List<EstimateResponse> estimateResponses =
          voted.stream().map(estimateMapper::toResponse).toList();
      List<String> points = voted.stream().map(Estimate::storyPoints).toList();
      results =
          new LiveRoomResults()
              .estimates(estimateResponses)
              .averagePoints(EstimationStats.computeAverage(points))
              .medianPoints(EstimationStats.computeMedian(points));
    }

    List<String> points = estimates.stream().map(Estimate::storyPoints).toList();
    long questionVotesCount = EstimationStats.countQuestionVotes(points);

    return new LiveRoomStateResponse()
        .roomId(room.id())
        .slug(room.slug())
        .title(room.title())
        .topic(room.topic())
        .status(roomMapper.mapStatus(room.status()))
        .roundNumber(room.roundNumber())
        .commentTemplate(room.commentTemplate())
        .commentRequired(room.commentRequired())
        .questionVotesCount((int) questionVotesCount)
        .taskId(phantom.id())
        .myEstimate(myEstimate)
        .participants(participantStatuses)
        .results(results);
  }

  private RoomDetailResponse buildDetailResponse(Room room, UUID participantId) {
    List<Task> tasks = taskService.listByRoom(room.id());
    boolean roomRevealed = RoomService.isRevealedStatus(room.status());

    List<UUID> taskIds = tasks.stream().map(Task::id).toList();
    Map<UUID, List<Estimate>> estimatesByTask = estimateService.getEstimatesByTaskIds(taskIds);

    long totalParticipants;
    if (roomRevealed) {
      totalParticipants =
          estimatesByTask.values().stream()
              .flatMap(List::stream)
              .map(e -> e.participant().id())
              .distinct()
              .count();
    } else {
      totalParticipants = participantService.countByProject(room.project().id());
    }

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
            estimates.stream().filter(Estimate::hasVoted).map(estimateMapper::toResponse).toList();
      }

      List<String> points = estimates.stream().map(Estimate::storyPoints).toList();
      long questionVotesCount = EstimationStats.countQuestionVotes(points);
      int votedCount = (int) estimates.stream().filter(Estimate::hasVoted).count();

      TaskWithEstimateResponse taskResponse =
          new TaskWithEstimateResponse()
              .id(task.id())
              .title(task.title())
              .description(task.description())
              .sortOrder(task.sortOrder())
              .myEstimate(myEstimate)
              .allEstimates(allEstimates)
              .votedCount(votedCount)
              .totalParticipants((int) totalParticipants)
              .finalEstimate(task.finalEstimate())
              .revealed(task.revealed())
              .active(task.active());

      taskResponse
          .averagePoints(EstimationStats.computeAverage(points))
          .medianPoints(EstimationStats.computeMedian(points))
          .questionVotesCount((int) questionVotesCount);

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
        .autoRevealOnDeadline(room.autoRevealOnDeadline())
        .commentTemplate(room.commentTemplate())
        .commentRequired(room.commentRequired())
        .tasks(taskResponses);
  }

  private String escapeCsv(String value) {
    if (value.contains(",")
        || value.contains("\"")
        || value.contains("\n")
        || value.contains("\r")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
