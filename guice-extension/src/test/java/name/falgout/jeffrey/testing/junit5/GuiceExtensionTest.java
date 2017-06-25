package name.falgout.jeffrey.testing.junit5;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.mockito.ArgumentCaptor;

import com.google.inject.Injector;
import com.google.inject.Key;

import name.falgout.jeffrey.testing.junit5.BadUseCases.BadModule2;

@ExtendWith(GuiceExtension.class)
@IncludeModule(TestModule.class)
public class GuiceExtensionTest {
  @Inject static int STATIC_INJECTION;
  @Inject Object memberInjection;
  @Inject @SomeQualifyingAnnotation String qualifiedField;

  TestExecutionListener listener = mock(TestExecutionListener.class);
  ArgumentCaptor<TestIdentifier> identifierCaptor = ArgumentCaptor.forClass(TestIdentifier.class);
  ArgumentCaptor<TestExecutionResult> resultCaptor =
      ArgumentCaptor.forClass(TestExecutionResult.class);

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
  void doesNotInjectParametersWithMultipleBindingAnnotations() throws NoSuchMethodException {
    testMethod((id, result) -> {
      assertThat(result.getStatus()).isEqualTo(Status.FAILED);
      Throwable t = result.getThrowable().get();
      assertThat(t).isInstanceOf(ParameterResolutionException.class);
    }, BadUseCases.class, "tooManyBindingAnnotations", String.class);
  }

  @Test
  void modulesMustHaveZeroArgConstructor() throws NoSuchMethodException {
    testMethod((id, result) -> {
      assertThat(result.getStatus()).isEqualTo(Status.FAILED);
      Throwable t = result.getThrowable().get();
      assertThat(t).isInstanceOf(ParameterResolutionException.class);
      assertThat(t.getCause()).isInstanceOf(NoSuchMethodException.class);
      assertThat(t.getCause().getMessage()).contains("BadModule1.<init>()");
    }, BadUseCases.class, "moduleWithoutZeroArgConstructor", String.class);
  }

  @Test
  void moduleConstructorCannotThrowException() throws NoSuchMethodException {
    testMethod((id, result) -> {
      assertThat(result.getStatus()).isEqualTo(Status.FAILED);
      Throwable t = result.getThrowable().get();
      assertThat(t).isInstanceOf(ParameterResolutionException.class);
      assertThat(t.getCause()).isInstanceOf(InvocationTargetException.class);
      assertThat(t.getCause().getCause()).isInstanceOf(IllegalArgumentException.class);
      assertThat(t.getCause().getCause().getMessage()).isEqualTo(BadModule2.MESSAGE);
    }, BadUseCases.class, "moduleConstructorThrowsException", String.class);
  }

  private void testMethod(
      BiConsumer<? super TestIdentifier, ? super TestExecutionResult> resultTest,
      Class<?> clazz, String methodName, Class<?>... parameterTypes)
      throws NoSuchMethodException {
    Launcher launcher = LauncherFactory.create();
    LauncherDiscoveryRequest discoveryRequest = LauncherDiscoveryRequestBuilder.request()
        .selectors(selectMethod(BadUseCases.class, methodName, parameterTypes))
        .build();

    launcher.registerTestExecutionListeners(listener);
    launcher.execute(discoveryRequest);
    verify(listener, atLeast(1)).executionFinished(identifierCaptor.capture(),
        resultCaptor.capture());

    int index = getIndentifierIndex(methodName, identifierCaptor.getAllValues());
    resultTest.accept(identifierCaptor.getAllValues().get(index),
        resultCaptor.getAllValues().get(index));
  }

  private static int getIndentifierIndex(String methodName, List<TestIdentifier> identifiers) {
    for (int i = 0; i < identifiers.size(); i++) {
      TestIdentifier identifier = identifiers.get(i);
      if (identifier.getSource().filter(source -> source instanceof MethodSource).isPresent()) {
        MethodSource source = (MethodSource) identifier.getSource().get();
        if (source.getMethodName().equals(methodName)) {
          return i;
        }
      }
    }

    throw new NoSuchElementException(
        "No identifier with methodName: " + methodName + " found in " + identifiers);
  }

  static MethodSelector selectMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes)
      throws NoSuchMethodException {
    return DiscoverySelectors.selectMethod(clazz,
        clazz.getDeclaredMethod(methodName, parameterTypes));
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
      return new String[] {"foo", "bar"};
    }
  }
}
