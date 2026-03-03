package com.andreasik.efipoker.estimation.estimate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.andreasik.efipoker.auth.UserEntityMapper;
import com.andreasik.efipoker.estimation.room.RoomEntity;
import com.andreasik.efipoker.estimation.room.RoomEntityMapperImpl;
import com.andreasik.efipoker.estimation.task.TaskEntity;
import com.andreasik.efipoker.estimation.task.TaskEntityMapperImpl;
import com.andreasik.efipoker.participant.ParticipantEntity;
import com.andreasik.efipoker.participant.ParticipantEntityMapperImpl;
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

@DisplayName("EstimateEntityMapper")
class EstimateEntityMapperTest extends BaseUnitTest {

  private final EstimateEntityMapper mapper;

  EstimateEntityMapperTest() {
    // Wire the full dependency chain via constructors
    UserEntityMapper userMapper = Mappers.getMapper(UserEntityMapper.class);
    ProjectEntityMapperImpl projectMapper = new ProjectEntityMapperImpl(userMapper);
    RoomEntityMapperImpl roomMapper = new RoomEntityMapperImpl(projectMapper);
    TaskEntityMapperImpl taskMapper = new TaskEntityMapperImpl(roomMapper);
    ParticipantEntityMapperImpl participantMapper = new ParticipantEntityMapperImpl(projectMapper);
    mapper = new EstimateEntityMapperImpl(taskMapper, participantMapper);
  }

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
      TaskEntity task =
          TaskEntity.builder()
              .id(UUID.randomUUID())
              .room(room)
              .title("Task A")
              .sortOrder(0)
              .build();
      ParticipantEntity participant =
          ParticipantEntity.builder()
              .id(UUID.randomUUID())
              .project(project)
              .nickname("Alice")
              .build();
      EstimateEntity entity =
          EstimateEntity.builder()
              .id(id)
              .task(task)
              .participant(participant)
              .storyPoints("8")
              .comment("Complex feature")
              .createdAt(now)
              .updatedAt(now)
              .build();

      // Act
      Estimate result = mapper.toDomain(entity);

      // Assert
      assertAll(
          () -> assertThat(result.id()).isEqualTo(id),
          () -> assertThat(result.task()).isNotNull(),
          () -> assertThat(result.task().title()).isEqualTo("Task A"),
          () -> assertThat(result.participant()).isNotNull(),
          () -> assertThat(result.participant().nickname()).isEqualTo("Alice"),
          () -> assertThat(result.storyPoints()).isEqualTo("8"),
          () -> assertThat(result.comment()).isEqualTo("Complex feature"),
          () -> assertThat(result.createdAt()).isEqualTo(now),
          () -> assertThat(result.updatedAt()).isEqualTo(now));
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
      Estimate domain =
          Estimate.builder()
              .id(id)
              .storyPoints("5")
              .comment("Simple task")
              .createdAt(now)
              .updatedAt(now)
              .build();

      // Act
      EstimateEntity result = mapper.toEntity(domain);

      // Assert
      assertAll(
          () -> assertThat(result.getId()).isEqualTo(id),
          () -> assertThat(result.getStoryPoints()).isEqualTo("5"),
          () -> assertThat(result.getComment()).isEqualTo("Simple task"),
          () -> assertThat(result.getCreatedAt()).isEqualTo(now),
          () -> assertThat(result.getUpdatedAt()).isEqualTo(now));
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
      TaskEntity task =
          TaskEntity.builder().id(UUID.randomUUID()).room(room).title("Task A").build();
      ParticipantEntity participant =
          ParticipantEntity.builder()
              .id(UUID.randomUUID())
              .project(project)
              .nickname("Alice")
              .build();
      EstimateEntity entity1 =
          EstimateEntity.builder()
              .id(UUID.randomUUID())
              .task(task)
              .participant(participant)
              .storyPoints("5")
              .build();
      EstimateEntity entity2 =
          EstimateEntity.builder()
              .id(UUID.randomUUID())
              .task(task)
              .participant(participant)
              .storyPoints("8")
              .build();

      // Act
      List<Estimate> result = mapper.toDomainList(List.of(entity1, entity2));

      // Assert
      assertAll(
          () -> assertThat(result).hasSize(2),
          () -> assertThat(result.get(0).storyPoints()).isEqualTo("5"),
          () -> assertThat(result.get(1).storyPoints()).isEqualTo("8"));
    }

    @Test
    void should_return_null_for_null() {
      assertThat(mapper.toDomainList(null)).isNull();
    }
  }
}
