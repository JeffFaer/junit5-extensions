package name.falgout.jeffrey.testing.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runs a test, expecting a failure.
 *
 * <p>Should always be used with {@link ExpectFailureExceptionHandler}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface ExpectFailure {
  /**
   * A list of causes used to match against the nested causes of an exception.
   */
  Cause[] value();

  @interface Cause {
    Class<? extends Throwable> type() default Throwable.class;

    String message() default "";
  }
}