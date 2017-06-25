package name.falgout.jeffrey.testing.junit5;

import static java.util.stream.Collectors.toList;

import com.google.common.collect.Iterables;
import java.lang.reflect.Parameter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public final class MockitoExtension implements TestInstancePostProcessor, AfterEachCallback,
    ParameterResolver {
  private final Set<ParameterFactory> parameterFactories;

  public MockitoExtension() {
    parameterFactories = new LinkedHashSet<>();
    parameterFactories.add(new MockParameterFactory());
    parameterFactories.add(new CaptorParameterFactory());
  }

  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context)
      throws Exception {
    MockitoAnnotations.initMocks(testInstance);
  }

  @Override
  public void afterEach(TestExtensionContext context) throws Exception {
    Mockito.validateMockitoUsage();
  }

  @Override
  public boolean supports(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return getSupportedFactories(parameterContext.getParameter()).findAny().isPresent();
  }

  @Override
  public Object resolve(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    List<ParameterFactory> validFactories =
        getSupportedFactories(parameterContext.getParameter()).collect(toList());

    if (validFactories.size() > 1) {
      throw new ParameterResolutionException(
          String.format("Too many factories: %s for parameter: %s",
              validFactories,
              parameterContext.getParameter()));
    }

    return Iterables.getOnlyElement(validFactories)
        .getParameterValue(parameterContext.getParameter());
  }

  private Stream<ParameterFactory> getSupportedFactories(Parameter parameter) {
    return parameterFactories.stream().filter(factory -> factory.supports(parameter));
  }
}
