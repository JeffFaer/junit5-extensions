package name.falgout.jeffrey.testing.junit5;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.truth.ThrowableSubject;
import java.util.Optional;
import name.falgout.jeffrey.testing.junit5.ExpectFailure.Cause;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExtensionContext;

final class ExpectFailureExceptionHandler implements TestExecutionExceptionHandler,
    AfterEachCallback {
  private static final Namespace NAMESPACE =
      Namespace.create("name", "falgout", "jeffrey", "testing", "junit5", "expected", "failure");

  @Override
  public void handleTestExecutionException(TestExtensionContext context, Throwable throwable)
      throws Throwable {
    Optional<ExpectFailure> annotation = getAnnotation(context);

    if (!annotation.isPresent()) {
      throw throwable;
    }

    ExpectFailure expectedFailure = annotation.get();
    context.getStore(NAMESPACE).put(expectedFailure, throwable);

    ThrowableSubject subject = assertThat(throwable);
    for (Cause cause : expectedFailure.value()) {
      subject.isInstanceOf(cause.type());
      if (!cause.message().isEmpty()) {
        subject.hasMessageThat().contains(cause.message());
      }
      subject = subject.hasCauseThat();
    }
  }

  @Override
  public void afterEach(TestExtensionContext context) throws Exception {
    Optional<ExpectFailure> annotation = getAnnotation(context);
    if (!annotation.isPresent()) {
      return;
    }

    Throwable failure = context.getStore(NAMESPACE).get(annotation.get(), Throwable.class);
    if (failure == null) {
      throw new AssertionError("No exception was thrown!");
    }
  }

  private static Optional<ExpectFailure> getAnnotation(TestExtensionContext context) {
    return context.getElement().map(element -> element.getAnnotation(ExpectFailure.class));
  }
}
