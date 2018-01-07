package name.falgout.jeffrey.testing.junit.guice;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.Module;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
@Inherited
@Repeatable(IncludeModules.class)
@ExtendWith(GuiceExtension.class)
public @interface IncludeModule {
  Class<? extends Module>[] value();
}
