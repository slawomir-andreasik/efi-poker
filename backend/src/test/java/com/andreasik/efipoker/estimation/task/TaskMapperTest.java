package com.andreasik.efipoker.estimation.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.andreasik.efipoker.api.model.TaskResponse;
import com.andreasik.efipoker.estimation.room.Room;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("TaskMapper")
class TaskMapperTest extends BaseUnitTest {

  private final TaskMapper mapper = Mappers.getMapper(TaskMapper.class);

  @Nested
  @DisplayName("toResponse")
  class ToResponse {

    @Test
    void should_map_all_fields() {
      // Arrange
      UUID id = UUID.randomUUID();
      UUID roomId = UUID.randomUUID();
      Room room = Room.builder().id(roomId).build();
      Instant now = Instant.now();
      Task task =
          Task.builder()
              .id(id)
              .room(room)
              .title("Implement login")
              .description("OAuth2 + JWT flow")
              .sortOrder(3)
              .revealed(true)
              .active(false)
              .createdAt(now)
              .build();

      // Act
      TaskResponse response = mapper.toResponse(task);

      // Assert
      assertAll(
          () -> assertThat(response.getId()).isEqualTo(id),
          () -> assertThat(response.getRoomId()).isEqualTo(roomId),
          () -> assertThat(response.getTitle()).isEqualTo("Implement login"),
          () -> assertThat(response.getDescription()).isEqualTo("OAuth2 + JWT flow"),
          () -> assertThat(response.getSortOrder()).isEqualTo(3),
          () -> assertThat(response.getRevealed()).isTrue(),
          () -> assertThat(response.getActive()).isFalse(),
          () -> assertThat(response.getCreatedAt()).isEqualTo(now));
    }

    @Test
    void should_return_null_for_null() {
      // Act
      TaskResponse response = mapper.toResponse(null);

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
      Room room1 = Room.builder().id(UUID.randomUUID()).build();
      Room room2 = Room.builder().id(UUID.randomUUID()).build();
      Task t1 =
          Task.builder()
              .id(UUID.randomUUID())
              .room(room1)
              .title("Task A")
              .sortOrder(1)
              .createdAt(Instant.now())
              .build();
      Task t2 =
          Task.builder()
              .id(UUID.randomUUID())
              .room(room2)
              .title("Task B")
              .sortOrder(2)
              .createdAt(Instant.now())
              .build();

      // Act
      List<TaskResponse> responses = mapper.toResponseList(List.of(t1, t2));

      // Assert
      assertAll(
          () -> assertThat(responses).hasSize(2),
          () -> assertThat(responses.get(0).getTitle()).isEqualTo("Task A"),
          () -> assertThat(responses.get(1).getTitle()).isEqualTo("Task B"));
    }
  }
}
