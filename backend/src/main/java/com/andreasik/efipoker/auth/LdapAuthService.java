package com.andreasik.efipoker.auth;

import java.util.List;
import java.util.Optional;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.support.LdapEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.ldap.enabled", havingValue = "true")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LdapAuthService {

  private final LdapContextSource ldapContextSource;
  private final LdapTemplate ldapTemplate;
  private final LdapProperties ldapProperties;

  public Optional<LdapUserInfo> authenticate(String username, String password) {
    log.debug("LDAP authentication attempt: username={}", username);

    // Guard: reject blank credentials (LDAP servers may treat empty password as anonymous bind)
    if (username == null || username.isBlank() || password == null || password.isBlank()) {
      log.debug("LDAP authentication rejected: blank username or password");
      return Optional.empty();
    }

    // Step 1: Search for user DN and attributes using service account
    List<LdapUserInfo> found = searchUser(username);
    if (found.isEmpty()) {
      log.debug("LDAP user not found: username={}", username);
      return Optional.empty();
    }

    LdapUserInfo userInfo = found.getFirst();

    // Step 2: Attempt bind with user credentials
    String fullDn =
        "uid="
            + LdapEncoder.nameEncode(username)
            + ","
            + ldapProperties.usersDn()
            + ","
            + ldapProperties.baseDn();
    if (!bindAsUser(fullDn, password)) {
      log.debug("LDAP bind failed: username={}", username);
      return Optional.empty();
    }

    // Step 3: Check admin group membership
    boolean isAdmin = checkAdminGroup(username);

    log.info("LDAP authentication successful: username={}, isAdmin={}", username, isAdmin);
    return Optional.of(new LdapUserInfo(userInfo.uid(), userInfo.mail(), isAdmin));
  }

  private List<LdapUserInfo> searchUser(String username) {
    String filter = ldapProperties.userFilter().replace("{0}", LdapEncoder.filterEncode(username));
    try {
      return ldapTemplate.search(
          ldapProperties.usersDn(),
          filter,
          (AttributesMapper<LdapUserInfo>)
              attrs -> {
                String uid = getAttr(attrs, ldapProperties.uidAttribute());
                String mail = getAttr(attrs, ldapProperties.mailAttribute());
                return new LdapUserInfo(uid, mail, false);
              });
    } catch (Exception e) {
      log.warn("LDAP user search failed: username={}, error={}", username, e.getMessage());
      return List.of();
    }
  }

  private boolean bindAsUser(String fullDn, String password) {
    DirContext ctx = null;
    try {
      ctx = ldapContextSource.getContext(fullDn, password);
      return true;
    } catch (Exception e) {
      log.debug("LDAP bind failed for DN: error={}", e.getMessage());
      return false;
    } finally {
      if (ctx != null) {
        try {
          ctx.close();
        } catch (NamingException e) {
          log.debug("Failed to close LDAP context: error={}", e.getMessage());
        }
      }
    }
  }

  private boolean checkAdminGroup(String username) {
    String adminGroup = ldapProperties.adminGroup();
    if (adminGroup == null || adminGroup.isBlank()) {
      return false;
    }
    try {
      String groupFilter =
          "(&(objectClass=posixGroup)(cn="
              + LdapEncoder.filterEncode(adminGroup)
              + ")(memberUid="
              + LdapEncoder.filterEncode(username)
              + "))";
      List<String> results =
          ldapTemplate.search(
              "ou=groups", groupFilter, (AttributesMapper<String>) attrs -> "match");
      return !results.isEmpty();
    } catch (Exception e) {
      log.warn("LDAP admin group check failed: username={}, error={}", username, e.getMessage());
      return false;
    }
  }

  private static String getAttr(Attributes attrs, String name) throws NamingException {
    var attr = attrs.get(name);
    return attr != null ? (String) attr.get() : null;
  }

  public record LdapUserInfo(String uid, String mail, boolean isAdmin) {}
}
