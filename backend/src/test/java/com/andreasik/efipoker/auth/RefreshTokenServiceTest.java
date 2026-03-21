package com.andreasik.efipoker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.andreasik.efipoker.shared.exception.UnauthorizedException;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest extends BaseUnitTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private UserRepository userRepository;
  @Mock private UserEntityMapper userEntityMapper;

  private RefreshTokenService service;

  @BeforeEach
  void setUp() {
    JwtProperties props = new JwtProperties("secret", 3600, 7776000, 600, 1200);
    service =
        new RefreshTokenService(refreshTokenRepository, userRepository, userEntityMapper, props);
  }

  @Nested
  @DisplayName("createRefreshToken")
  class CreateRefreshToken {

    @Test
    void should_create_token_and_store_hash() {
      when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      String rawToken = service.createRefreshToken(UUID.randomUUID(), false);

      assertThat(rawToken).hasSize(64); // 32 bytes = 64 hex chars
      ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
      verify(refreshTokenRepository).save(captor.capture());
      assertThat(captor.getValue().getTokenHash()).isNotEqualTo(rawToken);
      assertThat(captor.getValue().getTokenHash()).hasSize(64); // SHA-256 = 64 hex chars
    }

    @Test
    void should_use_longer_ttl_for_remember_me() {
      when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.createRefreshToken(UUID.randomUUID(), true);

      ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
      verify(refreshTokenRepository).save(captor.capture());
      // Remember me TTL is 1200s, default is 600s
      Instant expiresAt = captor.getValue().getExpiresAt();
      assertThat(expiresAt).isAfter(Instant.now().plusSeconds(1000));
    }
  }

  @Nested
  @DisplayName("rotateRefreshToken")
  class RotateRefreshToken {

    @Test
    void should_reject_invalid_token() {
      when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.rotateRefreshToken("invalid"))
          .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void should_reject_expired_token() {
      RefreshTokenEntity entity = new RefreshTokenEntity();
      entity.setExpiresAt(Instant.now().minusSeconds(60));
      when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(entity));

      assertThatThrownBy(() -> service.rotateRefreshToken("expired"))
          .isInstanceOf(UnauthorizedException.class)
          .hasMessageContaining("expired");
    }

    @Test
    void should_rotate_valid_token() {
      UUID userId = UUID.randomUUID();
      RefreshTokenEntity entity = new RefreshTokenEntity();
      entity.setTokenHash("old-hash");
      entity.setUserId(userId);
      entity.setExpiresAt(Instant.now().plusSeconds(3600));
      entity.setCreatedAt(Instant.now().minusSeconds(100));

      UserEntity userEntity = UserEntity.builder().id(userId).username("test").role("USER").build();
      User user = User.builder().id(userId).username("test").role("USER").build();

      when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(entity));
      when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
      when(userEntityMapper.toDomain(userEntity)).thenReturn(user);
      when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      RefreshTokenService.RotationResult result = service.rotateRefreshToken("valid-token");

      assertThat(result.newRawToken()).isNotNull().hasSize(64);
      assertThat(result.user().username()).isEqualTo("test");
      verify(refreshTokenRepository).delete(entity);
    }
  }

  @Nested
  @DisplayName("hashToken")
  class HashToken {

    @Test
    void should_produce_consistent_hash() {
      String hash1 = RefreshTokenService.hashToken("test");
      String hash2 = RefreshTokenService.hashToken("test");
      assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void should_produce_different_hashes_for_different_inputs() {
      String hash1 = RefreshTokenService.hashToken("token1");
      String hash2 = RefreshTokenService.hashToken("token2");
      assertThat(hash1).isNotEqualTo(hash2);
    }
  }
}
