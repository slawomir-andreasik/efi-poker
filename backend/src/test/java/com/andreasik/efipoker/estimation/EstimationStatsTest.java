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
}
