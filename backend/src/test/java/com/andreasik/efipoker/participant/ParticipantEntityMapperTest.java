package com.andreasik.efipoker.participant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.andreasik.efipoker.auth.UserEntityMapper;
import com.andreasik.efipoker.project.ProjectEntity;
import com.andreasik.efipoker.project.ProjectEntityMapperImpl;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("ParticipantEntityMapper")
class ParticipantEntityMapperTest extends BaseUnitTest {

  private final ParticipantEntityMapper mapper =
      new ParticipantEntityMapperImpl(
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
      ParticipantEntity entity =
          ParticipantEntity.builder()
              .id(id)
              .project(project)
              .nickname("Alice")
              .createdAt(now)
              .build();

      // Act
      Participant result = mapper.toDomain(entity);

      // Assert
      assertAll(
          () -> assertThat(result.id()).isEqualTo(id),
          () -> assertThat(result.project()).isNotNull(),
          () -> assertThat(result.project().name()).isEqualTo("Project"),
          () -> assertThat(result.nickname()).isEqualTo("Alice"),
          () -> assertThat(result.invitedRoomIds()).isNull(),
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
      Participant domain = Participant.builder().id(id).nickname("Alice").createdAt(now).build();

      // Act
      ParticipantEntity result = mapper.toEntity(domain);

      // Assert
      assertAll(
          () -> assertThat(result.getId()).isEqualTo(id),
          () -> assertThat(result.getNickname()).isEqualTo("Alice"),
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
      ParticipantEntity entity1 =
          ParticipantEntity.builder()
              .id(UUID.randomUUID())
              .project(project)
              .nickname("Alice")
              .build();
      ParticipantEntity entity2 =
          ParticipantEntity.builder()
              .id(UUID.randomUUID())
              .project(project)
              .nickname("Bob")
              .build();

      // Act
      List<Participant> result = mapper.toDomainList(List.of(entity1, entity2));

      // Assert
      assertAll(
          () -> assertThat(result).hasSize(2),
          () -> assertThat(result.get(0).nickname()).isEqualTo("Alice"),
          () -> assertThat(result.get(1).nickname()).isEqualTo("Bob"));
    }

    @Test
    void should_return_null_for_null() {
      assertThat(mapper.toDomainList(null)).isNull();
    }
  }
}
