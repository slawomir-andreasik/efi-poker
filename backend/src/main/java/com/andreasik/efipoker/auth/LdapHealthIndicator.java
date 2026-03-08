package com.andreasik.efipoker.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.ldap.enabled", havingValue = "true")
@RequiredArgsConstructor
public class LdapHealthIndicator implements HealthIndicator {

  private final LdapTemplate ldapTemplate;
  private final LdapProperties ldapProperties;

  @Override
  public Health health() {
    try {
      ldapTemplate.search(
          ldapProperties.usersDn(), "(objectClass=*)", (AttributesMapper<String>) attrs -> "ok");
      return Health.up().withDetail("url", ldapProperties.url()).build();
    } catch (Exception e) {
      log.warn("LDAP health check failed: error={}", e.getMessage());
      return Health.down(e).withDetail("url", ldapProperties.url()).build();
    }
  }
}
