package com.andreasik.efipoker.participant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

import com.andreasik.efipoker.auth.UserEntity;
import com.andreasik.efipoker.project.ProjectApi;
import com.andreasik.efipoker.project.ProjectEntity;
import com.andreasik.efipoker.shared.exception.ResourceNotFoundException;
import com.andreasik.efipoker.shared.exception.UnauthorizedException;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("ParticipantService")
class ParticipantServiceTest extends BaseUnitTest {

  @Mock private ParticipantRepository participantRepository;
  @Mock private ProjectApi projectApi;
  @Mock private EntityManager entityManager;
  @Mock private ParticipantEntityMapper participantEntityMapper;
  @InjectMocks private ParticipantService participantService;

  @Nested
  @DisplayName("joinProject")
  class JoinProject {

    private final UUID projectId = UUID.randomUUID();
    private final UUID roomId = UUID.randomUUID();
    private final ProjectEntity projectEntity = ProjectEntity.builder().id(projectId).build();

    @Test
    void should_create_new_participant_with_room_access() {
      // Arrange
      ParticipantEntity newEntity =
          ParticipantEntity.builder()
              .id(UUID.randomUUID())
              .project(projectEntity)
              .nickname("Alice")
              .build();
      Participant domain = Participant.builder().id(newEntity.getId()).nickname("Alice").build();

      given(participantRepository.findByProjectIdAndNickname(projectId, "Alice"))
          .willReturn(Optional.empty());
      given(entityManager.getReference(ProjectEntity.class, projectId)).willReturn(projectEntity);
      given(participantRepository.save(any())).willReturn(newEntity);
      given(participantEntityMapper.toDomain(newEntity)).willReturn(domain);
      given(participantRepository.existsRoomInProject(roomId, projectId)).willReturn(true);
      given(participantRepository.findInvitedRoomIds(newEntity.getId())).willReturn(Set.of(roomId));

      // Act
      Participant result = participantService.joinProject(projectId, "Alice", null, roomId);

      // Assert
      assertThat(result.invitedRoomIds()).containsExactly(roomId);
      then(participantRepository).should().addRoomAccess(newEntity.getId(), roomId);
      // mutant #53: validateProjectExists must be called when creating a new participant
      then(projectApi).should().validateProjectExists(projectId);
    }

    @Test
    void should_create_new_participant_with_user_reference_when_userId_provided() {
      // Arrange - covers mutant #61: userId != null conditional when building new entity
      UUID userId = UUID.randomUUID();
      UserEntity userRef = new UserEntity();
      ParticipantEntity newEntity =
          ParticipantEntity.builder()
              .id(UUID.randomUUID())
              .project(projectEntity)
              .nickname("Alice")
              .user(userRef)
              .build();
      Participant domain = Participant.builder().id(newEntity.getId()).nickname("Alice").build();

      given(participantRepository.findByProjectIdAndNickname(projectId, "Alice"))
          .willReturn(Optional.empty());
      given(entityManager.getReference(ProjectEntity.class, projectId)).willReturn(projectEntity);
      given(entityManager.getReference(UserEntity.class, userId)).willReturn(userRef);
      given(participantRepository.save(any())).willReturn(newEntity);
      given(participantEntityMapper.toDomain(newEntity)).willReturn(domain);
      given(participantRepository.findInvitedRoomIds(newEntity.getId())).willReturn(Set.of());

      // Act
      participantService.joinProject(projectId, "Alice", userId, null);

      // Assert: user reference must be fetched and saved as part of the new entity
      then(entityManager).should().getReference(UserEntity.class, userId);
      ArgumentCaptor<ParticipantEntity> captor = ArgumentCaptor.forClass(ParticipantEntity.class);
      verify(participantRepository).save(captor.capture());
      assertThat(captor.getValue().getUser()).isEqualTo(userRef);
    }

    @Test
    void should_clear_room_access_on_project_wide_join() {
      // Arrange
      ParticipantEntity entity =
          ParticipantEntity.builder()
              .id(UUID.randomUUID())
              .project(projectEntity)
              .nickname("Alice")
              .build();
      Participant domain = Participant.builder().id(entity.getId()).nickname("Alice").build();

      given(participantRepository.findByProjectIdAndNickname(projectId, "Alice"))
          .willReturn(Optional.of(entity));
      given(participantEntityMapper.toDomain(entity)).willReturn(domain);
      given(participantRepository.findInvitedRoomIds(entity.getId())).willReturn(Set.of());

      // Act
      Participant result = participantService.joinProject(projectId, "Alice", null, null);

      // Assert
      assertThat(result.invitedRoomIds()).isEmpty();
      then(participantRepository).should().clearRoomAccess(entity.getId());
    }

    @Test
    void should_throw_not_found_for_invalid_room() {
      // Arrange
      ParticipantEntity entity =
          ParticipantEntity.builder()
              .id(UUID.randomUUID())
              .project(projectEntity)
              .nickname("Alice")
              .build();

      given(participantRepository.findByProjectIdAndNickname(projectId, "Alice"))
          .willReturn(Optional.of(entity));
      given(participantRepository.existsRoomInProject(roomId, projectId)).willReturn(false);

      // Act & Assert
      assertThatThrownBy(() -> participantService.joinProject(projectId, "Alice", null, roomId))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_backfill_user_id_on_existing_participant() {
      // Arrange - covers mutant #42: setUser must actually be called on the entity
      UUID userId = UUID.randomUUID();
      ParticipantEntity entity =
          ParticipantEntity.builder()
              .id(UUID.randomUUID())
              .project(projectEntity)
              .nickname("Alice")
              .build();
      UserEntity userRef = new UserEntity();
      Participant domain = Participant.builder().id(entity.getId()).nickname("Alice").build();

      given(participantRepository.findByProjectIdAndNickname(projectId, "Alice"))
          .willReturn(Optional.of(entity));
      given(entityManager.getReference(UserEntity.class, userId)).willReturn(userRef);
      given(participantRepository.save(entity)).willReturn(entity);
      given(participantEntityMapper.toDomain(entity)).willReturn(domain);
      given(participantRepository.findInvitedRoomIds(entity.getId())).willReturn(Set.of());

      // Act
      participantService.joinProject(projectId, "Alice", userId, null);

      // Assert: setUser must have been called - verify entity's user field was set
      assertThat(entity.getUser())
          .as("setUser must be called on existing entity to backfill userId")
          .isEqualTo(userRef);
      then(participantRepository).should().save(entity);
    }
  }

  @Nested
  @DisplayName("validateParticipantExists")
  class ValidateParticipantExists {

    @Test
    void should_pass_for_existing_participant() {
      // Arrange
      UUID participantId = UUID.randomUUID();
      given(participantRepository.existsById(participantId)).willReturn(true);

      // Act & Assert
      assertThatCode(() -> participantService.validateParticipantExists(participantId))
          .doesNotThrowAnyException();
    }

    @Test
    void should_throw_unauthorized_for_nonexistent_participant() {
      // Arrange
      UUID participantId = UUID.randomUUID();
      given(participantRepository.existsById(participantId)).willReturn(false);

      // Act & Assert
      assertThatThrownBy(() -> participantService.validateParticipantExists(participantId))
          .isInstanceOf(UnauthorizedException.class)
          .hasMessage("Invalid or unknown participant");
    }

    @Test
    void should_throw_unauthorized_for_null_participant_id() {
      // Act & Assert
      assertThatThrownBy(() -> participantService.validateParticipantExists(null))
          .isInstanceOf(UnauthorizedException.class)
          .hasMessage("Invalid or unknown participant");
    }
  }

  @Nested
  @DisplayName("listParticipants")
  class ListParticipants {

    @Test
    void should_return_participants_with_room_access_enriched() {
      // Arrange - covers mutants #201 (conditional), #202 (replace return), #214 (lambda null)
      UUID projectId = UUID.randomUUID();
      UUID aliceId = UUID.randomUUID();
      UUID bobId = UUID.randomUUID();
      UUID roomId = UUID.randomUUID();
      ProjectEntity projectEntity = ProjectEntity.builder().id(projectId).build();
      List<ParticipantEntity> entities =
          List.of(
              ParticipantEntity.builder()
                  .id(aliceId)
                  .project(projectEntity)
                  .nickname("Alice")
                  .build(),
              ParticipantEntity.builder().id(bobId).project(projectEntity).nickname("Bob").build());
      given(participantRepository.findByProjectIdAndArchivedFalse(projectId)).willReturn(entities);
      given(participantEntityMapper.toDomainList(entities))
          .willReturn(
              List.of(
                  Participant.builder().id(aliceId).nickname("Alice").build(),
                  Participant.builder().id(bobId).nickname("Bob").build()));
      // Alice has room access, Bob does not
      List<Object[]> roomRows = List.<Object[]>of(new Object[] {aliceId, roomId});
      given(participantRepository.findInvitedRoomIdsByParticipantIds(List.of(aliceId, bobId)))
          .willReturn(roomRows);

      // Act
      List<Participant> result = participantService.listParticipants(projectId);

      // Assert: list is non-empty (mutant #201/#202 caught), room IDs populated (mutant #214
      // caught)
      assertThat(result).hasSize(2);
      Participant alice =
          result.stream().filter(p -> p.id().equals(aliceId)).findFirst().orElseThrow();
      Participant bob = result.stream().filter(p -> p.id().equals(bobId)).findFirst().orElseThrow();
      assertThat(alice.invitedRoomIds())
          .as("Alice should have room access enriched")
          .containsExactly(roomId);
      assertThat(bob.invitedRoomIds()).as("Bob should have empty room access").isEmpty();
    }

    @Test
    void should_return_empty_list_when_no_participants() {
      // Arrange - covers mutant #201: empty-list early return path
      UUID projectId = UUID.randomUUID();
      given(participantRepository.findByProjectIdAndArchivedFalse(projectId)).willReturn(List.of());
      given(participantEntityMapper.toDomainList(List.of())).willReturn(List.of());

      // Act
      List<Participant> result = participantService.listParticipants(projectId);

      // Assert: empty result returned, no call to findInvitedRoomIdsByParticipantIds
      assertThat(result).isEmpty();
      then(participantRepository).should(never()).findInvitedRoomIdsByParticipantIds(any());
    }
  }

  @Nested
  @DisplayName("deleteParticipant")
  class DeleteParticipant {

    @Test
    void should_delete_existing_participant() {
      // Arrange
      UUID projectId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();
      ParticipantEntity entity =
          ParticipantEntity.builder()
              .id(participantId)
              .project(ProjectEntity.builder().id(projectId).build())
              .nickname("Alice")
              .build();
      given(participantRepository.findByIdAndProjectId(participantId, projectId))
          .willReturn(Optional.of(entity));

      // Act
      participantService.deleteParticipant(projectId, participantId);

      // Assert
      then(participantRepository).should().delete(entity);
    }

    @Test
    void should_throw_not_found_when_participant_missing() {
      // Arrange
      UUID projectId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();
      given(participantRepository.findByIdAndProjectId(participantId, projectId))
          .willReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> participantService.deleteParticipant(projectId, participantId))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("getParticipant")
  class GetParticipant {

    @Test
    void should_return_participant_when_found() {
      // Arrange
      UUID projectId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();
      UUID roomId = UUID.randomUUID();
      ParticipantEntity entity =
          ParticipantEntity.builder()
              .id(participantId)
              .project(ProjectEntity.builder().id(projectId).build())
              .nickname("Alice")
              .build();
      Participant domain = Participant.builder().id(participantId).nickname("Alice").build();
      given(participantRepository.findByIdAndProjectId(participantId, projectId))
          .willReturn(Optional.of(entity));
      given(participantEntityMapper.toDomain(entity)).willReturn(domain);
      given(participantRepository.findInvitedRoomIds(participantId)).willReturn(Set.of(roomId));

      // Act
      Participant result = participantService.getParticipant(projectId, participantId);

      // Assert
      assertThat(result.nickname()).isEqualTo("Alice");
      assertThat(result.invitedRoomIds()).containsExactly(roomId);
    }

    @Test
    void should_throw_not_found_when_participant_missing() {
      // Arrange
      UUID projectId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();
      given(participantRepository.findByIdAndProjectId(participantId, projectId))
          .willReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> participantService.getParticipant(projectId, participantId))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("updateNickname")
  class UpdateNickname {

    @Test
    void should_update_nickname() {
      // Arrange
      UUID projectId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();
      ParticipantEntity entity =
          ParticipantEntity.builder()
              .id(participantId)
              .project(ProjectEntity.builder().id(projectId).build())
              .nickname("Alice")
              .build();
      Participant updatedDomain =
          Participant.builder().id(participantId).nickname("AliceNew").build();

      given(participantRepository.findByIdAndProjectId(participantId, projectId))
          .willReturn(Optional.of(entity));
      given(participantRepository.findByProjectIdAndNickname(projectId, "AliceNew"))
          .willReturn(Optional.empty());
      given(participantRepository.save(entity)).willReturn(entity);
      given(participantEntityMapper.toDomain(entity)).willReturn(updatedDomain);

      // Act
      Participant result = participantService.updateNickname(projectId, participantId, "AliceNew");

      // Assert: mutant #134 - setNickname must be called with the new value
      assertThat(entity.getNickname())
          .as("setNickname must be called on entity with the new value")
          .isEqualTo("AliceNew");
      assertThat(result.nickname()).isEqualTo("AliceNew");
      then(participantRepository).should().save(entity);
    }

    @Test
    void should_return_same_when_nickname_unchanged() {
      // Arrange
      UUID projectId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();
      ParticipantEntity entity =
          ParticipantEntity.builder()
              .id(participantId)
              .project(ProjectEntity.builder().id(projectId).build())
              .nickname("Alice")
              .build();
      Participant domain = Participant.builder().id(participantId).nickname("Alice").build();

      given(participantRepository.findByIdAndProjectId(participantId, projectId))
          .willReturn(Optional.of(entity));
      given(participantEntityMapper.toDomain(entity)).willReturn(domain);

      // Act
      Participant result = participantService.updateNickname(projectId, participantId, "Alice");

      // Assert
      assertThat(result.nickname()).isEqualTo("Alice");
      then(participantRepository).should(never()).save(entity);
    }

    @Test
    void should_throw_conflict_when_nickname_taken() {
      // Arrange
      UUID projectId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();
      ParticipantEntity entity =
          ParticipantEntity.builder()
              .id(participantId)
              .project(ProjectEntity.builder().id(projectId).build())
              .nickname("Alice")
              .build();
      ParticipantEntity existing =
          ParticipantEntity.builder()
              .id(UUID.randomUUID())
              .project(ProjectEntity.builder().id(projectId).build())
              .nickname("Bob")
              .build();

      given(participantRepository.findByIdAndProjectId(participantId, projectId))
          .willReturn(Optional.of(entity));
      given(participantRepository.findByProjectIdAndNickname(projectId, "Bob"))
          .willReturn(Optional.of(existing));

      // Act & Assert
      assertThatThrownBy(() -> participantService.updateNickname(projectId, participantId, "Bob"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("already taken");
    }

    @Test
    void should_throw_not_found_when_participant_missing() {
      // Arrange
      UUID projectId = UUID.randomUUID();
      UUID participantId = UUID.randomUUID();
      given(participantRepository.findByIdAndProjectId(participantId, projectId))
          .willReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(
              () -> participantService.updateNickname(projectId, participantId, "NewName"))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("countByProject")
  class CountByProject {

    @Test
    void should_return_count() {
      // Arrange
      UUID projectId = UUID.randomUUID();
      given(participantRepository.countByProjectIdAndArchivedFalse(projectId)).willReturn(5L);

      // Act
      long result = participantService.countByProject(projectId);

      // Assert
      assertThat(result).isEqualTo(5L);
    }
  }

  @Nested
  @DisplayName("countAll")
  class CountAll {

    @Test
    void should_return_total_count() {
      // Arrange - covers mutant #149: return value must be asserted (not replaced with 0)
      given(participantRepository.count()).willReturn(42L);

      // Act
      long result = participantService.countAll();

      // Assert
      assertThat(result).isEqualTo(42L);
    }

    @Test
    void should_return_zero_when_no_participants() {
      // Arrange
      given(participantRepository.count()).willReturn(0L);

      // Act
      long result = participantService.countAll();

      // Assert
      assertThat(result).isZero();
    }
  }
}
