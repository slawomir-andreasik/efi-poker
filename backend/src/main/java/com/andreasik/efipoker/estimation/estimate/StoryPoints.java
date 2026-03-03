package com.andreasik.efipoker.estimation.estimate;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StoryPoints {
  SP_0("0"),
  SP_0_5("0.5"),
  SP_1("1"),
  SP_2("2"),
  SP_3("3"),
  SP_5("5"),
  SP_8("8"),
  SP_13("13"),
  SP_21("21"),
  UNSURE("?");

  private final String value;

  public static StoryPoints fromValue(String value) {
    return Arrays.stream(values())
        .filter(sp -> sp.value.equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid story points value: " + value));
  }
}
