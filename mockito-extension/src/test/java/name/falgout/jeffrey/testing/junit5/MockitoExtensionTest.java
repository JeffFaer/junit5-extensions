package name.falgout.jeffrey.testing.junit5;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Supplier;
import name.falgout.jeffrey.testing.junit5.TestPlanExecutionReport.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;

@ExtendWith(MockitoExtension.class)
public class MockitoExtensionTest {
  @Mock Function<String, String> mock;
  @Spy ArrayList<String> spy;
  @Captor ArgumentCaptor<String> captor;

  @Test
  void mocksAreInitialized() {
    assertThat(mock).isNotNull();
  }

  @Test
  void spiesAreInitialized() {
    assertThat(spy).isNotNull();
  }

  @Test
  void captorsAreInitialized() {
    assertThat(captor).isNotNull();
  }

  @Test
  void canCreateMocksForTests(@Mock Supplier<String> supplier) {
    assertThat(supplier).isNotNull();
  }

  @Test
  void canCreateCaptors(ArgumentCaptor<String> captor) {
    assertThat(captor).isNotNull();

    mock.apply("foo");

    verify(mock).apply(captor.capture());
    assertThat(captor.getAllValues()).containsExactly("foo");
  }

  @Test
  void negativeExamples() {
    TestPlanExecutionReport report = ExtensionTester.runTests(NegativeExamples.class);

    DisplayName tooManyFactories = DisplayName.create("tooManyFactories(ArgumentCaptor)");
    DisplayName notAnnotated = DisplayName.create("notAnnotated(Supplier)");
    assertAll(
        () -> assertThat(report.getTests()).hasSize(2),
        () ->
            assertThat(report.getFailures().keySet())
                .containsAllOf(
                    tooManyFactories,
                    notAnnotated),
        () -> {
          Throwable failure = report.getFailure(tooManyFactories).get();
          assertThat(failure).isInstanceOf(ParameterResolutionException.class);
          assertThat(failure).hasMessageThat().contains("Too many factories");
        },
        () -> {
          Throwable failure  = report.getFailure(notAnnotated).get();
          assertThat(failure).isInstanceOf(ParameterResolutionException.class);
          assertThat(failure).hasMessageThat().contains("No ParameterResolver registered");
        }
    );
  }

  @SuppressWarnings("unused")
  @ExtendWith(MockitoExtension.class)
  static class NegativeExamples {
    @Test
    void tooManyFactories(@Mock ArgumentCaptor<String> whoShouldWin) {}

    @Test
    void notAnnotated(Supplier<String> notAnnotatedWithMock) {}
  }
}
