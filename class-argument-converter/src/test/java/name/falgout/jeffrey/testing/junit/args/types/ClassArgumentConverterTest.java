package name.falgout.jeffrey.testing.junit.args.types;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.reflect.TypeToken;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.converter.ArgumentConversionException;

public class ClassArgumentConverterTest {
  static final TypeToken<? extends Class<?>> THROWABLE = new TypeToken<Class<Throwable>>() {};
  static final TypeToken<? extends Class<?>> EXTENDS_THROWABLE = new TypeToken<Class<? extends Throwable>>() {};
  static final TypeToken<? extends Class<?>> SUPER_THROWABLE = new TypeToken<Class<? super Throwable>>() {};

  ClassArgumentConverter converter;

  @BeforeEach
  void setUp() {
    converter = new ClassArgumentConverter();
  }

  @Test
  void convertToSimpleType() {
    assertThat(converter.convert("java.lang.Throwable", THROWABLE))
        .isEqualTo(Throwable.class);
  }

  @Test
  void convertToSimpleType_cannotBeSubclass() {
    testIncompatible("java.lang.Exception", THROWABLE);
  }

  @Test
  void convertToSimpleType_cannotBeSuperclass() {
    testIncompatible("java.lang.Object", THROWABLE);
  }

  @Test
  void convertToWildcardType_upperBound_onTheBound() {
    assertThat(converter.convert("java.lang.Throwable", EXTENDS_THROWABLE))
        .isEqualTo(Throwable.class);
  }

  @Test
  void convertToWildcardType_upperBound_beneathTheBound() {
    assertThat(converter.convert("java.lang.Exception", EXTENDS_THROWABLE))
        .isEqualTo(Exception.class);
  }

  @Test
  void convertToWildcardType_upperBound_aboveTheBound() {
    testIncompatible("java.lang.Object", EXTENDS_THROWABLE);
  }

  @Test
  void convertToWildcardType_lowerBound_onTheBound() {
    assertThat(converter.convert("java.lang.Throwable", SUPER_THROWABLE))
        .isEqualTo(Throwable.class);
  }

  @Test
  void convertToWildcardType_lowerBound_beneathTheBound() {
    testIncompatible("java.lang.Exception", SUPER_THROWABLE);
  }

  @Test
  void convertToWildcardType_lowerBound_aboveTheBound() {
    assertThat(converter.convert("java.lang.Object", SUPER_THROWABLE))
        .isEqualTo(Object.class);
  }

  @Test
  void parameterizedTypes_doNotWork() {
    testIncompatible("java.util.List", new TypeToken<Class<List<String>>>() {});
  }

  @Test
  void classDoesNotExist() {
    ArgumentConversionException e = assertThrows(ArgumentConversionException.class,
        () -> converter.convert("doesnotexist", THROWABLE));
    assertThat(e).hasMessageThat().contains("Could not convert: doesnotexist");
    assertThat(e).hasCauseThat().isInstanceOf(ClassNotFoundException.class);
  }

  void testIncompatible(String input, TypeToken<? extends Class<?>> type) {
    ArgumentConversionException e = assertThrows(ArgumentConversionException.class,
        () -> converter.convert(input, type));
    assertThat(e).hasMessageThat().contains("is not assignable to");
  }
}
