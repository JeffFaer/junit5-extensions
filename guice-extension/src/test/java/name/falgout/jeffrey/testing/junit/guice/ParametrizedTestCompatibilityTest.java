package name.falgout.jeffrey.testing.junit.guice;

import com.google.inject.AbstractModule;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import name.falgout.jeffrey.testing.junit.testing.ExpectFailure;
import name.falgout.jeffrey.testing.junit.testing.ExpectFailure.Cause;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ParametrizedTestCompatibilityTest {
    static final class TestModule extends AbstractModule {
        static final String STRING = "testModuleString";

        @Override
        protected void configure() {
            bind(String.class).toInstance(STRING);
            bind(String.class).annotatedWith(Names.named("named")).toInstance(STRING);
            bind(String.class).annotatedWith(SomeBindingAnnotation.class).toInstance(STRING);
            bind(String.class).annotatedWith(SomeQualifyingAnnotation.class).toInstance(STRING);
        }
    }

    @Nested
    class PositiveCases {

        @ExtendWith(GuiceExtension.class)
        @ParameterizedTest
        @ValueSource(strings = "valueSourceString")
        void parametrizedTestWithStringsShouldWorkWithGuiceExtension(String value) {
            assertEquals("valueSourceString", value);
        }

        @ExtendWith(GuiceExtension.class)
        @ParameterizedTest
        @ValueSource(classes = Integer.class)
        void parametrizedTestWithoutStringsShouldWorkWithGuiceExtension(Class<Object> clazz) {
            assertSame(Integer.class, clazz);
        }
    }

    @Nested
    class NegativeCases {
        @ExpectFailure(
                @Cause(
                        type = ParameterResolutionException.class,
                        message = "Discovered multiple competing ParameterResolvers"
                )
        )
        @IncludeModule(TestModule.class)
        @ParameterizedTest
        @ValueSource(strings = "valueSourceString")
        void explicitBindingStringShouldConflictWithValueSource(String value) {
        }

        @ExpectFailure(
                @Cause(
                        type = ParameterResolutionException.class,
                        message = "Discovered multiple competing ParameterResolvers"
                )
        )
        @IncludeModule(TestModule.class)
        @ParameterizedTest
        @ValueSource(strings = "valueSourceString")
        void explicitBindingStringShouldConflictWithValueSource2(@SomeBindingAnnotation String value) {
        }

        @ExpectFailure(
                @Cause(
                        type = ParameterResolutionException.class,
                        message = "Discovered multiple competing ParameterResolvers"
                )
        )
        @IncludeModule(TestModule.class)
        @ParameterizedTest
        @ValueSource(strings = "valueSourceString")
        void explicitBindingStringShouldConflictWithValueSource3(@SomeQualifyingAnnotation String value) {
        }

        @ExpectFailure(
                @Cause(
                        type = ParameterResolutionException.class,
                        message = "Discovered multiple competing ParameterResolvers"
                )
        )
        @IncludeModule(TestModule.class)
        @ParameterizedTest
        @ValueSource(strings = "valueSourceString")
        void explicitBindingStringShouldConflictWithValueSource4(@Named("named") String value) {
        }
    }
}
