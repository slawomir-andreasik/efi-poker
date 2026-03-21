package com.andreasik.efipoker.arch;

import static org.assertj.core.api.Assertions.assertThat;

import com.andreasik.efipoker.shared.test.BaseArchUnitTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SecurityArchitectureTest")
class SecurityArchitectureTest extends BaseArchUnitTest {

  private static final Path PROJECT_ROOT =
      Path.of(System.getProperty("user.dir")).getParent(); // backend/../ = project root
  private static final Path OPENAPI_SCHEMAS =
      PROJECT_ROOT.resolve("api/src/main/resources/openapi/schemas/request");
  private static final Path BACKEND_SRC =
      PROJECT_ROOT.resolve("backend/src/main/java/com/andreasik/efipoker");
  private static final Path NGINX_CONF = PROJECT_ROOT.resolve("frontend/nginx.conf");

  @Nested
  @DisplayName("Password complexity validation")
  class PasswordComplexity {

    @Test
    void password_fields_should_have_pattern_constraint() throws IOException {
      List<String> violations = new ArrayList<>();
      List<String> passwordSchemas =
          List.of(
              "RegisterRequest.yaml", "ChangePasswordRequest.yaml", "AdminCreateUserRequest.yaml");

      for (String schemaFile : passwordSchemas) {
        Path path = OPENAPI_SCHEMAS.resolve(schemaFile);
        if (!Files.exists(path)) {
          violations.add(schemaFile + ": file not found");
          continue;
        }
        String content = Files.readString(path);
        // Check new password fields (not currentPassword - existing passwords may not meet rules)
        for (String field : List.of("password", "newPassword")) {
          if (containsField(content, field) && !containsPatternForField(content, field)) {
            violations.add(schemaFile + ": field '" + field + "' missing 'pattern:' constraint");
          }
        }
      }

      assertThat(violations)
          .as("All password fields in OpenAPI schemas should have a pattern constraint")
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("HTML input sanitization")
  class HtmlInputSanitization {

    @Test
    void text_fields_should_reject_html_metacharacters() throws IOException {
      List<String> violations = new ArrayList<>();

      // Map of schema files to fields that should reject <> characters
      record FieldCheck(String file, String field) {}
      List<FieldCheck> checks =
          List.of(
              new FieldCheck("JoinProjectRequest.yaml", "nickname"),
              new FieldCheck("UpdateParticipantRequest.yaml", "nickname"),
              new FieldCheck("CreateRoomRequest.yaml", "title"),
              new FieldCheck("CreateTaskRequest.yaml", "title"),
              new FieldCheck("CreateProjectRequest.yaml", "name"),
              new FieldCheck("RegisterRequest.yaml", "username"),
              new FieldCheck("NewRoundRequest.yaml", "topic"));

      for (FieldCheck check : checks) {
        Path path = OPENAPI_SCHEMAS.resolve(check.file());
        if (!Files.exists(path)) {
          violations.add(check.file() + ": file not found");
          continue;
        }
        String content = Files.readString(path);
        if (!containsPatternForField(content, check.field())) {
          violations.add(
              check.file() + ": field '" + check.field() + "' missing 'pattern:' constraint");
        }
      }

      assertThat(violations)
          .as("Text input fields should have pattern constraints rejecting HTML metacharacters")
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("DB password validation in ProdSecurityValidator")
  class DbPasswordValidation {

    @Test
    void prod_security_validator_should_check_database_password() throws IOException {
      Path validatorPath = BACKEND_SRC.resolve("auth/ProdSecurityValidator.java");
      String content = Files.readString(validatorPath);

      assertThat(content)
          .as("ProdSecurityValidator should reference datasource password")
          .containsAnyOf("datasource.password", "POSTGRES_PASSWORD", "dbPassword");
    }
  }

  @Nested
  @DisplayName("LDAP/Auth0 validation in ProdSecurityValidator")
  class LdapAuth0Validation {

    @Test
    void prod_security_validator_should_check_ldap_and_auth0_config() throws IOException {
      Path validatorPath = BACKEND_SRC.resolve("auth/ProdSecurityValidator.java");
      String content = Files.readString(validatorPath);

      assertThat(content)
          .as("ProdSecurityValidator should reference LDAP config")
          .containsAnyOf("ldap", "LDAP", "LdapProperties");

      assertThat(content)
          .as("ProdSecurityValidator should reference Auth0 config")
          .containsAnyOf("auth0", "Auth0", "Auth0Properties");
    }
  }

  @Nested
  @DisplayName("Admin code hashing")
  class AdminCodeHashing {

    @Test
    void project_service_should_use_password_encoder_for_admin_code() throws IOException {
      Path servicePath = BACKEND_SRC.resolve("project/ProjectService.java");
      String content = Files.readString(servicePath);

      assertThat(content)
          .as("ProjectService should use adminCodeEncoder for admin code comparison")
          .contains("adminCodeEncoder");
    }
  }

  @Nested
  @DisplayName("Security headers in all nginx location blocks")
  class NginxLocationHeaders {

    @Test
    void all_location_blocks_should_have_security_headers() throws IOException {
      String content = Files.readString(NGINX_CONF);
      List<String> requiredHeaders =
          List.of("X-Frame-Options", "X-Content-Type-Options", "Content-Security-Policy");
      List<String> violations = new ArrayList<>();

      // Split by location directives (line starts with whitespace + "location")
      String[] blocks = content.split("(?m)(?=^\\s+location )");
      for (String block : blocks) {
        if (!block.stripLeading().startsWith("location ")) continue;
        String locationName = block.stripLeading().lines().findFirst().orElse("").trim();
        // Skip try_files-only blocks (no add_header needed - inherits from server)
        if (!block.contains("add_header") && !block.contains("proxy_pass")) continue;
        // Blocks with proxy_pass or add_header MUST have all security headers
        for (String header : requiredHeaders) {
          if (!block.contains(header)) {
            violations.add(locationName + ": missing " + header);
          }
        }
      }

      assertThat(violations)
          .as(
              "All nginx location blocks with add_header or proxy_pass must include security headers")
          .isEmpty();
    }
  }

  /// Checks if a YAML file contains a field definition (e.g. "password:").
  private static boolean containsField(String yamlContent, String fieldName) {
    for (String line : yamlContent.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.startsWith(fieldName + ":") || trimmed.equals(fieldName + ":")) {
        return true;
      }
    }
    return false;
  }

  /// Checks if a YAML field has a 'pattern:' key within the next 10 lines.
  private static boolean containsPatternForField(String yamlContent, String fieldName) {
    String[] lines = yamlContent.split("\n");
    for (int i = 0; i < lines.length; i++) {
      String trimmed = lines[i].trim();
      if (!trimmed.startsWith(fieldName + ":")) {
        continue;
      }
      int fieldIndent = lines[i].indexOf(fieldName.charAt(0));
      for (int j = i + 1; j < Math.min(i + 10, lines.length); j++) {
        String line = lines[j].trim();
        if (line.startsWith("pattern:")) {
          return true;
        }
        if (!line.isEmpty()
            && Character.isLetter(line.charAt(0))
            && lines[j].indexOf(line.charAt(0)) <= fieldIndent) {
          break;
        }
      }
    }
    return false;
  }
}
