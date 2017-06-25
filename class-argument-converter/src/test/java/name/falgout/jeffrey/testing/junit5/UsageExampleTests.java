package name.falgout.jeffrey.testing.junit5;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
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
    void badLowerBound(@ConvertWith(ClassArgumentConverter.class) Class<? super Collection> clazz) {}


    @ParameterizedTest
    @ValueSource(strings = "java.lang.Object")
    void badUpperound(@ConvertWith(ClassArgumentConverter.class) Class<? extends Collection> clazz) {}
  }

  @Test
  void negativeExamplesTest() {
    Launcher launcher = LauncherFactory.create();

    LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(selectClass(NegativeExamples.class))
        .build();

    SummaryGeneratingListener listener = new SummaryGeneratingListener();
    launcher.execute(request, listener);

    TestExecutionSummary summary = listener.getSummary();
    int numMethods = NegativeExamples.class.getDeclaredMethods().length;
    assertAll(
        () ->
            assertWithMessage("Num tests")
                .that(summary.getTestsStartedCount())
                .isEqualTo(numMethods),
        () ->
            assertWithMessage("Num failures")
                .that(summary.getTestsFailedCount())
                .isEqualTo(numMethods)
    );
  }
}
