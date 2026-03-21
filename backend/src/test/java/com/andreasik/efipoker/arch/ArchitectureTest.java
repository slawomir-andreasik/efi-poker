package com.andreasik.efipoker.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

import com.andreasik.efipoker.shared.test.BaseArchUnitTest;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@DisplayName("ArchitectureTest")
class ArchitectureTest extends BaseArchUnitTest {

  private final JavaClasses classes = ArchTestUtils.importProductionClasses();

  @Nested
  @DisplayName("NamingConventions")
  class NamingConventions {

    @Test
    void should_end_controllers_with_controller_suffix() {
      classes()
          .that()
          .resideInAnyPackage(ArchTestUtils.basePackage() + "..")
          .and()
          .areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
          .should()
          .haveSimpleNameEndingWith("Controller")
          .check(classes);
    }

    @Test
    void should_end_mappers_with_mapper_suffix() {
      classes()
          .that()
          .resideInAnyPackage(ArchTestUtils.basePackage() + "..")
          .and()
          .areAnnotatedWith(org.mapstruct.Mapper.class)
          .should()
          .haveSimpleNameEndingWith("Mapper")
          .check(classes);
    }

    @Test
    void should_place_exceptions_in_exception_package() {
      classes()
          .that()
          .resideInAnyPackage(ArchTestUtils.basePackage() + "..")
          .and()
          .areAssignableTo(RuntimeException.class)
          .should()
          .resideInAPackage("..exception..")
          .check(classes);
    }
  }

  @Nested
  @DisplayName("Dependencies")
  class Dependencies {

    @Test
    void should_be_free_of_cycles() {
      // All cycles eliminated - clean dependency graph
      slices()
          .matching(ArchTestUtils.basePackage() + ".(*)..")
          .should()
          .beFreeOfCycles()
          .check(classes);
    }
  }

  @Nested
  @DisplayName("ModuleBoundaries")
  class ModuleBoundaries {

    @Test
    void should_not_access_project_repository_from_outside_project_package() {
      classes()
          .that()
          .haveSimpleName("ProjectRepository")
          .should()
          .onlyBeAccessed()
          .byAnyPackage(
              ArchTestUtils.basePackage() + ".project..",
              ArchTestUtils.basePackage() + ".observability..")
          .because("ProjectRepository is internal to the project module - use ProjectApi instead")
          .check(classes);
    }

    @Test
    void should_not_access_participant_repository_from_outside_participant_package() {
      classes()
          .that()
          .haveSimpleName("ParticipantRepository")
          .should()
          .onlyBeAccessed()
          .byAnyPackage(
              ArchTestUtils.basePackage() + ".participant..",
              ArchTestUtils.basePackage() + ".observability..")
          .because(
              "ParticipantRepository is internal to the participant module - use ParticipantApi instead")
          .check(classes);
    }
  }

  @Nested
  @DisplayName("ConventionEnforcement")
  class ConventionEnforcement {

    @Test
    void services_should_have_class_level_transactional_readonly() {
      classes()
          .that()
          .areAnnotatedWith(Service.class)
          .should(
              new ArchCondition<JavaClass>("have @Transactional(readOnly = true) at class level") {
                @Override
                public void check(JavaClass clazz, ConditionEvents events) {
                  boolean hasReadOnlyTransactional =
                      clazz.getAnnotations().stream()
                          .filter(
                              a -> a.getRawType().getName().equals(Transactional.class.getName()))
                          .anyMatch(
                              a -> {
                                try {
                                  return a.as(Transactional.class).readOnly();
                                } catch (Exception e) {
                                  return false;
                                }
                              });
                  if (!hasReadOnlyTransactional) {
                    events.add(
                        SimpleConditionEvent.violated(
                            clazz,
                            clazz.getName()
                                + " is missing @Transactional(readOnly = true) at class level"));
                  }
                }
              })
          .check(ArchTestUtils.importProductionClasses());
    }

    @Test
    void public_service_methods_should_have_at_most_4_parameters() {
      methods()
          .that()
          .areDeclaredInClassesThat()
          .areAnnotatedWith(Service.class)
          .and()
          .arePublic()
          .should(
              new ArchCondition<JavaMethod>("have at most 4 parameters") {
                @Override
                public void check(JavaMethod method, ConditionEvents events) {
                  if (method.getRawParameterTypes().size() > 4) {
                    events.add(
                        SimpleConditionEvent.violated(
                            method,
                            method.getFullName()
                                + " has "
                                + method.getRawParameterTypes().size()
                                + " parameters (max 4, use a command record)"));
                  }
                }
              })
          .check(ArchTestUtils.importProductionClasses());
    }

    @Test
    void should_use_stream_toList_instead_of_collectors() {
      noClasses()
          .that()
          .resideInAPackage("com.andreasik.efipoker..")
          .should()
          .callMethodWhere(
              JavaCall.Predicates.target(HasName.Predicates.name("toList"))
                  .and(
                      JavaCall.Predicates.target(
                          HasOwner.Predicates.With.owner(
                              HasName.Predicates.name("java.util.stream.Collectors")))))
          .because("use .toList() instead of .collect(Collectors.toList())")
          .check(ArchTestUtils.importProductionClasses());
    }

    @Test
    void module_api_classes_should_be_interfaces() {
      classes()
          .that()
          .haveSimpleNameEndingWith("Api")
          .and()
          .resideInAPackage("com.andreasik.efipoker..")
          .and()
          .resideOutsideOfPackage("..api..") // exclude generated OpenAPI
          .should()
          .beInterfaces()
          .because("module APIs define cross-module contracts and must be interfaces")
          .check(ArchTestUtils.importProductionClasses());
    }
  }

  /// Source-level checks that ArchUnit can't do (ArchUnit operates on bytecode where all references
  /// are fully qualified regardless of source-level imports).
  @Nested
  @DisplayName("SourceCodeQuality")
  class SourceCodeQuality {

    private static final Path SRC_ROOT = Path.of(System.getProperty("user.dir"), "src");

    private static final Pattern FQN_PATTERN =
        Pattern.compile("com\\.andreasik\\.efipoker\\.[a-z]+(\\.[a-z]+)*\\.[A-Z]\\w+");

    /// Per-file exceptions where FQN is unavoidable due to name collision
    /// (two classes with the same simple name from different packages used in one file).
    ///
    /// Format: "SimpleFileName.java -> fqn.prefix" (the FQN that is allowed ONLY in that file).
    ///
    /// If this test fails:
    ///
    /// - Usually: add an `import` and use the simple class name
    /// - Name collision: add an entry here for the specific file + the less-imported FQN
    private static final List<String> ALLOWED_FQN_PER_FILE =
        List.of(
            // RoomMapper uses api.model.RoomType (imported) + domain RoomType (FQN for param)
            "RoomMapper.java -> com.andreasik.efipoker.estimation.room.RoomType",
            // RoomMapperTest uses domain RoomType (imported) + api.model.RoomType (FQN in asserts)
            "RoomMapperTest.java -> com.andreasik.efipoker.api.model.RoomType");

    @Test
    void should_not_use_lombok_experimental() throws IOException {
      List<String> violations = new ArrayList<>();
      try (Stream<Path> files =
          Files.walk(SRC_ROOT.resolve("main")).filter(p -> p.toString().endsWith(".java"))) {
        files.forEach(
            path -> {
              try {
                String content = Files.readString(path);
                if (content.contains("lombok.experimental")) {
                  violations.add(path.getFileName().toString());
                }
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
      }
      assertThat(violations)
          .as(
              "No lombok.experimental imports - use stable Lombok API only (lombok.config also blocks this)")
          .isEmpty();
    }

    @Test
    void entities_should_follow_lombok_conventions() throws IOException {
      List<String> violations = new ArrayList<>();
      try (Stream<Path> files =
          Files.walk(SRC_ROOT.resolve("main")).filter(p -> p.toString().endsWith("Entity.java"))) {
        files.forEach(
            path -> {
              try {
                String content = Files.readString(path);
                String filename = path.getFileName().toString();
                if (!content.contains("@Entity")) return;

                if (!content.contains("@ToString(onlyExplicitlyIncluded = true)")) {
                  violations.add(filename + ": missing @ToString(onlyExplicitlyIncluded = true)");
                }
                if (content.contains("@Data")) {
                  violations.add(filename + ": @Data forbidden on entities");
                }
                if (content.contains("@EqualsAndHashCode")) {
                  violations.add(
                      filename + ": @EqualsAndHashCode forbidden on entities (use manual impl)");
                }
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
      }
      assertThat(violations)
          .as(
              "JPA entities must follow Lombok conventions (no @Data, no @EqualsAndHashCode,"
                  + " @ToString(onlyExplicitlyIncluded = true) required)")
          .isEmpty();
    }

    /// Known tech debt: RateLimitFilterTest uses Thread.sleep for window expiry.
    /// New tests MUST NOT add Thread.sleep - use mocked clocks or pre-built test data instead.
    private static final Set<String> THREAD_SLEEP_EXCEPTIONS =
        Set.of("RateLimitFilterTest.java", "ArchitectureTest.java");

    @Test
    void tests_should_not_use_thread_sleep() throws IOException {
      List<String> violations = new ArrayList<>();
      try (Stream<Path> files =
          Files.walk(SRC_ROOT.resolve("test")).filter(p -> p.toString().endsWith(".java"))) {
        files.forEach(
            path -> {
              try {
                if (THREAD_SLEEP_EXCEPTIONS.contains(path.getFileName().toString())) return;
                List<String> lines = Files.readAllLines(path);
                for (int i = 0; i < lines.size(); i++) {
                  String line = lines.get(i).trim();
                  if (line.startsWith("//") || line.startsWith("*")) continue;
                  if (line.contains("Thread.sleep(") || line.contains("Thread.sleep (")) {
                    String rel = SRC_ROOT.getParent().relativize(path).toString();
                    violations.add(rel + ":" + (i + 1));
                  }
                }
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
      }
      assertThat(violations)
          .as(
              "Thread.sleep() in tests is an anti-pattern - use mocked clocks, "
                  + "pre-expired tokens, or Awaitility instead")
          .isEmpty();
    }

    @Test
    void should_not_use_inline_fqn_when_import_is_possible() throws IOException {
      assertThat(SRC_ROOT.toFile())
          .as("SRC_ROOT must point to backend/src (check user.dir)")
          .isDirectory();

      List<String> violations = new ArrayList<>();
      Set<String> usedExceptions = new HashSet<>();

      try (Stream<Path> files = Files.walk(SRC_ROOT).filter(p -> p.toString().endsWith(".java"))) {
        files.forEach(
            path -> {
              try {
                List<String> lines = Files.readAllLines(path);
                for (int i = 0; i < lines.size(); i++) {
                  String line = lines.get(i).trim();
                  if (line.startsWith("import ")
                      || line.startsWith("package ")
                      || line.startsWith("//")
                      || line.startsWith("*")
                      || line.startsWith("/*")
                      || line.contains("\"")) {
                    continue;
                  }
                  String fileName = path.getFileName().toString();
                  Matcher matcher = FQN_PATTERN.matcher(line);
                  while (matcher.find()) {
                    String fqn = matcher.group();
                    boolean allowed =
                        ALLOWED_FQN_PER_FILE.stream()
                            .anyMatch(
                                entry -> {
                                  String[] parts = entry.split(" -> ");
                                  boolean matches =
                                      fileName.equals(parts[0])
                                          && (fqn.equals(parts[1])
                                              || fqn.startsWith(parts[1] + "."));
                                  if (matches) {
                                    usedExceptions.add(entry);
                                  }
                                  return matches;
                                });
                    if (!allowed) {
                      String relativePath =
                          SRC_ROOT.getParent() != null
                              ? SRC_ROOT.getParent().relativize(path).toString()
                              : path.toString();
                      violations.add(
                          relativePath + ":" + (i + 1) + " -> " + fqn + " (use import instead)");
                    }
                  }
                }
              } catch (IOException e) {
                throw new RuntimeException("Failed to read " + path, e);
              }
            });
      }

      assertThat(violations)
          .as(
              "Inline fully-qualified class names found. Fix: add an import statement and use "
                  + "the simple class name. If two classes share the same name (e.g. domain "
                  + "RoomType vs api.model.RoomType), add a per-file exception to "
                  + "ALLOWED_FQN_PER_FILE in this test: \"File.java -> fqn.to.allow\"")
          .isEmpty();

      List<String> unusedExceptions =
          ALLOWED_FQN_PER_FILE.stream().filter(e -> !usedExceptions.contains(e)).toList();
      assertThat(unusedExceptions)
          .as("Stale entries in ALLOWED_FQN_PER_FILE - the FQN is no longer used, remove them")
          .isEmpty();
    }
  }
}
