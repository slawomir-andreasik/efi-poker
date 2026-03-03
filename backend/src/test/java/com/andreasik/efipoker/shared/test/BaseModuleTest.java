package com.andreasik.efipoker.shared.test;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Tag("module")
@ActiveProfiles("test")
@Transactional
public abstract class BaseModuleTest {

  @ServiceConnection static final PostgreSQLContainer postgres;

  static {
    postgres = BaseComponentTest.postgres;
  }
}
