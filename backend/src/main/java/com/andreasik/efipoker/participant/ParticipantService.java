package com.andreasik.efipoker.participant;

import com.andreasik.efipoker.auth.UserEntity;
import com.andreasik.efipoker.project.ProjectApi;
import com.andreasik.efipoker.project.ProjectEntity;
import com.andreasik.efipoker.shared.exception.ResourceNotFoundException;
import com.andreasik.efipoker.shared.exception.UnauthorizedException;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Transactional(readOnly = true)
public class ParticipantService implements ParticipantApi {

  private final ParticipantRepository participantRepository;
  private final ProjectApi projectApi;
  private final EntityManager entityManager;
  private final ParticipantEntityMapper participantEntityMapper;

  @Transactional
  public Participant joinProject(
      UUID projectId, String nickname, @Nullable UUID userId, @Nullable UUID roomId) {
    ParticipantEntity entity =
        participantRepository
            .findByProjectIdAndNickname(projectId, nickname)
            .map(
                existing -> {
                  if (existing.getUser() == null && userId != null) {
                    existing.setUser(entityManager.getReference(UserEntity.class, userId));
                    existing = participantRepository.save(existing);
                    log.info(
                        "Backfilled userId for participant: projectId={}, nickname={}",
                        projectId,
                        nickname);
                  }
                  return existing;
                })
            .orElseGet(
                () -> {
                  projectApi.validateProjectExists(projectId);
                  ProjectEntity project =
                      entityManager.getReference(ProjectEntity.class, projectId);
                  ParticipantEntity newEntity =
                      ParticipantEntity.builder()
                          .project(project)
                          .nickname(nickname)
                          .user(
                              userId != null
                                  ? entityManager.getReference(UserEntity.class, userId)
                                  : null)
                          .build();
                  ParticipantEntity saved = participantRepository.save(newEntity);
                  log.info(
                      "Participant joined project: projectId={}, nickname={}, roomScoped={}",
                      projectId,
                      nickname,
                      roomId != null);
                  return saved;
                });

    if (roomId != null) {
      if (!participantRepository.existsRoomInProject(roomId, projectId)) {
        log.warn("Invalid roomId for project: roomId={}, projectId={}", roomId, projectId);
        throw new ResourceNotFoundException("Room", roomId);
      }
      participantRepository.addRoomAccess(entity.getId(), roomId);
      log.info("Added room access: participantId={}, roomId={}", entity.getId(), roomId);
    } else {
      participantRepository.clearRoomAccess(entity.getId());
      log.info("Cleared room access (project-wide): participantId={}", entity.getId());
    }

    return enrichWithRoomAccess(participantEntityMapper.toDomain(entity));
  }

  public List<Participant> listParticipants(UUID projectId) {
    log.debug("listParticipants: projectId={}", projectId);
    List<Participant> participants =
        participantEntityMapper.toDomainList(participantRepository.findByProjectId(projectId));
    return enrichListWithRoomAccess(participants);
  }

  public Participant getParticipant(UUID projectId, UUID participantId) {
    ParticipantEntity entity =
        participantRepository
            .findByIdAndProjectId(participantId, projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Participant", participantId));
    return enrichWithRoomAccess(participantEntityMapper.toDomain(entity));
  }

  @Transactional
  public void deleteParticipant(UUID projectId, UUID participantId) {
    ParticipantEntity entity =
        participantRepository
            .findByIdAndProjectId(participantId, projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Participant", participantId));
    participantRepository.delete(entity);
    log.info("Participant deleted: projectId={}, participantId={}", projectId, participantId);
  }

  @Transactional
  public Participant updateNickname(UUID projectId, UUID participantId, String newNickname) {
    ParticipantEntity entity =
        participantRepository
            .findByIdAndProjectId(participantId, projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Participant", participantId));

    if (entity.getNickname().equals(newNickname)) {
      return participantEntityMapper.toDomain(entity);
    }

    participantRepository
        .findByProjectIdAndNickname(projectId, newNickname)
        .ifPresent(
            existing -> {
              log.warn("Nickname taken: projectId={}, nickname={}", projectId, newNickname);
              throw new IllegalStateException(
                  "Nickname '" + newNickname + "' is already taken in this project");
            });

    entity.setNickname(newNickname);
    ParticipantEntity saved = participantRepository.save(entity);
    log.info(
        "Participant nickname updated: projectId={}, participantId={}, newNickname={}",
        projectId,
        participantId,
        newNickname);
    return participantEntityMapper.toDomain(saved);
  }

  public long countByProject(UUID projectId) {
    return participantRepository.countByProjectId(projectId);
  }

  public long countAll() {
    return participantRepository.count();
  }

  @Override
  public void validateParticipantExists(UUID participantId) {
    if (participantId == null || !participantRepository.existsById(participantId)) {
      log.warn("Invalid participant: id={}", participantId);
      throw new UnauthorizedException("Invalid or unknown participant");
    }
  }

  @Override
  public List<UUID> listParticipatedProjectIds(UUID userId) {
    return participantRepository.findProjectIdsByUserId(userId);
  }

  private Participant enrichWithRoomAccess(Participant participant) {
    Set<UUID> roomIds = participantRepository.findInvitedRoomIds(participant.id());
    return participant.toBuilder().invitedRoomIds(roomIds).build();
  }

  private List<Participant> enrichListWithRoomAccess(List<Participant> participants) {
    if (participants.isEmpty()) {
      return participants;
    }
    List<UUID> ids = participants.stream().map(Participant::id).toList();
    Map<UUID, Set<UUID>> roomIdsByParticipant = new HashMap<>();
    for (Object[] row : participantRepository.findInvitedRoomIdsByParticipantIds(ids)) {
      UUID participantId = (UUID) row[0];
      UUID roomId = (UUID) row[1];
      roomIdsByParticipant.computeIfAbsent(participantId, k -> new HashSet<>()).add(roomId);
    }
    return participants.stream()
        .map(
            p ->
                p.toBuilder()
                    .invitedRoomIds(roomIdsByParticipant.getOrDefault(p.id(), Set.of()))
                    .build())
        .toList();
  }
}
