package name.falgout.jeffrey.testing.junit5;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import javax.inject.Qualifier;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;

public final class GuiceExtension implements TestInstancePostProcessor, ParameterResolver {
  private static final Namespace NAMESPACE =
      Namespace.create("name", "falgout", "jeffrey", "junit5", "guice");

  public GuiceExtension() {}

  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context)
      throws Exception {
    getOrCreateInjector(context).ifPresent(injector -> {
      injector.injectMembers(testInstance);
    });
  }

  private static Optional<Injector> getOrCreateInjector(ExtensionContext context)
      throws NoSuchMethodException, InstantiationException, IllegalAccessException,
      InvocationTargetException {
    if (!context.getElement().isPresent()) {
      return Optional.empty();
    }

    AnnotatedElement element = context.getElement().get();
    Store store = context.getStore(NAMESPACE);

    Injector injector = store.get(element, Injector.class);
    if (injector == null) {
      injector = createInjector(context);
      store.put(element, injector);
    }

    return Optional.of(injector);
  }

  private static Injector createInjector(ExtensionContext context) throws NoSuchMethodException,
      InstantiationException, IllegalAccessException, InvocationTargetException {
    Optional<Injector> parentInjector = getParentInjector(context);
    List<? extends Module> modules = getNewModules(context);

    return parentInjector.map(injector -> injector.createChildInjector(modules))
        .orElse(Guice.createInjector(modules));
  }

  private static Optional<Injector> getParentInjector(ExtensionContext context)
      throws NoSuchMethodException, InstantiationException, IllegalAccessException,
      InvocationTargetException {
    if (context.getParent().isPresent()) {
      return getOrCreateInjector(context.getParent().get());
    }

    return Optional.empty();
  }

  private static List<? extends Module> getNewModules(ExtensionContext context)
      throws NoSuchMethodException, InstantiationException, IllegalAccessException,
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
   * @return module types that are introduced for the first time by the given context (they do not
   *         appear in the enclosing context).
   */
  private static Set<Class<? extends Module>> getNewModuleTypes(ExtensionContext context) {
    if (!context.getElement().isPresent()) {
      return Collections.emptySet();
    }

    Set<Class<? extends Module>> moduleTypes = getAllModuleTypes(context.getElement().get());
    context.getParent()
        .map(GuiceExtension::getContextModuleTypes)
        .ifPresent(moduleTypes::removeAll);

    return moduleTypes;
  }

  private static Set<Class<? extends Module>> getContextModuleTypes(ExtensionContext context) {
    return getContextModuleTypes(Optional.of(context));
  }

  /**
   * @return module types that are present in the given context or any of its enclosing contexts.
   */
  private static Set<Class<? extends Module>> getContextModuleTypes(
      Optional<ExtensionContext> context) {
    // TODO: Cache?

    Set<Class<? extends Module>> contextModuleTypes = new LinkedHashSet<>();
    ExtensionContext currentContext;
    while ((currentContext = context.orElse(null)) != null
        && (currentContext.getElement().isPresent() || currentContext.getParent().isPresent())) {
      currentContext.getElement()
          .map(GuiceExtension::getAllModuleTypes)
          .ifPresent(contextModuleTypes::addAll);
      context = currentContext.getParent();
    }

    return contextModuleTypes;
  }

  private static Set<Class<? extends Module>> getAllModuleTypes(AnnotatedElement element) {
    if (element instanceof Class<?>) {
      return getAllModuleTypes((Class<?>) element);
    }

    return getModuleTypes(element);
  }

  /**
   * @return module types that are present on the class or any of its superclasses.
   */
  private static Set<Class<? extends Module>> getAllModuleTypes(Class<?> clazz) {
    Set<Class<? extends Module>> moduleTypes = new LinkedHashSet<>();
    Set<Class<?>> visited = new HashSet<>();
    Queue<Class<?>> toVisit = new LinkedList<>();
    toVisit.add(clazz);

    while (!toVisit.isEmpty()) {
      Class<?> current = toVisit.poll();
      if (current == null || visited.contains(current)) {
        continue;
      }

      moduleTypes.addAll(getModuleTypes(current));
      toVisit.add(current.getSuperclass());
      toVisit.addAll(Arrays.asList(current.getInterfaces()));
    }

    return moduleTypes;
  }

  /**
   * @return module types present on the element.
   */
  private static Set<Class<? extends Module>> getModuleTypes(AnnotatedElement element) {
    Set<Class<? extends Module>> moduleTypes = new LinkedHashSet<>();
    for (IncludeModule included : element.getAnnotationsByType(IncludeModule.class)) {
      for (Class<? extends Module> moduleType : included.value()) {
        moduleTypes.add(moduleType);
      }
    }

    return moduleTypes;
  }

  @Override
  public boolean supports(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    Parameter parameter = parameterContext.getParameter();
    if (getBindingAnnotations(parameter).size() > 1) {
      return false;
    }

    Key<?> key = getKey(parameter);
    Injector injector;
    try {
      Optional<Injector> optInjector = getOrCreateInjector(extensionContext);
      if (!optInjector.isPresent()) {
        return false;
      }

      injector = optInjector.get();
    } catch (Exception e) {
      throw new ParameterResolutionException(
          "Could not create injector for: " + extensionContext.getDisplayName(), e);
    }

    return injector.getExistingBinding(key) != null;
  }

  @Override
  public Object resolve(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    Parameter parameter = parameterContext.getParameter();
    Key<?> key = getKey(parameter);
    Injector injector;
    try {
      injector =
          getOrCreateInjector(extensionContext).orElseThrow(() -> new ParameterResolutionException(
              "Could not create injector for: " + extensionContext.getDisplayName()));
    } catch (Exception e) {
      throw new ParameterResolutionException(
          "Could not create injector for: " + extensionContext.getDisplayName(), e);
    }

    return injector.getInstance(key);
  }

  private static Key<?> getKey(Parameter parameter) {
    TypeToken<?> classType = TypeToken.of(parameter.getDeclaringExecutable().getDeclaringClass());
    Type resolvedType = classType.resolveType(parameter.getParameterizedType()).getType();

    Optional<Key<?>> key =
        getOnlyBindingAnnotation(parameter).map(annotation -> Key.get(resolvedType, annotation));
    return key.orElse(Key.get(resolvedType));
  }

  /**
   * @throws IllegalArgumentException if the given element has more than one binding annotation.
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
