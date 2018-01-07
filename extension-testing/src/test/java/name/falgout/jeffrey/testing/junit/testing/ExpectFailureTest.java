package name.falgout.jeffrey.testing.junit.testing;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import name.falgout.jeffrey.testing.junit.testing.ExpectFailure.Cause;
import name.falgout.jeffrey.testing.junit.testing.TestPlanExecutionReport.DisplayName;
import org.junit.jupiter.api.Test;

public class ExpectFailureTest {
  @ExpectFailure(@Cause(type = IllegalArgumentException.class, message = "bar"))
  @Test
  void thisShouldPass() {
    throw new IllegalArgumentException("Foo bar");
  }

  @ExpectFailure(@Cause(type = RuntimeException.class, message = "bar"))
  @Test
  void matchesSubTypes() {
    throw new IllegalArgumentException("Foo bar");
  }

  @ExpectFailure({
      @Cause(type = IllegalArgumentException.class, message = "bar"),
      @Cause(type = IOException.class, message = "something else"),
  })
  @Test
  void matchingCauses() {
    throw new IllegalArgumentException("Foo bar", new IOException("something else"));
  }

  @ExpectFailure(@Cause(type = IllegalArgumentException.class))
  @Test
  void messageIsOptional() {
    throw new IllegalArgumentException("Anything you want");
  }

  @ExpectFailure(@Cause(message = "only message"))
  @Test
  void typeIsOptional() {
    throw new Error("only message");
  }

  @ExpectFailure
  @Test
  void causeIsOptional() {
    throw new IllegalArgumentException();
  }

  @Test
  void negativeExamples() {
    TestPlanExecutionReport report = ExtensionTester.runTests(NegativeExamples.class);

    DisplayName noExceptionThrown = DisplayName.create("noExceptionThrown()");
    DisplayName wrongExceptionThrown = DisplayName.create("wrongExceptionThrown()");
    DisplayName wrongCauseThrown = DisplayName.create("wrongCauseThrown()");
    DisplayName noCauseThrown = DisplayName.create("noCauseThrown()");
    DisplayName wrongMessage = DisplayName.create("wrongMessage()");
    assertAll(
        () -> assertThat(report.getTests()).hasSize(5),
        () ->
            assertThat(report.getFailures().keySet())
                .containsAllOf(
                    noExceptionThrown,
                    wrongExceptionThrown,
                    wrongCauseThrown,
                    noCauseThrown,
                    wrongMessage),
        () -> {
          Throwable failure = report.getFailure(noExceptionThrown).get();
          assertThat(failure).isInstanceOf(AssertionError.class);
          assertThat(failure).hasMessageThat().contains("No exception was thrown!");
        },
        () -> {
          Throwable failure = report.getFailure(wrongExceptionThrown).get();
          assertThat(failure).isInstanceOf(AssertionError.class);
          assertThat(failure).hasMessageThat()
              .contains("Not true that <java.lang.RuntimeException: Oops>"
                  + " is an instance of <java.lang.IllegalArgumentException>");
        },
        () -> {
          Throwable failure = report.getFailure(wrongCauseThrown).get();
          assertThat(failure).isInstanceOf(AssertionError.class);
          assertThat(failure).hasMessageThat()
              .contains("Unexpected cause for java.lang.IllegalArgumentException");
          assertThat(failure).hasMessageThat()
              .contains("Not true that <java.lang.RuntimeException: bar>"
                  + " is an instance of <java.io.IOException>");
        },
        () -> {
          Throwable failure = report.getFailure(noCauseThrown).get();
          assertThat(failure).isInstanceOf(AssertionError.class);
          assertThat(failure).hasMessageThat()
              .contains("Unexpected cause for java.lang.IllegalArgumentException");
          assertThat(failure).hasMessageThat()
              .contains("Not true that <null> is an instance of <java.io.IOException>");
        },
        () -> {
          Throwable failure = report.getFailure(wrongMessage).get();
          assertThat(failure).isInstanceOf(AssertionError.class);
          assertThat(failure).hasMessageThat()
              .contains("Not true that <\"54321\"> contains <\"12345\">");
        }
    );
  }

  static class NegativeExamples {
    @ExpectFailure(@Cause(type = IllegalArgumentException.class))
    @Test
    void noExceptionThrown() {}

    @ExpectFailure(@Cause(type = IllegalArgumentException.class))
    @Test
    void wrongExceptionThrown() {
      throw new RuntimeException("Oops");
    }

    @ExpectFailure({
        @Cause(type = IllegalArgumentException.class),
        @Cause(type = IOException.class)
    })
    @Test
    void wrongCauseThrown() {
      throw new IllegalArgumentException("foo", new RuntimeException("bar"));
    }

    @ExpectFailure({
        @Cause(type = IllegalArgumentException.class),
        @Cause(type = IOException.class)
    })
    @Test
    void noCauseThrown() {
      throw new IllegalArgumentException("foo");
    }

    @ExpectFailure(@Cause(message = "12345"))
    @Test
    void wrongMessage() {
      throw new IllegalArgumentException("54321");
    }
  }
}
