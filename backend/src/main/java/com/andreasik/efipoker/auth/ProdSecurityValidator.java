package com.andreasik.efipoker.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("prod")
class ProdSecurityValidator implements CommandLineRunner {

  private final AdminProperties adminProperties;
  private final JwtProperties jwtProperties;

  ProdSecurityValidator(AdminProperties adminProperties, JwtProperties jwtProperties) {
    this.adminProperties = adminProperties;
    this.jwtProperties = jwtProperties;
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

    log.info("Production security validation passed");
  }
}
