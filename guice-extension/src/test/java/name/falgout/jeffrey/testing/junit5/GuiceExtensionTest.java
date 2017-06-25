package name.falgout.jeffrey.testing.junit5;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import java.lang.reflect.InvocationTargetException;
import javax.inject.Inject;
import name.falgout.jeffrey.testing.junit5.NegativeExamples.BadModule2;
import name.falgout.jeffrey.testing.junit5.GuiceExtensionTest.TestModule;
import name.falgout.jeffrey.testing.junit5.TestPlanExecutionReport.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

@ExtendWith(GuiceExtension.class)
@IncludeModule(TestModule.class)
public class GuiceExtensionTest {
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

  @SuppressWarnings("unused")
  @ExtendWith(GuiceExtension.class)
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

  @Test
  void negativeExamplesTest() {
    TestPlanExecutionReport report = ExtensionTester.runTests(NegativeExamples.class);

    DisplayName tooManyBindingAnnotations = DisplayName.create("tooManyBindingAnnotations(String)");
    DisplayName withoutZeroArgConstructor =
        DisplayName.create("moduleWithoutZeroArgConstructor(String)");
    DisplayName constructorThrowsException =
        DisplayName.create("moduleConstructorThrowsException(String)");
    DisplayName doNotHaveBinding = DisplayName.create("doNotHaveBinding(NegativeExamples)");
    assertAll(
        () -> assertThat(report.getTests()).hasSize(4),
        () ->
            assertThat(report.getFailures().keySet())
                .containsAllOf(
                    tooManyBindingAnnotations,
                    withoutZeroArgConstructor,
                    constructorThrowsException,
                    doNotHaveBinding),
        () -> {
          Throwable failure = report.getFailure(tooManyBindingAnnotations).get();
          assertThat(failure).isInstanceOf(ParameterResolutionException.class);
        },
        () -> {
          Throwable failure = report.getFailure(withoutZeroArgConstructor).get();
          assertThat(failure).isInstanceOf(ParameterResolutionException.class);
          assertThat(failure).hasMessageThat().contains("Could not find a suitable constructor");
          assertThat(failure).hasCauseThat().isInstanceOf(NoSuchMethodException.class);
        },
        () -> {
          Throwable failure = report.getFailure(constructorThrowsException).get();
          assertThat(failure).isInstanceOf(ParameterResolutionException.class);
          assertThat(failure).hasCauseThat().isInstanceOf(InvocationTargetException.class);
          assertThat(failure).hasCauseThat().hasCauseThat()
              .isInstanceOf(IllegalArgumentException.class);
          assertThat(failure).hasCauseThat().hasCauseThat().hasMessageThat()
              .isEqualTo(BadModule2.MESSAGE);
        },
        () -> {
          Throwable failure = report.getFailure(doNotHaveBinding).get();
          assertThat(failure).isInstanceOf(ParameterResolutionException.class);
        }
    );
  }

  static final class FooBarExtension implements ParameterResolver {
    @Override
    public boolean supports(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException {
      return parameterContext.getParameter().getType() == String[].class;
    }

    @Override
    public Object resolve(ParameterContext parameterContext, ExtensionContext extensionContext)
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
}
