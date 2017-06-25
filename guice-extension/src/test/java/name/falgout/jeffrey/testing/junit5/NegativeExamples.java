package name.falgout.jeffrey.testing.junit5;

import com.google.inject.AbstractModule;
import name.falgout.jeffrey.testing.junit5.GuiceExtensionTest.TestModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * For some reason, javac really didn't like a static inner class annotated with @ExtendWith.
 */
@ExtendWith(GuiceExtension.class)
@IncludeModule(TestModule.class)
class NegativeExamples {
  @Test
  void tooManyBindingAnnotations(@SomeBindingAnnotation @SomeQualifyingAnnotation String arg) {}

  @Test
  @IncludeModule(BadModule1.class)
  void moduleWithoutZeroArgConstructor(String string) {}

  @Test
  @IncludeModule(BadModule2.class)
  void moduleConstructorThrowsException(String string) {}

  @Test
  void doNotHaveBinding(NegativeExamples examples) {}

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
