package com.andreasik.efipoker.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("prod")
class ProdSecurityValidator implements CommandLineRunner {

  private final AdminProperties adminProperties;
  private final JwtProperties jwtProperties;
  private final String dbPassword;
  private final LdapProperties ldapProperties;
  private final Auth0Properties auth0Properties;

  ProdSecurityValidator(
      AdminProperties adminProperties,
      JwtProperties jwtProperties,
      @Value("${spring.datasource.password}") String dbPassword,
      LdapProperties ldapProperties,
      Auth0Properties auth0Properties) {
    this.adminProperties = adminProperties;
    this.jwtProperties = jwtProperties;
    this.dbPassword = dbPassword;
    this.ldapProperties = ldapProperties;
    this.auth0Properties = auth0Properties;
  }

  @Override
  public void run(String... args) {
    if ("changeme".equals(adminProperties.password())) {
      throw new IllegalStateException(
          "CRITICAL: Admin password is set to default 'changeme'. "
              + "Set ADMIN_PASSWORD environment variable before starting in production.");
    }

    if (jwtProperties.secret() != null && jwtProperties.secret().contains("dev-secret")) {
      throw new IllegalStateException(
          "JWT secret contains 'dev-secret'. "
              + "Set JWT_SECRET environment variable before starting in production.");
    }

    if (jwtProperties.secret() != null && jwtProperties.secret().length() < 64) {
      throw new IllegalStateException(
          "JWT secret is too short (%d chars). HS512 requires at least 64 characters. "
                  .formatted(jwtProperties.secret().length())
              + "Set JWT_SECRET to a secure random string of 64+ characters.");
    }

    if ("changeme".equals(dbPassword)) {
      throw new IllegalStateException(
          "Database password is set to default 'changeme'. "
              + "Set POSTGRES_PASSWORD environment variable before starting in production.");
    }

    if (ldapProperties.enabled() && "dc=example,dc=com".equals(ldapProperties.baseDn())) {
      throw new IllegalStateException(
          "LDAP base DN is set to default 'dc=example,dc=com'. "
              + "Set LDAP_BASE_DN environment variable before starting in production.");
    }

    if (auth0Properties.enabled() && "disabled".equals(auth0Properties.domain())) {
      throw new IllegalStateException(
          "Auth0 domain is set to default 'disabled'. "
              + "Set AUTH0_DOMAIN environment variable before starting in production.");
    }

    log.info("Production security validation passed");
  }
}
