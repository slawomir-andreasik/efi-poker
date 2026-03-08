package com.andreasik.efipoker.participant;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ParticipantRepository extends JpaRepository<ParticipantEntity, UUID> {

  @Query(
      """
      SELECT pt FROM ParticipantEntity pt
      JOIN FETCH pt.project p
      LEFT JOIN FETCH p.createdBy
      LEFT JOIN FETCH pt.user
      WHERE pt.project.id = :projectId
        AND pt.nickname = :nickname
      """)
  Optional<ParticipantEntity> findByProjectIdAndNickname(
      @Param("projectId") UUID projectId, @Param("nickname") String nickname);

  @Query(
      """
      SELECT pt FROM ParticipantEntity pt
      JOIN FETCH pt.project p
      LEFT JOIN FETCH p.createdBy
      LEFT JOIN FETCH pt.user
      WHERE pt.project.id = :projectId
      """)
  List<ParticipantEntity> findByProjectId(@Param("projectId") UUID projectId);

  @Query(
      """
      SELECT pt FROM ParticipantEntity pt
      JOIN FETCH pt.project p
      LEFT JOIN FETCH p.createdBy
      LEFT JOIN FETCH pt.user
      WHERE pt.id = :id
        AND pt.project.id = :projectId
      """)
  Optional<ParticipantEntity> findByIdAndProjectId(
      @Param("id") UUID id, @Param("projectId") UUID projectId);

  @Query(
      """
      SELECT DISTINCT pt.project.id FROM ParticipantEntity pt
      WHERE pt.user.id = :userId
      """)
  List<UUID> findProjectIdsByUserId(@Param("userId") UUID userId);

  boolean existsByIdAndProjectId(UUID id, UUID projectId);

  long countByProjectId(UUID projectId);

  @Query(
      nativeQuery = true,
      value =
          "SELECT EXISTS(SELECT 1 FROM rooms r WHERE r.id = :roomId AND r.project_id = :projectId)")
  boolean existsRoomInProject(@Param("roomId") UUID roomId, @Param("projectId") UUID projectId);

  @Query(
      nativeQuery = true,
      value = "SELECT room_id FROM participant_room_access WHERE participant_id = :participantId")
  Set<UUID> findInvitedRoomIds(@Param("participantId") UUID participantId);

  @Query(
      nativeQuery = true,
      value =
          """
      SELECT participant_id, room_id
      FROM participant_room_access
      WHERE participant_id IN (:participantIds)
      """)
  List<Object[]> findInvitedRoomIdsByParticipantIds(
      @Param("participantIds") List<UUID> participantIds);

  @Modifying
  @Query(
      nativeQuery = true,
      value =
          """
      INSERT INTO participant_room_access (participant_id, room_id)
      VALUES (:participantId, :roomId)
      ON CONFLICT DO NOTHING
      """)
  void addRoomAccess(@Param("participantId") UUID participantId, @Param("roomId") UUID roomId);

  @Modifying
  @Query(
      nativeQuery = true,
      value = "DELETE FROM participant_room_access WHERE participant_id = :participantId")
  void clearRoomAccess(@Param("participantId") UUID participantId);
}
