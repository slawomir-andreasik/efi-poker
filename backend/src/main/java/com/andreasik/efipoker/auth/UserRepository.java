package com.andreasik.efipoker.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

  Optional<UserEntity> findByUsername(String username);

  Optional<UserEntity> findByEmail(String email);

  Optional<UserEntity> findByAuthProviderAndAuthProviderId(
      String authProvider, String authProviderId);

  @Query(
      "SELECT u FROM UserEntity u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))"
          + " OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
  Page<UserEntity> searchByUsernameOrEmail(String search, Pageable pageable);
}
