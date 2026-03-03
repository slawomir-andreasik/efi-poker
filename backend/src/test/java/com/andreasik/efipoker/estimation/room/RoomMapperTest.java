package com.andreasik.efipoker.estimation.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.andreasik.efipoker.api.model.RoomResponse;
import com.andreasik.efipoker.api.model.RoomSlugResponse;
import com.andreasik.efipoker.api.model.RoomType;
import com.andreasik.efipoker.project.Project;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("RoomMapper")
class RoomMapperTest extends BaseUnitTest {

  private final RoomMapper mapper = Mappers.getMapper(RoomMapper.class);

  @Nested
  @DisplayName("toResponse")
  class ToResponse {

    @Test
    void should_map_all_fields() {
      // Arrange
      UUID id = UUID.randomUUID();
      UUID projectId = UUID.randomUUID();
      Project project = Project.builder().id(projectId).build();
      Instant now = Instant.now();
      Instant deadline = now.plus(7, ChronoUnit.DAYS);
      Room room =
          Room.builder()
              .id(id)
              .project(project)
              .slug("X4P-Q2R")
              .title("Sprint 1 Planning")
              .description("Estimate sprint items")
              .roomType("ASYNC")
              .deadline(deadline)
              .status("OPEN")
              .createdAt(now)
              .build();

      // Act
      RoomResponse response = mapper.toResponse(room);

      // Assert
      assertAll(
          () -> assertThat(response.getId()).isEqualTo(id),
          () -> assertThat(response.getSlug()).isEqualTo("X4P-Q2R"),
          () -> assertThat(response.getProjectId()).isEqualTo(projectId),
          () -> assertThat(response.getTitle()).isEqualTo("Sprint 1 Planning"),
          () -> assertThat(response.getDescription()).isEqualTo("Estimate sprint items"),
          () -> assertThat(response.getRoomType()).isEqualTo(RoomType.ASYNC),
          () -> assertThat(response.getDeadline()).isEqualTo(deadline),
          () ->
              assertThat(response.getStatus())
                  .isEqualTo(com.andreasik.efipoker.api.model.RoomStatus.OPEN),
          () -> assertThat(response.getCreatedAt()).isEqualTo(now));
    }

    @Test
    void should_return_null_for_null() {
      // Act
      RoomResponse response = mapper.toResponse(null);

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
      Project project = Project.builder().id(UUID.randomUUID()).build();
      Room room1 =
          Room.builder()
              .id(UUID.randomUUID())
              .project(project)
              .title("Room 1")
              .roomType("ASYNC")
              .status("OPEN")
              .createdAt(Instant.now())
              .build();
      Room room2 =
          Room.builder()
              .id(UUID.randomUUID())
              .project(project)
              .title("Room 2")
              .roomType("LIVE")
              .status("REVEALED")
              .createdAt(Instant.now())
              .build();

      // Act
      List<RoomResponse> responses = mapper.toResponseList(List.of(room1, room2));

      // Assert
      assertAll(
          () -> assertThat(responses).hasSize(2),
          () -> assertThat(responses.get(0).getTitle()).isEqualTo("Room 1"),
          () -> assertThat(responses.get(1).getTitle()).isEqualTo("Room 2"));
    }
  }

  @Nested
  @DisplayName("toSlugResponse")
  class ToSlugResponse {

    @Test
    void should_map_all_fields() {
      // Arrange
      UUID id = UUID.randomUUID();
      Project project =
          Project.builder().id(UUID.randomUUID()).slug("my-project").name("My Project").build();
      Room room =
          Room.builder()
              .id(id)
              .project(project)
              .slug("A3X-K7B")
              .title("Sprint 1 Planning")
              .status("OPEN")
              .roomType("ASYNC")
              .createdAt(Instant.now())
              .build();

      // Act
      RoomSlugResponse response = mapper.toSlugResponse(room);

      // Assert
      assertAll(
          () -> assertThat(response.getRoomId()).isEqualTo(id),
          () -> assertThat(response.getRoomTitle()).isEqualTo("Sprint 1 Planning"),
          () -> assertThat(response.getRoomSlug()).isEqualTo("A3X-K7B"),
          () -> assertThat(response.getProjectSlug()).isEqualTo("my-project"),
          () -> assertThat(response.getProjectName()).isEqualTo("My Project"));
    }

    @Test
    void should_return_null_for_null() {
      assertThat(mapper.toSlugResponse(null)).isNull();
    }
  }

  @Nested
  @DisplayName("mapStatus")
  class MapStatus {

    @Test
    void should_map_open_status() {
      assertThat(mapper.mapStatus("OPEN"))
          .isEqualTo(com.andreasik.efipoker.api.model.RoomStatus.OPEN);
    }

    @Test
    void should_map_revealed_status() {
      assertThat(mapper.mapStatus("REVEALED"))
          .isEqualTo(com.andreasik.efipoker.api.model.RoomStatus.REVEALED);
    }

    @Test
    void should_map_closed_status() {
      assertThat(mapper.mapStatus("CLOSED"))
          .isEqualTo(com.andreasik.efipoker.api.model.RoomStatus.CLOSED);
    }

    @Test
    void should_return_null_for_null_status() {
      assertThat(mapper.mapStatus(null)).isNull();
    }
  }

  @Nested
  @DisplayName("mapRoomType")
  class MapRoomType {

    @Test
    void should_map_async_type() {
      assertThat(mapper.mapRoomType("ASYNC")).isEqualTo(RoomType.ASYNC);
    }

    @Test
    void should_map_live_type() {
      assertThat(mapper.mapRoomType("LIVE")).isEqualTo(RoomType.LIVE);
    }

    @Test
    void should_return_null_for_null_type() {
      assertThat(mapper.mapRoomType(null)).isNull();
    }
  }
}
