package com.andreasik.efipoker.estimation.task;

import com.andreasik.efipoker.estimation.estimate.StoryPoints;
import com.andreasik.efipoker.estimation.room.RoomEntity;
import com.andreasik.efipoker.estimation.room.RoomRepository;
import com.andreasik.efipoker.estimation.room.RoomService;
import com.andreasik.efipoker.shared.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class TaskService {

  private final TaskRepository taskRepository;
  private final RoomRepository roomRepository;
  private final TaskEntityMapper taskEntityMapper;

  public Task getTask(UUID taskId) {
    log.debug("getTask: id={}", taskId);
    TaskEntity entity =
        taskRepository
            .findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));
    return taskEntityMapper.toDomain(entity);
  }

  @Transactional
  public Task createTask(UUID roomId, String title, String description, int sortOrder) {
    RoomEntity room =
        roomRepository
            .findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));

    if ("LIVE".equals(room.getRoomType())) {
      log.warn("Cannot create task in LIVE room: roomId={}", roomId);
      throw new IllegalStateException("Tasks cannot be manually created in a LIVE room.");
    }

    TaskEntity entity =
        TaskEntity.builder()
            .room(room)
            .title(title)
            .description(description)
            .sortOrder(sortOrder)
            .build();

    TaskEntity saved = taskRepository.save(entity);
    log.info("Task created: id={}, roomId={}, title={}", saved.getId(), roomId, title);
    return taskEntityMapper.toDomain(saved);
  }

  @Transactional
  public List<Task> importTasks(UUID roomId, List<String> titles) {
    RoomEntity room =
        roomRepository
            .findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));

    if ("LIVE".equals(room.getRoomType())) {
      log.warn("Cannot import tasks into LIVE room: roomId={}", roomId);
      throw new IllegalStateException("Tasks cannot be imported into a LIVE room.");
    }

    List<TaskEntity> entities = new ArrayList<>();
    for (int i = 0; i < titles.size(); i++) {
      TaskEntity entity = TaskEntity.builder().room(room).title(titles.get(i)).sortOrder(i).build();
      entities.add(entity);
    }

    List<TaskEntity> saved = taskRepository.saveAll(entities);
    log.info("Imported {} tasks for room: {}", saved.size(), roomId);
    return taskEntityMapper.toDomainList(saved);
  }

  @Transactional
  public Task updateTask(UUID taskId, String title, String description, Integer sortOrder) {
    TaskEntity entity =
        taskRepository
            .findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

    if (title != null) {
      entity.setTitle(title);
    }
    if (description != null) {
      entity.setDescription(description);
    }
    if (sortOrder != null) {
      entity.setSortOrder(sortOrder);
    }

    TaskEntity saved = taskRepository.save(entity);
    log.info("Task updated: id={}", taskId);
    return taskEntityMapper.toDomain(saved);
  }

  @Transactional
  public Task setFinalEstimate(UUID taskId, String storyPoints) {
    StoryPoints.fromValue(storyPoints);
    TaskEntity entity =
        taskRepository
            .findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));
    entity.setFinalEstimate(storyPoints);
    TaskEntity saved = taskRepository.save(entity);
    log.info("Final estimate set: taskId={}, sp={}", taskId, storyPoints);
    return taskEntityMapper.toDomain(saved);
  }

  public List<Task> listByRoom(UUID roomId) {
    log.debug("listByRoom: roomId={}", roomId);
    return taskEntityMapper.toDomainList(taskRepository.findByRoomId(roomId));
  }

  @Transactional
  public void deleteTask(UUID taskId) {
    if (!taskRepository.existsById(taskId)) {
      throw new ResourceNotFoundException("Task", taskId);
    }
    taskRepository.deleteById(taskId);
    log.info("Task deleted: id={}", taskId);
  }

  public Task getPhantomTask(UUID roomId) {
    return taskRepository
        .findByRoomIdAndTitle(roomId, RoomService.PHANTOM_TASK_TITLE)
        .map(taskEntityMapper::toDomain)
        .orElseThrow(() -> new ResourceNotFoundException("Phantom task for room", roomId));
  }
}
