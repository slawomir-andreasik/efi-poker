package com.andreasik.efipoker.participant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.andreasik.efipoker.api.model.ParticipantResponse;
import com.andreasik.efipoker.project.Project;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("ParticipantMapper")
class ParticipantMapperTest extends BaseUnitTest {

  private final ParticipantMapper mapper = Mappers.getMapper(ParticipantMapper.class);

  @Nested
  @DisplayName("toResponse")
  class ToResponse {

    @Test
    void should_map_all_fields() {
      // Arrange
      UUID id = UUID.randomUUID();
      UUID roomId1 = UUID.randomUUID();
      UUID roomId2 = UUID.randomUUID();
      Instant now = Instant.now();
      Participant participant =
          Participant.builder()
              .id(id)
              .project(Project.builder().id(UUID.randomUUID()).build())
              .nickname("Alice")
              .invitedRoomIds(Set.of(roomId1, roomId2))
              .createdAt(now)
              .build();

      // Act
      ParticipantResponse response = mapper.toResponse(participant);

      // Assert
      assertAll(
          () -> assertThat(response.getId()).isEqualTo(id),
          () -> assertThat(response.getNickname()).isEqualTo("Alice"),
          () ->
              assertThat(response.getInvitedRoomIds()).containsExactlyInAnyOrder(roomId1, roomId2),
          () -> assertThat(response.getCreatedAt()).isEqualTo(now));
    }

    @Test
    void should_map_empty_invited_room_ids() {
      // Arrange
      Participant participant =
          Participant.builder()
              .id(UUID.randomUUID())
              .project(Project.builder().id(UUID.randomUUID()).build())
              .nickname("Bob")
              .invitedRoomIds(Set.of())
              .createdAt(Instant.now())
              .build();

      // Act
      ParticipantResponse response = mapper.toResponse(participant);

      // Assert
      assertThat(response.getInvitedRoomIds()).isEmpty();
    }

    @Test
    void should_return_null_for_null() {
      // Act
      ParticipantResponse response = mapper.toResponse(null);

      // Assert
      assertThat(response).isNull();
    }
  }

  @Nested
  @DisplayName("toResponseList")
  class ToResponseList {

    @Test
    void should_map_list() {
      // Arrange
      Participant p1 =
          Participant.builder()
              .id(UUID.randomUUID())
              .project(Project.builder().id(UUID.randomUUID()).build())
              .nickname("Alice")
              .createdAt(Instant.now())
              .build();
      Participant p2 =
          Participant.builder()
              .id(UUID.randomUUID())
              .project(Project.builder().id(UUID.randomUUID()).build())
              .nickname("Bob")
              .createdAt(Instant.now())
              .build();

      // Act
      List<ParticipantResponse> responses = mapper.toResponseList(List.of(p1, p2));

      // Assert
      assertAll(
          () -> assertThat(responses).hasSize(2),
          () -> assertThat(responses.get(0).getNickname()).isEqualTo("Alice"),
          () -> assertThat(responses.get(1).getNickname()).isEqualTo("Bob"));
    }
  }
}
