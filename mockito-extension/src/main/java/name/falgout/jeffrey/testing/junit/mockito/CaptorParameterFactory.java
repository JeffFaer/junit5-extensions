package name.falgout.jeffrey.testing.junit.mockito;

import com.google.common.reflect.TypeToken;
import java.lang.reflect.Parameter;
import org.mockito.ArgumentCaptor;

/**
 * Processes parameters of type {@link ArgumentCaptor}.
 */
final class CaptorParameterFactory implements ParameterFactory {
  @Override
  public boolean supports(Parameter parameter) {
    return parameter.getType() == ArgumentCaptor.class;
  }

  @Override
  public Object getParameterValue(Parameter parameter) {
    TypeToken<?> parameterType = TypeToken.of(parameter.getParameterizedType());
    TypeToken<?> captorParameter =
        parameterType.resolveType(ArgumentCaptor.class.getTypeParameters()[0]);
    return ArgumentCaptor.forClass(captorParameter.getRawType());
  }
}
