package com.andreasik.efipoker.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

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
}
