package com.andreasik.efipoker.estimation.room;

import com.andreasik.efipoker.estimation.estimate.Estimate;
import com.andreasik.efipoker.estimation.estimate.EstimateService;
import com.andreasik.efipoker.estimation.task.Task;
import com.andreasik.efipoker.estimation.task.TaskService;
import com.andreasik.efipoker.participant.Participant;
import com.andreasik.efipoker.participant.ParticipantService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomCsvExporter {

  private final TaskService taskService;
  private final EstimateService estimateService;
  private final ParticipantService participantService;

  String export(Room room) {
    List<Task> tasks =
        taskService.listByRoom(room.id()).stream()
            .filter(t -> !RoomService.PHANTOM_TASK_TITLE.equals(t.title()))
            .toList();
    List<UUID> taskIds = tasks.stream().map(Task::id).toList();
    Map<UUID, List<Estimate>> estimatesByTask = estimateService.getEstimatesByTaskIds(taskIds);
    List<Participant> participants = participantService.listParticipants(room.project().id());

    boolean hasComments =
        estimatesByTask.values().stream()
            .flatMap(List::stream)
            .anyMatch(e -> e.comment() != null && !e.comment().isBlank());

    StringBuilder csv = new StringBuilder(8192);
    csv.append("Task");
    for (Participant p : participants) {
      csv.append(",").append(escapeCsv(p.nickname()));
      if (hasComments) {
        csv.append(",").append(escapeCsv(p.nickname() + " Comment"));
      }
    }
    csv.append(",Final SP");
    csv.append("\n");

    for (Task task : tasks) {
      csv.append(escapeCsv(task.title()));
      List<Estimate> estimates = estimatesByTask.getOrDefault(task.id(), List.of());
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

    return csv.toString();
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
