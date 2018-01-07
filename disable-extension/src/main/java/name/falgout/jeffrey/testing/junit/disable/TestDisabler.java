package name.falgout.jeffrey.testing.junit.disable;

import static org.junit.platform.commons.support.AnnotationSupport.findRepeatableAnnotations;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

final class TestDisabler implements ExecutionCondition {
  TestDisabler() {}

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
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
