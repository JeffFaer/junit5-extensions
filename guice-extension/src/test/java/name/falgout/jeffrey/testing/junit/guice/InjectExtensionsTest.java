package name.falgout.jeffrey.testing.junit.guice;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.inject.AbstractModule;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

@IncludeModule(InjectableExtensionModule.class)
class InjectExtensionsTest {
  @RegisterExtension @Inject InjectableExtension customExtension;

  @Test
  void instanceExtensionInjection() {
    assertThat(customExtension).isNotNull();
  }

  @Test
  void thisTestShouldBeSkipped() {
    fail("This test should have been skipped.");
  }
}

final class InjectableExtensionModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(String.class).toInstance("thisTestShouldBeSkipped");
  }
}

final class InjectableExtension implements ExecutionCondition {
  private final String testMethodToSkip;

  @Inject
  InjectableExtension(String testMethodToSkip) {
    this.testMethodToSkip = testMethodToSkip;
  }

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    if (context.getRequiredTestMethod().getName().equals(testMethodToSkip)) {
      return ConditionEvaluationResult.disabled("Disabled by InjectableExtension.");
    }

    return ConditionEvaluationResult.enabled("");
  }
}
