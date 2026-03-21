package com.andreasik.efipoker.estimation.task;

import com.andreasik.efipoker.api.TasksApi;
import com.andreasik.efipoker.api.model.CreateTaskRequest;
import com.andreasik.efipoker.api.model.ImportTasksRequest;
import com.andreasik.efipoker.api.model.SetFinalEstimateRequest;
import com.andreasik.efipoker.api.model.TaskResponse;
import com.andreasik.efipoker.api.model.UpdateTaskRequest;
import com.andreasik.efipoker.estimation.room.Room;
import com.andreasik.efipoker.estimation.room.RoomService;
import com.andreasik.efipoker.shared.exception.UnauthorizedException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TaskController implements TasksApi {

  private final TaskService taskService;
  private final RoomService roomService;
  private final TaskMapper taskMapper;

  @Override
  public ResponseEntity<TaskResponse> createTask(UUID roomId, CreateTaskRequest createTaskRequest) {
    log.debug("POST /rooms/{}/tasks", roomId);
    roomService.validateAdminAndGetRoom(roomId);

    int sortOrder = createTaskRequest.getSortOrder() != null ? createTaskRequest.getSortOrder() : 0;

    Task task =
        taskService.createTask(
            roomId, createTaskRequest.getTitle(), createTaskRequest.getDescription(), sortOrder);
    return ResponseEntity.status(HttpStatus.CREATED).body(taskMapper.toResponse(task));
  }

  @Override
  public ResponseEntity<List<TaskResponse>> importTasks(
      UUID roomId, ImportTasksRequest importTasksRequest) {
    log.debug("POST /rooms/{}/tasks/import", roomId);
    roomService.validateAdminAndGetRoom(roomId);

    List<Task> tasks = taskService.importTasks(roomId, importTasksRequest.getTitles());
    return ResponseEntity.status(HttpStatus.CREATED).body(taskMapper.toResponseList(tasks));
  }

  @Override
  public ResponseEntity<TaskResponse> updateTask(UUID taskId, UpdateTaskRequest updateTaskRequest) {
    log.debug("PATCH /tasks/{}", taskId);
    Task task = taskService.getTask(taskId);
    roomService.validateAdminAndGetRoom(task.room().id());

    Task updated =
        taskService.updateTask(
            taskId,
            updateTaskRequest.getTitle(),
            updateTaskRequest.getDescription(),
            updateTaskRequest.getSortOrder());
    return ResponseEntity.ok(taskMapper.toResponse(updated));
  }

  @Override
  public ResponseEntity<TaskResponse> setFinalEstimate(
      UUID taskId, SetFinalEstimateRequest setFinalEstimateRequest) {
    log.debug("PUT /tasks/{}/final-estimate", taskId);
    Task task = taskService.getTask(taskId);
    Room room = roomService.validateAdminAndGetRoom(task.room().id());

    if (!RoomService.isRevealedStatus(room.status())) {
      throw new UnauthorizedException("Final estimate can only be set after votes are revealed");
    }

    String storyPoints = setFinalEstimateRequest.getStoryPoints().getValue();
    Task updated = taskService.setFinalEstimate(taskId, storyPoints);
    return ResponseEntity.ok(taskMapper.toResponse(updated));
  }

  @Override
  public ResponseEntity<Void> deleteTask(UUID taskId) {
    log.debug("DELETE /tasks/{}", taskId);
    Task task = taskService.getTask(taskId);
    roomService.validateAdminAndGetRoom(task.room().id());

    taskService.deleteTask(taskId);
    return ResponseEntity.noContent().build();
  }
}
