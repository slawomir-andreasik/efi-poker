package com.andreasik.efipoker.estimation.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.andreasik.efipoker.auth.UserEntityMapper;
import com.andreasik.efipoker.estimation.room.RoomEntity;
import com.andreasik.efipoker.estimation.room.RoomEntityMapperImpl;
import com.andreasik.efipoker.project.ProjectEntity;
import com.andreasik.efipoker.project.ProjectEntityMapperImpl;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("TaskEntityMapper")
class TaskEntityMapperTest extends BaseUnitTest {

  private final TaskEntityMapper mapper =
      new TaskEntityMapperImpl(
          new RoomEntityMapperImpl(
              new ProjectEntityMapperImpl(Mappers.getMapper(UserEntityMapper.class))));

  @Nested
  @DisplayName("toDomain")
  class ToDomain {

    @Test
    void should_map_all_fields() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      ProjectEntity project =
          ProjectEntity.builder().id(UUID.randomUUID()).name("Project").slug("project").build();
      RoomEntity room =
          RoomEntity.builder()
              .id(UUID.randomUUID())
              .project(project)
              .title("Sprint 1")
              .status("OPEN")
              .roomType("ASYNC")
              .deadline(now.plus(7, ChronoUnit.DAYS))
              .build();
      TaskEntity entity =
          TaskEntity.builder()
              .id(id)
              .room(room)
              .title("Implement login")
              .description("Login page with OAuth")
              .sortOrder(3)
              .finalEstimate("8")
              .revealed(true)
              .active(false)
              .createdAt(now)
              .build();

      // Act
      Task result = mapper.toDomain(entity);

      // Assert
      assertAll(
          () -> assertThat(result.id()).isEqualTo(id),
          () -> assertThat(result.room()).isNotNull(),
          () -> assertThat(result.room().title()).isEqualTo("Sprint 1"),
          () -> assertThat(result.title()).isEqualTo("Implement login"),
          () -> assertThat(result.description()).isEqualTo("Login page with OAuth"),
          () -> assertThat(result.sortOrder()).isEqualTo(3),
          () -> assertThat(result.finalEstimate()).isEqualTo("8"),
          () -> assertThat(result.revealed()).isTrue(),
          () -> assertThat(result.active()).isFalse(),
          () -> assertThat(result.createdAt()).isEqualTo(now));
    }

    @Test
    void should_return_null_for_null() {
      assertThat(mapper.toDomain(null)).isNull();
    }
  }

  @Nested
  @DisplayName("toEntity")
  class ToEntity {

    @Test
    void should_map_all_fields() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      Task domain =
          Task.builder()
              .id(id)
              .title("Implement login")
              .description("Login page")
              .sortOrder(2)
              .finalEstimate("5")
              .revealed(false)
              .active(true)
              .createdAt(now)
              .build();

      // Act
      TaskEntity result = mapper.toEntity(domain);

      // Assert
      assertAll(
          () -> assertThat(result.getId()).isEqualTo(id),
          () -> assertThat(result.getTitle()).isEqualTo("Implement login"),
          () -> assertThat(result.getSortOrder()).isEqualTo(2),
          () -> assertThat(result.getFinalEstimate()).isEqualTo("5"),
          () -> assertThat(result.isRevealed()).isFalse(),
          () -> assertThat(result.isActive()).isTrue(),
          () -> assertThat(result.getCreatedAt()).isEqualTo(now));
    }

    @Test
    void should_return_null_for_null() {
      assertThat(mapper.toEntity(null)).isNull();
    }
  }

  @Nested
  @DisplayName("toDomainList")
  class ToDomainList {

    @Test
    void should_map_list() {
      // Arrange
      ProjectEntity project =
          ProjectEntity.builder().id(UUID.randomUUID()).name("Project").slug("project").build();
      RoomEntity room =
          RoomEntity.builder()
              .id(UUID.randomUUID())
              .project(project)
              .title("Sprint 1")
              .status("OPEN")
              .roomType("ASYNC")
              .build();
      TaskEntity entity1 =
          TaskEntity.builder()
              .id(UUID.randomUUID())
              .room(room)
              .title("Task A")
              .sortOrder(0)
              .build();
      TaskEntity entity2 =
          TaskEntity.builder()
              .id(UUID.randomUUID())
              .room(room)
              .title("Task B")
              .sortOrder(1)
              .build();

      // Act
      List<Task> result = mapper.toDomainList(List.of(entity1, entity2));

      // Assert
      assertAll(
          () -> assertThat(result).hasSize(2),
          () -> assertThat(result.get(0).title()).isEqualTo("Task A"),
          () -> assertThat(result.get(1).title()).isEqualTo("Task B"));
    }

    @Test
    void should_return_null_for_null() {
      assertThat(mapper.toDomainList(null)).isNull();
    }
  }
}
