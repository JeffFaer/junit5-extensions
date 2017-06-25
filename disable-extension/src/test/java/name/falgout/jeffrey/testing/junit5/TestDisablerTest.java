package name.falgout.jeffrey.testing.junit5;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Disable(value = "foo()", reason = "foo!")
@Disable({"NestedTests", "bar()"})
@Disable({"NestedTests2"})
@Disable({"parameterizedTest(int)", "3"})
public class TestDisablerTest {
  @Test
  void foo() {
    fail("Should have been disabled.");
  }

  @Test
  @Disable({})
  void disableSpecificTest() {
    fail("Should have been disabled.");
  }

  @DisabledTests({
      @Disable(value = "baz()", reason = "baz!"),
      @Disable("Different.Display.Name!")
  })
  @Nested
  class NestedTests {
    @Test
    void foo() {}

    @Test
    void bar() {
      fail("Should have been disabled.");
    }

    @Test
    void baz() {
      fail("Should have been disabled.");
    }

    @DisplayName("Different.Display.Name!")
    @Test
    void differentDisplayName() {
      fail("Should have been disabled.");
    }
  }

  @Nested
  class NestedTests2 {
    @Test
    void wholeContainerShouldBeDisabled() {
      fail("Should have been disabled.");
    }
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(ints = {1, 2, 3})
  void parameterizedTest(int value) {
    assertFalse(value > 2);
  }

  static class BadTest {
    @Test
    void badTest() {
      fail("Should have been disabled.");
    }
  }

  @Nested
  @Disable("badTest()")
  class Subclass extends BadTest {}
}