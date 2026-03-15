package com.andreasik.efipoker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

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

  @Nested
  @DisplayName("findOrCreateOAuth2User - LDAP filter guard")
  class LdapFilterGuard {

    @Test
    void should_skip_ldap_user_and_create_new_user_on_email_match() {
      // Arrange - guards the !LDAP conditional: filter must block merging with LDAP accounts
      String auth0Sub = "auth0|xyz";
      String email = "ldapuser@example.com";
      UserEntity ldapUser =
          UserEntity.builder()
              .id(UUID.randomUUID())
              .username("ldapuser")
              .email(email)
              .authProvider("LDAP")
              .authProviderId("ldapuser")
              .build();
      UserEntity newUser =
          UserEntity.builder()
              .id(UUID.randomUUID())
              .username("ldapuser1")
              .email(email)
              .authProvider("AUTH0")
              .authProviderId(auth0Sub)
              .build();
      User expectedUser = User.builder().id(newUser.getId()).username("ldapuser1").build();

      given(userRepository.findByAuthProviderAndAuthProviderId("AUTH0", auth0Sub))
          .willReturn(Optional.empty());
      given(userRepository.findByEmail(email)).willReturn(Optional.of(ldapUser));
      given(userRepository.findByUsername("ldapuser")).willReturn(Optional.of(ldapUser));
      given(userRepository.findByUsername("ldapuser1")).willReturn(Optional.empty());
      given(userRepository.save(any(UserEntity.class))).willReturn(newUser);
      given(userEntityMapper.toDomain(newUser)).willReturn(expectedUser);

      // Act
      User result = userService.findOrCreateOAuth2User(auth0Sub, email, "Ldap User");

      // Assert - LDAP user must NOT be modified (filter blocked the merge)
      assertThat(ldapUser.getAuthProvider()).isEqualTo("LDAP");
      assertThat(ldapUser.getAuthProviderId()).isEqualTo("ldapuser");
      assertThat(result.username()).isEqualTo("ldapuser1");
    }

    @Test
    void should_merge_non_ldap_user_on_email_match() {
      // Arrange - filter passes (LOCAL != LDAP) -> merging must happen
      String auth0Sub = "auth0|abc";
      String email = "local@example.com";
      UUID existingId = UUID.randomUUID();
      UserEntity localUser =
          UserEntity.builder()
              .id(existingId)
              .username("localuser")
              .email(email)
              .authProvider("LOCAL")
              .build();
      User expectedUser = User.builder().id(existingId).username("localuser").build();

      given(userRepository.findByAuthProviderAndAuthProviderId("AUTH0", auth0Sub))
          .willReturn(Optional.empty());
      given(userRepository.findByEmail(email)).willReturn(Optional.of(localUser));
      given(userRepository.save(localUser)).willReturn(localUser);
      given(userEntityMapper.toDomain(localUser)).willReturn(expectedUser);

      // Act
      User result = userService.findOrCreateOAuth2User(auth0Sub, email, "Local User");

      // Assert - provider overwritten (filter let it through)
      assertThat(localUser.getAuthProvider()).isEqualTo("AUTH0");
      assertThat(localUser.getAuthProviderId()).isEqualTo(auth0Sub);
      assertThat(result.id()).isEqualTo(existingId);
    }
  }

  @Nested
  @DisplayName("buildUsername (via findOrCreateOAuth2User)")
  class BuildUsername {

    @Test
    void should_use_name_as_base_when_name_is_present() {
      // Arrange - name takes priority over email (guards name != null && !blank check)
      String auth0Sub = "auth0|name-test";
      String email = "fallback@example.com";
      UserEntity newUser =
          UserEntity.builder()
              .id(UUID.randomUUID())
              .username("johnsmith")
              .authProvider("AUTH0")
              .authProviderId(auth0Sub)
              .build();
      User expectedUser = User.builder().id(newUser.getId()).username("johnsmith").build();

      given(userRepository.findByAuthProviderAndAuthProviderId("AUTH0", auth0Sub))
          .willReturn(Optional.empty());
      given(userRepository.findByEmail(email)).willReturn(Optional.empty());
      given(userRepository.findByUsername("johnsmith")).willReturn(Optional.empty());
      given(userRepository.save(any(UserEntity.class))).willReturn(newUser);
      given(userEntityMapper.toDomain(newUser)).willReturn(expectedUser);

      // Act
      User result = userService.findOrCreateOAuth2User(auth0Sub, email, "John Smith");

      // Assert - username derived from name, not email
      assertThat(result.username()).isEqualTo("johnsmith");
    }

    @Test
    void should_fall_back_to_email_local_part_when_name_is_null() {
      // Arrange - name null -> use email local part (guards line 264 null-check)
      String auth0Sub = "auth0|email-fallback";
      String email = "alice@example.com";
      UserEntity newUser =
          UserEntity.builder()
              .id(UUID.randomUUID())
              .username("alice")
              .authProvider("AUTH0")
              .authProviderId(auth0Sub)
              .build();
      User expectedUser = User.builder().id(newUser.getId()).username("alice").build();

      given(userRepository.findByAuthProviderAndAuthProviderId("AUTH0", auth0Sub))
          .willReturn(Optional.empty());
      given(userRepository.findByEmail(email)).willReturn(Optional.empty());
      given(userRepository.findByUsername("alice")).willReturn(Optional.empty());
      given(userRepository.save(any(UserEntity.class))).willReturn(newUser);
      given(userEntityMapper.toDomain(newUser)).willReturn(expectedUser);

      // Act
      User result = userService.findOrCreateOAuth2User(auth0Sub, email, null);

      // Assert
      assertThat(result.username()).isEqualTo("alice");
    }

    @Test
    void should_fall_back_to_email_local_part_when_name_is_blank() {
      // Arrange - blank name -> use email (guards line 264 isBlank-check)
      String auth0Sub = "auth0|blank-name";
      String email = "bob@example.com";
      UserEntity newUser =
          UserEntity.builder()
              .id(UUID.randomUUID())
              .username("bob")
              .authProvider("AUTH0")
              .authProviderId(auth0Sub)
              .build();
      User expectedUser = User.builder().id(newUser.getId()).username("bob").build();

      given(userRepository.findByAuthProviderAndAuthProviderId("AUTH0", auth0Sub))
          .willReturn(Optional.empty());
      given(userRepository.findByEmail(email)).willReturn(Optional.empty());
      given(userRepository.findByUsername("bob")).willReturn(Optional.empty());
      given(userRepository.save(any(UserEntity.class))).willReturn(newUser);
      given(userEntityMapper.toDomain(newUser)).willReturn(expectedUser);

      // Act
      User result = userService.findOrCreateOAuth2User(auth0Sub, email, "   ");

      // Assert
      assertThat(result.username()).isEqualTo("bob");
    }

    @Test
    void should_fall_back_to_user_literal_when_name_and_email_are_null() {
      // Arrange - both null -> base becomes "user" (guards line 272 null-check for email)
      String auth0Sub = "auth0|no-info";
      UserEntity newUser =
          UserEntity.builder()
              .id(UUID.randomUUID())
              .username("user")
              .authProvider("AUTH0")
              .authProviderId(auth0Sub)
              .build();
      User expectedUser = User.builder().id(newUser.getId()).username("user").build();

      given(userRepository.findByAuthProviderAndAuthProviderId("AUTH0", auth0Sub))
          .willReturn(Optional.empty());
      given(userRepository.findByEmail(null)).willReturn(Optional.empty());
      given(userRepository.findByUsername("user")).willReturn(Optional.empty());
      given(userRepository.save(any(UserEntity.class))).willReturn(newUser);
      given(userEntityMapper.toDomain(newUser)).willReturn(expectedUser);

      // Act
      User result = userService.findOrCreateOAuth2User(auth0Sub, null, null);

      // Assert
      assertThat(result.username()).isEqualTo("user");
    }

    @Test
    void should_append_incrementing_suffix_on_collision() {
      // Arrange - "carol" taken, "carol1" taken, "carol2" free
      // Verifies: suffix increments UP (not down), and a non-empty result is returned
      String auth0Sub = "auth0|collision";
      String email = "carol@example.com";
      UserEntity taken1 = UserEntity.builder().id(UUID.randomUUID()).username("carol").build();
      UserEntity taken2 = UserEntity.builder().id(UUID.randomUUID()).username("carol1").build();
      UserEntity newUser =
          UserEntity.builder()
              .id(UUID.randomUUID())
              .username("carol2")
              .authProvider("AUTH0")
              .authProviderId(auth0Sub)
              .build();
      User expectedUser = User.builder().id(newUser.getId()).username("carol2").build();

      given(userRepository.findByAuthProviderAndAuthProviderId("AUTH0", auth0Sub))
          .willReturn(Optional.empty());
      given(userRepository.findByEmail(email)).willReturn(Optional.empty());
      given(userRepository.findByUsername("carol")).willReturn(Optional.of(taken1));
      given(userRepository.findByUsername("carol1")).willReturn(Optional.of(taken2));
      given(userRepository.findByUsername("carol2")).willReturn(Optional.empty());
      given(userRepository.save(any(UserEntity.class))).willReturn(newUser);
      given(userEntityMapper.toDomain(newUser)).willReturn(expectedUser);

      // Act
      User result = userService.findOrCreateOAuth2User(auth0Sub, email, "Carol");

      // Assert - suffix went 1 -> 2, not backwards; result is non-empty
      assertThat(result.username()).isEqualTo("carol2");
      assertThat(result.username()).isNotEmpty();
      // findByUsername called for "carol", "carol1", "carol2" (ascending order)
      then(userRepository).should(times(1)).findByUsername("carol");
      then(userRepository).should(times(1)).findByUsername("carol1");
      then(userRepository).should(times(1)).findByUsername("carol2");
    }
  }
}
