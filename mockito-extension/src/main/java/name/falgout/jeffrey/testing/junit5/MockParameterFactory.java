package name.falgout.jeffrey.testing.junit5;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import org.mockito.Mock;
import org.mockito.MockSettings;
import org.mockito.Mockito;

/**
 * Processes parameters annotated with {@link Mock}.
 */
final class MockParameterFactory implements ParameterFactory {
  @Override
  public boolean supports(Parameter parameter) {
    return parameter.isAnnotationPresent(Mock.class);
  }

  @Override
  public Object getParameterValue(Parameter parameter) {
    Mock annotation = parameter.getAnnotation(Mock.class);
    MockSettings settings = Mockito.withSettings();
    if (annotation.extraInterfaces().length > 0) {
      settings.extraInterfaces(annotation.extraInterfaces());
    }
    if (annotation.serializable()) {
      settings.serializable();
    }
    settings.name(annotation.name().isEmpty() ? parameter.getName() : annotation.name());
    settings.defaultAnswer(annotation.answer());

    return Mockito.mock(parameter.getType(), settings);
  }
}
