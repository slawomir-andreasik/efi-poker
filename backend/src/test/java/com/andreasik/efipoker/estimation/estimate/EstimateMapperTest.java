package com.andreasik.efipoker.estimation.estimate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.andreasik.efipoker.api.model.EstimateResponse;
import com.andreasik.efipoker.participant.Participant;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("EstimateMapper")
class EstimateMapperTest extends BaseUnitTest {

  private final EstimateMapper mapper = Mappers.getMapper(EstimateMapper.class);

  @Nested
  @DisplayName("toResponse")
  class ToResponse {

    @Test
    void should_map_all_fields() {
      // Arrange
      UUID id = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();
      Participant participant = Participant.builder().id(participantId).nickname("Alice").build();
      Instant now = Instant.now();
      Estimate estimate =
          Estimate.builder()
              .id(id)
              .participant(participant)
              .storyPoints("8")
              .comment("Complex task")
              .createdAt(now)
              .updatedAt(now)
              .build();

      // Act
      EstimateResponse response = mapper.toResponse(estimate);

      // Assert
      assertAll(
          () -> assertThat(response.getId()).isEqualTo(id),
          () -> assertThat(response.getParticipantId()).isEqualTo(participantId),
          () -> assertThat(response.getStoryPoints()).isEqualTo("8"),
          () -> assertThat(response.getComment()).isEqualTo("Complex task"),
          () -> assertThat(response.getCreatedAt()).isEqualTo(now),
          () -> assertThat(response.getParticipantNickname()).isEqualTo("Alice"));
    }

    @Test
    void should_map_participant_nickname() {
      // Arrange
      Participant participant = Participant.builder().id(UUID.randomUUID()).nickname("Bob").build();
      Estimate estimate =
          Estimate.builder()
              .id(UUID.randomUUID())
              .participant(participant)
              .storyPoints("5")
              .createdAt(Instant.now())
              .updatedAt(Instant.now())
              .build();

      // Act
      EstimateResponse response = mapper.toResponse(estimate);

      // Assert
      assertThat(response.getParticipantNickname()).isEqualTo("Bob");
    }
  }

  @Nested
  @DisplayName("NullHandling")
  class NullHandling {

    @Test
    void should_return_null_for_null() {
      // Act
      EstimateResponse response = mapper.toResponse(null);

      // Assert
      assertThat(response).isNull();
    }
  }
}
