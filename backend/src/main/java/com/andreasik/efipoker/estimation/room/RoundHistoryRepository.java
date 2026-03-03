package com.andreasik.efipoker.estimation.room;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoundHistoryRepository extends JpaRepository<RoundHistoryEntity, UUID> {

  @Query(
      """
      SELECT rh FROM RoundHistoryEntity rh
      JOIN FETCH rh.room r
      JOIN FETCH r.project p
      LEFT JOIN FETCH p.createdBy
      WHERE rh.room.id = :roomId
      ORDER BY rh.roundNumber ASC
      """)
  List<RoundHistoryEntity> findByRoomId(@Param("roomId") UUID roomId);
}
