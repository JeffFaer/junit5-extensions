package name.falgout.jeffrey.testing.junit5;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import name.falgout.jeffrey.testing.junit5.TestPlanExecutionReport.DisplayName;
import org.junit.jupiter.api.Test;

public class TestDisablerMetaTest {
  @Test
  void metaTest() {
    TestPlanExecutionReport report = ExtensionTester.runTests(TestDisablerTest.class);

    DisplayName foo = DisplayName.create("foo()");
    DisplayName disableSpecificTest = DisplayName.create("disableSpecificTest()");

    DisplayName nestedFoo = DisplayName.create("NestedTests", "foo()");
    DisplayName nestedBar = DisplayName.create("NestedTests", "bar()");
    DisplayName nestedBaz = DisplayName.create("NestedTests", "baz()");
    DisplayName nestedDifferentDisplayName = DisplayName
        .create("NestedTests", "Different.Display.Name!");

    DisplayName nested2 = DisplayName.create("NestedTests2");

    DisplayName parameterized1 = DisplayName.create("parameterizedTest(int)", "1");
    DisplayName parameterized2 = DisplayName.create("parameterizedTest(int)", "2");
    DisplayName parameterized3 = DisplayName.create("parameterizedTest(int)", "3");
    assertAll(
        () -> assertThat(report.getTests()).hasSize(9),
        () ->
            assertThat(report.getSuccessful())
                .containsAllOf(
                    nestedFoo,
                    parameterized1,
                    parameterized2),
        () ->
            assertThat(report.getSkipped().keySet())
                .containsAllOf(
                    foo,
                    disableSpecificTest,
                    nestedBar,
                    nestedBaz,
                    nestedDifferentDisplayName,
                    nested2,
                    parameterized3),
        () -> assertThat(report.getSkippedCause(foo)).hasValue("foo!"),
        () -> assertThat(report.getSkippedCause(nestedBaz)).hasValue("baz!")
    );
  }
}
