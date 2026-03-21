package com.andreasik.efipoker.estimation.estimate;

import com.andreasik.efipoker.api.EstimatesApi;
import com.andreasik.efipoker.api.model.EstimateResponse;
import com.andreasik.efipoker.api.model.SubmitEstimateRequest;
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

  @Override
  public ResponseEntity<EstimateResponse> submitEstimate(
      UUID taskId, SubmitEstimateRequest submitEstimateRequest) {
    UUID participantId = resolveParticipantId();
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
    UUID participantId = resolveParticipantId();
    log.debug("DELETE /tasks/{}/estimate participantId={}", taskId, participantId);
    estimateService.deleteEstimate(taskId, participantId);
    return ResponseEntity.noContent().build();
  }

  private UUID resolveParticipantId() {
    UUID participantId = SecurityUtils.getCurrentParticipantId();
    if (participantId == null) {
      throw new UnauthorizedException("Participant identity required");
    }
    return participantId;
  }
}
