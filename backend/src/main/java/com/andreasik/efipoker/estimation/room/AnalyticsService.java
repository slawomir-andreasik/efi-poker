package com.andreasik.efipoker.estimation.room;

import com.andreasik.efipoker.api.model.ContentiousTaskEntry;
import com.andreasik.efipoker.api.model.ParticipantLeaderboardEntry;
import com.andreasik.efipoker.api.model.ParticipationMatrixEntry;
import com.andreasik.efipoker.api.model.ProjectAnalyticsResponse;
import com.andreasik.efipoker.api.model.ProjectAnalyticsSummary;
import com.andreasik.efipoker.api.model.RoomAnalyticsResponse;
import com.andreasik.efipoker.api.model.RoomAnalyticsSummary;
import com.andreasik.efipoker.api.model.RoomStatsEntry;
import com.andreasik.efipoker.api.model.StoryPoints;
import com.andreasik.efipoker.api.model.TaskAnalyticsEntry;
import com.andreasik.efipoker.estimation.EstimationStats;
import com.andreasik.efipoker.estimation.estimate.Estimate;
import com.andreasik.efipoker.estimation.estimate.EstimateService;
import com.andreasik.efipoker.estimation.task.Task;
import com.andreasik.efipoker.estimation.task.TaskService;
import com.andreasik.efipoker.participant.Participant;
import com.andreasik.efipoker.participant.ParticipantService;
import com.andreasik.efipoker.project.Project;
import com.andreasik.efipoker.project.ProjectService;
import com.andreasik.efipoker.shared.exception.UnauthorizedException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Transactional(readOnly = true)
public class AnalyticsService {

  private final RoomService roomService;
  private final EstimateService estimateService;
  private final ProjectService projectService;
  private final ParticipantService participantService;
  private final TaskService taskService;

  public RoomAnalyticsResponse computeRoomAnalytics(UUID roomId) {
    log.debug("computeRoomAnalytics: roomId={}", roomId);
    Room room = roomService.getRoom(roomId);
    if (!RoomService.isRevealedStatus(room.status())) {
      log.warn(
          "Analytics requested on non-revealed room: roomId={}, status={}", roomId, room.status());
      throw new UnauthorizedException("Room is not yet revealed: " + roomId);
    }

    // Load tasks (filter phantom), load participants - batch, no N+1
    List<Task> tasks =
        taskService.listByRoom(roomId).stream()
            .filter(t -> !RoomService.PHANTOM_TASK_TITLE.equals(t.title()))
            .toList();

    List<Participant> participants = participantService.listParticipants(room.project().id());

    // Batch load all estimates for all tasks - 1 query
    List<UUID> taskIds = tasks.stream().map(Task::id).toList();
    Map<UUID, List<Estimate>> estimatesByTask = estimateService.getEstimatesByTaskIds(taskIds);

    // Pre-build participant -> {taskId -> storyPoints} map in O(estimates) pass
    Map<UUID, Map<String, String>> votesByParticipant = new HashMap<>();
    estimatesByTask.forEach(
        (taskId, ests) ->
            ests.stream()
                .filter(Estimate::hasVoted)
                .forEach(
                    e ->
                        votesByParticipant
                            .computeIfAbsent(e.participant().id(), k -> new HashMap<>())
                            .put(taskId.toString(), e.storyPoints())));

    // Build task analytics entries
    List<TaskAnalyticsEntry> taskAnalytics = new ArrayList<>();
    int consensusCount = 0;
    double totalSP = 0.0;

    for (Task task : tasks) {
      List<Estimate> estimates = estimatesByTask.getOrDefault(task.id(), List.of());
      List<String> spValues = estimates.stream().map(Estimate::storyPoints).toList();
      List<Double> numeric = EstimationStats.extractNumericPoints(spValues);

      // Vote distribution
      Map<String, Integer> voteDistribution = new HashMap<>();
      for (String sp : spValues) {
        voteDistribution.merge(sp, 1, Integer::sum);
      }

      Double average = EstimationStats.computeAverage(spValues);
      Double median = EstimationStats.computeMedian(spValues);
      Double spread = EstimationStats.computeStdDeviation(spValues);

      if (EstimationStats.isConsensus(numeric)) {
        consensusCount++;
      }

      totalSP += EstimationStats.computeTaskSP(task.finalEstimate(), numeric);

      TaskAnalyticsEntry entry = new TaskAnalyticsEntry();
      entry.setTaskId(task.id());
      entry.setTitle(task.title());
      entry.setAveragePoints(average);
      entry.setMedianPoints(median);
      entry.setFinalEstimate(
          task.finalEstimate() != null ? StoryPoints.fromValue(task.finalEstimate()) : null);
      entry.setSpread(spread);
      entry.setVoteDistribution(voteDistribution);
      taskAnalytics.add(entry);
    }

    int totalParticipants = participants.size();
    double participationRate =
        totalParticipants > 0 ? (votesByParticipant.size() * 100.0 / totalParticipants) : 0.0;

    RoomAnalyticsSummary summary = new RoomAnalyticsSummary();
    summary.setTotalTasks(tasks.size());
    summary.setTotalStoryPoints(totalSP);
    summary.setConsensusCount(consensusCount);
    summary.setParticipationRate(participationRate);
    summary.setTotalParticipants(totalParticipants);

    // Participation matrix: for each participant, map taskId -> storyPoints
    List<ParticipationMatrixEntry> participationMatrix = new ArrayList<>();
    for (Participant participant : participants) {
      ParticipationMatrixEntry matrixEntry = new ParticipationMatrixEntry();
      matrixEntry.setParticipantId(participant.id());
      matrixEntry.setNickname(participant.nickname());
      matrixEntry.setTaskVotes(votesByParticipant.getOrDefault(participant.id(), Map.of()));
      participationMatrix.add(matrixEntry);
    }

    RoomAnalyticsResponse response = new RoomAnalyticsResponse();
    response.setRoomId(roomId);
    response.setTitle(room.title());
    response.setSlug(room.slug());
    response.setSummary(summary);
    response.setTaskAnalytics(taskAnalytics);
    response.setParticipationMatrix(participationMatrix);
    return response;
  }

  public ProjectAnalyticsResponse computeProjectAnalytics(String slug) {
    log.debug("computeProjectAnalytics: slug={}", slug);
    Project project = projectService.getProjectBySlug(slug);

    // Load all rooms for the project, filter to revealed/closed
    List<Room> allRooms = roomService.listByProject(project.id());
    List<Room> revealedRooms =
        allRooms.stream().filter(r -> RoomService.isRevealedStatus(r.status())).toList();

    Set<UUID> revealedRoomIds = revealedRooms.stream().map(Room::id).collect(Collectors.toSet());

    // Batch load all tasks for the project - 1 query (phantom excluded)
    List<Task> allTasks = taskService.listByProjectExcludingPhantom(project.id());

    // Filter to only tasks belonging to revealed/closed rooms
    List<Task> tasks =
        allTasks.stream().filter(t -> revealedRoomIds.contains(t.room().id())).toList();

    // Batch load all estimates for all tasks - 1 query
    List<UUID> taskIds = tasks.stream().map(Task::id).toList();
    Map<UUID, List<Estimate>> estimatesByTask = estimateService.getEstimatesByTaskIds(taskIds);

    // Load participants
    List<Participant> participants = participantService.listParticipants(project.id());

    // Group tasks by room
    Map<UUID, List<Task>> tasksByRoom =
        tasks.stream().collect(Collectors.groupingBy(t -> t.room().id()));

    // Per-room stats
    List<RoomStatsEntry> roomStats = new ArrayList<>();
    double totalConsensusRateSum = 0.0;
    int roomsWithTasks = 0;

    for (Room room : revealedRooms) {
      List<Task> roomTasks = tasksByRoom.getOrDefault(room.id(), List.of());
      if (roomTasks.isEmpty()) {
        RoomStatsEntry entry = new RoomStatsEntry();
        entry.setRoomId(room.id());
        entry.setTitle(room.title());
        entry.setTotalStoryPoints(0.0);
        entry.setTaskCount(0);
        entry.setConsensusRate(0.0);
        entry.setCreatedAt(room.createdAt());
        roomStats.add(entry);
        continue;
      }

      double roomSP = 0.0;
      int consensusCount = 0;

      for (Task task : roomTasks) {
        List<Estimate> estimates = estimatesByTask.getOrDefault(task.id(), List.of());
        List<Double> numeric =
            EstimationStats.extractNumericPoints(
                estimates.stream().map(Estimate::storyPoints).toList());

        roomSP += EstimationStats.computeTaskSP(task.finalEstimate(), numeric);

        if (EstimationStats.isConsensus(numeric)) {
          consensusCount++;
        }
      }

      double consensusRate = consensusCount * 100.0 / roomTasks.size();
      totalConsensusRateSum += consensusRate;
      roomsWithTasks++;

      RoomStatsEntry entry = new RoomStatsEntry();
      entry.setRoomId(room.id());
      entry.setTitle(room.title());
      entry.setTotalStoryPoints(roomSP);
      entry.setTaskCount(roomTasks.size());
      entry.setConsensusRate(consensusRate);
      entry.setCreatedAt(room.createdAt());
      roomStats.add(entry);
    }

    // Top 5 contentious tasks by spread (stddev)
    record TaskSpread(Task task, Double spread, int voteCount) {}
    List<TaskSpread> spreads = new ArrayList<>();
    for (Task task : tasks) {
      List<Estimate> estimates = estimatesByTask.getOrDefault(task.id(), List.of());
      List<String> spValues = estimates.stream().map(Estimate::storyPoints).toList();
      Double spread = EstimationStats.computeStdDeviation(spValues);
      if (spread != null) {
        spreads.add(new TaskSpread(task, spread, estimates.size()));
      }
    }
    spreads.sort(Comparator.comparingDouble(TaskSpread::spread).reversed());

    List<ContentiousTaskEntry> topContentious =
        spreads.stream()
            .limit(5)
            .map(
                ts -> {
                  ContentiousTaskEntry e = new ContentiousTaskEntry();
                  e.setTaskId(ts.task().id());
                  e.setTaskTitle(ts.task().title());
                  e.setRoomTitle(ts.task().room().title());
                  e.setSpread(ts.spread());
                  e.setVoteCount(ts.voteCount());
                  return e;
                })
            .toList();

    // Participant leaderboard - pre-compute vote counts per participant
    int totalTasks = tasks.size();
    Map<UUID, Integer> voteCountByParticipant = new HashMap<>();
    for (List<Estimate> estimates : estimatesByTask.values()) {
      for (Estimate e : estimates) {
        voteCountByParticipant.merge(e.participant().id(), 1, Integer::sum);
      }
    }

    List<ParticipantLeaderboardEntry> leaderboard = new ArrayList<>();
    for (Participant participant : participants) {
      int voted = voteCountByParticipant.getOrDefault(participant.id(), 0);
      double rate = totalTasks > 0 ? (voted * 100.0 / totalTasks) : 0.0;
      ParticipantLeaderboardEntry entry = new ParticipantLeaderboardEntry();
      entry.setParticipantId(participant.id());
      entry.setNickname(participant.nickname());
      entry.setTasksVoted(voted);
      entry.setTotalTasks(totalTasks);
      entry.setParticipationRate(rate);
      leaderboard.add(entry);
    }
    leaderboard.sort(
        Comparator.comparingInt(ParticipantLeaderboardEntry::getTasksVoted).reversed());

    // Project-level totals
    double projectTotalSP =
        roomStats.stream()
            .mapToDouble(r -> r.getTotalStoryPoints() != null ? r.getTotalStoryPoints() : 0.0)
            .sum();
    double avgConsensusRate = roomsWithTasks > 0 ? totalConsensusRateSum / roomsWithTasks : 0.0;

    ProjectAnalyticsSummary summary = new ProjectAnalyticsSummary();
    summary.setTotalRooms(revealedRooms.size());
    summary.setTotalTasks(totalTasks);
    summary.setTotalStoryPoints(projectTotalSP);
    summary.setAverageConsensusRate(avgConsensusRate);

    ProjectAnalyticsResponse response = new ProjectAnalyticsResponse();
    response.setProjectId(project.id());
    response.setProjectName(project.name());
    response.setSlug(project.slug());
    response.setSummary(summary);
    response.setRoomStats(roomStats);
    response.setTopContentiousTasks(topContentious);
    response.setParticipantLeaderboard(leaderboard);
    return response;
  }
}
