package name.falgout.jeffrey.testing.junit5;

import static org.junit.platform.commons.support.AnnotationSupport.findRepeatableAnnotations;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ContainerExecutionCondition;
import org.junit.jupiter.api.extension.ContainerExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionCondition;
import org.junit.jupiter.api.extension.TestExtensionContext;

final class TestDisabler implements TestExecutionCondition, ContainerExecutionCondition {
  TestDisabler() {}

  @Override
  public ConditionEvaluationResult evaluate(ContainerExtensionContext context) {
    return evaluate((ExtensionContext) context);
  }

  @Override
  public ConditionEvaluationResult evaluate(TestExtensionContext context) {
    return evaluate((ExtensionContext) context);
  }

  public ConditionEvaluationResult evaluate(ExtensionContext context) {
    return evaluate(new LinkedList<>(), context);
  }

  private ConditionEvaluationResult evaluate(
      LinkedList<String> displayName,
      ExtensionContext context) {
    Optional<ConditionEvaluationResult> result =
        context
            .getElement()
            .flatMap(element -> evaluateElement(displayName, element));

    if (result.isPresent()) {
      return result.get();
    }

    displayName.addFirst(context.getDisplayName());
    return context.getParent()
        .map(parent -> evaluate(displayName, parent))
        .orElse(ConditionEvaluationResult.enabled(null));
  }

  Optional<ConditionEvaluationResult> evaluateElement(
      List<String> currentDisplayName,
      AnnotatedElement element) {
    List<Disable> disabledTests = findRepeatableAnnotations(element, Disable.class);

    return disabledTests
        .stream()
        .filter(
            disabledTest -> Arrays.asList(disabledTest.value()).equals(currentDisplayName))
        .map(disabledTest -> ConditionEvaluationResult.disabled(disabledTest.reason()))
        .findFirst();
  }
}