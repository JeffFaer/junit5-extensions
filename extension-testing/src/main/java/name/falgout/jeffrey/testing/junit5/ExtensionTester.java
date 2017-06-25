package name.falgout.jeffrey.testing.junit5;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

/**
 * A utility for executing JUnit 5 tests in a JUnit 5 test.
 */
public final class ExtensionTester {
  private ExtensionTester() {}

  public static TestPlanExecutionReport runTests(Class<?> clazz) {
    Launcher launcher = LauncherFactory.create();
    LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(selectClass(clazz))
        .build();

    ExecutionReportListener listener = new ExecutionReportListener().addClassToSkip(clazz);
    launcher.execute(request, listener);
    return listener.getReport();
  }
}
