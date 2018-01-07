package name.falgout.jeffrey.testing.junit.testing;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * A way of summarizing a {@link TestPlan} execution that is more useful for testing than {@link
 * org.junit.platform.launcher.listeners.TestExecutionSummary TestExecutionSummary}.
 */
@AutoValue
public abstract class TestPlanExecutionReport {
  public static Builder builder(TestPlan testPlan) {
    return new AutoValue_TestPlanExecutionReport.Builder().setTestPlan(testPlan);
  }

  TestPlanExecutionReport() {}

  public abstract ImmutableSet<DisplayName> getTests();

  public final boolean wasSkipped(DisplayName displayName) {
    return getSkipped().containsKey(displayName);
  }

  public final boolean wasSuccessful(DisplayName displayName) {
    return getSuccessful().contains(displayName);
  }

  public final boolean wasFailure(DisplayName displayName) {
    return getFailures().containsKey(displayName);
  }

  public final Optional<String> getSkippedCause(DisplayName displayName) {
    return Optional.ofNullable(getSkipped().get(displayName));
  }

  public final Optional<Throwable> getFailure(DisplayName displayName) {
    return Optional.ofNullable(getFailures().get(displayName));
  }

  public abstract ImmutableMap<DisplayName, String> getSkipped();

  public abstract ImmutableSet<DisplayName> getSuccessful();

  public abstract ImmutableMap<DisplayName, Throwable> getFailures();

  @AutoValue.Builder
  public static abstract class Builder {
    private TestPlan testPlan;
    private final Set<Class<?>> classesToSkip = new HashSet<>();

    final Builder setTestPlan(TestPlan testPlan) {
      this.testPlan = testPlan;
      return this;
    }

    /**
     * In normal circumstances, the {@link DisplayName} for a {@link TestIdentifier} will include
     * the engine and outermost enclosing class. Generally, that's undesirable for testing since it
     * increases the verbosity of the asserts. By adding the test class here, it and any container
     * above it will be excluded from the {@link DisplayName}.
     */
    final Builder addClassToSkip(Class<?> clazz) {
      classesToSkip.add(clazz);
      return this;
    }

    final Builder addAllClassesToSkip(Collection<? extends Class<?>> classes) {
      classesToSkip.addAll(classes);
      return this;
    }

    abstract ImmutableSet.Builder<DisplayName> testsBuilder();

    abstract ImmutableMap.Builder<DisplayName, String> skippedBuilder();

    abstract ImmutableSet.Builder<DisplayName> successfulBuilder();

    abstract ImmutableMap.Builder<DisplayName, Throwable> failuresBuilder();

    public final Builder addSkipped(TestIdentifier identifier, @Nullable String reason) {
      DisplayName displayName = getDisplayName(identifier);

      if (identifier.isTest()) {
        testsBuilder().add(displayName);
      }

      skippedBuilder().put(displayName, reason);

      return this;
    }

    public final Builder addResult(TestIdentifier identifier, TestExecutionResult result) {
      DisplayName displayName = getDisplayName(identifier);

      if (identifier.isTest()) {
        testsBuilder().add(displayName);
      }

      switch (result.getStatus()) {
        case SUCCESSFUL:
          successfulBuilder().add(displayName);
          return this;
        case FAILED:
          failuresBuilder().put(displayName, result.getThrowable().orElse(null));
          return this;
        default:
          throw new AssertionError("Unhandled case in enum: " + result.getStatus());
      }
    }

    private DisplayName getDisplayName(TestIdentifier testIdentifier) {
      LinkedList<String> names = new LinkedList<>();
      Optional<TestIdentifier> id = Optional.of(testIdentifier);
      do {
        TestIdentifier identifier = id.get();
        Optional<ClassSource> classSource = identifier.getSource()
            .filter(source -> source instanceof ClassSource)
            .map(source -> (ClassSource) source)
            .filter(source -> !source.getPosition().isPresent())
            .filter(source -> classesToSkip.contains(source.getJavaClass()));
        if (classSource.isPresent()) {
          break;
        }

        names.addFirst(identifier.getDisplayName());

        id = id.flatMap(testPlan::getParent);
      } while (id.isPresent());

      return DisplayName.create(names);
    }

    public abstract TestPlanExecutionReport build();
  }

  /**
   * Represents fully-qualified display name of a test.
   */
  @AutoValue
  public static abstract class DisplayName {
    public static DisplayName create(String... names) {
      return create(Arrays.asList(names));
    }

    public static DisplayName create(Collection<? extends String> names) {
      return new AutoValue_TestPlanExecutionReport_DisplayName(ImmutableList.copyOf(names));
    }

    DisplayName() {}

    public abstract ImmutableList<String> getNames();
  }
}
