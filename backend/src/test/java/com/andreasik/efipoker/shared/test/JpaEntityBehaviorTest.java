package com.andreasik.efipoker.shared.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.andreasik.efipoker.auth.UserEntity;
import com.andreasik.efipoker.estimation.estimate.EstimateEntity;
import com.andreasik.efipoker.estimation.room.RoomEntity;
import com.andreasik.efipoker.estimation.task.TaskEntity;
import com.andreasik.efipoker.participant.ParticipantEntity;
import com.andreasik.efipoker.project.ProjectEntity;
import jakarta.persistence.EntityManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("JpaEntityBehaviorTest")
class JpaEntityBehaviorTest extends BaseComponentTest {

  @Autowired private EntityManager entityManager;

  @Nested
  @DisplayName("equals/hashCode contract")
  class EqualsHashCode {

    @Test
    void should_not_consider_two_new_entities_as_equal() {
      // Arrange - two unsaved entities with null IDs
      ProjectEntity a = Fixtures.projectEntity("Project A", "slug-a");
      ProjectEntity b = Fixtures.projectEntity("Project B", "slug-b");

      // Act & Assert
      // Old Lombok @EqualsAndHashCode(of = "id") would say equal (null == null)
      assertThat(a).isNotEqualTo(b);
    }

    @Test
    void should_maintain_stable_hashcode_across_persist() {
      // Arrange
      UserEntity owner = userRepository.save(userEntity("hash-owner"));
      ProjectEntity entity = projectEntityWithOwner("Hash Test", owner);
      int hashBefore = entity.hashCode();

      // Act - persist assigns UUID
      ProjectEntity saved = projectRepository.save(entity);
      entityManager.flush();

      // Assert - hashCode must be stable
      // Old Lombok pattern: hashCode changes from 0 to UUID.hashCode()
      assertThat(saved.hashCode()).isEqualTo(hashBefore);
    }

    @Test
    void should_find_entity_in_set_after_persist() {
      // Arrange
      UserEntity owner = userRepository.save(userEntity("set-owner"));
      ProjectEntity entity = projectEntityWithOwner("Set Test", owner);
      Set<ProjectEntity> set = new HashSet<>();
      set.add(entity);

      // Act - persist changes id from null to UUID
      projectRepository.save(entity);
      entityManager.flush();

      // Assert - entity must still be findable in the set
      // Old Lombok: hashCode changed after persist -> wrong bucket -> contains() false
      assertThat(set).contains(entity);
    }

    @Test
    void should_support_equals_across_different_query_results() {
      // Arrange
      UserEntity owner = userRepository.save(userEntity("eq-owner"));
      ProjectEntity saved = projectRepository.save(projectEntityWithOwner("Eq Test", owner));
      entityManager.flush();
      entityManager.clear();

      // Act - load same entity via two different queries
      ProjectEntity fromSlug = projectRepository.findBySlug(saved.getSlug()).orElseThrow();
      entityManager.clear();
      ProjectEntity fromId = projectRepository.findById(saved.getId()).orElseThrow();

      // Assert
      assertThat(fromSlug).isEqualTo(fromId);
    }
  }

  @Nested
  @DisplayName("LAZY fetch + JOIN FETCH")
  class LazyFetchJoinFetch {

    @Test
    void should_load_project_relation_via_join_fetch() {
      // Arrange
      UserEntity owner = userRepository.save(userEntity("room-owner"));
      ProjectEntity project = projectRepository.save(projectEntityWithOwner("Room Proj", owner));
      RoomEntity room = roomRepository.save(Fixtures.roomEntity(project));
      String roomSlug = room.getSlug();
      entityManager.flush();
      entityManager.clear();

      // Act - load room via findBySlug (has JOIN FETCH r.project p)
      RoomEntity loaded = roomRepository.findBySlug(roomSlug).orElseThrow();
      entityManager.clear(); // detach - LazyInitializationException if not fetched

      // Assert - accessing project.name must work (JOIN FETCH loaded it)
      assertDoesNotThrow(() -> loaded.getProject().getName());
      assertThat(loaded.getProject().getName()).isEqualTo("Room Proj");
    }

    @Test
    void should_load_estimate_relations_in_single_query() {
      // Arrange - same pattern as buildVoteSnapshots()
      UserEntity owner = userRepository.save(userEntity("est-owner"));
      ProjectEntity project = projectRepository.save(projectEntityWithOwner("Est Proj", owner));
      RoomEntity room = roomRepository.save(Fixtures.roomEntity(project));
      TaskEntity task = taskRepository.save(Fixtures.taskEntity(room));
      ParticipantEntity p1 =
          participantRepository.save(Fixtures.participantEntity(project, "Alice"));
      ParticipantEntity p2 = participantRepository.save(Fixtures.participantEntity(project, "Bob"));
      estimateRepository.save(Fixtures.estimateEntity(task, p1, "5"));
      estimateRepository.save(Fixtures.estimateEntity(task, p2, "8"));
      entityManager.flush();
      entityManager.clear();

      // Act
      List<EstimateEntity> estimates = estimateRepository.findByTaskId(task.getId());
      entityManager.clear(); // detach all

      // Assert - accessing nested relations must work without session
      assertAll(
          () -> assertThat(estimates).hasSize(2),
          () ->
              assertDoesNotThrow(
                  () -> {
                    for (EstimateEntity e : estimates) {
                      e.getParticipant().getNickname();
                      e.getTask().getRoom().getProject().getName();
                    }
                  }));
    }

    @Test
    void should_access_project_owner_for_admin_validation() {
      // Arrange - same pattern as isOwner()
      UserEntity owner = userRepository.save(userEntity("admin-owner"));
      ProjectEntity project = projectRepository.save(projectEntityWithOwner("Admin Proj", owner));
      entityManager.flush();
      entityManager.clear();

      // Act
      ProjectEntity loaded = projectRepository.findBySlug(project.getSlug()).orElseThrow();
      entityManager.clear(); // detach

      // Assert - createdBy.getId() must work (LEFT JOIN FETCH loaded it)
      assertDoesNotThrow(() -> loaded.getCreatedBy().getId());
      assertThat(loaded.getCreatedBy().getId()).isEqualTo(owner.getId());
    }

    @Test
    void should_not_produce_cartesian_product_with_multiple_join_fetches() {
      // Arrange - multiple participants + estimates for same task
      UserEntity owner = userRepository.save(userEntity("cart-owner"));
      ProjectEntity project = projectRepository.save(projectEntityWithOwner("Cart Proj", owner));
      RoomEntity room = roomRepository.save(Fixtures.roomEntity(project));
      TaskEntity task = taskRepository.save(Fixtures.taskEntity(room));
      ParticipantEntity p1 = participantRepository.save(Fixtures.participantEntity(project, "P1"));
      ParticipantEntity p2 = participantRepository.save(Fixtures.participantEntity(project, "P2"));
      ParticipantEntity p3 = participantRepository.save(Fixtures.participantEntity(project, "P3"));
      estimateRepository.save(Fixtures.estimateEntity(task, p1, "3"));
      estimateRepository.save(Fixtures.estimateEntity(task, p2, "5"));
      estimateRepository.save(Fixtures.estimateEntity(task, p3, "8"));
      entityManager.flush();
      entityManager.clear();

      // Act - query with multiple JOIN FETCHes (two branches)
      List<EstimateEntity> estimates = estimateRepository.findByTaskId(task.getId());

      // Assert - exactly 3 results (not 3*N from Cartesian product)
      assertThat(estimates).hasSize(3);

      // Verify all nested relations are accessible
      entityManager.clear(); // detach
      assertAll(
          () -> {
            for (EstimateEntity e : estimates) {
              e.getParticipant().getNickname();
              e.getTask().getRoom().getProject().getName();
              e.getTask().getRoom().getProject().getCreatedBy().getUsername();
            }
          });
    }
  }

  // ── Helper methods ──

  private UserEntity userEntity(String username) {
    return UserEntity.builder()
        .username(username + "-" + UUID.randomUUID().toString().substring(0, 6))
        .email(username + "@test.com")
        .passwordHash("$2a$10$dummy")
        .role("USER")
        .build();
  }

  private ProjectEntity projectEntityWithOwner(String name, UserEntity owner) {
    return ProjectEntity.builder()
        .name(name)
        .slug("slug-" + UUID.randomUUID().toString().substring(0, 6))
        .adminCode(UUID.randomUUID().toString())
        .createdBy(owner)
        .build();
  }
}
