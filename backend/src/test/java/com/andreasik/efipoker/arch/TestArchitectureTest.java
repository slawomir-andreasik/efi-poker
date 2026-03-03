package com.andreasik.efipoker.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.andreasik.efipoker.shared.test.BaseArchUnitTest;
import com.andreasik.efipoker.shared.test.BaseComponentTest;
import com.andreasik.efipoker.shared.test.BaseModuleTest;
import com.andreasik.efipoker.shared.test.BaseUnitTest;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TestArchitectureTest")
class TestArchitectureTest extends BaseArchUnitTest {

  private final JavaClasses testClasses =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
          .importPackages(ArchTestUtils.basePackage());

  @Nested
  @DisplayName("BaseClassEnforcement")
  class BaseClassEnforcement {

    @Test
    void should_extend_base_test_class() {
      classes()
          .that()
          .haveSimpleNameEndingWith("Test")
          .and()
          .areNotAssignableTo(BaseUnitTest.class)
          .and()
          .areNotAssignableTo(BaseArchUnitTest.class)
          .and()
          .areNotAssignableTo(BaseComponentTest.class)
          .and()
          .areNotAssignableTo(BaseModuleTest.class)
          .should()
          .beAssignableTo(BaseUnitTest.class)
          .orShould()
          .beAssignableTo(BaseArchUnitTest.class)
          .orShould()
          .beAssignableTo(BaseComponentTest.class)
          .orShould()
          .beAssignableTo(BaseModuleTest.class)
          .allowEmptyShould(true)
          .because("all test classes must extend a Base*Test class")
          .check(testClasses);
    }
  }
}
