package name.falgout.jeffrey.testing.junit.guice;

import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.ProvisionException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.junit.platform.commons.support.AnnotationSupport.findRepeatableAnnotations;
import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;

public final class GuiceExtension implements TestInstancePostProcessor, ParameterResolver {
  private static final Namespace NAMESPACE =
      Namespace.create("name", "falgout", "jeffrey", "testing", "junit", "guice");

  private static final ConcurrentMap<Set<? extends Class<?>>, Injector> INJECTOR_CACHE = new ConcurrentHashMap<>();

  public GuiceExtension() {}

  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context)
      throws Exception {
    getOrCreateInjector(context).ifPresent(injector -> injector.injectMembers(testInstance));
  }

  /**
   * Returns an injector for the given context if and only if the given context has an {@link
   * ExtensionContext#getElement() annotated element}.
   */
  private static Optional<Injector> getOrCreateInjector(ExtensionContext context)
      throws NoSuchMethodException,
      InstantiationException,
      IllegalAccessException,
      InvocationTargetException {
    if (!context.getElement().isPresent()) {
      return Optional.empty();
    }
    AnnotatedElement element = context.getElement().get();
    Store store = context.getStore(NAMESPACE);
    Injector injector = store.get(element, Injector.class);
    boolean sharedInjector = isSharedInjector(context);
    Set<Class<? extends Module>> moduleClasses = Collections.emptySet();
    if (injector == null && sharedInjector) {
      moduleClasses = getContextModuleTypes(context);
      injector = INJECTOR_CACHE.get(moduleClasses);
    }
    if (injector == null) {
      injector = createInjector(context);
      store.put(element, injector);
      if (sharedInjector && !moduleClasses.isEmpty()) {
        INJECTOR_CACHE.put(moduleClasses, injector);
      }
    }
    return Optional.of(injector);
  }

  private static Injector createInjector(ExtensionContext context)
      throws NoSuchMethodException,
      InstantiationException,
      IllegalAccessException,
      InvocationTargetException {
    Optional<Injector> parentInjector = getParentInjector(context);
    List<? extends Module> modules = getNewModules(context);
    return parentInjector
        .map(injector -> injector.createChildInjector(modules))
        .orElseGet(() -> Guice.createInjector(modules));
  }

  private static boolean isSharedInjector(ExtensionContext context) {
    if (!context.getElement().isPresent()) {
      return false;
    }
    AnnotatedElement element = context.getElement().get();
    return isAnnotated(element, SharedInjectors.class);
  }

  private static Optional<Injector> getParentInjector(ExtensionContext context)
      throws NoSuchMethodException,
      InstantiationException,
      IllegalAccessException,
      InvocationTargetException {
    if (context.getParent().isPresent()) {
      return getOrCreateInjector(context.getParent().get());
    }

    return Optional.empty();
  }

  /**
   * @throws NoSuchMethodException there is no zero-args constructor for a module
   * @throws InstantiationException one of the module classes is abstract
   * @throws IllegalAccessException we call setAccessible(true), so this shouldn't happen
   * @throws InvocationTargetException a module's constructor threw an exception
   */
  private static List<? extends Module> getNewModules(ExtensionContext context)
      throws NoSuchMethodException,
      InstantiationException,
      IllegalAccessException,
      InvocationTargetException {
    Set<Class<? extends Module>> moduleTypes = getNewModuleTypes(context);
    List<Module> modules = new ArrayList<>(moduleTypes.size());
    for (Class<? extends Module> moduleType : moduleTypes) {
      Constructor<? extends Module> moduleCtor = moduleType.getDeclaredConstructor();
      moduleCtor.setAccessible(true);

      modules.add(moduleCtor.newInstance());
    }

    context.getElement().ifPresent(element -> {
      if (element instanceof Class) {
        modules.add(new AbstractModule() {
          @Override
          protected void configure() {
            requestStaticInjection((Class<?>) element);
          }
        });
      }
    });

    return modules;
  }

  /**
   * Returns module types that are introduced for the first time by the given context (they do not
   * appear in an enclosing context).
   */
  private static Set<Class<? extends Module>> getNewModuleTypes(ExtensionContext context) {
    if (!context.getElement().isPresent()) {
      return Collections.emptySet();
    }

    Set<Class<? extends Module>> moduleTypes = getModuleTypes(context.getElement().get());
    context.getParent()
        .map(GuiceExtension::getContextModuleTypes)
        .ifPresent(moduleTypes::removeAll);

    return moduleTypes;
  }

  private static Set<Class<? extends Module>> getContextModuleTypes(ExtensionContext context) {
    return getContextModuleTypes(Optional.of(context));
  }

  /**
   * Returns module types that are present on the given context or any of its enclosing contexts.
   */
  private static Set<Class<? extends Module>> getContextModuleTypes(
      Optional<ExtensionContext> context) {
    // TODO: Cache?

    Set<Class<? extends Module>> contextModuleTypes = new LinkedHashSet<>();
    while (context.isPresent() && (hasAnnotatedElement(context) || hasParent(context))) {
      context
          .flatMap(ExtensionContext::getElement)
          .map(GuiceExtension::getModuleTypes)
          .ifPresent(contextModuleTypes::addAll);
      context = context.flatMap(ExtensionContext::getParent);
    }

    return contextModuleTypes;
  }

  private static boolean hasAnnotatedElement(Optional<ExtensionContext> context) {
    return context.flatMap(ExtensionContext::getElement).isPresent();
  }

  private static boolean hasParent(Optional<ExtensionContext> context) {
    return context.flatMap(ExtensionContext::getParent).isPresent();
  }

  private static Set<Class<? extends Module>> getModuleTypes(AnnotatedElement element) {
    return
        findRepeatableAnnotations(element, IncludeModule.class)
            .stream()
            .map(IncludeModule::value)
            .flatMap(Stream::of)
            .collect(toSet());
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext)
      throws ParameterResolutionException {
    Parameter parameter = parameterContext.getParameter();
    if (getBindingAnnotations(parameter).size() > 1) {
      return false;
    }

    Key<?> key = getKey(
        extensionContext.getTestClass(),
        parameter);
    Optional<Injector> optInjector = getInjectorForParameterResolution(extensionContext);
    return optInjector.filter(injector -> {

      // Do not bind String without explicit bindings.
      if (key.equals(Key.get(String.class)) && injector.getExistingBinding(key) == null) {
        return false;
      }

      try {
        injector.getInstance(key);
        return true;
      } catch (ConfigurationException | ProvisionException e) {
        // If we throw a ParameterResolutionException here instead of returning false, we'll block
        // other ParameterResolvers from being able to work.
        return false;
      }
    }).isPresent();
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext)
      throws ParameterResolutionException {
    Parameter parameter = parameterContext.getParameter();
    Key<?> key = getKey(extensionContext.getTestClass(), parameter);
    Injector injector = getInjectorForParameterResolution(extensionContext)
        .orElseThrow(() ->
            new ParameterResolutionException(
                String.format(
                    "Could not create injector for: %s It has no annotated element.",
                    extensionContext.getDisplayName())));

    return injector.getInstance(key);
  }

  /**
   * Wrap {@link #getOrCreateInjector(ExtensionContext)} and rethrow exceptions as {@link
   * ParameterResolutionException}.
   */
  private static Optional<Injector> getInjectorForParameterResolution(
      ExtensionContext extensionContext) throws ParameterResolutionException {
    try {
      return getOrCreateInjector(extensionContext);
    } catch (NoSuchMethodException e) {
      throw new ParameterResolutionException("Could not find a suitable constructor for a module.",
          e);
    } catch (InstantiationException e) {
      throw new ParameterResolutionException("One of the modules is abstract!", e);
    } catch (IllegalAccessException e) {
      throw new ParameterResolutionException("We setAccessible(true), this shouldn't happen.", e);
    } catch (InvocationTargetException e) {
      throw new ParameterResolutionException("A module constructor threw an exception.", e);
    }
  }

  private static Key<?> getKey(Optional<Class<?>> containingElement, Parameter parameter) {
    Class<?> clazz =
        containingElement.orElseGet(() -> parameter.getDeclaringExecutable().getDeclaringClass());
    TypeToken<?> classType = TypeToken.of(clazz);
    Type resolvedType = classType.resolveType(parameter.getParameterizedType()).getType();

    Optional<Key<?>> key =
        getOnlyBindingAnnotation(parameter).map(annotation -> Key.get(resolvedType, annotation));
    return key.orElse(Key.get(resolvedType));
  }

  /**
   * @throws IllegalArgumentException if the given element has more than one binding
   *     annotation.
   */
  private static Optional<? extends Annotation> getOnlyBindingAnnotation(AnnotatedElement element) {
    return Optional.ofNullable(Iterables.getOnlyElement(getBindingAnnotations(element), null));
  }

  private static List<Annotation> getBindingAnnotations(AnnotatedElement element) {
    List<Annotation> annotations = new ArrayList<>();
    for (Annotation annotation : element.getAnnotations()) {
      if (isBindingAnnotation(annotation)) {
        annotations.add(annotation);
      }
    }

    return annotations;
  }

  private static boolean isBindingAnnotation(Annotation annotation) {
    Class<? extends Annotation> annotationType = annotation.annotationType();
    return annotationType.isAnnotationPresent(Qualifier.class)
        || annotationType.isAnnotationPresent(BindingAnnotation.class);
  }
}
