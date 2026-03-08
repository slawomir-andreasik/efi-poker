package com.andreasik.efipoker.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.ldap.enabled", havingValue = "true")
@RequiredArgsConstructor
public class LdapConfig {

  private final LdapProperties ldapProperties;

  @Bean
  public LdapContextSource ldapContextSource() {
    LdapContextSource contextSource = new LdapContextSource();
    contextSource.setUrl(ldapProperties.url());
    contextSource.setBase(ldapProperties.baseDn());
    if (!ldapProperties.bindDn().isBlank()) {
      contextSource.setUserDn(ldapProperties.bindDn());
      contextSource.setPassword(ldapProperties.bindPassword());
    }
    contextSource.setPooled(true);
    contextSource.afterPropertiesSet();
    log.info(
        "LDAP context source configured: url={}, baseDn={}",
        ldapProperties.url(),
        ldapProperties.baseDn());
    return contextSource;
  }

  @Bean
  public LdapTemplate ldapTemplate(LdapContextSource ldapContextSource) {
    return new LdapTemplate(ldapContextSource);
  }
}
