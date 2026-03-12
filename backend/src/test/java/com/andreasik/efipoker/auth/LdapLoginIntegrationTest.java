package com.andreasik.efipoker.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andreasik.efipoker.shared.test.BaseComponentTest;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DisplayName("LDAP Login Integration")
class LdapLoginIntegrationTest extends BaseComponentTest {

  private static final String BASE_DN = "dc=example,dc=com";
  private static InMemoryDirectoryServer ldapServer;

  @BeforeAll
  static void startLdap() throws Exception {
    InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(BASE_DN);
    config.addAdditionalBindCredentials("cn=admin," + BASE_DN, "adminpass");
    config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", 0));
    config.setSchema(null);

    ldapServer = new InMemoryDirectoryServer(config);
    ldapServer.startListening();

    ldapServer.add("dn: " + BASE_DN, "objectClass: organization", "o: example");
    ldapServer.add("dn: ou=users," + BASE_DN, "objectClass: organizationalUnit", "ou: users");
    ldapServer.add("dn: ou=groups," + BASE_DN, "objectClass: organizationalUnit", "ou: groups");
    ldapServer.add(
        "dn: uid=ldapuser,ou=users," + BASE_DN,
        "objectClass: inetOrgPerson",
        "objectClass: posixAccount",
        "uid: ldapuser",
        "cn: LDAP User",
        "sn: User",
        "mail: ldapuser@example.com",
        "description: userInternal",
        "description: active",
        "uidNumber: 10001",
        "gidNumber: 10001",
        "homeDirectory: /home/ldapuser",
        "userPassword: ldappassword");
    ldapServer.add(
        "dn: uid=ldapadmin,ou=users," + BASE_DN,
        "objectClass: inetOrgPerson",
        "objectClass: posixAccount",
        "uid: ldapadmin",
        "cn: LDAP Admin",
        "sn: Admin",
        "mail: ldapadmin@example.com",
        "description: userInternal",
        "description: active",
        "uidNumber: 10002",
        "gidNumber: 10002",
        "homeDirectory: /home/ldapadmin",
        "userPassword: ldapadminpassword");
    ldapServer.add(
        "dn: cn=efipoker-admins,ou=groups," + BASE_DN,
        "objectClass: posixGroup",
        "cn: efipoker-admins",
        "gidNumber: 20000",
        "memberUid: ldapadmin");
  }

  @AfterAll
  static void stopLdap() {
    if (ldapServer != null) {
      ldapServer.shutDown(true);
    }
  }

  @DynamicPropertySource
  static void ldapProperties(DynamicPropertyRegistry registry) {
    registry.add("app.ldap.enabled", () -> "true");
    registry.add("app.ldap.url", () -> "ldap://localhost:" + ldapServer.getListenPort());
    registry.add("app.ldap.base-dn", () -> BASE_DN);
    registry.add("app.ldap.users-dn", () -> "ou=users");
    registry.add("app.ldap.bind-dn", () -> "cn=admin," + BASE_DN);
    registry.add("app.ldap.bind-password", () -> "adminpass");
    registry.add(
        "app.ldap.user-filter", () -> "(&(uid={0})(description=userInternal)(description=active))");
    registry.add("app.ldap.uid-attribute", () -> "uid");
    registry.add("app.ldap.mail-attribute", () -> "mail");
    registry.add("app.ldap.admin-group", () -> "efipoker-admins");
    registry.add("app.registration.enabled", () -> "false");
  }

  private String loginLdapUser(String username, String password) throws Exception {
    String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    String json =
        mockMvc
            .perform(
                post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(json).get("token").asText();
  }

  @Nested
  @DisplayName("POST /api/v1/auth/login - LDAP")
  class LdapLogin {

    @Test
    void should_login_ldap_user_and_provision() throws Exception {
      // Act
      // language=JSON
      String body =
          """
          {"username":"ldapuser","password":"ldappassword"}
          """;

      mockMvc
          .perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.token").isNotEmpty())
          .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void should_login_ldap_admin_with_admin_role() throws Exception {
      // Act
      String token = loginLdapUser("ldapadmin", "ldapadminpassword");

      // Assert - check /auth/me returns ADMIN role
      mockMvc
          .perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.username").value("ldapadmin"))
          .andExpect(jsonPath("$.role").value("ADMIN"))
          .andExpect(jsonPath("$.authProvider").value("LDAP"));
    }

    @Test
    void should_reject_wrong_ldap_password() throws Exception {
      // Act
      // language=JSON
      String body =
          """
          {"username":"ldapuser","password":"wrongpassword"}
          """;

      mockMvc
          .perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isForbidden());
    }

    @Test
    void should_still_login_local_user() throws Exception {
      // The bootstrap testadmin (LOCAL auth) should still work
      // language=JSON
      String body =
          """
          {"username":"testadmin","password":"testpassword"}
          """;

      mockMvc
          .perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.token").isNotEmpty());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/auth/config - LDAP enabled")
  class AuthConfig {

    @Test
    void should_return_ldap_enabled_in_config() throws Exception {
      mockMvc
          .perform(get("/api/v1/auth/config"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.ldapEnabled").value(true));
    }
  }

  @Nested
  @DisplayName("GET /api/v1/auth/me - LDAP user")
  class LdapUserProfile {

    @Test
    void should_return_ldap_auth_provider() throws Exception {
      // Arrange
      String token = loginLdapUser("ldapuser", "ldappassword");

      // Act & Assert
      mockMvc
          .perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.authProvider").value("LDAP"))
          .andExpect(jsonPath("$.hasPassword").value(false));
    }
  }

  @Nested
  @DisplayName("GET /actuator/health - LDAP health")
  class LdapHealth {

    @Test
    void should_report_ldap_health_up() throws Exception {
      mockMvc
          .perform(get("/actuator/health"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.components.ldap.status").value("UP"));
    }
  }

  @Nested
  @DisplayName("POST /api/v1/auth/register - disabled")
  class RegistrationDisabled {

    @Test
    void should_reject_registration_when_disabled() throws Exception {
      // Act & Assert
      // language=JSON
      String body =
          """
          {"username":"newuser","password":"password123"}
          """;

      mockMvc
          .perform(
              post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("PUT /api/v1/auth/me/password - LDAP user blocked")
  class LdapPasswordChange {

    @Test
    void should_block_password_change_for_ldap_user() throws Exception {
      // Arrange
      String token = loginLdapUser("ldapuser", "ldappassword");
      // language=JSON
      String body =
          """
          {"newPassword":"newpass123"}
          """;

      // Act & Assert
      mockMvc
          .perform(
              put("/api/v1/auth/me/password")
                  .header("Authorization", "Bearer " + token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isForbidden());
    }
  }
}
