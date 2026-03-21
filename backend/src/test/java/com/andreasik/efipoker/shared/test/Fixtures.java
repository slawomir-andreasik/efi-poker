package com.andreasik.efipoker.shared.test;

import com.andreasik.efipoker.estimation.estimate.Estimate;
import com.andreasik.efipoker.estimation.estimate.EstimateEntity;
import com.andreasik.efipoker.estimation.room.Room;
import com.andreasik.efipoker.estimation.room.RoomEntity;
import com.andreasik.efipoker.estimation.room.RoomStatus;
import com.andreasik.efipoker.estimation.room.RoomType;
import com.andreasik.efipoker.estimation.task.Task;
import com.andreasik.efipoker.estimation.task.TaskEntity;
import com.andreasik.efipoker.participant.Participant;
import com.andreasik.efipoker.participant.ParticipantEntity;
import com.andreasik.efipoker.project.Project;
import com.andreasik.efipoker.project.ProjectEntity;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class Fixtures {

  private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

  /// Well-known raw admin code for integration tests (used by admin code exchange endpoint).
  public static final String TEST_ADMIN_CODE = "test-admin-code-for-integration-tests";

  private static final String TEST_ADMIN_CODE_HASH = ENCODER.encode(TEST_ADMIN_CODE);

  private static final String SLUG_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final SecureRandom RANDOM = new SecureRandom();

  private Fixtures() {}

  private static String randomRoomSlug() {
    StringBuilder sb = new StringBuilder(7);
    for (int i = 0; i < 3; i++) {
      sb.append(SLUG_CHARS.charAt(RANDOM.nextInt(SLUG_CHARS.length())));
    }
    sb.append('-');
    for (int i = 0; i < 3; i++) {
      sb.append(SLUG_CHARS.charAt(RANDOM.nextInt(SLUG_CHARS.length())));
    }
    return sb.toString();
  }

  // ── Domain fixtures (unit/mapper tests) ──

  public static Project project() {
    return project("Test Project", "tp" + UUID.randomUUID().toString().substring(0, 6));
  }

  public static Project project(String name, String slug) {
    return Project.builder().name(name).slug(slug).adminCode(UUID.randomUUID().toString()).build();
  }

  public static Room room(Project project) {
    return room(project, RoomType.ASYNC);
  }

  public static Room room(Project project, RoomType roomType) {
    return Room.builder()
        .project(project)
        .slug(randomRoomSlug())
        .title("Sprint 1 Planning")
        .description("Estimate sprint backlog items")
        .roomType(roomType)
        .deadline(Instant.now().plus(7, ChronoUnit.DAYS))
        .status(RoomStatus.OPEN.name())
        .build();
  }

  public static Room revealedRoom(Project project) {
    return Room.builder()
        .project(project)
        .slug(randomRoomSlug())
        .title("Sprint 2 Planning")
        .description("Revealed room")
        .roomType(RoomType.ASYNC)
        .deadline(Instant.now().plus(7, ChronoUnit.DAYS))
        .status(RoomStatus.REVEALED.name())
        .build();
  }

  public static Room closedRoom(Project project) {
    return Room.builder()
        .project(project)
        .slug(randomRoomSlug())
        .title("Sprint 3 Planning")
        .description("Closed room")
        .roomType(RoomType.ASYNC)
        .deadline(Instant.now().plus(7, ChronoUnit.DAYS))
        .status(RoomStatus.CLOSED.name())
        .build();
  }

  public static Task task(Room room) {
    return task(room, "Implement login page", 0);
  }

  public static Task task(Room room, String title, int sortOrder) {
    return Task.builder().room(room).title(title).sortOrder(sortOrder).build();
  }

  public static Participant participant(Project project) {
    return participant(project, "dev-" + UUID.randomUUID().toString().substring(0, 4));
  }

  public static Participant participant(Project project, String nickname) {
    return Participant.builder().project(project).nickname(nickname).build();
  }

  public static Estimate estimate(Task task, Participant participant) {
    return estimate(task, participant, "5");
  }

  public static Estimate estimate(Task task, Participant participant, String storyPoints) {
    return Estimate.builder().task(task).participant(participant).storyPoints(storyPoints).build();
  }

  // ── Entity fixtures (integration tests with repository.save()) ──

  public static ProjectEntity projectEntity() {
    return projectEntity("Test Project", "tp" + UUID.randomUUID().toString().substring(0, 6));
  }

  public static ProjectEntity projectEntity(String name, String slug) {
    return ProjectEntity.builder().name(name).slug(slug).adminCode(TEST_ADMIN_CODE_HASH).build();
  }

  public static RoomEntity roomEntity(ProjectEntity project) {
    return roomEntity(project, "ASYNC");
  }

  public static RoomEntity roomEntity(ProjectEntity project, String roomType) {
    return RoomEntity.builder()
        .project(project)
        .slug(randomRoomSlug())
        .title("Sprint 1 Planning")
        .description("Estimate sprint backlog items")
        .roomType(roomType)
        .deadline(Instant.now().plus(7, ChronoUnit.DAYS))
        .status(RoomStatus.OPEN.name())
        .build();
  }

  public static RoomEntity revealedRoomEntity(ProjectEntity project) {
    return RoomEntity.builder()
        .project(project)
        .slug(randomRoomSlug())
        .title("Sprint 2 Planning")
        .description("Revealed room")
        .roomType("ASYNC")
        .deadline(Instant.now().plus(7, ChronoUnit.DAYS))
        .status(RoomStatus.REVEALED.name())
        .build();
  }

  public static RoomEntity closedRoomEntity(ProjectEntity project) {
    return RoomEntity.builder()
        .project(project)
        .slug(randomRoomSlug())
        .title("Sprint 3 Planning")
        .description("Closed room")
        .roomType("ASYNC")
        .deadline(Instant.now().plus(7, ChronoUnit.DAYS))
        .status(RoomStatus.CLOSED.name())
        .build();
  }

  public static RoomEntity expiredRoomEntity(ProjectEntity project) {
    return RoomEntity.builder()
        .project(project)
        .slug(randomRoomSlug())
        .title("Expired Sprint Planning")
        .description("Room with past deadline")
        .roomType("ASYNC")
        .deadline(Instant.now().minus(1, ChronoUnit.HOURS))
        .status(RoomStatus.OPEN.name())
        .build();
  }

  public static TaskEntity taskEntity(RoomEntity room) {
    return taskEntity(room, "Implement login page", 0);
  }

  public static TaskEntity taskEntity(RoomEntity room, String title, int sortOrder) {
    return TaskEntity.builder().room(room).title(title).sortOrder(sortOrder).build();
  }

  public static ParticipantEntity participantEntity(ProjectEntity project) {
    return participantEntity(project, "dev-" + UUID.randomUUID().toString().substring(0, 4));
  }

  public static ParticipantEntity participantEntity(ProjectEntity project, String nickname) {
    return ParticipantEntity.builder().project(project).nickname(nickname).build();
  }

  public static EstimateEntity estimateEntity(TaskEntity task, ParticipantEntity participant) {
    return estimateEntity(task, participant, "5");
  }

  public static EstimateEntity estimateEntity(
      TaskEntity task, ParticipantEntity participant, String storyPoints) {
    return EstimateEntity.builder()
        .task(task)
        .participant(participant)
        .storyPoints(storyPoints)
        .build();
  }
}
