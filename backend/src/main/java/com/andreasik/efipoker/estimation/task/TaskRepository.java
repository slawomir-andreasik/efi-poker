package com.andreasik.efipoker.estimation.task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {

  @Query(
      """
      SELECT t FROM TaskEntity t
      JOIN FETCH t.room r
      JOIN FETCH r.project p
      LEFT JOIN FETCH p.createdBy
      WHERE t.room.id = :roomId
      ORDER BY t.sortOrder ASC
      """)
  List<TaskEntity> findByRoomId(@Param("roomId") UUID roomId);

  @Query(
      """
      SELECT t FROM TaskEntity t
      JOIN FETCH t.room r
      JOIN FETCH r.project p
      LEFT JOIN FETCH p.createdBy
      WHERE t.room.id = :roomId
        AND t.title = :title
      """)
  Optional<TaskEntity> findByRoomIdAndTitle(
      @Param("roomId") UUID roomId, @Param("title") String title);

  @Override
  @Query(
      """
      SELECT t FROM TaskEntity t
      JOIN FETCH t.room r
      JOIN FETCH r.project p
      LEFT JOIN FETCH p.createdBy
      WHERE t.id = :id
      """)
  Optional<TaskEntity> findById(@Param("id") UUID id);
}
