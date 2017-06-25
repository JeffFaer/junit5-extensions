package name.falgout.jeffrey.testing.junit5;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.inject.AbstractModule;

@ExtendWith(GuiceExtension.class)
@IncludeModule(TestModule.class)
public class BadUseCases {
  @Test
  void tooManyBindingAnnotations(@SomeBindingAnnotation @SomeQualifyingAnnotation String arg) {}

  @Test
  @IncludeModule(BadModule1.class)
  void moduleWithoutZeroArgConstructor(String string) {}

  @Test
  @IncludeModule(BadModule2.class)
  void moduleConstructorThrowsException(String string) {}

  static final class BadModule1 extends AbstractModule {
    public BadModule1(String argument) {}

    @Override
    protected void configure() {}
  }

  static final class BadModule2 extends AbstractModule {
    static final String MESSAGE = "abc123";

    public BadModule2() {
      throw new IllegalArgumentException(MESSAGE);
    }

    @Override
    protected void configure() {}
  }
}
