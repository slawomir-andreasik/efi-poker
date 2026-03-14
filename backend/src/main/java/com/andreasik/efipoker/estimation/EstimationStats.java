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
    return median(extractNumericPoints(storyPoints));
  }

  public static Double computeStdDeviation(List<String> storyPoints) {
    return stdDeviation(extractNumericPoints(storyPoints));
  }

  public static Double computeSum(List<String> storyPoints) {
    List<Double> numeric = extractNumericPoints(storyPoints);
    if (numeric.isEmpty()) {
      return null;
    }
    return numeric.stream().mapToDouble(Double::doubleValue).sum();
  }

  public static boolean isNumeric(String value) {
    if (value == null) {
      return false;
    }
    try {
      Double.parseDouble(value);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public static boolean isConsensus(List<Double> numeric) {
    return numeric.size() >= 2 && numeric.stream().distinct().count() == 1;
  }

  public static double computeTaskSP(String finalEstimate, List<Double> numeric) {
    if (finalEstimate != null) {
      try {
        return Double.parseDouble(finalEstimate);
      } catch (NumberFormatException ignored) {
        // Fall through to median
      }
    }
    Double med = median(numeric);
    return med != null ? med : 0.0;
  }

  public static List<Double> extractNumericPoints(List<String> storyPoints) {
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

  static Double median(List<Double> numeric) {
    if (numeric.isEmpty()) {
      return null;
    }
    List<Double> sorted = new ArrayList<>(numeric);
    Collections.sort(sorted);
    int size = sorted.size();
    if (size % 2 == 0) {
      return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
    }
    return sorted.get(size / 2);
  }

  static Double stdDeviation(List<Double> numeric) {
    if (numeric.size() < 2) {
      return null;
    }
    double mean = numeric.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    double variance =
        numeric.stream().mapToDouble(v -> (v - mean) * (v - mean)).average().orElse(0.0);
    return Math.sqrt(variance);
  }
}
