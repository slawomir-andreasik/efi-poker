package com.andreasik.efipoker.estimation.estimate;

import com.andreasik.efipoker.api.EstimatesApi;
import com.andreasik.efipoker.api.model.EstimateResponse;
import com.andreasik.efipoker.api.model.SubmitEstimateRequest;
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
      UUID taskId, SubmitEstimateRequest submitEstimateRequest, UUID xParticipantId) {
    log.debug("PUT /tasks/{}/estimate participantId={}", taskId, xParticipantId);

    String storyPoints = submitEstimateRequest.getStoryPoints().getValue();
    String comment = submitEstimateRequest.getComment();

    Estimate estimate =
        estimateService.submitEstimate(taskId, xParticipantId, storyPoints, comment);
    return ResponseEntity.ok(estimateMapper.toResponse(estimate));
  }

  @Override
  public ResponseEntity<Void> deleteEstimate(UUID taskId, UUID xParticipantId) {
    log.debug("DELETE /tasks/{}/estimate participantId={}", taskId, xParticipantId);
    estimateService.deleteEstimate(taskId, xParticipantId);
    return ResponseEntity.noContent().build();
  }
}
