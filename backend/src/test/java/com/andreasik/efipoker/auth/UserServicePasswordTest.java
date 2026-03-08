package com.andreasik.efipoker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.andreasik.efipoker.shared.exception.ResourceNotFoundException;
import com.andreasik.efipoker.shared.exception.UnauthorizedException;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("UserService - Password")
class UserServicePasswordTest extends BaseUnitTest {

  @Mock private UserRepository userRepository;
  @Mock private UserEntityMapper userEntityMapper;
  @Mock private PasswordEncoder passwordEncoder;
  @InjectMocks private UserService userService;

  @Nested
  @DisplayName("changePassword")
  class ChangePassword {

    @Test
    void should_change_password_with_valid_current_password() {
      // Arrange
      UUID userId = UUID.randomUUID();
      UserEntity entity =
          UserEntity.builder()
              .id(userId)
              .username("john")
              .passwordHash("$2a$old_hash")
              .authProvider("LOCAL")
              .build();

      given(userRepository.findById(userId)).willReturn(Optional.of(entity));
      given(passwordEncoder.matches("oldpass", "$2a$old_hash")).willReturn(true);
      given(passwordEncoder.encode("newpass123")).willReturn("$2a$new_hash");

      // Act
      userService.changePassword(userId, "oldpass", "newpass123");

      // Assert
      ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
      then(userRepository).should().save(captor.capture());
      assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$new_hash");
    }

    @Test
    void should_reject_wrong_current_password() {
      // Arrange
      UUID userId = UUID.randomUUID();
      UserEntity entity =
          UserEntity.builder()
              .id(userId)
              .username("john")
              .passwordHash("$2a$old_hash")
              .authProvider("LOCAL")
              .build();

      given(userRepository.findById(userId)).willReturn(Optional.of(entity));
      given(passwordEncoder.matches("wrongpass", "$2a$old_hash")).willReturn(false);

      // Act & Assert
      assertThatThrownBy(() -> userService.changePassword(userId, "wrongpass", "newpass123"))
          .isInstanceOf(UnauthorizedException.class)
          .hasMessageContaining("Current password is incorrect");

      then(userRepository).should(never()).save(entity);
    }

    @Test
    void should_reject_null_current_password_when_user_has_password() {
      // Arrange
      UUID userId = UUID.randomUUID();
      UserEntity entity =
          UserEntity.builder()
              .id(userId)
              .username("john")
              .passwordHash("$2a$old_hash")
              .authProvider("LOCAL")
              .build();

      given(userRepository.findById(userId)).willReturn(Optional.of(entity));

      // Act & Assert
      assertThatThrownBy(() -> userService.changePassword(userId, null, "newpass123"))
          .isInstanceOf(UnauthorizedException.class);

      then(userRepository).should(never()).save(entity);
    }

    @Test
    void should_allow_auth0_user_to_set_password_without_current() {
      // Arrange
      UUID userId = UUID.randomUUID();
      UserEntity entity =
          UserEntity.builder()
              .id(userId)
              .username("auth0user")
              .passwordHash(null)
              .authProvider("AUTH0")
              .build();

      given(userRepository.findById(userId)).willReturn(Optional.of(entity));
      given(passwordEncoder.encode("newpass123")).willReturn("$2a$new_hash");

      // Act
      userService.changePassword(userId, null, "newpass123");

      // Assert
      ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
      then(userRepository).should().save(captor.capture());
      assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$new_hash");
    }

    @Test
    void should_block_password_change_for_ldap_user() {
      // Arrange
      UUID userId = UUID.randomUUID();
      UserEntity entity =
          UserEntity.builder()
              .id(userId)
              .username("ldapuser")
              .passwordHash(null)
              .authProvider("LDAP")
              .build();

      given(userRepository.findById(userId)).willReturn(Optional.of(entity));

      // Act & Assert
      assertThatThrownBy(() -> userService.changePassword(userId, null, "newpass123"))
          .isInstanceOf(UnauthorizedException.class)
          .hasMessageContaining("LDAP");

      then(userRepository).should(never()).save(entity);
    }

    @Test
    void should_throw_not_found_for_unknown_user() {
      // Arrange
      UUID userId = UUID.randomUUID();
      given(userRepository.findById(userId)).willReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> userService.changePassword(userId, "old", "new12345"))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("adminResetPassword")
  class AdminResetPassword {

    @Test
    void should_reset_password() {
      // Arrange
      UUID userId = UUID.randomUUID();
      UserEntity entity =
          UserEntity.builder()
              .id(userId)
              .username("john")
              .passwordHash("$2a$old_hash")
              .authProvider("LOCAL")
              .build();

      given(userRepository.findById(userId)).willReturn(Optional.of(entity));
      given(passwordEncoder.encode("adminreset")).willReturn("$2a$admin_hash");

      // Act
      userService.adminResetPassword(userId, "adminreset");

      // Assert
      ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
      then(userRepository).should().save(captor.capture());
      assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$admin_hash");
    }

    @Test
    void should_block_admin_reset_for_ldap_user() {
      // Arrange
      UUID userId = UUID.randomUUID();
      UserEntity entity =
          UserEntity.builder()
              .id(userId)
              .username("ldapuser")
              .passwordHash(null)
              .authProvider("LDAP")
              .build();

      given(userRepository.findById(userId)).willReturn(Optional.of(entity));

      // Act & Assert
      assertThatThrownBy(() -> userService.adminResetPassword(userId, "newpass123"))
          .isInstanceOf(UnauthorizedException.class)
          .hasMessageContaining("LDAP");

      then(userRepository).should(never()).save(entity);
    }

    @Test
    void should_throw_not_found_for_unknown_user() {
      // Arrange
      UUID userId = UUID.randomUUID();
      given(userRepository.findById(userId)).willReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> userService.adminResetPassword(userId, "newpass12"))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }
}
