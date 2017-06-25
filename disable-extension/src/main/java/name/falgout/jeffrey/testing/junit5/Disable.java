package name.falgout.jeffrey.testing.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Disable a specific test, including inherited, nested tests.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(TestDisabler.class)
@Repeatable(DisabledTests.class)
public @interface Disable {
  /** The specific test to disable. */
  String[] value();

  /** The reason why the test is disabled. */
  String reason() default "";
}