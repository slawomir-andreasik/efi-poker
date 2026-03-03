package com.andreasik.efipoker.estimation.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.andreasik.efipoker.auth.UserEntityMapper;
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

@DisplayName("RoomEntityMapper")
class RoomEntityMapperTest extends BaseUnitTest {

  private final RoomEntityMapper mapper =
      new RoomEntityMapperImpl(
          new ProjectEntityMapperImpl(Mappers.getMapper(UserEntityMapper.class)));

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
      RoomEntity entity =
          RoomEntity.builder()
              .id(id)
              .project(project)
              .slug("A3X-K7B")
              .title("Sprint 1 Planning")
              .description("Estimate sprint items")
              .roomType("ASYNC")
              .deadline(now.plus(7, ChronoUnit.DAYS))
              .status("OPEN")
              .createdAt(now)
              .build();

      // Act
      Room result = mapper.toDomain(entity);

      // Assert
      assertAll(
          () -> assertThat(result.id()).isEqualTo(id),
          () -> assertThat(result.project()).isNotNull(),
          () -> assertThat(result.project().name()).isEqualTo("Project"),
          () -> assertThat(result.slug()).isEqualTo("A3X-K7B"),
          () -> assertThat(result.title()).isEqualTo("Sprint 1 Planning"),
          () -> assertThat(result.description()).isEqualTo("Estimate sprint items"),
          () -> assertThat(result.roomType()).isEqualTo("ASYNC"),
          () -> assertThat(result.status()).isEqualTo("OPEN"),
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
      Room domain =
          Room.builder()
              .id(id)
              .slug("B2Y-M9N")
              .title("Sprint 1 Planning")
              .description("Estimate sprint items")
              .roomType("LIVE")
              .status("OPEN")
              .createdAt(now)
              .build();

      // Act
      RoomEntity result = mapper.toEntity(domain);

      // Assert
      assertAll(
          () -> assertThat(result.getId()).isEqualTo(id),
          () -> assertThat(result.getSlug()).isEqualTo("B2Y-M9N"),
          () -> assertThat(result.getTitle()).isEqualTo("Sprint 1 Planning"),
          () -> assertThat(result.getRoomType()).isEqualTo("LIVE"),
          () -> assertThat(result.getStatus()).isEqualTo("OPEN"),
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
      RoomEntity entity1 =
          RoomEntity.builder()
              .id(UUID.randomUUID())
              .project(project)
              .title("Room 1")
              .status("OPEN")
              .roomType("ASYNC")
              .build();
      RoomEntity entity2 =
          RoomEntity.builder()
              .id(UUID.randomUUID())
              .project(project)
              .title("Room 2")
              .status("REVEALED")
              .roomType("LIVE")
              .build();

      // Act
      List<Room> result = mapper.toDomainList(List.of(entity1, entity2));

      // Assert
      assertAll(
          () -> assertThat(result).hasSize(2),
          () -> assertThat(result.get(0).title()).isEqualTo("Room 1"),
          () -> assertThat(result.get(1).title()).isEqualTo("Room 2"));
    }

    @Test
    void should_return_null_for_null() {
      assertThat(mapper.toDomainList(null)).isNull();
    }
  }
}
