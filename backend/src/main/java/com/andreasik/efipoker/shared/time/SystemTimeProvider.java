package com.andreasik.efipoker.shared.time;

import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SystemTimeProvider implements TimeProvider {

  @Override
  public Instant now() {
    return Instant.now();
  }
}
