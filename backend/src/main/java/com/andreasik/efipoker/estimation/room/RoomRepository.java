package com.andreasik.efipoker.estimation.room;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, UUID> {

  @Query(
      """
      SELECT r FROM RoomEntity r
      JOIN FETCH r.project p
      LEFT JOIN FETCH p.createdBy
      WHERE r.slug = :slug
      """)
  Optional<RoomEntity> findBySlug(@Param("slug") String slug);

  @Override
  @Query(
      """
      SELECT r FROM RoomEntity r
      JOIN FETCH r.project p
      LEFT JOIN FETCH p.createdBy
      WHERE r.id = :id
      """)
  Optional<RoomEntity> findById(@Param("id") UUID id);

  @Query(
      """
      SELECT r FROM RoomEntity r
      JOIN FETCH r.project p
      LEFT JOIN FETCH p.createdBy
      WHERE r.project.id = :projectId
      ORDER BY r.createdAt DESC
      """)
  List<RoomEntity> findByProjectId(@Param("projectId") UUID projectId);

  @Query(
      """
      SELECT r FROM RoomEntity r
      JOIN FETCH r.project p
      LEFT JOIN FETCH p.createdBy
      WHERE r.status = :status
        AND r.roomType = :roomType
        AND r.deadline < :deadline
      """)
  List<RoomEntity> findExpired(
      @Param("status") String status,
      @Param("roomType") String roomType,
      @Param("deadline") Instant deadline);

  long countByStatus(String status);
}
