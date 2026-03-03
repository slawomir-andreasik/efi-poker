package com.andreasik.efipoker.shared.time;

import java.time.Instant;

public interface TimeProvider {

  Instant now();
}
