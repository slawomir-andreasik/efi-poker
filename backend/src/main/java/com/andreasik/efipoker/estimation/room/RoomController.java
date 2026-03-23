package com.andreasik.efipoker.estimation.room;

import com.andreasik.efipoker.api.RoomsApi;
import com.andreasik.efipoker.api.model.CreateRoomRequest;
import com.andreasik.efipoker.api.model.FinishSessionResponse;
import com.andreasik.efipoker.api.model.LiveRoomStateResponse;
import com.andreasik.efipoker.api.model.NewRoundRequest;
import com.andreasik.efipoker.api.model.ParticipantProgressResponse;
import com.andreasik.efipoker.api.model.RoomAdminResponse;
import com.andreasik.efipoker.api.model.RoomDetailResponse;
import com.andreasik.efipoker.api.model.RoomProgressResponse;
import com.andreasik.efipoker.api.model.RoomResponse;
import com.andreasik.efipoker.api.model.RoomResultsResponse;
import com.andreasik.efipoker.api.model.RoomSlugResponse;
import com.andreasik.efipoker.api.model.RoundHistoryEntry;
import com.andreasik.efipoker.api.model.UpdateRoomRequest;
import com.andreasik.efipoker.participant.Participant;
import com.andreasik.efipoker.participant.ParticipantService;
import com.andreasik.efipoker.project.Project;
import com.andreasik.efipoker.project.ProjectService;
import com.andreasik.efipoker.shared.exception.UnauthorizedException;
import com.andreasik.efipoker.shared.security.SecurityUtils;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RoomController implements RoomsApi {

  private final RoomService roomService;
  private final ProjectService projectService;
  private final ParticipantService participantService;
  private final RoomMapper roomMapper;
  private final RoundHistoryService roundHistoryService;
  private final RoomResponseAssembler responseAssembler;
  private final RoomCsvExporter csvExporter;

  @Override
  public ResponseEntity<RoomResponse> createRoom(String slug, CreateRoomRequest createRoomRequest) {
    log.debug("POST /projects/{}/rooms type={}", slug, createRoomRequest.getRoomType());
    Project project = projectService.validateAdminAccessBySlug(slug);

    RoomType roomType = RoomType.valueOf(createRoomRequest.getRoomType().getValue());
    boolean autoReveal =
        createRoomRequest.getAutoRevealOnDeadline() == null
            || createRoomRequest.getAutoRevealOnDeadline();
    boolean commentRequired = Boolean.TRUE.equals(createRoomRequest.getCommentRequired());
    Room room =
        roomService.createRoom(
            new CreateRoomCommand(
                project.id(),
                createRoomRequest.getTitle(),
                createRoomRequest.getDescription(),
                roomType,
                createRoomRequest.getDeadline(),
                autoReveal,
                createRoomRequest.getCommentTemplate(),
                commentRequired));
    return ResponseEntity.status(HttpStatus.CREATED).body(roomMapper.toResponse(room));
  }

  @Override
  public ResponseEntity<List<RoomResponse>> listRooms(String slug) {
    log.debug("GET /projects/{}/rooms", slug);
    Project project = projectService.getProjectBySlug(slug);
    List<Room> rooms = roomService.listByProject(project.id());
    UUID participantId = SecurityUtils.getCurrentParticipantId();
    if (participantId != null) {
      Participant participant = participantService.getParticipant(project.id(), participantId);
      Set<UUID> invitedRoomIds = participant.invitedRoomIds();
      if (invitedRoomIds != null && !invitedRoomIds.isEmpty()) {
        rooms = rooms.stream().filter(r -> invitedRoomIds.contains(r.id())).toList();
      }
    }
    return ResponseEntity.ok(roomMapper.toResponseList(rooms));
  }

  @Override
  public ResponseEntity<RoomDetailResponse> getRoom(UUID roomId) {
    log.debug("GET /rooms/{}", roomId);
    Room room = roomService.getRoom(roomId);
    UUID participantId = SecurityUtils.getCurrentParticipantId();
    return ResponseEntity.ok(responseAssembler.buildDetailResponse(room, participantId));
  }

  @Override
  public ResponseEntity<RoomSlugResponse> getRoomBySlug(String roomSlug) {
    log.debug("GET /rooms/by-slug/{}", roomSlug);
    Room room = roomService.getRoomBySlug(roomSlug);
    return ResponseEntity.ok(roomMapper.toSlugResponse(room));
  }

  @Override
  public ResponseEntity<RoomAdminResponse> getRoomAdmin(UUID roomId) {
    log.debug("GET /rooms/{}/admin", roomId);
    Room room = roomService.validateAdminAndGetRoom(roomId);
    return ResponseEntity.ok(responseAssembler.buildAdminResponse(room));
  }

  @Override
  public ResponseEntity<RoomResponse> updateRoom(UUID roomId, UpdateRoomRequest updateRoomRequest) {
    log.debug("PATCH /rooms/{}", roomId);
    roomService.validateAdminAndGetRoom(roomId);

    Room updated =
        roomService.updateRoom(
            new UpdateRoomCommand(
                roomId,
                updateRoomRequest.getTitle(),
                updateRoomRequest.getDescription(),
                updateRoomRequest.getDeadline(),
                updateRoomRequest.getTopic(),
                updateRoomRequest.getCommentTemplate(),
                updateRoomRequest.getCommentRequired(),
                updateRoomRequest.getAutoRevealOnDeadline()));
    return ResponseEntity.ok(roomMapper.toResponse(updated));
  }

  @Override
  public ResponseEntity<Void> deleteRoom(UUID roomId) {
    log.debug("DELETE /rooms/{}", roomId);
    Room room = roomService.validateAdminAndGetRoom(roomId);
    roomService.deleteRoom(room);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<RoomDetailResponse> revealEstimates(UUID roomId) {
    log.debug("POST /rooms/{}/reveal", roomId);
    roomService.validateAdminAndGetRoom(roomId);
    Room revealed = roomService.revealRoom(roomId);
    return ResponseEntity.ok(responseAssembler.buildDetailResponse(revealed, null));
  }

  @Override
  public ResponseEntity<RoomDetailResponse> reopenRoom(UUID roomId) {
    log.debug("POST /rooms/{}/reopen", roomId);
    roomService.validateAdminAndGetRoom(roomId);
    Room reopened = roomService.reopenRoom(roomId);
    return ResponseEntity.ok(responseAssembler.buildDetailResponse(reopened, null));
  }

  @Override
  public ResponseEntity<FinishSessionResponse> finishSession(UUID roomId, Boolean revealVotes) {
    log.debug("POST /rooms/{}/finish revealVotes={}", roomId, revealVotes);
    roomService.validateAdminAndGetRoom(roomId);
    boolean reveal = revealVotes == null || revealVotes;
    RoomService.FinishSessionResult result = roomService.finishSession(roomId, reveal);
    return ResponseEntity.ok(responseAssembler.buildFinishSessionResponse(result));
  }

  @Override
  public ResponseEntity<RoomProgressResponse> getRoomProgress(UUID roomId) {
    log.debug("GET /rooms/{}/progress", roomId);
    Room room = roomService.getRoom(roomId);
    return ResponseEntity.ok(responseAssembler.buildProgressResponse(room));
  }

  @Override
  public ResponseEntity<ParticipantProgressResponse> getParticipantProgress(UUID roomId) {
    log.debug("GET /rooms/{}/participant-progress", roomId);
    Room room = roomService.getRoom(roomId);
    return ResponseEntity.ok(responseAssembler.buildParticipantProgressResponse(room));
  }

  @Override
  public ResponseEntity<RoomResultsResponse> getRoomResults(UUID roomId) {
    log.debug("GET /rooms/{}/results", roomId);
    Room room = roomService.getRoom(roomId);
    validateRevealed(room);
    return ResponseEntity.ok(responseAssembler.buildResultsResponse(room));
  }

  @Override
  public ResponseEntity<String> exportResults(UUID roomId) {
    log.debug("GET /rooms/{}/export", roomId);
    Room room = roomService.validateAdminAndGetRoom(roomId);
    validateRevealed(room);
    return ResponseEntity.ok()
        .header("Content-Type", "text/csv; charset=UTF-8")
        .header("Content-Disposition", "attachment; filename=\"room-" + roomId + ".csv\"")
        .body(csvExporter.export(room));
  }

  @Override
  public ResponseEntity<LiveRoomStateResponse> getLiveRoomState(UUID roomId) {
    log.debug("GET /rooms/{}/live", roomId);
    Room room = roomService.getRoom(roomId);
    validateLiveRoom(room);
    UUID participantId = SecurityUtils.getCurrentParticipantId();
    return ResponseEntity.ok(responseAssembler.buildLiveRoomStateResponse(room, participantId));
  }

  @Override
  public ResponseEntity<LiveRoomStateResponse> newRound(
      UUID roomId, NewRoundRequest newRoundRequest) {
    log.debug("POST /rooms/{}/new-round", roomId);
    roomService.validateAdminAndGetRoom(roomId);
    String topic = newRoundRequest != null ? newRoundRequest.getTopic() : null;
    Room updated = roomService.newRound(roomId, topic);
    return ResponseEntity.ok(responseAssembler.buildLiveRoomStateResponse(updated, null));
  }

  @Override
  public ResponseEntity<List<RoundHistoryEntry>> getRoundHistory(UUID roomId) {
    log.debug("GET /rooms/{}/history", roomId);
    Room room = roomService.getRoom(roomId);
    validateLiveRoom(room);
    return ResponseEntity.ok(roundHistoryService.getHistory(roomId));
  }

  private void validateRevealed(Room room) {
    if (!RoomService.isRevealedStatus(room.status())) {
      throw new UnauthorizedException("Results are not available until estimates are revealed");
    }
  }

  private void validateLiveRoom(Room room) {
    if (RoomType.LIVE != room.roomType()) {
      throw new IllegalStateException("This endpoint is only available for LIVE rooms");
    }
  }
}
