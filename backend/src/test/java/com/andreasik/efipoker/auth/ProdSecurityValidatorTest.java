package com.andreasik.efipoker.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.andreasik.efipoker.shared.test.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ProdSecurityValidator")
class ProdSecurityValidatorTest extends BaseUnitTest {

  private static final String SECURE_PASSWORD = "SecureP@ss123";
  private static final String SECURE_SECRET =
      "production-secret-that-is-at-least-64-characters-long-xxxxxxxxxxx";
  private static final String SECURE_DB_PASSWORD = "prod-db-password-123";

  private static final AdminProperties SECURE_ADMIN = new AdminProperties("admin", SECURE_PASSWORD);
  private static final JwtProperties SECURE_JWT =
      new JwtProperties(SECURE_SECRET, 86400, 7776000, 2592000, 7776000);
  private static final LdapProperties LDAP_DISABLED =
      new LdapProperties(
          false,
          "ldap://localhost:389",
          "dc=example,dc=com",
          "ou=users",
          "",
          "",
          "",
          "uid",
          "mail",
          "");
  private static final Auth0Properties AUTH0_DISABLED =
      new Auth0Properties(false, "disabled", "disabled", "disabled");

  private ProdSecurityValidator validator(
      AdminProperties admin,
      JwtProperties jwt,
      String dbPassword,
      LdapProperties ldap,
      Auth0Properties auth0) {
    return new ProdSecurityValidator(admin, jwt, dbPassword, ldap, auth0);
  }

  @Nested
  @DisplayName("Existing checks")
  class ExistingChecks {

    @Test
    void should_fail_when_admin_password_is_changeme() {
      var admin = new AdminProperties("admin", "changeme");
      var v = validator(admin, SECURE_JWT, SECURE_DB_PASSWORD, LDAP_DISABLED, AUTH0_DISABLED);

      assertThatThrownBy(v::run)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Admin password");
    }

    @Test
    void should_fail_when_jwt_secret_contains_dev_secret() {
      var jwt =
          new JwtProperties(
              "dev-secret-change-in-production-must-be-at-least-64-chars-long-for-hs512",
              86400,
              7776000,
              2592000,
              7776000);
      var v = validator(SECURE_ADMIN, jwt, SECURE_DB_PASSWORD, LDAP_DISABLED, AUTH0_DISABLED);

      assertThatThrownBy(v::run)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("JWT secret");
    }

    @Test
    void should_fail_when_jwt_secret_is_too_short() {
      var jwt =
          new JwtProperties("short-secret-only-32-characters!", 86400, 7776000, 2592000, 7776000);
      var v = validator(SECURE_ADMIN, jwt, SECURE_DB_PASSWORD, LDAP_DISABLED, AUTH0_DISABLED);

      assertThatThrownBy(v::run)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("too short");
    }

    @Test
    void should_pass_when_all_values_are_secure() {
      var v =
          validator(SECURE_ADMIN, SECURE_JWT, SECURE_DB_PASSWORD, LDAP_DISABLED, AUTH0_DISABLED);

      assertThatCode(v::run).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Database password validation")
  class DatabasePasswordValidation {

    @Test
    void should_fail_when_db_password_is_changeme() {
      var v = validator(SECURE_ADMIN, SECURE_JWT, "changeme", LDAP_DISABLED, AUTH0_DISABLED);

      assertThatThrownBy(v::run)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Database password");
    }

    @Test
    void should_pass_when_db_password_is_not_default() {
      var v =
          validator(SECURE_ADMIN, SECURE_JWT, "strong-db-password", LDAP_DISABLED, AUTH0_DISABLED);

      assertThatCode(v::run).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("LDAP/Auth0 secret validation")
  class LdapAuth0Validation {

    @Test
    void should_fail_when_ldap_enabled_with_default_base_dn() {
      var ldap =
          new LdapProperties(
              true,
              "ldap://prod:389",
              "dc=example,dc=com",
              "ou=users",
              "",
              "",
              "",
              "uid",
              "mail",
              "");
      var v = validator(SECURE_ADMIN, SECURE_JWT, SECURE_DB_PASSWORD, ldap, AUTH0_DISABLED);

      assertThatThrownBy(v::run)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("LDAP base DN");
    }

    @Test
    void should_pass_when_ldap_disabled_with_default_values() {
      // LDAP disabled - default base DN should not trigger validation
      var v =
          validator(SECURE_ADMIN, SECURE_JWT, SECURE_DB_PASSWORD, LDAP_DISABLED, AUTH0_DISABLED);

      assertThatCode(v::run).doesNotThrowAnyException();
    }

    @Test
    void should_fail_when_auth0_enabled_with_default_domain() {
      var auth0 = new Auth0Properties(true, "disabled", "client-id", "client-secret");
      var v = validator(SECURE_ADMIN, SECURE_JWT, SECURE_DB_PASSWORD, LDAP_DISABLED, auth0);

      assertThatThrownBy(v::run)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Auth0 domain");
    }

    @Test
    void should_pass_when_auth0_disabled_with_default_values() {
      var v =
          validator(SECURE_ADMIN, SECURE_JWT, SECURE_DB_PASSWORD, LDAP_DISABLED, AUTH0_DISABLED);

      assertThatCode(v::run).doesNotThrowAnyException();
    }

    @Test
    void should_pass_when_ldap_and_auth0_enabled_with_proper_config() {
      var ldap =
          new LdapProperties(
              true,
              "ldap://prod:389",
              "dc=company,dc=com",
              "ou=people",
              "",
              "",
              "",
              "uid",
              "mail",
              "");
      var auth0 = new Auth0Properties(true, "company.eu.auth0.com", "client-id", "client-secret");
      var v = validator(SECURE_ADMIN, SECURE_JWT, SECURE_DB_PASSWORD, ldap, auth0);

      assertThatCode(v::run).doesNotThrowAnyException();
    }
  }
}
