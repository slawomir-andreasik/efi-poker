package com.andreasik.efipoker.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.andreasik.efipoker.shared.test.BaseUnitTest;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

@DisplayName("LdapAuthService")
class LdapAuthServiceTest extends BaseUnitTest {

  private static InMemoryDirectoryServer ldapServer;
  private static LdapAuthService ldapAuthService;
  private static final String BASE_DN = "dc=example,dc=com";

  @BeforeAll
  static void startLdap() throws Exception {
    InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(BASE_DN);
    config.addAdditionalBindCredentials("cn=admin," + BASE_DN, "adminpass");
    config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", 0));
    config.setSchema(null);

    ldapServer = new InMemoryDirectoryServer(config);
    ldapServer.startListening();

    int port = ldapServer.getListenPort();

    // Seed test data
    ldapServer.add("dn: " + BASE_DN, "objectClass: organization", "o: example");
    ldapServer.add("dn: ou=users," + BASE_DN, "objectClass: organizationalUnit", "ou: users");
    ldapServer.add("dn: ou=groups," + BASE_DN, "objectClass: organizationalUnit", "ou: groups");
    ldapServer.add(
        "dn: uid=testuser,ou=users," + BASE_DN,
        "objectClass: inetOrgPerson",
        "objectClass: posixAccount",
        "uid: testuser",
        "cn: Test User",
        "sn: User",
        "mail: testuser@example.com",
        "description: userInternal",
        "description: active",
        "uidNumber: 10001",
        "gidNumber: 10001",
        "homeDirectory: /home/testuser",
        "userPassword: testpassword");
    ldapServer.add(
        "dn: uid=testadmin,ou=users," + BASE_DN,
        "objectClass: inetOrgPerson",
        "objectClass: posixAccount",
        "uid: testadmin",
        "cn: Test Admin",
        "sn: Admin",
        "mail: testadmin@example.com",
        "description: userInternal",
        "description: active",
        "uidNumber: 10002",
        "gidNumber: 10002",
        "homeDirectory: /home/testadmin",
        "userPassword: adminpassword");
    ldapServer.add(
        "dn: uid=inactive,ou=users," + BASE_DN,
        "objectClass: inetOrgPerson",
        "objectClass: posixAccount",
        "uid: inactive",
        "cn: Inactive User",
        "sn: Inactive",
        "mail: inactive@example.com",
        "description: userInternal",
        "description: inactive",
        "uidNumber: 10003",
        "gidNumber: 10003",
        "homeDirectory: /home/inactive",
        "userPassword: inactivepassword");
    ldapServer.add(
        "dn: cn=efipoker-admins,ou=groups," + BASE_DN,
        "objectClass: posixGroup",
        "cn: efipoker-admins",
        "gidNumber: 20000",
        "memberUid: testadmin");

    // Configure Spring LDAP
    LdapContextSource contextSource = new LdapContextSource();
    contextSource.setUrl("ldap://localhost:" + port);
    contextSource.setBase(BASE_DN);
    contextSource.setUserDn("cn=admin," + BASE_DN);
    contextSource.setPassword("adminpass");
    contextSource.afterPropertiesSet();

    LdapTemplate ldapTemplate = new LdapTemplate(contextSource);

    LdapProperties properties =
        new LdapProperties(
            true,
            "ldap://localhost:" + port,
            BASE_DN,
            "ou=users",
            "cn=admin," + BASE_DN,
            "adminpass",
            "(&(uid={0})(description=userInternal)(description=active))",
            "uid",
            "mail",
            "efipoker-admins");

    ldapAuthService = new LdapAuthService(contextSource, ldapTemplate, properties);
  }

  @AfterAll
  static void stopLdap() {
    if (ldapServer != null) {
      ldapServer.shutDown(true);
    }
  }

  @Nested
  @DisplayName("authenticate")
  class Authenticate {

    @Test
    void should_authenticate_valid_user() {
      // Act
      Optional<LdapAuthService.LdapUserInfo> result =
          ldapAuthService.authenticate("testuser", "testpassword");

      // Assert
      assertThat(result).isPresent();
      assertThat(result.get().uid()).isEqualTo("testuser");
      assertThat(result.get().mail()).isEqualTo("testuser@example.com");
      assertThat(result.get().isAdmin()).isFalse();
    }

    @Test
    void should_return_empty_for_wrong_password() {
      // Act
      Optional<LdapAuthService.LdapUserInfo> result =
          ldapAuthService.authenticate("testuser", "wrongpassword");

      // Assert
      assertThat(result).isEmpty();
    }

    @Test
    void should_return_empty_for_nonexistent_user() {
      // Act
      Optional<LdapAuthService.LdapUserInfo> result =
          ldapAuthService.authenticate("nobody", "password");

      // Assert
      assertThat(result).isEmpty();
    }

    @Test
    void should_filter_inactive_user() {
      // Act
      Optional<LdapAuthService.LdapUserInfo> result =
          ldapAuthService.authenticate("inactive", "inactivepassword");

      // Assert
      assertThat(result).isEmpty();
    }

    @Test
    void should_detect_admin_group_membership() {
      // Act
      Optional<LdapAuthService.LdapUserInfo> result =
          ldapAuthService.authenticate("testadmin", "adminpassword");

      // Assert
      assertThat(result).isPresent();
      assertThat(result.get().uid()).isEqualTo("testadmin");
      assertThat(result.get().isAdmin()).isTrue();
    }

    @Test
    void should_reject_empty_password() {
      // Act
      Optional<LdapAuthService.LdapUserInfo> result = ldapAuthService.authenticate("testuser", "");

      // Assert
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("security - LDAP injection")
  class LdapInjection {

    @Test
    void should_not_match_wildcard_username() {
      // Act
      Optional<LdapAuthService.LdapUserInfo> result =
          ldapAuthService.authenticate("*", "testpassword");

      // Assert
      assertThat(result).isEmpty();
    }

    @Test
    void should_not_match_filter_injection_username() {
      // Act
      Optional<LdapAuthService.LdapUserInfo> result =
          ldapAuthService.authenticate("testuser)(uid=*", "testpassword");

      // Assert
      assertThat(result).isEmpty();
    }

    @Test
    void should_not_match_dn_injection_username() {
      // Act
      Optional<LdapAuthService.LdapUserInfo> result =
          ldapAuthService.authenticate("testuser,ou=admins", "testpassword");

      // Assert
      assertThat(result).isEmpty();
    }

    @Test
    void should_not_match_null_byte_username() {
      // Act
      Optional<LdapAuthService.LdapUserInfo> result =
          ldapAuthService.authenticate("testuser\u0000", "testpassword");

      // Assert
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("admin group detection")
  class AdminGroupDetection {

    @Test
    void should_not_detect_admin_when_no_group_configured() {
      // Arrange - create service without admin group
      LdapProperties noAdminGroupProps =
          new LdapProperties(
              true,
              "ldap://unused:389",
              BASE_DN,
              "ou=users",
              "",
              "",
              "(&(uid={0})(description=userInternal)(description=active))",
              "uid",
              "mail",
              "");

      // Reuse same context source via reflection-free approach
      LdapContextSource contextSource = new LdapContextSource();
      contextSource.setUrl("ldap://localhost:" + ldapServer.getListenPort());
      contextSource.setBase(BASE_DN);
      contextSource.setUserDn("cn=admin," + BASE_DN);
      contextSource.setPassword("adminpass");
      contextSource.afterPropertiesSet();

      LdapAuthService serviceNoAdmin =
          new LdapAuthService(contextSource, new LdapTemplate(contextSource), noAdminGroupProps);

      // Act
      Optional<LdapAuthService.LdapUserInfo> result =
          serviceNoAdmin.authenticate("testadmin", "adminpassword");

      // Assert
      assertThat(result).isPresent();
      assertThat(result.get().isAdmin()).isFalse();
    }
  }
}
