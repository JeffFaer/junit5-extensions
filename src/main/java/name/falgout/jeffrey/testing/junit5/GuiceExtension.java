package name.falgout.jeffrey.testing.junit5;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
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

  private Optional<Injector> getOrCreateInjector(ExtensionContext context) throws Exception {
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

  private Injector createInjector(ExtensionContext context) throws Exception {
    Optional<Injector> parentInjector = getParentInjector(context);
    List<? extends Module> modules = getNewModules(context);

    return parentInjector.map(injector -> injector.createChildInjector(modules))
        .orElse(Guice.createInjector(modules));
  }

  private Optional<Injector> getParentInjector(ExtensionContext context) throws Exception {
    if (context.getParent().isPresent()) {
      return getOrCreateInjector(context.getParent().get());
    }

    return Optional.empty();
  }

  private static List<? extends Module> getNewModules(ExtensionContext context) throws Exception {
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

  private static Set<Class<? extends Module>> getNewModuleTypes(ExtensionContext context) {
    if (!context.getElement().isPresent()) {
      return Collections.emptySet();
    }

    Set<Class<? extends Module>> moduleTypes = getModuleTypes(context.getElement().get());
    context.getParent().map(GuiceExtension::getAllModuleTypes).ifPresent(moduleTypes::removeAll);

    return moduleTypes;
  }

  private static Set<Class<? extends Module>> getAllModuleTypes(ExtensionContext context) {
    return getAllModuleTypes(Optional.of(context));
  }

  private static Set<Class<? extends Module>> getAllModuleTypes(
      Optional<ExtensionContext> context) {
    // TODO: Cache?

    Set<Class<? extends Module>> allModuleTypes = new LinkedHashSet<>();
    ExtensionContext currentContext;
    while ((currentContext = context.orElse(null)) != null
        && (currentContext.getElement().isPresent() || currentContext.getParent().isPresent())) {
      currentContext.getElement()
          .map(GuiceExtension::getModuleTypes)
          .ifPresent(allModuleTypes::addAll);
      context = currentContext.getParent();
    }

    return allModuleTypes;
  }

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

  private Key<?> getKey(Parameter parameter) {
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
