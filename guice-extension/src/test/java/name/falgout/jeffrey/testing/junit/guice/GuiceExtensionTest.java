package name.falgout.jeffrey.testing.junit.guice;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import name.falgout.jeffrey.testing.junit.guice.GuiceExtensionTest.TestModule;
import name.falgout.jeffrey.testing.junit.testing.ExpectFailure;
import name.falgout.jeffrey.testing.junit.testing.ExpectFailure.Cause;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

@IncludeModule(TestModule.class)
class GuiceExtensionTest {
  @Inject static int STATIC_INJECTION;
  @Inject Object memberInjection;
  @Inject @SomeQualifyingAnnotation String qualifiedField;

  @Test
  void staticInjection() {
    assertThat(STATIC_INJECTION).isEqualTo(TestModule.INT);
  }

  @Test
  void memberInjection() {
    assertThat(memberInjection).isEqualTo(TestModule.OBJECT);
  }

  @Nested
  @IncludeModule(TestModule2.class)
  class NestedClass {
    @Inject byte nestedInjection;

    @Test
    void canAddModules() {
      assertThat(nestedInjection).isEqualTo(TestModule2.BYTE);
    }

    @Test
    void canInjectNestedParameters(String nestedParameter) {
      assertThat(nestedParameter).isEqualTo(TestModule.STRING);
    }
  }

  @Test
  void methodInjection(String parameterInjection) {
    assertThat(parameterInjection).isEqualTo(TestModule.STRING);
  }

  @Test
  @IncludeModule(TestModule2.class)
  void methodsCanIncludeModules(byte parameterInjection, String otherParameter) {
    assertThat(parameterInjection).isEqualTo(TestModule2.BYTE);
    assertThat(otherParameter).isEqualTo(TestModule.STRING);
  }

  @Test
  @ExtendWith(FooBarExtension.class)
  void doesNotResolveEveryParameter(String string, String[] fooBar, Injector injector) {
    assertThat(string).isEqualTo(TestModule.STRING);
    assertThat(fooBar).asList().containsExactly("foo", "bar").inOrder();

    assertNull(injector.getExistingBinding(Key.get(String[].class)));
  }

  @Test
  void canInjectQualifiedFields() {
    assertThat(qualifiedField).isEqualTo(TestModule.QUALIFIED);
  }

  @Test
  void canInjectQualifiedParameters(@SomeBindingAnnotation String bound) {
    assertThat(bound).isEqualTo(TestModule.BOUND);
  }

  @Test
  void canInjectJustInTimeBindings(
      @SomeBindingAnnotation String bound,
      ImplicitlyInjectable justInTime) {
    assertThat(justInTime).isNotNull();
    assertThat(justInTime.getArg()).isEqualTo(bound);
  }

  @SuppressWarnings("unused")
  @IncludeModule(TestModule.class)
  static abstract class BaseType {
    @Inject String baseClassFieldInjection;

    @Test
    void baseClassIsInjected(byte baseClassParameterInjection) {
      assertAll(() -> assertThat(baseClassFieldInjection).isEqualTo(TestModule.STRING),
          () -> assertThat(baseClassParameterInjection).isEqualTo(TestModule2.BYTE));
    }
  }

  @Nested
  @IncludeModule(TestModule2.class)
  class DerivedType extends BaseType {
    @Test
    void subTypeCanIncludeModule(byte parameter) {
      assertThat(parameter).isEqualTo(TestModule2.BYTE);
    }
  }

  @SuppressWarnings("unused")
  static abstract class GenericBaseType<T> {
    @Test
    void checkNotNull(T t) {
      assertThat(t).isNotNull();
    }
  }

  @Nested
  @IncludeModule(TestModule.class)
  class GenericDerivedType extends GenericBaseType<String> {}

  @SuppressWarnings("unused")
  @Nested
  class NegativeExamples {
    @ExpectFailure(
        @Cause(type = ParameterResolutionException.class, message = "No ParameterResolver")
    )
    @Test
    void tooManyBindingAnnotations(@SomeBindingAnnotation @SomeQualifyingAnnotation String arg) {}

    @ExpectFailure({
        @Cause(
            type = ParameterResolutionException.class,
            message = "Could not find a suitable constructor"
        ),
        @Cause(type = NoSuchMethodException.class, message = "BadModule1.<init>()")
    })
    @Test
    @IncludeModule(BadModule1.class)
    void moduleWithoutZeroArgConstructor(String string) {}

    @ExpectFailure({
        @Cause(
            type = ParameterResolutionException.class,
            message = "constructor threw an exception"),
        @Cause(type = InvocationTargetException.class),
        @Cause(type = IllegalArgumentException.class, message = BadModule2.MESSAGE)
    })
    @Test
    @IncludeModule(BadModule2.class)
    void moduleConstructorThrowsException(String string) {}

    @ExpectFailure(
        @Cause(type = ParameterResolutionException.class, message = "No ParameterResolver")
    )
    @Test
    void doesNotHaveInjectConstructor(NotInjectable.Arg arg) {}

    @ExpectFailure(
        @Cause(type = ParameterResolutionException.class, message = "No ParameterResolver")
    )
    @Test
    void cannotBeInjected(NotInjectable notInjectable) {}
  }

    @Nested
    @IncludeModule(CachedModule.class)
    class FirstCachedInjectorTest {

        @Test
        void firstTest(long i) {
            assertEquals(1, i);
        }
        @Test
        void secondTest(long i) {
            assertEquals(1, i);
        }
    }

    @Nested
    @IncludeModule(CachedModule.class)
    class SecondCachedInjectorTest {

        @Test
        void firstTest(long i) {
            assertEquals(1, i);
        }
        @Test
        void secondTest(long i) {
            assertEquals(1, i);
        }
    }

  static final class FooBarExtension implements ParameterResolver {
    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
        ExtensionContext extensionContext)
        throws ParameterResolutionException {
      return parameterContext.getParameter().getType() == String[].class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
        ExtensionContext extensionContext)
        throws ParameterResolutionException {
      return new String[]{"foo", "bar"};
    }
  }

  static final class TestModule extends AbstractModule {
    static final String STRING = "abc";
    static final int INT = 5;
    static final Object OBJECT = new Object();
    static final String QUALIFIED = "qualifying";
    static final String BOUND = "binding";

    @Override
    protected void configure() {
      bind(String.class).toInstance(STRING);
      bind(int.class).toInstance(INT);
      bind(Object.class).toInstance(OBJECT);

      bind(String.class).annotatedWith(SomeBindingAnnotation.class).toInstance(BOUND);
      bind(String.class).annotatedWith(SomeQualifyingAnnotation.class).toInstance(QUALIFIED);
    }
  }

  static final class TestModule2 extends AbstractModule {
    static final byte BYTE = 1;

    @Override
    protected void configure() {
      bind(byte.class).toInstance(BYTE);
    }
  }

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

  private static class ImplicitlyInjectable {
    private String arg;

    @Inject
    ImplicitlyInjectable(@SomeBindingAnnotation String arg) {
      this.arg = arg;
    }

    public String getArg() {
      return arg;
    }
  }

  private static class NotInjectable {
    @Inject
    NotInjectable(Arg arg) {}

    private static class Arg {
      private Arg(String s) {}
    }
  }

  static final class CachedModule extends AbstractModule {
    static final AtomicLong ATOMIC_LONG = new AtomicLong(0);
    @Override
    protected void configure() {
      bind(long.class).toInstance(ATOMIC_LONG.incrementAndGet());
    }
  }
}
