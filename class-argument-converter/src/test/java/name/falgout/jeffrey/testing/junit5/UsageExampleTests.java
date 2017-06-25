package name.falgout.jeffrey.testing.junit5;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.util.Collection;
import java.util.List;
import name.falgout.jeffrey.testing.junit5.TestPlanExecutionReport.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

public class UsageExampleTests {
  @Nested
  class PositiveExamples {
    @ParameterizedTest
    @ValueSource(strings = {
        "java.lang.Object",
        "name.falgout.jeffrey.testing.junit5.ClassArgumentConverter",
    })
    void anything(@ConvertWith(ClassArgumentConverter.class) Class<?> anything) {
      assertThat(anything).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "java.util.List",
        "java.util.Collection",
        "java.lang.Object",
    })
    void anySuperclass(
        @ConvertWith(ClassArgumentConverter.class) Class<? super List> superclassOfList) {
      assertThat(superclassOfList).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "java.util.List",
        "java.util.ArrayList",
    })
    void anySubclass(@ConvertWith(ClassArgumentConverter.class) Class<? extends List> anything) {
      assertThat(anything).isNotNull();
    }
  }

  @SuppressWarnings("unused")
  static class NegativeExamples {
    @ParameterizedTest
    @ValueSource(strings = "123ClassDoesNotExist")
    void classNotFound(@ConvertWith(ClassArgumentConverter.class) Class<?> clazz) {}

    @ParameterizedTest
    @ValueSource(strings = "java.lang.Object")
    void badParameterType(@ConvertWith(ClassArgumentConverter.class) String clazz) {}

    @ParameterizedTest
    @ValueSource(strings = "java.util.List")
    void wrongClass(@ConvertWith(ClassArgumentConverter.class) Class<Collection> clazz) {}

    @ParameterizedTest
    @ValueSource(strings = "java.util.List")
    void badLowerBound(
        @ConvertWith(ClassArgumentConverter.class) Class<? super Collection> clazz) {}

    @ParameterizedTest
    @ValueSource(strings = "java.lang.Object")
    void badUpperBound(
        @ConvertWith(ClassArgumentConverter.class) Class<? extends Collection> clazz) {}
  }

  @Test
  void negativeExamplesTest() {
    TestPlanExecutionReport report = ExtensionTester.runTests(NegativeExamples.class);

    DisplayName classNotFound =
        DisplayName.create("classNotFound(Class)", "[1] 123ClassDoesNotExist");
    DisplayName badParameterType =
        DisplayName.create("badParameterType(String)", "[1] java.lang.Object");
    DisplayName wrongClass =
        DisplayName.create("wrongClass(Class)", "[1] java.util.List");
    DisplayName badLowerBound =
        DisplayName.create("badLowerBound(Class)", "[1] java.util.List");
    DisplayName badUpperBound =
        DisplayName.create("badUpperBound(Class)", "[1] java.lang.Object");
    assertAll(
        () -> assertThat(report.getTests()).hasSize(5),
        () ->
            assertThat(report.getFailures().keySet())
                .containsExactly(
                    classNotFound,
                    badParameterType,
                    wrongClass,
                    badLowerBound,
                    badUpperBound),
        () -> {
          Throwable failure = report.getFailure(classNotFound).get();
          assertThat(failure)
              .hasCauseThat()
              .isInstanceOf(ArgumentConversionException.class);
          assertThat(failure)
              .hasCauseThat()
              .hasCauseThat()
              .isInstanceOf(ClassNotFoundException.class);
        },
        () -> {
          Throwable failure = report.getFailure(badParameterType).get();
          assertThat(failure)
              .hasCauseThat()
              .isInstanceOf(ArgumentConversionException.class);
          assertThat(failure)
              .hasCauseThat()
              .hasMessageThat()
              .contains("Invalid parameter type");
        },
        () -> {
          Throwable failure = report.getFailure(wrongClass).get();
          assertThat(failure)
              .hasCauseThat()
              .isInstanceOf(ArgumentConversionException.class);
          assertThat(failure)
              .hasCauseThat()
              .hasMessageThat()
              .isEqualTo(
                  "java.lang.Class<java.util.List> is not assignable to java.lang.Class<java.util.Collection>");
        },
        () -> {
          Throwable failure = report.getFailure(badLowerBound).get();
          assertThat(failure)
              .hasCauseThat()
              .isInstanceOf(ArgumentConversionException.class);
          assertThat(failure)
              .hasCauseThat()
              .hasMessageThat()
              .contains("not assignable to");
        },
        () -> {
          Throwable failure = report.getFailure(badUpperBound).get();
          assertThat(failure)
              .hasCauseThat()
              .isInstanceOf(ArgumentConversionException.class);
          assertThat(failure)
              .hasCauseThat()
              .hasMessageThat()
              .contains("not assignable to");
        }
    );
  }
}
