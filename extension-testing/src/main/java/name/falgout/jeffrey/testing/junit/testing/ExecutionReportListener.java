package name.falgout.jeffrey.testing.junit.testing;

import java.util.HashSet;
import java.util.Set;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * A {@link TestExecutionListener} that constructs a {@link TestPlanExecutionReport}.
 */
public final class ExecutionReportListener implements TestExecutionListener {
  private final Set<Class<?>> classesToSkip = new HashSet<>();
  private TestPlanExecutionReport.Builder executionReportBuilder;

  public ExecutionReportListener() {}

  @Override
  public void testPlanExecutionStarted(TestPlan testPlan) {
    executionReportBuilder = TestPlanExecutionReport.builder(testPlan);
    executionReportBuilder.addAllClassesToSkip(classesToSkip);
    classesToSkip.clear();
  }

  @Override
  public void executionSkipped(TestIdentifier testIdentifier, String reason) {
    executionReportBuilder.addSkipped(testIdentifier, reason);
  }

  @Override
  public void executionFinished(TestIdentifier testIdentifier,
      TestExecutionResult testExecutionResult) {
    executionReportBuilder.addResult(testIdentifier, testExecutionResult);
  }

  public ExecutionReportListener addClassToSkip(Class<?> clazz) {
    if (executionReportBuilder == null) {
      classesToSkip.add(clazz);
    } else {
      executionReportBuilder.addClassToSkip(clazz);
    }
    return this;
  }

  public TestPlanExecutionReport getReport() {
    return executionReportBuilder.build();
  }
}
