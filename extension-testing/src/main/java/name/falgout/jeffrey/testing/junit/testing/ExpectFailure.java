package name.falgout.jeffrey.testing.junit.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Runs a test, expecting a failure.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@ExtendWith(ExpectFailureExceptionHandler.class)
public @interface ExpectFailure {
  /**
   * A list of causes used to match against the nested causes of an exception.
   */
  Cause[] value() default {};

  @interface Cause {
    Class<? extends Throwable> type() default Throwable.class;

    String message() default "";
  }
}
