package name.falgout.jeffrey.testing.junit.testing;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.truth.ThrowableSubject;
import java.util.Optional;
import name.falgout.jeffrey.testing.junit.testing.ExpectFailure.Cause;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

final class ExpectFailureExceptionHandler implements TestExecutionExceptionHandler,
    AfterEachCallback {
  private static final Namespace NAMESPACE =
      Namespace.create("name", "falgout", "jeffrey", "testing", "junit", "expected", "failure");

  @Override
  public void handleTestExecutionException(ExtensionContext context, Throwable throwable)
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
  public void afterEach(ExtensionContext context) throws Exception {
    Optional<ExpectFailure> annotation = getAnnotation(context);
    if (!annotation.isPresent()) {
      return;
    }

    Throwable failure = context.getStore(NAMESPACE).get(annotation.get(), Throwable.class);
    if (failure == null) {
      throw new AssertionError("No exception was thrown!");
    }
  }

  private static Optional<ExpectFailure> getAnnotation(ExtensionContext context) {
    return context.getElement().map(element -> element.getAnnotation(ExpectFailure.class));
  }
}
