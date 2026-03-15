package com.andreasik.efipoker.auth;

import com.andreasik.efipoker.shared.exception.ConflictException;
import com.andreasik.efipoker.shared.exception.ResourceNotFoundException;
import com.andreasik.efipoker.shared.exception.UnauthorizedException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class UserService {

  private final UserRepository userRepository;
  private final UserEntityMapper userEntityMapper;
  private final PasswordEncoder passwordEncoder;

  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username).map(userEntityMapper::toDomain);
  }

  public Optional<User> findById(UUID id) {
    return userRepository.findById(id).map(userEntityMapper::toDomain);
  }

  @Transactional
  public void updateLastLogin(String username) {
    log.info("updateLastLogin: username={}", username);
    UserEntity entity =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    entity.setLastLoginAt(Instant.now());
    userRepository.save(entity);
  }

  @Transactional
  public User registerLocalUser(String username, String password, String email) {
    if (userRepository.findByUsername(username).isPresent()) {
      log.warn("Registration attempt with existing username: {}", username);
      throw new ConflictException("Username already taken: " + username);
    }

    UserEntity entity =
        UserEntity.builder()
            .username(username)
            .passwordHash(passwordEncoder.encode(password))
            .email(email)
            .authProvider(AuthProvider.LOCAL.name())
            .role(UserRole.USER.name())
            .build();

    UserEntity saved = userRepository.save(entity);
    log.info("User registered: username={}", username);
    return userEntityMapper.toDomain(saved);
  }

  @Transactional
  public User findOrCreateOAuth2User(String sub, String email, String name) {
    // Look up by auth provider + provider ID first (stable across email changes)
    return userRepository
        .findByAuthProviderAndAuthProviderId(AuthProvider.AUTH0.name(), sub)
        .map(
            entity -> {
              log.debug("Found existing OAuth2 user by provider ID: {}", entity.getUsername());
              return userEntityMapper.toDomain(entity);
            })
        .orElseGet(
            () -> {
              // Fall back to email match (merges with existing local account, but not LDAP)
              return userRepository
                  .findByEmail(email)
                  .filter(entity -> !AuthProvider.LDAP.name().equals(entity.getAuthProvider()))
                  .map(
                      entity -> {
                        log.info(
                            "Linking Auth0 sub to existing user by email: {}...",
                            email != null
                                ? email.substring(0, Math.min(5, email.length()))
                                : "null");
                        entity.setAuthProvider(AuthProvider.AUTH0.name());
                        entity.setAuthProviderId(sub);
                        return userEntityMapper.toDomain(userRepository.save(entity));
                      })
                  .orElseGet(
                      () -> {
                        log.info("Creating new OAuth2 user for Auth0 sub");
                        String username = buildUsername(name, email);
                        UserEntity newUser =
                            UserEntity.builder()
                                .username(username)
                                .email(email)
                                .authProvider(AuthProvider.AUTH0.name())
                                .authProviderId(sub)
                                .role(UserRole.USER.name())
                                .build();
                        return userEntityMapper.toDomain(userRepository.save(newUser));
                      });
            });
  }

  @Transactional
  public User findOrCreateLdapUser(String uid, String email, boolean isAdmin) {
    return userRepository
        .findByAuthProviderAndAuthProviderId(AuthProvider.LDAP.name(), uid)
        .map(
            entity -> {
              log.debug("Found existing LDAP user: {}", entity.getUsername());
              String expectedRole = isAdmin ? UserRole.ADMIN.name() : UserRole.USER.name();
              if (!expectedRole.equals(entity.getRole())) {
                entity.setRole(expectedRole);
                userRepository.save(entity);
                log.info(
                    "LDAP user role synced: username={}, role={}",
                    entity.getUsername(),
                    expectedRole);
              }
              return userEntityMapper.toDomain(entity);
            })
        .orElseGet(
            () -> {
              log.info("Creating new LDAP user: uid={}", uid);
              String role = isAdmin ? UserRole.ADMIN.name() : UserRole.USER.name();
              UserEntity newUser =
                  UserEntity.builder()
                      .username(uid)
                      .email(email)
                      .authProvider(AuthProvider.LDAP.name())
                      .authProviderId(uid)
                      .role(role)
                      .build();
              return userEntityMapper.toDomain(userRepository.save(newUser));
            });
  }

  @Transactional
  public void changePassword(UUID userId, String currentPassword, String newPassword) {
    UserEntity entity =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

    if (AuthProvider.LDAP.name().equals(entity.getAuthProvider())) {
      log.warn("Password change blocked for LDAP user: id={}", userId);
      throw new UnauthorizedException("Password changes are not allowed for LDAP users");
    }

    if (entity.getPasswordHash() != null) {
      // User has existing password - must verify current password
      if (currentPassword == null
          || !passwordEncoder.matches(currentPassword, entity.getPasswordHash())) {
        log.warn("Password change failed: invalid current password for user={}", userId);
        throw new UnauthorizedException("Current password is incorrect");
      }
    }
    // Auth0 user without password can set one without currentPassword

    entity.setPasswordHash(passwordEncoder.encode(newPassword));
    userRepository.save(entity);
    log.info("Password changed for user: id={}", userId);
  }

  @Transactional
  public void adminResetPassword(UUID userId, String newPassword) {
    UserEntity entity =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

    if (AuthProvider.LDAP.name().equals(entity.getAuthProvider())) {
      log.warn("Admin password reset blocked for LDAP user: id={}", userId);
      throw new UnauthorizedException("Password resets are not allowed for LDAP users");
    }

    entity.setPasswordHash(passwordEncoder.encode(newPassword));
    userRepository.save(entity);
    log.info("Admin reset password for user: id={}", userId);
  }

  public Page<User> listUsers(int page, int size, String search) {
    int clampedSize = Math.min(size, 100);
    PageRequest pageRequest = PageRequest.of(page, clampedSize, Sort.by("createdAt").descending());
    Page<UserEntity> entities;
    if (search != null && !search.isBlank()) {
      String trimmed = search.trim();
      if (trimmed.length() > 100) {
        trimmed = trimmed.substring(0, 100);
      }
      entities = userRepository.searchByUsernameOrEmail(trimmed, pageRequest);
    } else {
      entities = userRepository.findAll(pageRequest);
    }
    return entities.map(userEntityMapper::toDomain);
  }

  @Transactional
  public User adminCreateUser(String username, String password, String email, String role) {
    if (userRepository.findByUsername(username).isPresent()) {
      log.warn("Admin create user: username already taken: {}", username);
      throw new ConflictException("Username already taken: " + username);
    }

    UserEntity entity =
        UserEntity.builder()
            .username(username)
            .passwordHash(passwordEncoder.encode(password))
            .email(email)
            .authProvider(AuthProvider.LOCAL.name())
            .role(role)
            .build();

    UserEntity saved = userRepository.save(entity);
    log.info("Admin created user: username={}, role={}", username, role);
    return userEntityMapper.toDomain(saved);
  }

  @Transactional
  public User adminUpdateUser(UUID id, String email, String role) {
    UserEntity entity =
        userRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

    if (email != null) {
      entity.setEmail(email);
    }
    if (role != null) {
      entity.setRole(role);
    }

    UserEntity saved = userRepository.save(entity);
    log.info("Admin updated user: id={}, role={}", id, role);
    return userEntityMapper.toDomain(saved);
  }

  @Transactional
  public void deleteUser(UUID id, UUID currentUserId) {
    if (id.equals(currentUserId)) {
      log.warn("Admin attempted to delete themselves: id={}", id);
      throw new ConflictException("Cannot delete yourself");
    }

    if (!userRepository.existsById(id)) {
      throw new ResourceNotFoundException("User not found: " + id);
    }

    userRepository.deleteById(id);
    log.info("Admin deleted user: id={}", id);
  }

  private String buildUsername(String name, String email) {
    String base;
    if (name != null && !name.isBlank()) {
      String cleaned = name.toLowerCase().replaceAll("[^a-z0-9]", "");
      base = cleaned.substring(0, Math.min(cleaned.length(), 20));
    } else if (email != null) {
      base = email.split("@")[0];
    } else {
      base = "user";
    }
    if (base.isBlank()) {
      base = "user";
    }
    String candidate = base;
    int suffix = 1;
    while (userRepository.findByUsername(candidate).isPresent()) {
      candidate = base + suffix++;
    }
    return candidate;
  }
}
