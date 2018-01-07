package name.falgout.jeffrey.testing.junit.mockito;

import java.lang.reflect.Parameter;

interface ParameterFactory {
  boolean supports(Parameter parameter);

  Object getParameterValue(Parameter parameter);
}
