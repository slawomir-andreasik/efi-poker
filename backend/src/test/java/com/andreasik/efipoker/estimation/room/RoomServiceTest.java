package com.andreasik.efipoker.estimation.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.andreasik.efipoker.estimation.estimate.EstimateEntity;
import com.andreasik.efipoker.estimation.estimate.EstimateRepository;
import com.andreasik.efipoker.estimation.task.TaskEntity;
import com.andreasik.efipoker.estimation.task.TaskRepository;
import com.andreasik.efipoker.project.ProjectApi;
import com.andreasik.efipoker.project.ProjectEntity;
import com.andreasik.efipoker.shared.event.RoomCreatedEvent;
import com.andreasik.efipoker.shared.event.RoundStartedEvent;
import com.andreasik.efipoker.shared.exception.ResourceNotFoundException;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
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

    private final UUID roomId = UUID.randomUUID();
    private final UUID phantomTaskId = UUID.randomUUID();

    private RoomEntity newRoundEntity(String status, int roundNumber) {
      return RoomEntity.builder()
          .id(roomId)
          .roomType("LIVE")
          .status(status)
          .roundNumber(roundNumber)
          .title("Live Planning")
          .build();
    }

    private TaskEntity phantomTask() {
      return TaskEntity.builder()
          .id(phantomTaskId)
          .title(RoomService.PHANTOM_TASK_TITLE)
          .sortOrder(0)
          .build();
    }

    private void stubNewRound(RoomEntity entity, int nextRound) {
      RoomEntity saved =
          RoomEntity.builder().id(roomId).roomType("LIVE").roundNumber(nextRound).build();
      Room domain =
          Room.builder().id(roomId).roomType(RoomType.LIVE).roundNumber(nextRound).build();
      given(roomRepository.findById(roomId)).willReturn(Optional.of(entity));
      given(taskRepository.findByRoomIdAndTitle(roomId, RoomService.PHANTOM_TASK_TITLE))
          .willReturn(Optional.of(phantomTask()));
      given(roomRepository.save(entity)).willReturn(saved);
      given(roomEntityMapper.toDomain(saved)).willReturn(domain);
    }

    @Test
    void should_increment_round_and_clear_estimates() {
      RoomEntity entity = newRoundEntity("OPEN", 1);
      stubNewRound(entity, 2);

      roomService.newRound(roomId, "Topic A");

      then(estimateRepository).should().deleteByTaskId(phantomTaskId);
      then(roomRepository).should().save(entity);
      assertThat(entity.getStatus()).isEqualTo(RoomStatus.OPEN.name());
      assertThat(entity.getRoundNumber()).isEqualTo(2);
    }

    @Test
    void should_publish_round_started_event() {
      RoomEntity entity = newRoundEntity("OPEN", 3);
      stubNewRound(entity, 4);

      roomService.newRound(roomId, null);

      ArgumentCaptor<RoundStartedEvent> captor = ArgumentCaptor.forClass(RoundStartedEvent.class);
      then(eventPublisher).should().publishEvent(captor.capture());
      assertThat(captor.getValue().roomId()).isEqualTo(roomId);
    }

    @Test
    void should_save_round_snapshot_when_room_is_revealed() {
      RoomEntity entity = newRoundEntity("REVEALED", 2);
      List<EstimateEntity> estimates = List.of(new EstimateEntity(), new EstimateEntity());
      stubNewRound(entity, 3);
      given(estimateRepository.findByTaskId(phantomTaskId)).willReturn(estimates);

      roomService.newRound(roomId, null);

      then(roundHistoryService).should().saveRoundSnapshot(entity, estimates);
    }

    @Test
    void should_not_save_round_snapshot_when_room_is_open() {
      RoomEntity entity = newRoundEntity("OPEN", 1);
      stubNewRound(entity, 2);

      roomService.newRound(roomId, null);

      then(roundHistoryService).should(never()).saveRoundSnapshot(any(), any());
    }

    @Test
    void should_throw_when_room_is_not_live() {
      UUID asyncRoomId = UUID.randomUUID();
      RoomEntity entity = RoomEntity.builder().id(asyncRoomId).roomType("ASYNC").build();
      given(roomRepository.findById(asyncRoomId)).willReturn(Optional.of(entity));

      assertThatThrownBy(() -> roomService.newRound(asyncRoomId, null))
          .isInstanceOf(IllegalStateException.class);
      then(estimateRepository).should(never()).deleteByTaskId(any());
    }

    @Test
    void should_throw_when_room_not_found() {
      UUID missingId = UUID.randomUUID();
      given(roomRepository.findById(missingId)).willReturn(Optional.empty());

      assertThatThrownBy(() -> roomService.newRound(missingId, null))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_set_topic_when_provided() {
      RoomEntity entity = newRoundEntity("OPEN", 1);
      entity.setTopic(null);
      RoomEntity saved =
          RoomEntity.builder()
              .id(roomId)
              .roomType("LIVE")
              .roundNumber(2)
              .topic("Sprint Goal")
              .build();
      Room domain =
          Room.builder()
              .id(roomId)
              .roomType(RoomType.LIVE)
              .roundNumber(2)
              .topic("Sprint Goal")
              .build();
      given(roomRepository.findById(roomId)).willReturn(Optional.of(entity));
      given(taskRepository.findByRoomIdAndTitle(roomId, RoomService.PHANTOM_TASK_TITLE))
          .willReturn(Optional.of(phantomTask()));
      given(roomRepository.save(entity)).willReturn(saved);
      given(roomEntityMapper.toDomain(saved)).willReturn(domain);

      Room result = roomService.newRound(roomId, "Sprint Goal");

      assertThat(entity.getTopic()).isEqualTo("Sprint Goal");
      assertThat(result.topic()).isEqualTo("Sprint Goal");
    }

    @Test
    void should_clear_topic_when_null_provided() {
      RoomEntity entity = newRoundEntity("OPEN", 3);
      entity.setTopic("old topic from round 3");
      stubNewRound(entity, 4);

      roomService.newRound(roomId, null);

      assertThat(entity.getTopic()).isNull();
    }
  }

  @Nested
  @DisplayName("updateRoom")
  class UpdateRoom {

    private final UUID roomId = UUID.randomUUID();

    private RoomEntity updateEntity() {
      return RoomEntity.builder().id(roomId).title("Room").roomType("LIVE").status("OPEN").build();
    }

    private void stubUpdate(RoomEntity entity) {
      RoomEntity saved = RoomEntity.builder().id(roomId).build();
      Room domain = Room.builder().id(roomId).build();
      given(roomRepository.findById(roomId)).willReturn(Optional.of(entity));
      given(roomRepository.save(entity)).willReturn(saved);
      given(roomEntityMapper.toDomain(saved)).willReturn(domain);
    }

    private UpdateRoomCommand command(
        String title,
        String description,
        Instant deadline,
        String topic,
        String commentTemplate,
        Boolean commentRequired,
        Boolean autoReveal) {
      return new UpdateRoomCommand(
          roomId,
          title,
          description,
          deadline,
          topic,
          commentTemplate,
          commentRequired,
          autoReveal);
    }

    @Test
    void should_update_topic() {
      RoomEntity entity = updateEntity();
      stubUpdate(entity);

      roomService.updateRoom(command(null, null, null, "Sprint Goal", null, null, null));

      assertThat(entity.getTopic()).isEqualTo("Sprint Goal");
    }

    @Test
    void should_clear_topic_when_blank() {
      RoomEntity entity = updateEntity();
      entity.setTopic("Old topic");
      stubUpdate(entity);

      roomService.updateRoom(command(null, null, null, "  ", null, null, null));

      assertThat(entity.getTopic()).isNull();
    }

    @Test
    void should_not_change_topic_when_null() {
      RoomEntity entity = updateEntity();
      entity.setTopic("Existing topic");
      stubUpdate(entity);

      roomService.updateRoom(command(null, null, null, null, null, null, null));

      assertThat(entity.getTopic()).isEqualTo("Existing topic");
    }

    @Test
    void should_update_title_when_provided() {
      RoomEntity entity = updateEntity();
      entity.setTitle("Old Title");
      stubUpdate(entity);

      roomService.updateRoom(command("New Title", null, null, null, null, null, null));

      assertThat(entity.getTitle()).isEqualTo("New Title");
    }

    @Test
    void should_not_change_title_when_null() {
      RoomEntity entity = updateEntity();
      entity.setTitle("Existing Title");
      stubUpdate(entity);

      roomService.updateRoom(command(null, null, null, null, null, null, null));

      assertThat(entity.getTitle()).isEqualTo("Existing Title");
    }

    @Test
    void should_update_description_when_provided() {
      RoomEntity entity = updateEntity();
      entity.setDescription("Old desc");
      stubUpdate(entity);

      roomService.updateRoom(command(null, "New desc", null, null, null, null, null));

      assertThat(entity.getDescription()).isEqualTo("New desc");
    }

    @Test
    void should_not_change_description_when_null() {
      RoomEntity entity = updateEntity();
      entity.setDescription("Existing desc");
      stubUpdate(entity);

      roomService.updateRoom(command(null, null, null, null, null, null, null));

      assertThat(entity.getDescription()).isEqualTo("Existing desc");
    }

    @Test
    void should_update_deadline_when_provided() {
      Instant newDeadline = Instant.parse("2030-01-01T00:00:00Z");
      RoomEntity entity = updateEntity();
      entity.setDeadline(Instant.parse("2025-01-01T00:00:00Z"));
      stubUpdate(entity);

      roomService.updateRoom(command(null, null, newDeadline, null, null, null, null));

      assertThat(entity.getDeadline()).isEqualTo(newDeadline);
    }

    @Test
    void should_not_change_deadline_when_null() {
      Instant existingDeadline = Instant.parse("2025-01-01T00:00:00Z");
      RoomEntity entity = updateEntity();
      entity.setDeadline(existingDeadline);
      stubUpdate(entity);

      roomService.updateRoom(command(null, null, null, null, null, null, null));

      assertThat(entity.getDeadline()).isEqualTo(existingDeadline);
    }

    @Test
    void should_update_comment_template_when_provided() {
      RoomEntity entity = updateEntity();
      entity.setCommentTemplate("Old template");
      stubUpdate(entity);

      roomService.updateRoom(command(null, null, null, null, "New template", null, null));

      assertThat(entity.getCommentTemplate()).isEqualTo("New template");
    }

    @Test
    void should_not_change_comment_template_when_null() {
      RoomEntity entity = updateEntity();
      entity.setCommentTemplate("Existing template");
      stubUpdate(entity);

      roomService.updateRoom(command(null, null, null, null, null, null, null));

      assertThat(entity.getCommentTemplate()).isEqualTo("Existing template");
    }

    @Test
    void should_update_comment_required_when_provided() {
      RoomEntity entity = updateEntity();
      entity.setCommentRequired(false);
      stubUpdate(entity);

      roomService.updateRoom(command(null, null, null, null, null, true, null));

      assertThat(entity.isCommentRequired()).isTrue();
    }

    @Test
    void should_not_change_comment_required_when_null() {
      RoomEntity entity = updateEntity();
      entity.setCommentRequired(true);
      stubUpdate(entity);

      roomService.updateRoom(command(null, null, null, null, null, null, null));

      assertThat(entity.isCommentRequired()).isTrue();
    }

    @Test
    void should_update_auto_reveal_when_provided() {
      RoomEntity entity = updateEntity();
      entity.setAutoRevealOnDeadline(false);
      stubUpdate(entity);

      roomService.updateRoom(command(null, null, null, null, null, null, true));

      assertThat(entity.isAutoRevealOnDeadline()).isTrue();
    }

    @Test
    void should_not_change_auto_reveal_when_null() {
      RoomEntity entity = updateEntity();
      entity.setAutoRevealOnDeadline(true);
      stubUpdate(entity);

      roomService.updateRoom(command(null, null, null, null, null, null, null));

      assertThat(entity.isAutoRevealOnDeadline()).isTrue();
    }
  }

  @Nested
  @DisplayName("closeExpiredRooms")
  class CloseExpiredRooms {

    @Test
    void should_return_empty_list_when_no_expired_rooms() {
      // Arrange - mutant: removed isEmpty() check -> would call saveAll(emptyList) instead of early
      // return
      given(roomRepository.findExpired(any(), any(), any())).willReturn(List.of());

      // Act
      List<Room> result = roomService.closeExpiredRooms();

      // Assert - empty list returned, saveAll NOT called
      assertThat(result).isEmpty();
      then(roomRepository).should(never()).saveAll(any());
    }

    @Test
    void should_close_expired_rooms_and_return_them() {
      // Arrange - complement: non-empty path does call saveAll
      UUID roomId = UUID.randomUUID();
      RoomEntity expired =
          RoomEntity.builder()
              .id(roomId)
              .roomType("ASYNC")
              .status("OPEN")
              .title("Async Room")
              .build();
      RoomEntity closed =
          RoomEntity.builder().id(roomId).roomType("ASYNC").status("CLOSED").build();
      Room domain = Room.builder().id(roomId).roomType(RoomType.ASYNC).build();

      given(roomRepository.findExpired(any(), any(), any())).willReturn(List.of(expired));
      given(roomRepository.saveAll(List.of(expired))).willReturn(List.of(closed));
      given(roomEntityMapper.toDomainList(List.of(closed))).willReturn(List.of(domain));

      // Act
      List<Room> result = roomService.closeExpiredRooms();

      // Assert
      assertThat(result).hasSize(1);
      assertThat(expired.getStatus()).isEqualTo(RoomStatus.CLOSED.name());
    }
  }

  @Nested
  @DisplayName("countOpenRooms")
  class CountOpenRooms {

    @Test
    void should_return_count_from_repository() {
      // Arrange - mutant: replaced return with 0
      given(roomRepository.countByStatus(RoomStatus.OPEN.name())).willReturn(42L);

      // Act
      long result = roomService.countOpenRooms();

      // Assert
      assertThat(result).isEqualTo(42L);
    }

    @Test
    void should_return_zero_when_no_open_rooms() {
      // Arrange - ensure 0 is a valid result from the repo, not a hardcoded return
      given(roomRepository.countByStatus(RoomStatus.OPEN.name())).willReturn(0L);

      // Act
      long result = roomService.countOpenRooms();

      // Assert - 0 is valid but must come from the repo call, not be hardcoded
      assertThat(result).isZero();
      then(roomRepository).should().countByStatus(RoomStatus.OPEN.name());
    }
  }

  @Nested
  @DisplayName("deleteRoom")
  class DeleteRoom {

    @Test
    void should_delete_via_reference() {
      // Arrange
      UUID roomId = UUID.randomUUID();
      Room room = Room.builder().id(roomId).slug("A3X-K7B").roomType(RoomType.ASYNC).build();
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
  @DisplayName("createRoom")
  class CreateRoom {

    private RoomEntity savedLiveRoom(UUID projectId) {
      ProjectEntity project = ProjectEntity.builder().id(projectId).build();
      return RoomEntity.builder()
          .id(UUID.randomUUID())
          .project(project)
          .slug("ABC-123")
          .title("Room")
          .roomType("LIVE")
          .build();
    }

    private void stubLiveRoom(UUID projectId, RoomEntity savedRoom) {
      ProjectEntity project = ProjectEntity.builder().id(projectId).build();
      given(entityManager.getReference(ProjectEntity.class, projectId)).willReturn(project);
      given(roomRepository.save(any(RoomEntity.class))).willReturn(savedRoom);
      given(taskRepository.save(any(TaskEntity.class))).willReturn(new TaskEntity());
      given(roomEntityMapper.toDomain(savedRoom))
          .willReturn(Room.builder().id(savedRoom.getId()).roomType(RoomType.LIVE).build());
    }

    @Test
    void should_validate_project_exists() {
      // Arrange
      UUID projectId = UUID.randomUUID();
      RoomEntity savedRoom = savedLiveRoom(projectId);
      stubLiveRoom(projectId, savedRoom);

      // Act
      roomService.createRoom(
          new CreateRoomCommand(projectId, "Room", null, RoomType.LIVE, null, true, null, false));

      // Assert - mutant: removed call to validateProjectExists
      then(projectApi).should().validateProjectExists(projectId);
    }

    @Test
    void should_throw_when_async_without_deadline() {
      // Arrange
      UUID projectId = UUID.randomUUID();
      ProjectEntity project = ProjectEntity.builder().id(projectId).build();
      given(entityManager.getReference(ProjectEntity.class, projectId)).willReturn(project);

      // Act & Assert - mutant: removed conditional (ASYNC && deadline == null)
      assertThatThrownBy(
              () ->
                  roomService.createRoom(
                      new CreateRoomCommand(
                          projectId, "Room", null, RoomType.ASYNC, null, true, null, false)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("deadline");
    }

    @Test
    void should_not_throw_when_async_with_deadline() {
      // Arrange - tests the OTHER branch: ASYNC + deadline present -> must NOT throw
      UUID projectId = UUID.randomUUID();
      ProjectEntity project = ProjectEntity.builder().id(projectId).build();
      RoomEntity savedRoom =
          RoomEntity.builder()
              .id(UUID.randomUUID())
              .project(project)
              .slug("ABC-123")
              .title("Room")
              .roomType("ASYNC")
              .build();
      given(entityManager.getReference(ProjectEntity.class, projectId)).willReturn(project);
      given(roomRepository.save(any(RoomEntity.class))).willReturn(savedRoom);
      given(roomEntityMapper.toDomain(savedRoom))
          .willReturn(Room.builder().id(savedRoom.getId()).roomType(RoomType.ASYNC).build());

      // Act & Assert - mutant: removed conditional (ASYNC && deadline == null) - other branch
      Room result =
          roomService.createRoom(
              new CreateRoomCommand(
                  projectId, "Room", null, RoomType.ASYNC, Instant.now(), true, null, false));

      assertThat(result).isNotNull();
    }

    @Test
    void should_publish_room_created_event() {
      // Arrange
      UUID projectId = UUID.randomUUID();
      RoomEntity savedRoom = savedLiveRoom(projectId);
      stubLiveRoom(projectId, savedRoom);

      // Act
      roomService.createRoom(
          new CreateRoomCommand(projectId, "Room", null, RoomType.LIVE, null, true, null, false));

      // Assert - mutant: removed call to publishEvent
      ArgumentCaptor<RoomCreatedEvent> captor = ArgumentCaptor.forClass(RoomCreatedEvent.class);
      then(eventPublisher).should().publishEvent(captor.capture());
      assertThat(captor.getValue().roomId()).isEqualTo(savedRoom.getId());
    }

    @Test
    void should_return_mapped_domain_room() {
      // Arrange
      UUID projectId = UUID.randomUUID();
      RoomEntity savedRoom = savedLiveRoom(projectId);
      Room expected = Room.builder().id(savedRoom.getId()).roomType(RoomType.LIVE).build();
      ProjectEntity project = ProjectEntity.builder().id(projectId).build();
      given(entityManager.getReference(ProjectEntity.class, projectId)).willReturn(project);
      given(roomRepository.save(any(RoomEntity.class))).willReturn(savedRoom);
      given(taskRepository.save(any(TaskEntity.class))).willReturn(new TaskEntity());
      given(roomEntityMapper.toDomain(savedRoom)).willReturn(expected);

      // Act
      Room result =
          roomService.createRoom(
              new CreateRoomCommand(
                  projectId, "Room", null, RoomType.LIVE, null, true, null, false));

      // Assert - mutant: replaced return with null
      assertThat(result).isNotNull();
      assertThat(result).isSameAs(expected);
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
      Room domain =
          Room.builder().id(savedRoom.getId()).slug("A3X-K7B").roomType(RoomType.LIVE).build();

      given(entityManager.getReference(ProjectEntity.class, projectId)).willReturn(project);
      given(roomRepository.save(captor.capture())).willReturn(savedRoom);
      given(taskRepository.save(any(TaskEntity.class))).willReturn(new TaskEntity());
      given(roomEntityMapper.toDomain(savedRoom)).willReturn(domain);

      // Act
      roomService.createRoom(
          new CreateRoomCommand(projectId, "Room", null, RoomType.LIVE, null, true, null, false));

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
      Room domain = Room.builder().id(savedRoom.getId()).roomType(RoomType.LIVE).build();

      given(entityManager.getReference(ProjectEntity.class, projectId)).willReturn(project);
      given(roomRepository.save(any(RoomEntity.class))).willReturn(savedRoom);
      given(taskRepository.save(any(TaskEntity.class))).willReturn(new TaskEntity());
      given(roomEntityMapper.toDomain(savedRoom)).willReturn(domain);

      // Act
      roomService.createRoom(
          new CreateRoomCommand(
              projectId, "Live Room", null, RoomType.LIVE, null, true, null, false));

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
      Room domain = Room.builder().id(savedRoom.getId()).roomType(RoomType.ASYNC).build();

      given(entityManager.getReference(ProjectEntity.class, projectId)).willReturn(project);
      given(roomRepository.save(any(RoomEntity.class))).willReturn(savedRoom);
      given(roomEntityMapper.toDomain(savedRoom)).willReturn(domain);

      // Act
      roomService.createRoom(
          new CreateRoomCommand(
              projectId, "Async Room", null, RoomType.ASYNC, Instant.now(), true, null, false));

      // Assert
      then(taskRepository).should(never()).save(any(TaskEntity.class));
    }
  }
}
