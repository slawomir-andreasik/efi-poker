package com.andreasik.efipoker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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

@DisplayName("UserService - OAuth2")
class UserServiceOAuth2Test extends BaseUnitTest {

  @Mock private UserRepository userRepository;
  @Mock private UserEntityMapper userEntityMapper;
  @Mock private PasswordEncoder passwordEncoder;
  @InjectMocks private UserService userService;

  @Nested
  @DisplayName("findOrCreateOAuth2User - email merge")
  class EmailMerge {

    @Test
    void should_not_overwrite_ldap_user_on_auth0_email_match() {
      // Arrange
      String auth0Sub = "auth0|abc123";
      String email = "alice@example.com";
      UserEntity ldapUser =
          UserEntity.builder()
              .id(UUID.randomUUID())
              .username("alice")
              .email(email)
              .authProvider("LDAP")
              .authProviderId("alice")
              .build();

      given(userRepository.findByAuthProviderAndAuthProviderId("AUTH0", auth0Sub))
          .willReturn(Optional.empty());
      given(userRepository.findByEmail(email)).willReturn(Optional.of(ldapUser));

      // New user created instead of hijacking LDAP user
      UserEntity newUser =
          UserEntity.builder()
              .id(UUID.randomUUID())
              .username("alice1")
              .email(email)
              .authProvider("AUTH0")
              .authProviderId(auth0Sub)
              .build();
      User expectedUser = User.builder().id(newUser.getId()).username("alice1").build();

      given(userRepository.findByUsername("alice")).willReturn(Optional.of(ldapUser));
      given(userRepository.findByUsername("alice1")).willReturn(Optional.empty());
      given(userRepository.save(any(UserEntity.class))).willReturn(newUser);
      given(userEntityMapper.toDomain(newUser)).willReturn(expectedUser);

      // Act
      User result = userService.findOrCreateOAuth2User(auth0Sub, email, "Alice");

      // Assert
      assertThat(result.username()).isEqualTo("alice1");

      // Verify LDAP user was NOT modified
      assertThat(ldapUser.getAuthProvider()).isEqualTo("LDAP");
      assertThat(ldapUser.getAuthProviderId()).isEqualTo("alice");

      // Verify a NEW user was saved (not the LDAP entity)
      ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
      then(userRepository).should().save(captor.capture());
      UserEntity saved = captor.getValue();
      assertThat(saved.getAuthProvider()).isEqualTo("AUTH0");
      assertThat(saved.getAuthProviderId()).isEqualTo(auth0Sub);
    }

    @Test
    void should_merge_local_user_on_auth0_email_match() {
      // Arrange
      String auth0Sub = "auth0|def456";
      String email = "bob@example.com";
      UUID existingId = UUID.randomUUID();
      UserEntity localUser =
          UserEntity.builder()
              .id(existingId)
              .username("bob")
              .email(email)
              .authProvider("LOCAL")
              .authProviderId(null)
              .passwordHash("$2a$hash")
              .build();

      given(userRepository.findByAuthProviderAndAuthProviderId("AUTH0", auth0Sub))
          .willReturn(Optional.empty());
      given(userRepository.findByEmail(email)).willReturn(Optional.of(localUser));
      given(userRepository.save(localUser)).willReturn(localUser);

      User expectedUser = User.builder().id(existingId).username("bob").build();
      given(userEntityMapper.toDomain(localUser)).willReturn(expectedUser);

      // Act
      User result = userService.findOrCreateOAuth2User(auth0Sub, email, "Bob");

      // Assert
      assertThat(result.id()).isEqualTo(existingId);

      // Verify LOCAL user WAS merged (provider overwritten to AUTH0)
      assertThat(localUser.getAuthProvider()).isEqualTo("AUTH0");
      assertThat(localUser.getAuthProviderId()).isEqualTo(auth0Sub);
      then(userRepository).should().save(localUser);
    }

    @Test
    void should_return_existing_user_when_found_by_provider_id() {
      // Arrange
      String auth0Sub = "auth0|existing";
      UUID existingId = UUID.randomUUID();
      UserEntity existingUser =
          UserEntity.builder()
              .id(existingId)
              .username("existing")
              .authProvider("AUTH0")
              .authProviderId(auth0Sub)
              .build();

      given(userRepository.findByAuthProviderAndAuthProviderId("AUTH0", auth0Sub))
          .willReturn(Optional.of(existingUser));

      User expectedUser = User.builder().id(existingId).username("existing").build();
      given(userEntityMapper.toDomain(existingUser)).willReturn(expectedUser);

      // Act
      User result = userService.findOrCreateOAuth2User(auth0Sub, "any@email.com", "Any");

      // Assert
      assertThat(result.id()).isEqualTo(existingId);
      then(userRepository).should(never()).findByEmail(any());
      then(userRepository).should(never()).save(any());
    }
  }
}
