package com.andreasik.efipoker.estimation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Static utility for computing estimation statistics from story point values. */
public final class EstimationStats {

  private EstimationStats() {}

  public static Double computeAverage(List<String> storyPoints) {
    List<Double> numeric = extractNumericPoints(storyPoints);
    if (numeric.isEmpty()) {
      return null;
    }
    return numeric.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
  }

  public static Double computeMedian(List<String> storyPoints) {
    List<Double> numeric = extractNumericPoints(storyPoints);
    if (numeric.isEmpty()) {
      return null;
    }
    Collections.sort(numeric);
    int size = numeric.size();
    if (size % 2 == 0) {
      return (numeric.get(size / 2 - 1) + numeric.get(size / 2)) / 2.0;
    }
    return numeric.get(size / 2);
  }

  static List<Double> extractNumericPoints(List<String> storyPoints) {
    List<Double> numeric = new ArrayList<>();
    for (String sp : storyPoints) {
      try {
        numeric.add(Double.parseDouble(sp));
      } catch (NumberFormatException ignored) {
        // Skip non-numeric values like "?"
      }
    }
    return numeric;
  }
}
