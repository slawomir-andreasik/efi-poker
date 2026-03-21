package com.andreasik.efipoker.auth;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

  void deleteAllByUserId(UUID userId);

  @Modifying
  @Query("DELETE FROM RefreshTokenEntity r WHERE r.expiresAt < :now")
  int deleteExpired(Instant now);
}
