package name.falgout.jeffrey.testing.junit5;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ArgumentConverter;

/**
 * Converts strings to classes.
 */
public final class ClassArgumentConverter implements ArgumentConverter {
  public ClassArgumentConverter() {}

  @SuppressWarnings("unchecked")
  @Override
  public Object convert(Object input, ParameterContext context) throws ArgumentConversionException {
    TypeToken<?> parameterType = TypeToken.of(context.getParameter().getParameterizedType());
    if (parameterType.getRawType() != Class.class) {
      throw new ArgumentConversionException(
          String.format("Could not convert: %s. Invalid parameter type.", input));
    }

    return convert(input.toString(), (TypeToken<? extends Class<?>>) parameterType);
  }

  @VisibleForTesting
  Class<?> convert(String input, TypeToken<? extends Class<?>> targetType)
      throws ArgumentConversionException {
    Class<?> inputType;
    try {
      inputType = Class.forName(input);
    } catch (ClassNotFoundException e) {
      throw new ArgumentConversionException("Could not convert: " + input, e);
    }

    TypeToken<? extends Class<?>> inputClassType = asClassType(inputType);

    if (!targetType.isSupertypeOf(inputClassType)) {
      throw new ArgumentConversionException(
          String.format("%s is not assignable to %s", inputClassType, targetType));
    }

    return inputType;
  }

  private static <T> TypeToken<Class<T>> asClassType(Class<T> classParameter) {
    return new TypeToken<Class<T>>() {}.where(new TypeParameter<T>() {}, classParameter);
  }
}
