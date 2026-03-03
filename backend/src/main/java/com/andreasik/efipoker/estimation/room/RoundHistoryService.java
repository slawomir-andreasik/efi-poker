package com.andreasik.efipoker.estimation.room;

import com.andreasik.efipoker.api.model.RoundHistoryEntry;
import com.andreasik.efipoker.api.model.RoundHistoryVote;
import com.andreasik.efipoker.estimation.EstimationStats;
import com.andreasik.efipoker.estimation.estimate.EstimateEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Transactional(readOnly = true)
public class RoundHistoryService {

  private final RoundHistoryRepository roundHistoryRepository;
  private final ObjectMapper objectMapper;

  @Transactional
  public void saveRoundSnapshot(RoomEntity room, List<EstimateEntity> estimates) {
    log.debug("saveRoundSnapshot: roomId={}, round={}", room.getId(), room.getRoundNumber());
    List<VoteSnapshot> votes = buildVoteSnapshots(estimates);
    String votesJson = serializeVotes(votes);

    List<String> points = estimates.stream().map(EstimateEntity::getStoryPoints).toList();
    Double average = EstimationStats.computeAverage(points);
    Double median = EstimationStats.computeMedian(points);

    RoundHistoryEntity entry =
        RoundHistoryEntity.builder()
            .room(room)
            .roundNumber(room.getRoundNumber())
            .topic(room.getTopic())
            .averagePts(
                average != null
                    ? BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP)
                    : null)
            .medianPts(
                median != null
                    ? BigDecimal.valueOf(median).setScale(2, RoundingMode.HALF_UP)
                    : null)
            .voteCount(estimates.size())
            .votesJson(votesJson)
            .build();

    roundHistoryRepository.save(entry);
    log.info(
        "Round snapshot saved: roomId={}, round={}, votes={}",
        room.getId(),
        room.getRoundNumber(),
        estimates.size());
  }

  public List<RoundHistoryEntry> getHistory(UUID roomId) {
    log.debug("getHistory: roomId={}", roomId);
    return roundHistoryRepository.findByRoomId(roomId).stream().map(this::toResponse).toList();
  }

  private RoundHistoryEntry toResponse(RoundHistoryEntity entity) {
    List<RoundHistoryVote> votes = deserializeVotes(entity.getVotesJson());
    return new RoundHistoryEntry()
        .roundNumber(entity.getRoundNumber())
        .topic(entity.getTopic())
        .averagePts(entity.getAveragePts() != null ? entity.getAveragePts().doubleValue() : null)
        .medianPts(entity.getMedianPts() != null ? entity.getMedianPts().doubleValue() : null)
        .voteCount(entity.getVoteCount())
        .votes(votes)
        .completedAt(entity.getCompletedAt());
  }

  private List<VoteSnapshot> buildVoteSnapshots(List<EstimateEntity> estimates) {
    return estimates.stream()
        .map(e -> new VoteSnapshot(e.getParticipant().getNickname(), e.getStoryPoints()))
        .toList();
  }

  private String serializeVotes(List<VoteSnapshot> votes) {
    try {
      return objectMapper.writeValueAsString(votes);
    } catch (JsonProcessingException e) {
      log.warn("Failed to serialize votes, using empty array: {}", e.getMessage());
      return "[]";
    }
  }

  private List<RoundHistoryVote> deserializeVotes(String votesJson) {
    try {
      List<VoteSnapshot> snapshots =
          objectMapper.readValue(
              votesJson,
              objectMapper
                  .getTypeFactory()
                  .constructCollectionType(List.class, VoteSnapshot.class));
      return snapshots.stream()
          .map(s -> new RoundHistoryVote().nickname(s.nickname()).storyPoints(s.storyPoints()))
          .toList();
    } catch (JsonProcessingException e) {
      log.warn("Failed to deserialize votes: {}", e.getMessage());
      return List.of();
    }
  }

  private record VoteSnapshot(String nickname, String storyPoints) {}
}
