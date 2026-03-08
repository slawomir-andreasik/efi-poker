package com.andreasik.efipoker.estimation.estimate;

import com.andreasik.efipoker.estimation.task.TaskEntity;
import com.andreasik.efipoker.estimation.task.TaskRepository;
import com.andreasik.efipoker.participant.ParticipantApi;
import com.andreasik.efipoker.participant.ParticipantEntity;
import com.andreasik.efipoker.shared.event.EstimateSubmittedEvent;
import com.andreasik.efipoker.shared.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Transactional(readOnly = true)
public class EstimateService {

  private final EstimateRepository estimateRepository;
  private final TaskRepository taskRepository;
  private final ParticipantApi participantApi;
  private final EntityManager entityManager;
  private final EstimateEntityMapper estimateEntityMapper;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public Estimate submitEstimate(
      UUID taskId, UUID participantId, String storyPoints, String comment) {
    // Validate story points value
    StoryPoints.fromValue(storyPoints);

    Optional<EstimateEntity> existing =
        estimateRepository.findByTaskAndParticipant(taskId, participantId);

    if (existing.isPresent()) {
      EstimateEntity entity = existing.get();
      entity.setStoryPoints(storyPoints);
      entity.setComment(comment);
      EstimateEntity saved = estimateRepository.save(entity);
      log.info(
          "Estimate updated: taskId={}, participantId={}, sp={}",
          taskId,
          participantId,
          storyPoints);
      eventPublisher.publishEvent(new EstimateSubmittedEvent(taskId, participantId));
      return estimateEntityMapper.toDomain(saved);
    }

    TaskEntity task =
        taskRepository
            .findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));
    participantApi.validateParticipantExists(participantId);
    participantApi.validateParticipantBelongsToProject(
        participantId, task.getRoom().getProject().getId());
    ParticipantEntity participant =
        entityManager.getReference(ParticipantEntity.class, participantId);

    EstimateEntity entity =
        EstimateEntity.builder()
            .task(task)
            .participant(participant)
            .storyPoints(storyPoints)
            .comment(comment)
            .build();

    EstimateEntity saved = estimateRepository.save(entity);
    log.info(
        "Estimate submitted: taskId={}, participantId={}, sp={}",
        taskId,
        participantId,
        storyPoints);
    eventPublisher.publishEvent(new EstimateSubmittedEvent(taskId, participantId));
    return estimateEntityMapper.toDomain(saved);
  }

  public List<Estimate> getEstimatesForTask(UUID taskId) {
    log.debug("getEstimatesForTask: taskId={}", taskId);
    return estimateEntityMapper.toDomainList(estimateRepository.findByTaskId(taskId));
  }

  public Map<UUID, List<Estimate>> getEstimatesByTaskIds(Collection<UUID> taskIds) {
    if (taskIds.isEmpty()) {
      return Map.of();
    }
    return estimateEntityMapper.toDomainList(estimateRepository.findByTaskIds(taskIds)).stream()
        .collect(Collectors.groupingBy(e -> e.task().id()));
  }

  @Transactional
  public void deleteEstimate(UUID taskId, UUID participantId) {
    TaskEntity task =
        taskRepository
            .findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));
    participantApi.validateParticipantBelongsToProject(
        participantId, task.getRoom().getProject().getId());
    estimateRepository.deleteByTaskIdAndParticipantId(taskId, participantId);
    log.info("Estimate deleted: taskId={}, participantId={}", taskId, participantId);
  }
}
