package name.falgout.jeffrey.testing.junit5;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.inject.Inject;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import com.google.inject.Injector;
import com.google.inject.Key;

@ExtendWith(GuiceExtension.class)
@IncludeModule(TestModule.class)
public class GuiceExtensionTest {
  @Inject static int STATIC_INJECTION;
  @Inject Object memberInjection;

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
