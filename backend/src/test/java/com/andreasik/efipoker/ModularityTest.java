package com.andreasik.efipoker;

import com.andreasik.efipoker.shared.test.BaseArchUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

@DisplayName("ModularityTest")
class ModularityTest extends BaseArchUnitTest {

  static final ApplicationModules modules = ApplicationModules.of(EfiPokerApplication.class);

  @Test
  void should_verify_modular_structure() {
    modules.verify();
  }

  @Test
  void should_generate_module_documentation() {
    Documenter documenter = new Documenter(modules);
    documenter.writeDocumentation();
    documenter.writeModuleCanvases(Documenter.CanvasOptions.defaults());
  }
}
