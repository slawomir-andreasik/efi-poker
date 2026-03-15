package com.andreasik.efipoker.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {

  @Query(
      """
      SELECT p FROM ProjectEntity p
      LEFT JOIN FETCH p.createdBy
      WHERE p.slug = :slug
      """)
  Optional<ProjectEntity> findBySlug(@Param("slug") String slug);

  @Override
  @Query(
      """
      SELECT p FROM ProjectEntity p
      LEFT JOIN FETCH p.createdBy
      WHERE p.id = :id
      """)
  Optional<ProjectEntity> findById(@Param("id") UUID id);

  @Query(
      """
      SELECT p FROM ProjectEntity p
      LEFT JOIN FETCH p.createdBy
      WHERE p.createdBy.id = :ownerId
      ORDER BY p.createdAt DESC
      """)
  List<ProjectEntity> findByOwnerId(@Param("ownerId") UUID ownerId);

  @Query(
      """
      SELECT p FROM ProjectEntity p
      LEFT JOIN FETCH p.createdBy
      WHERE p.id IN :ids
      ORDER BY p.createdAt DESC
      """)
  List<ProjectEntity> findByIdIn(@Param("ids") List<UUID> ids);

  @Query(
      value =
          """
      SELECT DISTINCT pt.project_id FROM participants pt
      WHERE pt.user_id = :userId
      """,
      nativeQuery = true)
  List<UUID> findParticipatedProjectIds(@Param("userId") UUID userId);
}
