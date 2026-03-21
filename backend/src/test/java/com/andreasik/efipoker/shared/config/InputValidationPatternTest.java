package com.andreasik.efipoker.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.andreasik.efipoker.shared.test.BaseUnitTest;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("OpenAPI Input Validation Patterns")
class InputValidationPatternTest extends BaseUnitTest {

  // Patterns must match OpenAPI schemas exactly
  private static final Pattern HTML_SINGLE_LINE = Pattern.compile("^(?!.*<[a-zA-Z/]).*$");

  private static final Pattern HTML_MULTI_LINE =
      Pattern.compile("^(?![\\s\\S]*<[a-zA-Z/])[\\s\\S]*$", Pattern.DOTALL);

  private static final Pattern PASSWORD_COMPLEXITY =
      Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

  @Nested
  @DisplayName("HTML Rejection - Single-line")
  class HtmlSingleLine {

    static Stream<Arguments> allowedInputs() {
      return Stream.of(
          Arguments.of("normal text", "plain text"),
          Arguments.of("test < 13", "comparison with space"),
          Arguments.of("3 > 2", "greater than"),
          Arguments.of("a -> b", "arrow operator"),
          Arguments.of("<= 8", "less-or-equal"),
          Arguments.of("estimate <5", "less-than with digit"),
          Arguments.of("<3", "heart emoticon / less-than digit"),
          Arguments.of("", "empty string"),
          Arguments.of("special chars: @#$%^&*()", "non-HTML special chars"),
          Arguments.of("quotes: \"hello\" and 'world'", "quotes"),
          Arguments.of("url: https://example.com", "URL"),
          Arguments.of("SP <= 21 && SP >= 0", "double comparison"),
          Arguments.of("<<< not a tag", "multiple angle brackets"),
          Arguments.of("emoji: < 😀 >", "emoji between brackets"));
    }

    static Stream<Arguments> blockedInputs() {
      return Stream.of(
          Arguments.of("<script>alert(1)</script>", "script tag"),
          Arguments.of("<img onerror=x>", "img tag"),
          Arguments.of("</div>", "closing tag"),
          Arguments.of("<a href=x>", "link tag"),
          Arguments.of("text <Script> mixed", "uppercase script tag"),
          Arguments.of("before<br>after", "br tag no space"),
          Arguments.of("<iframe src=x>", "iframe tag"),
          Arguments.of("hello <b>bold</b>", "bold tag"),
          Arguments.of("<svg onload=alert(1)>", "SVG tag"),
          Arguments.of("test</script>", "closing script"),
          Arguments.of("<INPUT type=text>", "uppercase input tag"),
          Arguments.of("click<a>here", "inline link tag"));
    }

    @ParameterizedTest(name = "should allow: {1}")
    @MethodSource("allowedInputs")
    void should_allow_safe_input(String input, String description) {
      assertThat(HTML_SINGLE_LINE.matcher(input).matches())
          .as("Pattern should allow '%s' (%s)", input, description)
          .isTrue();
    }

    @ParameterizedTest(name = "should block: {1}")
    @MethodSource("blockedInputs")
    void should_block_html_tags(String input, String description) {
      assertThat(HTML_SINGLE_LINE.matcher(input).matches())
          .as("Pattern should block '%s' (%s)", input, description)
          .isFalse();
    }
  }

  @Nested
  @DisplayName("HTML Rejection - Multi-line")
  class HtmlMultiLine {

    static Stream<Arguments> allowedInputs() {
      return Stream.of(
          Arguments.of("normal text", "plain text"),
          Arguments.of("test < 13", "comparison with space"),
          Arguments.of("a -> b", "arrow operator"),
          Arguments.of("<= 8", "less-or-equal"),
          Arguments.of("", "empty string"),
          Arguments.of("line1\nline2", "simple newline"),
          Arguments.of("line1\ntest < 13\nline3", "comparison in middle line"),
          Arguments.of("paragraph1\n\nparagraph2", "double newline"),
          Arguments.of("tabs\there", "tab character"),
          Arguments.of("line1\r\nline2", "CRLF newline"),
          Arguments.of("multi\nline\nwith < 5\ncomparison", "comparison in multiline"),
          Arguments.of("## Heading\n- bullet 1\n- bullet 2", "markdown content"),
          Arguments.of("code:\n  if (x < 5) {\n    return;\n  }", "code-like content"));
    }

    static Stream<Arguments> blockedInputs() {
      return Stream.of(
          Arguments.of("<script>alert(1)</script>", "script tag"),
          Arguments.of("line1\n<script>alert(1)</script>\nline3", "script in middle line"),
          Arguments.of("safe text\n<img src=x>", "img tag in last line"),
          Arguments.of("<div>\ncontent\n</div>", "div wrapper"),
          Arguments.of("before\n<iframe src=evil>\nafter", "iframe in multiline"),
          Arguments.of("text\nmore text\n<svg onload=x>", "svg at end"));
    }

    @ParameterizedTest(name = "should allow: {1}")
    @MethodSource("allowedInputs")
    void should_allow_safe_multiline_input(String input, String description) {
      assertThat(HTML_MULTI_LINE.matcher(input).matches())
          .as("Pattern should allow '%s' (%s)", input, description)
          .isTrue();
    }

    @ParameterizedTest(name = "should block: {1}")
    @MethodSource("blockedInputs")
    void should_block_html_tags_in_multiline(String input, String description) {
      assertThat(HTML_MULTI_LINE.matcher(input).matches())
          .as("Pattern should block '%s' (%s)", input, description)
          .isFalse();
    }

    @Test
    @DisplayName("single-line blocked inputs should also be blocked in multi-line")
    void should_block_all_single_line_blocked_inputs() {
      HtmlSingleLine.blockedInputs()
          .forEach(
              args -> {
                String input = (String) args.get()[0];
                String desc = (String) args.get()[1];
                assertThat(HTML_MULTI_LINE.matcher(input).matches())
                    .as("Multi-line pattern should also block '%s' (%s)", input, desc)
                    .isFalse();
              });
    }
  }

  @Nested
  @DisplayName("Password Complexity")
  class PasswordComplexity {

    static Stream<Arguments> validPasswords() {
      return Stream.of(
          Arguments.of("Abcdefg1", "minimum valid (8 chars)"),
          Arguments.of("MyPassword123", "typical password"),
          Arguments.of("aA1!@#$%", "special chars with requirements"),
          Arguments.of("Str0ngP@ssword!", "strong password"),
          Arguments.of("a]B2cdef", "bracket in password"),
          Arguments.of("12345aB8", "digits-heavy with letters"));
    }

    static Stream<Arguments> invalidPasswords() {
      return Stream.of(
          Arguments.of("abcdefgh", "no uppercase, no digit"),
          Arguments.of("ABCDEFGH", "no lowercase, no digit"),
          Arguments.of("12345678", "no letters"),
          Arguments.of("Abcdefg", "only 7 chars"),
          Arguments.of("abcdefg1", "no uppercase"),
          Arguments.of("ABCDEFG1", "no lowercase"),
          Arguments.of("Abcdefgh", "no digit"),
          Arguments.of("", "empty string"),
          Arguments.of("Ab1", "too short with all requirements"));
    }

    @ParameterizedTest(name = "should accept: {1}")
    @MethodSource("validPasswords")
    void should_accept_valid_password(String password, String description) {
      assertThat(PASSWORD_COMPLEXITY.matcher(password).matches())
          .as("Pattern should accept '%s' (%s)", password, description)
          .isTrue();
    }

    @ParameterizedTest(name = "should reject: {1}")
    @MethodSource("invalidPasswords")
    void should_reject_invalid_password(String password, String description) {
      assertThat(PASSWORD_COMPLEXITY.matcher(password).matches())
          .as("Pattern should reject '%s' (%s)", password, description)
          .isFalse();
    }
  }
}
