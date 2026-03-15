package com.andreasik.efipoker.estimation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EstimationStats")
class EstimationStatsTest extends BaseUnitTest {

  @Nested
  @DisplayName("computeAverage")
  class ComputeAverage {

    @Test
    void should_return_null_for_empty_list() {
      assertThat(EstimationStats.computeAverage(List.of())).isNull();
    }

    @Test
    void should_return_value_for_single_element() {
      assertThat(EstimationStats.computeAverage(List.of("5"))).isCloseTo(5.0, within(0.001));
    }

    @Test
    void should_compute_average_of_numeric_values() {
      assertThat(EstimationStats.computeAverage(List.of("1", "3", "5")))
          .isCloseTo(3.0, within(0.001));
    }

    @Test
    void should_skip_non_numeric_values() {
      assertThat(EstimationStats.computeAverage(List.of("3", "?", "5")))
          .isCloseTo(4.0, within(0.001));
    }

    @Test
    void should_return_null_when_all_non_numeric() {
      assertThat(EstimationStats.computeAverage(List.of("?", "?"))).isNull();
    }
  }

  @Nested
  @DisplayName("computeMedian")
  class ComputeMedian {

    @Test
    void should_return_null_for_empty_list() {
      assertThat(EstimationStats.computeMedian(List.of())).isNull();
    }

    @Test
    void should_return_value_for_single_element() {
      assertThat(EstimationStats.computeMedian(List.of("8"))).isCloseTo(8.0, within(0.001));
    }

    @Test
    void should_compute_median_for_odd_count() {
      assertThat(EstimationStats.computeMedian(List.of("1", "3", "5")))
          .isCloseTo(3.0, within(0.001));
    }

    @Test
    void should_compute_median_for_even_count() {
      assertThat(EstimationStats.computeMedian(List.of("1", "3", "5", "8")))
          .isCloseTo(4.0, within(0.001));
    }

    @Test
    void should_skip_non_numeric_values() {
      assertThat(EstimationStats.computeMedian(List.of("2", "?", "8")))
          .isCloseTo(5.0, within(0.001));
    }

    @Test
    void should_return_null_when_all_non_numeric() {
      assertThat(EstimationStats.computeMedian(List.of("?"))).isNull();
    }
  }

  @Nested
  @DisplayName("extractNumericPoints")
  class ExtractNumericPoints {

    @Test
    void should_return_empty_for_empty_list() {
      assertThat(EstimationStats.extractNumericPoints(List.of())).isEmpty();
    }

    @Test
    void should_filter_question_marks() {
      List<Double> result = EstimationStats.extractNumericPoints(List.of("3", "?", "5"));
      assertThat(result).containsExactly(3.0, 5.0);
    }

    @Test
    void should_parse_decimal_values() {
      List<Double> result = EstimationStats.extractNumericPoints(List.of("0.5", "1.5"));
      assertThat(result).containsExactly(0.5, 1.5);
    }
  }

  @Nested
  @DisplayName("computeStdDeviation")
  class ComputeStdDeviation {

    @Test
    void should_return_null_for_empty_list() {
      assertThat(EstimationStats.computeStdDeviation(List.of())).isNull();
    }

    @Test
    void should_return_null_for_single_element() {
      assertThat(EstimationStats.computeStdDeviation(List.of("5"))).isNull();
    }

    @Test
    void should_return_zero_for_identical_values() {
      assertThat(EstimationStats.computeStdDeviation(List.of("3", "3", "3")))
          .isCloseTo(0.0, within(0.001));
    }

    @Test
    void should_compute_population_std_deviation() {
      // values: 2, 4, 4, 4, 5, 5, 7, 9 -> mean=5, variance=4, stddev=2
      assertThat(
              EstimationStats.computeStdDeviation(List.of("2", "4", "4", "4", "5", "5", "7", "9")))
          .isCloseTo(2.0, within(0.001));
    }

    @Test
    void should_skip_non_numeric_values() {
      // "?" excluded, only 3 and 5 -> mean=4, variance=1, stddev=1
      assertThat(EstimationStats.computeStdDeviation(List.of("3", "?", "5")))
          .isCloseTo(1.0, within(0.001));
    }

    @Test
    void should_return_null_when_fewer_than_two_numeric_values() {
      assertThat(EstimationStats.computeStdDeviation(List.of("?", "5"))).isNull();
    }
  }

  @Nested
  @DisplayName("computeTaskSP")
  class ComputeTaskSP {

    @Test
    void should_use_final_estimate_when_not_null() {
      // finalEstimate != null branch - mutant removes this check, falling through to median instead
      double result = EstimationStats.computeTaskSP("8", List.of(1.0, 3.0, 5.0));
      assertThat(result).isCloseTo(8.0, within(0.001));
    }

    @Test
    void should_fall_through_to_median_when_final_estimate_is_null() {
      // finalEstimate == null -> uses median of [1, 3, 5] = 3.0
      double result = EstimationStats.computeTaskSP(null, List.of(1.0, 3.0, 5.0));
      assertThat(result).isCloseTo(3.0, within(0.001));
    }

    @Test
    void should_return_zero_when_final_estimate_null_and_no_numeric_votes() {
      // med != null -> false branch: median([]) = null, fallback must be 0.0 not NPE
      double result = EstimationStats.computeTaskSP(null, List.of());
      assertThat(result).isCloseTo(0.0, within(0.001));
    }

    @Test
    void should_fall_through_to_median_when_final_estimate_is_non_numeric() {
      // finalEstimate present but unparseable -> falls through to median of [2, 4] = 3.0
      double result = EstimationStats.computeTaskSP("?", List.of(2.0, 4.0));
      assertThat(result).isCloseTo(3.0, within(0.001));
    }
  }

  @Nested
  @DisplayName("median (internal)")
  class Median {

    @Test
    void should_sort_before_computing_median() {
      // Unsorted input: [5, 1, 3] - without Collections.sort the "middle" element
      // at index 1 would be 1.0, not the true median 3.0
      Double result = EstimationStats.median(new java.util.ArrayList<>(List.of(5.0, 1.0, 3.0)));
      assertThat(result).isCloseTo(3.0, within(0.001));
    }

    @Test
    void should_sort_before_computing_even_count_median() {
      // Unsorted [8, 1, 5, 3] -> sorted [1,3,5,8] -> median = (3+5)/2 = 4.0
      // Without sort: (1+5)/2 = 3.0 (wrong)
      Double result =
          EstimationStats.median(new java.util.ArrayList<>(List.of(8.0, 1.0, 5.0, 3.0)));
      assertThat(result).isCloseTo(4.0, within(0.001));
    }
  }

  @Nested
  @DisplayName("stdDeviation (internal)")
  class StdDeviation {

    @Test
    void should_subtract_mean_not_add_it() {
      // [2, 4, 6]: mean=4, variance=((2-4)^2 + (4-4)^2 + (6-4)^2)/3 = (4+0+4)/3 = 8/3
      // stddev = sqrt(8/3) ≈ 1.6329
      // With mutation (v + mean): variance=((6)^2+(8)^2+(10)^2)/3 = (36+64+100)/3 = 200/3 ≈ 66.67
      // stddev ≈ 8.165 - tightly pinned assertion kills both subtraction mutants
      Double result =
          EstimationStats.stdDeviation(new java.util.ArrayList<>(List.of(2.0, 4.0, 6.0)));
      assertThat(result).isCloseTo(Math.sqrt(8.0 / 3.0), within(0.0001));
    }

    @Test
    void should_return_null_for_empty_list() {
      assertThat(EstimationStats.stdDeviation(List.of())).isNull();
    }

    @Test
    void should_return_null_for_single_element() {
      assertThat(EstimationStats.stdDeviation(List.of(5.0))).isNull();
    }
  }

  @Nested
  @DisplayName("computeSum")
  class ComputeSum {

    @Test
    void should_return_null_for_empty_list() {
      assertThat(EstimationStats.computeSum(List.of())).isNull();
    }

    @Test
    void should_return_null_when_all_non_numeric() {
      assertThat(EstimationStats.computeSum(List.of("?", "?"))).isNull();
    }

    @Test
    void should_sum_numeric_values() {
      assertThat(EstimationStats.computeSum(List.of("1", "3", "5"))).isCloseTo(9.0, within(0.001));
    }

    @Test
    void should_skip_non_numeric_values() {
      assertThat(EstimationStats.computeSum(List.of("3", "?", "5"))).isCloseTo(8.0, within(0.001));
    }

    @Test
    void should_return_value_for_single_element() {
      assertThat(EstimationStats.computeSum(List.of("8"))).isCloseTo(8.0, within(0.001));
    }
  }
}
