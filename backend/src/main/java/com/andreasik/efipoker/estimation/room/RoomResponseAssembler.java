package com.andreasik.efipoker.estimation.room;

import com.andreasik.efipoker.api.model.AutoAssignedEstimate;
import com.andreasik.efipoker.api.model.EstimateResponse;
import com.andreasik.efipoker.api.model.FinishSessionResponse;
import com.andreasik.efipoker.api.model.LiveParticipantStatus;
import com.andreasik.efipoker.api.model.LiveRoomResults;
import com.andreasik.efipoker.api.model.LiveRoomStateResponse;
import com.andreasik.efipoker.api.model.ParticipantProgressEntry;
import com.andreasik.efipoker.api.model.ParticipantProgressResponse;
import com.andreasik.efipoker.api.model.RoomAdminResponse;
import com.andreasik.efipoker.api.model.RoomDetailResponse;
import com.andreasik.efipoker.api.model.RoomProgressResponse;
import com.andreasik.efipoker.api.model.RoomResultsResponse;
import com.andreasik.efipoker.api.model.StoryPoints;
import com.andreasik.efipoker.api.model.TaskProgressResponse;
import com.andreasik.efipoker.api.model.TaskResultResponse;
import com.andreasik.efipoker.api.model.TaskWithAllEstimatesResponse;
import com.andreasik.efipoker.api.model.TaskWithEstimateResponse;
import com.andreasik.efipoker.estimation.EstimationStats;
import com.andreasik.efipoker.estimation.estimate.Estimate;
import com.andreasik.efipoker.estimation.estimate.EstimateMapper;
import com.andreasik.efipoker.estimation.estimate.EstimateService;
import com.andreasik.efipoker.estimation.task.Task;
import com.andreasik.efipoker.estimation.task.TaskService;
import com.andreasik.efipoker.participant.Participant;
import com.andreasik.efipoker.participant.ParticipantMapper;
import com.andreasik.efipoker.participant.ParticipantService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomResponseAssembler {

  private final TaskService taskService;
  private final EstimateService estimateService;
  private final ParticipantService participantService;
  private final RoomMapper roomMapper;
  private final EstimateMapper estimateMapper;
  private final ParticipantMapper participantMapper;

  record TasksWithEstimates(List<Task> tasks, Map<UUID, List<Estimate>> estimatesByTask) {}

  private TasksWithEstimates loadTasksAndEstimates(UUID roomId, boolean excludePhantom) {
    List<Task> tasks = taskService.listByRoom(roomId);
    if (excludePhantom) {
      tasks =
          tasks.stream().filter(t -> !RoomService.PHANTOM_TASK_TITLE.equals(t.title())).toList();
    }
    List<UUID> taskIds = tasks.stream().map(Task::id).toList();
    Map<UUID, List<Estimate>> estimatesByTask = estimateService.getEstimatesByTaskIds(taskIds);
    return new TasksWithEstimates(tasks, estimatesByTask);
  }

  private EstimateResponse findMyEstimate(List<Estimate> estimates, UUID participantId) {
    if (participantId == null) return null;
    return estimates.stream()
        .filter(e -> e.participant().id().equals(participantId))
        .findFirst()
        .map(estimateMapper::toResponse)
        .orElse(null);
  }

  RoomDetailResponse buildDetailResponse(Room room, UUID participantId) {
    TasksWithEstimates data = loadTasksAndEstimates(room.id(), false);
    List<Task> tasks = data.tasks();
    Map<UUID, List<Estimate>> estimatesByTask = data.estimatesByTask();
    boolean roomRevealed = RoomService.isRevealedStatus(room.status());

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

      EstimateResponse myEstimate = findMyEstimate(estimates, participantId);

      List<Estimate> voted = estimates.stream().filter(Estimate::hasVoted).toList();
      List<EstimateResponse> allEstimates = null;
      if (roomRevealed) {
        allEstimates = voted.stream().map(estimateMapper::toResponse).toList();
      }

      List<String> points = estimates.stream().map(Estimate::storyPoints).toList();
      long questionVotesCount = EstimationStats.countQuestionVotes(points);
      int votedCount = voted.size();

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

  LiveRoomStateResponse buildLiveRoomStateResponse(Room room, UUID participantId) {
    Task phantom = taskService.getPhantomTask(room.id());
    List<Participant> participants = participantService.listParticipants(room.project().id());
    boolean roomRevealed = RoomService.isRevealedStatus(room.status());

    List<Estimate> estimates = estimateService.getEstimatesForTask(phantom.id());
    List<Estimate> voted = estimates.stream().filter(Estimate::hasVoted).toList();
    Set<UUID> votedParticipantIds =
        voted.stream().map(e -> e.participant().id()).collect(Collectors.toSet());

    EstimateResponse myEstimate = findMyEstimate(estimates, participantId);

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

  RoomAdminResponse buildAdminResponse(Room room) {
    TasksWithEstimates data = loadTasksAndEstimates(room.id(), false);
    List<Task> tasks = data.tasks();
    Map<UUID, List<Estimate>> estimatesByTask = data.estimatesByTask();
    List<Participant> participants = participantService.listParticipants(room.project().id());

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

    return new RoomAdminResponse()
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
  }

  RoomProgressResponse buildProgressResponse(Room room) {
    TasksWithEstimates data = loadTasksAndEstimates(room.id(), false);
    long totalParticipants = participantService.countByProject(room.project().id());

    List<TaskProgressResponse> taskProgress = new ArrayList<>();
    for (Task task : data.tasks()) {
      List<Estimate> estimates = data.estimatesByTask().getOrDefault(task.id(), List.of());
      List<Estimate> voted = estimates.stream().filter(Estimate::hasVoted).toList();
      List<String> points = estimates.stream().map(Estimate::storyPoints).toList();
      long questionVotesCount = EstimationStats.countQuestionVotes(points);

      taskProgress.add(
          new TaskProgressResponse()
              .taskId(task.id())
              .title(task.title())
              .votedCount(voted.size())
              .questionVotesCount((int) questionVotesCount)
              .totalParticipants((int) totalParticipants));
    }

    return new RoomProgressResponse()
        .roomId(room.id())
        .slug(room.slug())
        .status(roomMapper.mapStatus(room.status()))
        .totalParticipants((int) totalParticipants)
        .tasks(taskProgress);
  }

  ParticipantProgressResponse buildParticipantProgressResponse(Room room) {
    TasksWithEstimates data = loadTasksAndEstimates(room.id(), true);
    List<Participant> participants = participantService.listParticipants(room.project().id());

    Map<UUID, List<Estimate>> estimatesByParticipant =
        data.estimatesByTask().values().stream()
            .flatMap(List::stream)
            .collect(Collectors.groupingBy(e -> e.participant().id()));

    int totalTasks = data.tasks().size();

    List<ParticipantProgressEntry> entries = new ArrayList<>();
    for (Participant participant : participants) {
      List<Estimate> voted =
          estimatesByParticipant.getOrDefault(participant.id(), List.of()).stream()
              .filter(Estimate::hasVoted)
              .toList();
      int votedCount = voted.size();
      boolean hasCommentedAll =
          votedCount > 0
              && voted.stream().allMatch(e -> e.comment() != null && !e.comment().isBlank());

      entries.add(
          new ParticipantProgressEntry()
              .nickname(participant.nickname())
              .votedCount(votedCount)
              .totalTasks(totalTasks)
              .hasCommentedAll(hasCommentedAll));
    }

    return new ParticipantProgressResponse()
        .roomId(room.id())
        .slug(room.slug())
        .totalTasks(totalTasks)
        .participants(entries);
  }

  RoomResultsResponse buildResultsResponse(Room room) {
    TasksWithEstimates data = loadTasksAndEstimates(room.id(), true);

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

    return new RoomResultsResponse()
        .roomId(room.id())
        .slug(room.slug())
        .title(room.title())
        .status(roomMapper.mapStatus(room.status()))
        .tasks(taskResults);
  }

  FinishSessionResponse buildFinishSessionResponse(RoomService.FinishSessionResult result) {
    List<AutoAssignedEstimate> autoAssigned =
        result.autoAssigned().stream()
            .map(
                a ->
                    new AutoAssignedEstimate()
                        .taskId(a.taskId())
                        .taskTitle(a.taskTitle())
                        .finalEstimate(a.finalEstimate()))
            .toList();

    return new FinishSessionResponse()
        .status(roomMapper.mapStatus(result.room().status()))
        .autoAssignedEstimates(autoAssigned);
  }
}
