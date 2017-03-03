package name.falgout.jeffrey.testing.junit5;

import static com.google.common.truth.Truth.assertThat;

import javax.inject.Inject;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.inject.AbstractModule;

import name.falgout.jeffrey.testing.junit5.GuiceExtensionTest.TestModule;

@ExtendWith(GuiceExtension.class)
@IncludeModule(TestModule.class)
public class GuiceExtensionTest {
  static final String STRING = "foo";
  static final int INT = 5;
  static final Object OBJECT = new Object();
  static final byte BYTE = 1;

  static final class TestModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(String.class).toInstance(STRING);
      bind(int.class).toInstance(INT);
      bind(Object.class).toInstance(OBJECT);
    }
  }

  static final class TestModule2 extends AbstractModule {
    @Override
    protected void configure() {
      bind(byte.class).toInstance(BYTE);
    }
  }

  @Inject static int STATIC_INJECTION;
  @Inject Object memberInjection;

  @Test
  void staticInjection() {
    assertThat(STATIC_INJECTION).isEqualTo(INT);
  }

  @Test
  void memberInjection() {
    assertThat(memberInjection).isEqualTo(OBJECT);
  }

  @Nested
  @IncludeModule(TestModule2.class)
  class NestedClass {
    @Inject byte nestedInjection;

    @Test
    void canAddModules() {
      assertThat(nestedInjection).isEqualTo(BYTE);
    }
  }
}
