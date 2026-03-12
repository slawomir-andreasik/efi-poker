package com.andreasik.efipoker.estimation.room;

import com.andreasik.efipoker.estimation.estimate.EstimateEntity;
import com.andreasik.efipoker.estimation.estimate.EstimateRepository;
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
import java.util.List;
import java.util.UUID;
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

  /**
   * Internal sentinel task title used by LIVE rooms for round-based voting. Never exposed to users.
   */
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
  public Room createRoom(
      UUID projectId,
      String title,
      String description,
      String roomType,
      Instant deadline,
      boolean autoRevealOnDeadline) {
    projectApi.validateProjectExists(projectId);
    ProjectEntity project = entityManager.getReference(ProjectEntity.class, projectId);

    if ("ASYNC".equals(roomType) && deadline == null) {
      log.warn("ASYNC room without deadline: projectId={}", projectId);
      throw new IllegalArgumentException("ASYNC rooms require a deadline");
    }

    RoomEntity saved = null;
    for (int attempt = 0; attempt < MAX_SLUG_RETRIES; attempt++) {
      String slug = generateSlug();
      RoomEntity entity =
          RoomEntity.builder()
              .project(project)
              .slug(slug)
              .title(title)
              .description(description)
              .roomType(roomType)
              .deadline(deadline)
              .autoRevealOnDeadline(autoRevealOnDeadline)
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
        projectId,
        title,
        roomType);
    eventPublisher.publishEvent(new RoomCreatedEvent(saved.getId(), roomType));

    if ("LIVE".equals(roomType)) {
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

  @Transactional
  public Room updateRoom(
      UUID id, String title, String description, Instant deadline, String topic) {
    RoomEntity entity =
        roomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Room", id));

    if (title != null) {
      entity.setTitle(title);
    }
    if (description != null) {
      entity.setDescription(description);
    }
    if (deadline != null) {
      entity.setDeadline(deadline);
    }
    if (topic != null) {
      entity.setTopic(topic.isBlank() ? null : topic);
    }

    RoomEntity saved = roomRepository.save(entity);
    log.info("Room updated: id={}", id);
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
    if (!"LIVE".equals(entity.getRoomType())) {
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

  @Transactional
  public List<Room> closeExpiredRooms() {
    log.trace("Checking for expired rooms");
    List<RoomEntity> expired =
        roomRepository.findExpired(RoomStatus.OPEN.name(), "ASYNC", Instant.now());

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
