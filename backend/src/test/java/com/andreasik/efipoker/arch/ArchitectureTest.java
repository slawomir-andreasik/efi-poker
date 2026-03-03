package com.andreasik.efipoker.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.andreasik.efipoker.shared.test.BaseArchUnitTest;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
}
