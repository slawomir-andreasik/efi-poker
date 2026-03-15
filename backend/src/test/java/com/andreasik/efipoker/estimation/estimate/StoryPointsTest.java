package com.andreasik.efipoker.estimation.estimate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.andreasik.efipoker.shared.test.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("StoryPoints")
class StoryPointsTest extends BaseUnitTest {

  @Nested
  @DisplayName("fromValue")
  class FromValue {

    @ParameterizedTest(name = "fromValue(\"{0}\") == {1}")
    @CsvSource({
      "0,    SP_0",
      "0.5,  SP_0_5",
      "1,    SP_1",
      "2,    SP_2",
      "3,    SP_3",
      "5,    SP_5",
      "8,    SP_8",
      "13,   SP_13",
      "21,   SP_21",
      "?,    UNSURE"
    })
    void should_return_correct_enum_constant_for_valid_value(String input, String expectedName) {
      StoryPoints result = StoryPoints.fromValue(input);

      assertThat(result).isNotNull();
      assertThat(result.name()).isEqualTo(expectedName);
    }

    @Test
    void should_return_matching_value_via_getValue() {
      // Verifies the filter predicate: sp.value.equals(value) matches exactly
      StoryPoints result = StoryPoints.fromValue("5");

      assertThat(result).isNotNull();
      assertThat(result.getValue()).isEqualTo("5");
    }

    @Test
    void should_not_match_partial_value() {
      // "1" must not match "13" or "21" - filter uses equals(), not contains()
      StoryPoints result = StoryPoints.fromValue("1");

      assertThat(result).isEqualTo(StoryPoints.SP_1);
      assertThat(result).isNotEqualTo(StoryPoints.SP_13);
      assertThat(result).isNotEqualTo(StoryPoints.SP_21);
    }

    @Test
    void should_not_match_substring_of_valid_value() {
      // "0" must not match "0.5" - filter uses equals(), not startsWith()
      StoryPoints result = StoryPoints.fromValue("0");

      assertThat(result).isEqualTo(StoryPoints.SP_0);
      assertThat(result).isNotEqualTo(StoryPoints.SP_0_5);
    }

    @Test
    void should_throw_for_unknown_value() {
      assertThatThrownBy(() -> StoryPoints.fromValue("99"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid story points value: 99");
    }

    @Test
    void should_throw_for_null_value() {
      // null doesn't equal any value string, so orElseThrow fires with IllegalArgumentException
      assertThatThrownBy(() -> StoryPoints.fromValue(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid story points value");
    }

    @Test
    void should_throw_for_blank_value() {
      assertThatThrownBy(() -> StoryPoints.fromValue(""))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid story points value");
    }

    @Test
    void should_be_case_sensitive() {
      // "?" is valid, but uppercase letters for numeric strings have no edge case;
      // validate that whitespace-padded values are rejected
      assertThatThrownBy(() -> StoryPoints.fromValue(" 5"))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> StoryPoints.fromValue("5 "))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}
