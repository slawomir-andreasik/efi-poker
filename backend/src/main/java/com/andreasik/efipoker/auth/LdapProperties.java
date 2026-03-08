package com.andreasik.efipoker.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.ldap")
public record LdapProperties(
    boolean enabled,
    @DefaultValue("ldap://localhost:389") String url,
    @DefaultValue("dc=example,dc=com") String baseDn,
    @DefaultValue("ou=users") String usersDn,
    @DefaultValue("") String bindDn,
    @DefaultValue("") String bindPassword,
    @DefaultValue("(&(uid={0})(info=userInternal)(info=active))") String userFilter,
    @DefaultValue("uid") String uidAttribute,
    @DefaultValue("mail") String mailAttribute,
    @DefaultValue("") String adminGroup) {}
