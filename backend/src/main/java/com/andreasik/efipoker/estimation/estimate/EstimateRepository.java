package com.andreasik.efipoker.estimation.estimate;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EstimateRepository extends JpaRepository<EstimateEntity, UUID> {

  @Query(
      """
      SELECT e FROM EstimateEntity e
      JOIN FETCH e.task t
      JOIN FETCH t.room r
      JOIN FETCH r.project p
      LEFT JOIN FETCH p.createdBy
      JOIN FETCH e.participant pt
      JOIN FETCH pt.project pp
      LEFT JOIN FETCH pp.createdBy
      WHERE e.task.id = :taskId
      """)
  List<EstimateEntity> findByTaskId(@Param("taskId") UUID taskId);

  @Query(
      """
      SELECT e FROM EstimateEntity e
      JOIN FETCH e.task t
      JOIN FETCH t.room r
      JOIN FETCH r.project p
      LEFT JOIN FETCH p.createdBy
      JOIN FETCH e.participant pt
      JOIN FETCH pt.project pp
      LEFT JOIN FETCH pp.createdBy
      WHERE e.task.id IN :taskIds
      """)
  List<EstimateEntity> findByTaskIds(@Param("taskIds") Collection<UUID> taskIds);

  @Query(
      """
      SELECT e FROM EstimateEntity e
      JOIN FETCH e.task t
      JOIN FETCH t.room r
      JOIN FETCH r.project p
      LEFT JOIN FETCH p.createdBy
      JOIN FETCH e.participant pt
      JOIN FETCH pt.project pp
      LEFT JOIN FETCH pp.createdBy
      WHERE e.task.id = :taskId
        AND e.participant.id = :participantId
      """)
  Optional<EstimateEntity> findByTaskAndParticipant(
      @Param("taskId") UUID taskId, @Param("participantId") UUID participantId);

  void deleteByTaskIdAndParticipantId(UUID taskId, UUID participantId);

  void deleteByTaskId(UUID taskId);
}
