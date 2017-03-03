package name.falgout.jeffrey.testing.junit5;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

@Documented
@Retention(RUNTIME)
@Target({FIELD, PARAMETER})
@Qualifier
public @interface SomeQualifyingAnnotation {}
