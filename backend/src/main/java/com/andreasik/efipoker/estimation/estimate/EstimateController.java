package com.andreasik.efipoker.estimation.estimate;

import com.andreasik.efipoker.api.EstimatesApi;
import com.andreasik.efipoker.api.model.EstimateResponse;
import com.andreasik.efipoker.api.model.SubmitEstimateRequest;
import com.andreasik.efipoker.estimation.task.TaskEntity;
import com.andreasik.efipoker.estimation.task.TaskRepository;
import com.andreasik.efipoker.participant.ParticipantApi;
import com.andreasik.efipoker.shared.exception.ResourceNotFoundException;
import com.andreasik.efipoker.shared.exception.UnauthorizedException;
import com.andreasik.efipoker.shared.security.SecurityUtils;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class EstimateController implements EstimatesApi {

  private final EstimateService estimateService;
  private final EstimateMapper estimateMapper;
  private final TaskRepository taskRepository;
  private final ParticipantApi participantApi;

  @Override
  public ResponseEntity<EstimateResponse> submitEstimate(
      UUID taskId, SubmitEstimateRequest submitEstimateRequest) {
    UUID participantId = resolveParticipantId(taskId);
    participantApi.validateParticipantNotArchived(participantId);
    log.debug("POST /tasks/{}/estimate participantId={}", taskId, participantId);

    String storyPoints =
        submitEstimateRequest.getStoryPoints() != null
            ? submitEstimateRequest.getStoryPoints().getValue()
            : null;
    String comment = submitEstimateRequest.getComment();

    Estimate estimate = estimateService.submitEstimate(taskId, participantId, storyPoints, comment);
    return ResponseEntity.ok(estimateMapper.toResponse(estimate));
  }

  @Override
  public ResponseEntity<Void> deleteEstimate(UUID taskId) {
    UUID participantId = resolveParticipantId(taskId);
    participantApi.validateParticipantNotArchived(participantId);
    log.debug("DELETE /tasks/{}/estimate participantId={}", taskId, participantId);
    estimateService.deleteEstimate(taskId, participantId);
    return ResponseEntity.noContent().build();
  }

  private UUID resolveParticipantId(UUID taskId) {
    UUID participantId = SecurityUtils.getCurrentParticipantId();
    if (participantId != null) {
      return participantId;
    }

    UUID userId = SecurityUtils.getCurrentUserId();
    if (userId != null) {
      TaskEntity task =
          taskRepository
              .findById(taskId)
              .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));
      UUID projectId = task.getRoom().getProject().getId();
      participantId = participantApi.findParticipantIdByProjectAndUser(projectId, userId);
      if (participantId != null) {
        return participantId;
      }
    }

    throw new UnauthorizedException("Participant identity required");
  }
}
