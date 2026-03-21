package com.andreasik.efipoker.estimation.room;

import com.andreasik.efipoker.estimation.EstimationStats;
import com.andreasik.efipoker.estimation.estimate.EstimateEntity;
import com.andreasik.efipoker.estimation.estimate.EstimateRepository;
import com.andreasik.efipoker.estimation.estimate.StoryPoints;
import com.andreasik.efipoker.estimation.task.TaskEntity;
import com.andreasik.efipoker.estimation.task.TaskRepository;
import com.andreasik.efipoker.project.ProjectApi;
import com.andreasik.efipoker.project.ProjectEntity;
import com.andreasik.efipoker.shared.event.RoomCreatedEvent;
import com.andreasik.efipoker.shared.event.RoundStartedEvent;
import com.andreasik.efipoker.shared.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Transactional(readOnly = true)
public class RoomService {

  /// Internal sentinel task title used by LIVE rooms for round-based voting.
  /// Never exposed to users.
  public static final String PHANTOM_TASK_TITLE = "__live__";

  private static final String SLUG_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final int SLUG_SEGMENT_LENGTH = 3;
  private static final int MAX_SLUG_RETRIES = 3;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final RoomRepository roomRepository;
  private final ProjectApi projectApi;
  private final EntityManager entityManager;
  private final TaskRepository taskRepository;
  private final EstimateRepository estimateRepository;
  private final RoomEntityMapper roomEntityMapper;
  private final RoundHistoryService roundHistoryService;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public Room createRoom(CreateRoomCommand command) {
    projectApi.validateProjectExists(command.projectId());
    ProjectEntity project = entityManager.getReference(ProjectEntity.class, command.projectId());

    if (RoomType.ASYNC == command.roomType() && command.deadline() == null) {
      log.warn("ASYNC room without deadline: projectId={}", command.projectId());
      throw new IllegalArgumentException("ASYNC rooms require a deadline");
    }

    RoomEntity saved = null;
    for (int attempt = 0; attempt < MAX_SLUG_RETRIES; attempt++) {
      String slug = generateSlug();
      RoomEntity entity =
          RoomEntity.builder()
              .project(project)
              .slug(slug)
              .title(command.title())
              .description(command.description())
              .roomType(command.roomType().name())
              .deadline(command.deadline())
              .autoRevealOnDeadline(command.autoRevealOnDeadline())
              .commentTemplate(command.commentTemplate())
              .commentRequired(command.commentRequired())
              .build();
      try {
        saved = roomRepository.save(entity);
        break;
      } catch (DataIntegrityViolationException e) {
        if (attempt == MAX_SLUG_RETRIES - 1) {
          throw e;
        }
        log.warn("Slug collision on attempt {}, retrying: slug={}", attempt + 1, slug);
      }
    }
    log.info(
        "Room created: id={}, slug={}, projectId={}, title={}, type={}",
        saved.getId(),
        saved.getSlug(),
        command.projectId(),
        command.title(),
        command.roomType());
    eventPublisher.publishEvent(new RoomCreatedEvent(saved.getId(), command.roomType().name()));

    if (RoomType.LIVE == command.roomType()) {
      TaskEntity phantom =
          TaskEntity.builder().room(saved).title(PHANTOM_TASK_TITLE).sortOrder(0).build();
      taskRepository.save(phantom);
      log.info("Phantom task created for LIVE room: roomId={}", saved.getId());
    }

    return roomEntityMapper.toDomain(saved);
  }

  public Room getRoom(UUID id) {
    log.debug("getRoom: id={}", id);
    RoomEntity entity =
        roomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Room", id));
    return roomEntityMapper.toDomain(entity);
  }

  public Room getRoomBySlug(String slug) {
    log.debug("getRoomBySlug: slug={}", slug);
    RoomEntity entity =
        roomRepository
            .findBySlug(slug.toUpperCase())
            .orElseThrow(() -> new ResourceNotFoundException("Room", slug));
    return roomEntityMapper.toDomain(entity);
  }

  public List<Room> listByProject(UUID projectId) {
    log.debug("listByProject: projectId={}", projectId);
    return roomEntityMapper.toDomainList(roomRepository.findByProjectId(projectId));
  }

  public Room validateAdminAndGetRoom(UUID roomId, String adminCode) {
    RoomEntity entity =
        roomRepository
            .findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
    projectApi.validateAdminCodeForProject(entity.getProject().getId(), adminCode);
    return roomEntityMapper.toDomain(entity);
  }

  @Transactional
  public Room updateRoom(UpdateRoomCommand command) {
    RoomEntity entity =
        roomRepository
            .findById(command.id())
            .orElseThrow(() -> new ResourceNotFoundException("Room", command.id()));

    if (command.title() != null) {
      entity.setTitle(command.title());
    }
    if (command.description() != null) {
      entity.setDescription(command.description());
    }
    if (command.deadline() != null) {
      entity.setDeadline(command.deadline());
    }
    if (command.topic() != null) {
      entity.setTopic(command.topic().isBlank() ? null : command.topic());
    }
    if (command.commentTemplate() != null) {
      entity.setCommentTemplate(
          command.commentTemplate().isBlank() ? null : command.commentTemplate());
    }
    if (command.commentRequired() != null) {
      entity.setCommentRequired(command.commentRequired());
    }
    if (command.autoRevealOnDeadline() != null) {
      entity.setAutoRevealOnDeadline(command.autoRevealOnDeadline());
    }

    RoomEntity saved = roomRepository.save(entity);
    log.info("Room updated: id={}", command.id());
    return roomEntityMapper.toDomain(saved);
  }

  @Transactional
  public Room revealRoom(UUID id) {
    RoomEntity entity =
        roomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Room", id));
    entity.setStatus(RoomStatus.REVEALED.name());
    RoomEntity saved = roomRepository.save(entity);
    log.info("Room revealed: id={}", id);
    return roomEntityMapper.toDomain(saved);
  }

  @Transactional
  public Room reopenRoom(UUID id) {
    RoomEntity entity =
        roomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Room", id));
    entity.setStatus(RoomStatus.OPEN.name());
    RoomEntity saved = roomRepository.save(entity);
    log.info("Room reopened: id={}", id);
    return roomEntityMapper.toDomain(saved);
  }

  public static boolean isRevealedStatus(String status) {
    return RoomStatus.REVEALED.name().equals(status) || RoomStatus.CLOSED.name().equals(status);
  }

  @Transactional
  public Room newRound(UUID roomId, String topic) {
    RoomEntity entity =
        roomRepository
            .findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
    if (!RoomType.LIVE.name().equals(entity.getRoomType())) {
      log.warn("newRound on non-LIVE room: roomId={}", roomId);
      throw new IllegalStateException("newRound is only available for LIVE rooms");
    }
    TaskEntity phantom =
        taskRepository
            .findByRoomIdAndTitle(roomId, PHANTOM_TASK_TITLE)
            .orElseThrow(() -> new ResourceNotFoundException("Phantom task for room", roomId));
    if (RoomStatus.REVEALED.name().equals(entity.getStatus())) {
      List<EstimateEntity> estimates = estimateRepository.findByTaskId(phantom.getId());
      roundHistoryService.saveRoundSnapshot(entity, estimates);
    }
    estimateRepository.deleteByTaskId(phantom.getId());
    entity.setStatus(RoomStatus.OPEN.name());
    entity.setRoundNumber(entity.getRoundNumber() + 1);
    // Always reset topic - if new topic provided use it, otherwise clear (don't carry over old
    // topic)
    entity.setTopic(topic);
    RoomEntity saved = roomRepository.save(entity);
    log.info(
        "New round started: roomId={}, round={}, topic={}", roomId, saved.getRoundNumber(), topic);
    eventPublisher.publishEvent(new RoundStartedEvent(roomId, saved.getRoundNumber()));
    return roomEntityMapper.toDomain(saved);
  }

  public record AutoAssignedEstimate(UUID taskId, String taskTitle, String finalEstimate) {}

  @Transactional
  public FinishSessionResult finishSession(UUID roomId, boolean revealVotes) {
    RoomEntity entity =
        roomRepository
            .findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));

    if (RoomStatus.CLOSED.name().equals(entity.getStatus())) {
      throw new IllegalStateException("Room is already finished");
    }

    // LIVE rooms: save current round snapshot before finishing
    if (RoomType.LIVE.name().equals(entity.getRoomType())
        && RoomStatus.OPEN.name().equals(entity.getStatus())) {
      TaskEntity phantom =
          taskRepository
              .findByRoomIdAndTitle(roomId, PHANTOM_TASK_TITLE)
              .orElseThrow(() -> new ResourceNotFoundException("Phantom task for room", roomId));
      List<EstimateEntity> estimates = estimateRepository.findByTaskId(phantom.getId());
      roundHistoryService.saveRoundSnapshot(entity, estimates);
      log.info(
          "Round snapshot saved on finish: roomId={}, round={}", roomId, entity.getRoundNumber());
    }

    // Auto-assign final estimates for tasks without one
    List<AutoAssignedEstimate> autoAssigned = autoAssignFinalEstimates(roomId);

    entity.setStatus(RoomStatus.CLOSED.name());
    RoomEntity saved = roomRepository.save(entity);
    log.info(
        "Session finished: id={}, revealVotes={}, autoAssigned={}",
        roomId,
        revealVotes,
        autoAssigned.size());
    return new FinishSessionResult(roomEntityMapper.toDomain(saved), autoAssigned);
  }

  public record FinishSessionResult(Room room, List<AutoAssignedEstimate> autoAssigned) {}

  private List<AutoAssignedEstimate> autoAssignFinalEstimates(UUID roomId) {
    List<TaskEntity> tasks =
        taskRepository.findByRoomId(roomId).stream()
            .filter(t -> !PHANTOM_TASK_TITLE.equals(t.getTitle()))
            .filter(t -> t.getFinalEstimate() == null)
            .toList();

    if (tasks.isEmpty()) {
      return List.of();
    }

    List<UUID> taskIds = tasks.stream().map(TaskEntity::getId).toList();
    Map<UUID, List<EstimateEntity>> estimatesByTask =
        estimateRepository.findByTaskIds(taskIds).stream()
            .collect(Collectors.groupingBy(e -> e.getTask().getId()));

    List<AutoAssignedEstimate> result = new ArrayList<>();
    List<TaskEntity> modified = new ArrayList<>();
    for (TaskEntity task : tasks) {
      List<EstimateEntity> estimates = estimatesByTask.getOrDefault(task.getId(), List.of());
      List<String> points = estimates.stream().map(EstimateEntity::getStoryPoints).toList();
      Double median = EstimationStats.computeMedian(points);
      if (median != null) {
        StoryPoints nearest = StoryPoints.nearestUp(median);
        task.setFinalEstimate(nearest.getValue());
        modified.add(task);
        result.add(new AutoAssignedEstimate(task.getId(), task.getTitle(), nearest.getValue()));
        log.info(
            "Auto-assigned final estimate: taskId={}, median={}, assigned={}",
            task.getId(),
            median,
            nearest.getValue());
      }
    }
    if (!modified.isEmpty()) {
      taskRepository.saveAll(modified);
    }
    return result;
  }

  @Transactional
  public List<Room> closeExpiredRooms() {
    log.trace("Checking for expired rooms");
    List<RoomEntity> expired =
        roomRepository.findExpired(RoomStatus.OPEN.name(), RoomType.ASYNC.name(), Instant.now());

    if (expired.isEmpty()) {
      return List.of();
    }

    expired.forEach(r -> r.setStatus(RoomStatus.CLOSED.name()));
    return roomEntityMapper.toDomainList(roomRepository.saveAll(expired));
  }

  @Transactional
  public void deleteRoom(Room room) {
    RoomEntity ref = entityManager.getReference(RoomEntity.class, room.id());
    roomRepository.delete(ref);
    log.info("Room deleted: id={}, slug={}", room.id(), room.slug());
  }

  public long countOpenRooms() {
    return roomRepository.countByStatus(RoomStatus.OPEN.name());
  }

  private String generateSlug() {
    StringBuilder sb = new StringBuilder(SLUG_SEGMENT_LENGTH * 2 + 1);
    for (int i = 0; i < SLUG_SEGMENT_LENGTH; i++) {
      sb.append(SLUG_CHARS.charAt(RANDOM.nextInt(SLUG_CHARS.length())));
    }
    sb.append('-');
    for (int i = 0; i < SLUG_SEGMENT_LENGTH; i++) {
      sb.append(SLUG_CHARS.charAt(RANDOM.nextInt(SLUG_CHARS.length())));
    }
    return sb.toString();
  }
}
