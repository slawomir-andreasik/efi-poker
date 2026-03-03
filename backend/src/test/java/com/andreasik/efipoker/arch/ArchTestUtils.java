package com.andreasik.efipoker.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

public final class ArchTestUtils {

  private static final String BASE_PACKAGE = "com.andreasik.efipoker";

  private ArchTestUtils() {}

  public static String basePackage() {
    return BASE_PACKAGE;
  }

  public static JavaClasses importProductionClasses() {
    return new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(BASE_PACKAGE);
  }

  public static JavaClasses importAllClasses() {
    return new ClassFileImporter().importPackages(BASE_PACKAGE);
  }
}
