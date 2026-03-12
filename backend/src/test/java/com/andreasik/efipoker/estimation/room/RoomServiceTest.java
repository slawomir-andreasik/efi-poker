package com.andreasik.efipoker.estimation.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.andreasik.efipoker.estimation.estimate.EstimateRepository;
import com.andreasik.efipoker.estimation.task.TaskEntity;
import com.andreasik.efipoker.estimation.task.TaskRepository;
import com.andreasik.efipoker.project.ProjectApi;
import com.andreasik.efipoker.project.ProjectEntity;
import com.andreasik.efipoker.shared.exception.ResourceNotFoundException;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

@DisplayName("RoomService")
class RoomServiceTest extends BaseUnitTest {

  @Mock private RoomRepository roomRepository;
  @Mock private ProjectApi projectApi;
  @Mock private EntityManager entityManager;
  @Mock private TaskRepository taskRepository;
  @Mock private EstimateRepository estimateRepository;
  @Mock private RoomEntityMapper roomEntityMapper;
  @Mock private RoundHistoryService roundHistoryService;
  @Mock private ApplicationEventPublisher eventPublisher;
  @InjectMocks private RoomService roomService;

  @Nested
  @DisplayName("newRound")
  class NewRound {

    @Test
    void should_increment_round_and_clear_estimates() {
      // Arrange
      UUID roomId = UUID.randomUUID();
      UUID phantomTaskId = UUID.randomUUID();
      RoomEntity entity =
          RoomEntity.builder()
              .id(roomId)
              .roomType("LIVE")
              .status("OPEN")
              .roundNumber(1)
              .title("Live Planning")
              .build();
      TaskEntity phantom =
          TaskEntity.builder()
              .id(phantomTaskId)
              .title(RoomService.PHANTOM_TASK_TITLE)
              .sortOrder(0)
              .build();
      RoomEntity saved = RoomEntity.builder().id(roomId).roomType("LIVE").roundNumber(2).build();
      Room domain = Room.builder().id(roomId).roomType("LIVE").roundNumber(2).build();

      given(roomRepository.findById(roomId)).willReturn(Optional.of(entity));
      given(taskRepository.findByRoomIdAndTitle(roomId, RoomService.PHANTOM_TASK_TITLE))
          .willReturn(Optional.of(phantom));
      given(roomRepository.save(entity)).willReturn(saved);
      given(roomEntityMapper.toDomain(saved)).willReturn(domain);

      // Act
      roomService.newRound(roomId, "Topic A");

      // Assert
      then(estimateRepository).should().deleteByTaskId(phantomTaskId);
      then(roomRepository).should().save(entity);
    }

    @Test
    void should_throw_when_room_is_not_live() {
      // Arrange
      UUID roomId = UUID.randomUUID();
      RoomEntity entity = RoomEntity.builder().id(roomId).roomType("ASYNC").build();
      given(roomRepository.findById(roomId)).willReturn(Optional.of(entity));

      // Act & Assert
      assertThatThrownBy(() -> roomService.newRound(roomId, null))
          .isInstanceOf(IllegalStateException.class);

      then(estimateRepository).should(never()).deleteByTaskId(any());
    }

    @Test
    void should_throw_when_room_not_found() {
      // Arrange
      UUID roomId = UUID.randomUUID();
      given(roomRepository.findById(roomId)).willReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> roomService.newRound(roomId, null))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_set_topic_when_provided() {
      // Arrange
      UUID roomId = UUID.randomUUID();
      UUID phantomId = UUID.randomUUID();
      RoomEntity entity =
          RoomEntity.builder()
              .id(roomId)
              .roomType("LIVE")
              .status("OPEN")
              .roundNumber(1)
              .topic(null)
              .title("Live Room")
              .build();
      TaskEntity phantom =
          TaskEntity.builder()
              .id(phantomId)
              .title(RoomService.PHANTOM_TASK_TITLE)
              .sortOrder(0)
              .build();
      RoomEntity saved =
          RoomEntity.builder()
              .id(roomId)
              .roomType("LIVE")
              .roundNumber(2)
              .topic("Sprint Goal")
              .build();
      Room domain =
          Room.builder().id(roomId).roomType("LIVE").roundNumber(2).topic("Sprint Goal").build();

      given(roomRepository.findById(roomId)).willReturn(Optional.of(entity));
      given(taskRepository.findByRoomIdAndTitle(roomId, RoomService.PHANTOM_TASK_TITLE))
          .willReturn(Optional.of(phantom));
      given(roomRepository.save(entity)).willReturn(saved);
      given(roomEntityMapper.toDomain(saved)).willReturn(domain);

      // Act
      Room result = roomService.newRound(roomId, "Sprint Goal");

      // Assert
      assertThat(entity.getTopic()).isEqualTo("Sprint Goal");
      assertThat(result.topic()).isEqualTo("Sprint Goal");
    }

    @Test
    void should_clear_topic_when_null_provided() {
      // Arrange - room has topic from previous round
      UUID roomId = UUID.randomUUID();
      UUID phantomId = UUID.randomUUID();
      RoomEntity entity =
          RoomEntity.builder()
              .id(roomId)
              .roomType("LIVE")
              .status("OPEN")
              .roundNumber(3)
              .topic("old topic from round 3")
              .title("Live Room")
              .build();
      TaskEntity phantom =
          TaskEntity.builder()
              .id(phantomId)
              .title(RoomService.PHANTOM_TASK_TITLE)
              .sortOrder(0)
              .build();
      RoomEntity saved =
          RoomEntity.builder().id(roomId).roomType("LIVE").roundNumber(4).topic(null).build();
      Room domain = Room.builder().id(roomId).roomType("LIVE").roundNumber(4).topic(null).build();

      given(roomRepository.findById(roomId)).willReturn(Optional.of(entity));
      given(taskRepository.findByRoomIdAndTitle(roomId, RoomService.PHANTOM_TASK_TITLE))
          .willReturn(Optional.of(phantom));
      given(roomRepository.save(entity)).willReturn(saved);
      given(roomEntityMapper.toDomain(saved)).willReturn(domain);

      // Act - no topic provided (null) -> must clear, not carry over old topic
      roomService.newRound(roomId, null);

      // Assert - topic must be null on the entity passed to save (not "old topic from round 3")
      assertThat(entity.getTopic()).isNull();
    }
  }

  @Nested
  @DisplayName("updateRoom")
  class UpdateRoom {

    @Test
    void should_update_topic() {
      // Arrange
      UUID roomId = UUID.randomUUID();
      RoomEntity entity =
          RoomEntity.builder().id(roomId).title("Room").roomType("LIVE").status("OPEN").build();
      RoomEntity saved = RoomEntity.builder().id(roomId).topic("Sprint Goal").build();
      Room domain = Room.builder().id(roomId).topic("Sprint Goal").build();

      given(roomRepository.findById(roomId)).willReturn(Optional.of(entity));
      given(roomRepository.save(entity)).willReturn(saved);
      given(roomEntityMapper.toDomain(saved)).willReturn(domain);

      // Act
      Room result = roomService.updateRoom(roomId, null, null, null, "Sprint Goal");

      // Assert
      assertThat(entity.getTopic()).isEqualTo("Sprint Goal");
      assertThat(result.topic()).isEqualTo("Sprint Goal");
    }

    @Test
    void should_clear_topic_when_blank() {
      // Arrange
      UUID roomId = UUID.randomUUID();
      RoomEntity entity =
          RoomEntity.builder()
              .id(roomId)
              .title("Room")
              .roomType("LIVE")
              .status("OPEN")
              .topic("Old topic")
              .build();
      RoomEntity saved = RoomEntity.builder().id(roomId).topic(null).build();
      Room domain = Room.builder().id(roomId).topic(null).build();

      given(roomRepository.findById(roomId)).willReturn(Optional.of(entity));
      given(roomRepository.save(entity)).willReturn(saved);
      given(roomEntityMapper.toDomain(saved)).willReturn(domain);

      // Act
      Room result = roomService.updateRoom(roomId, null, null, null, "  ");

      // Assert
      assertThat(entity.getTopic()).isNull();
      assertThat(result.topic()).isNull();
    }

    @Test
    void should_not_change_topic_when_null() {
      // Arrange
      UUID roomId = UUID.randomUUID();
      RoomEntity entity =
          RoomEntity.builder()
              .id(roomId)
              .title("Room")
              .roomType("LIVE")
              .status("OPEN")
              .topic("Existing topic")
              .build();
      RoomEntity saved = RoomEntity.builder().id(roomId).topic("Existing topic").build();
      Room domain = Room.builder().id(roomId).topic("Existing topic").build();

      given(roomRepository.findById(roomId)).willReturn(Optional.of(entity));
      given(roomRepository.save(entity)).willReturn(saved);
      given(roomEntityMapper.toDomain(saved)).willReturn(domain);

      // Act
      roomService.updateRoom(roomId, null, null, null, null);

      // Assert
      assertThat(entity.getTopic()).isEqualTo("Existing topic");
    }
  }

  @Nested
  @DisplayName("deleteRoom")
  class DeleteRoom {

    @Test
    void should_delete_via_reference() {
      // Arrange
      UUID roomId = UUID.randomUUID();
      Room room = Room.builder().id(roomId).slug("A3X-K7B").roomType("ASYNC").build();
      RoomEntity ref = new RoomEntity();
      given(entityManager.getReference(RoomEntity.class, roomId)).willReturn(ref);

      // Act
      roomService.deleteRoom(room);

      // Assert
      then(roomRepository).should().delete(ref);
    }
  }

  @Nested
  @DisplayName("isRevealedStatus")
  class IsRevealedStatus {

    @Test
    void should_return_true_for_revealed() {
      // Act & Assert
      assertThat(RoomService.isRevealedStatus("REVEALED")).isTrue();
    }

    @Test
    void should_return_true_for_closed() {
      // Act & Assert
      assertThat(RoomService.isRevealedStatus("CLOSED")).isTrue();
    }

    @Test
    void should_return_false_for_open() {
      // Act & Assert
      assertThat(RoomService.isRevealedStatus("OPEN")).isFalse();
    }

    @Test
    void should_return_false_for_null() {
      // Act & Assert
      assertThat(RoomService.isRevealedStatus(null)).isFalse();
    }
  }

  @Nested
  @DisplayName("createRoom - phantom task")
  class CreateRoomPhantomTask {

    @Test
    void should_generate_slug_matching_format() {
      // Arrange
      UUID projectId = UUID.randomUUID();
      ProjectEntity project = ProjectEntity.builder().id(projectId).build();
      ArgumentCaptor<RoomEntity> captor = ArgumentCaptor.forClass(RoomEntity.class);
      RoomEntity savedRoom =
          RoomEntity.builder()
              .id(UUID.randomUUID())
              .project(project)
              .slug("A3X-K7B")
              .title("Room")
              .roomType("LIVE")
              .build();
      Room domain = Room.builder().id(savedRoom.getId()).slug("A3X-K7B").roomType("LIVE").build();

      given(entityManager.getReference(ProjectEntity.class, projectId)).willReturn(project);
      given(roomRepository.save(captor.capture())).willReturn(savedRoom);
      given(taskRepository.save(any(TaskEntity.class))).willReturn(new TaskEntity());
      given(roomEntityMapper.toDomain(savedRoom)).willReturn(domain);

      // Act
      roomService.createRoom(projectId, "Room", null, "LIVE", null, true);

      // Assert
      String slug = captor.getValue().getSlug();
      assertThat(slug).matches("[A-Z0-9]{3}-[A-Z0-9]{3}");
    }

    @Test
    void should_create_phantom_task_for_live_room() {
      // Arrange
      UUID projectId = UUID.randomUUID();
      ProjectEntity project = ProjectEntity.builder().id(projectId).build();
      RoomEntity savedRoom =
          RoomEntity.builder()
              .id(UUID.randomUUID())
              .project(project)
              .title("Live Room")
              .roomType("LIVE")
              .build();
      Room domain = Room.builder().id(savedRoom.getId()).roomType("LIVE").build();

      given(entityManager.getReference(ProjectEntity.class, projectId)).willReturn(project);
      given(roomRepository.save(any(RoomEntity.class))).willReturn(savedRoom);
      given(taskRepository.save(any(TaskEntity.class))).willReturn(new TaskEntity());
      given(roomEntityMapper.toDomain(savedRoom)).willReturn(domain);

      // Act
      roomService.createRoom(projectId, "Live Room", null, "LIVE", null, true);

      // Assert
      then(taskRepository).should().save(any(TaskEntity.class));
    }

    @Test
    void should_not_create_phantom_task_for_async_room() {
      // Arrange
      UUID projectId = UUID.randomUUID();
      ProjectEntity project = ProjectEntity.builder().id(projectId).build();
      RoomEntity savedRoom =
          RoomEntity.builder()
              .id(UUID.randomUUID())
              .project(project)
              .title("Async Room")
              .roomType("ASYNC")
              .build();
      Room domain = Room.builder().id(savedRoom.getId()).roomType("ASYNC").build();

      given(entityManager.getReference(ProjectEntity.class, projectId)).willReturn(project);
      given(roomRepository.save(any(RoomEntity.class))).willReturn(savedRoom);
      given(roomEntityMapper.toDomain(savedRoom)).willReturn(domain);

      // Act
      roomService.createRoom(projectId, "Async Room", null, "ASYNC", java.time.Instant.now(), true);

      // Assert
      then(taskRepository).should(never()).save(any(TaskEntity.class));
    }
  }
}
