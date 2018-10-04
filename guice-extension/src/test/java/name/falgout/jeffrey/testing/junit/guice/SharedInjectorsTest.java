package name.falgout.jeffrey.testing.junit.guice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Named;
import name.falgout.jeffrey.testing.junit.guice.SharedInjectorsTest.OuterClassModule;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GuiceExtension.class)
@IncludeModule(OuterClassModule.class)
@SharedInjectors
class SharedInjectorsTest {

  @Test
  @IncludeModule(TestCaseModule.class)
  @SharedInjectors
  void test1(@Named("base") String base, @Named("test") String test) {
    assertEquals("base", base);
    assertEquals("test", test);
  }

  @Test
  @SharedInjectors
  @IncludeModule(OuterSharedClassModule.class)
  void testCase2(int value) {
    assertEquals(1, value);
  }


  @IncludeModule(InnerClassModule.class)
  @Nested
  class InnerClass {

    @SharedInjectors
    @IncludeModule(TestCaseModule.class)
    @Test
    void test1(@Named("base") String base,
        @Named("inner") String inner,
        @Named("test") String test) {
      assertEquals("base", base);
      assertEquals("test", test);
      assertEquals("inner", inner);
    }
  }

  @Nested
  @IncludeModule(CachedModule.class)
  @SharedInjectors
  class FirstCachedInjectorTest {

    @Test
    void firstTest(long i) {
      assertEquals(1, i);
    }

    @Test
    void secondTest(long i) {
      assertEquals(1, i);
    }
  }

  @Nested
  @IncludeModule(CachedModule.class)
  @SharedInjectors
  class SecondCachedInjectorTest {

    @Test
    void firstTest(long i) {
      assertEquals(1, i);
    }

    @Test
    void secondTest(long i) {
      assertEquals(1, i);
    }
  }

  @Nested
  @IncludeModule(NonCachedModule.class)
  class FirstNonCachedInjectorTest {

    @Test
    void test(long i) {
      long expectedValue = NonCachedModule.SECOND_EXECUTED.get() ? 2 : 1;
      assertEquals(expectedValue, i);
      NonCachedModule.FIRST_EXECUTED.set(true);
    }
  }

  @Nested
  @IncludeModule(NonCachedModule.class)
  class SecondNonCachedInjectorTest {

    @Test
    void test(long i) {
      long expectedValue = NonCachedModule.FIRST_EXECUTED.get() ? 2 : 1;
      assertEquals(expectedValue, i);
      NonCachedModule.SECOND_EXECUTED.set(true);
    }
  }

  @IncludeModule(OuterSharedClassModule.class)
  @SharedInjectors
  @Nested
  class OuterSharedClass {

    @IncludeModule(ExtraModule.class)
    @Test
    void testCase1(int value, @Named("extra") int extra) {
      assertEquals(1, value);
      assertEquals(ExtraModule.EXTRA2_EXECUTED.get() ? 2 : 1, extra);
      ExtraModule.EXTRA1_EXECUTED.set(true);
    }

    @Test
    @IncludeModule(ExtraModule.class)
    void testCase2(int value, @Named("extra") int extra) {
      assertEquals(1, value);
      assertEquals(ExtraModule.EXTRA1_EXECUTED.get() ? 2 : 1, extra);
      ExtraModule.EXTRA2_EXECUTED.set(true);
    }
  }

  static class OuterSharedClassModule extends AbstractModule {

    private final static AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    @Override
    protected void configure() {
      int value = ATOMIC_INTEGER.incrementAndGet();
      bind(int.class).toInstance(value);
      assertEquals(1, value);
    }
  }

  static class ExtraModule extends AbstractModule {

    static final AtomicBoolean EXTRA1_EXECUTED = new AtomicBoolean(false);
    static final AtomicBoolean EXTRA2_EXECUTED = new AtomicBoolean(false);
    static final AtomicInteger INTEGER = new AtomicInteger(0);

    @Override
    protected void configure() {
      bind(int.class).annotatedWith(Names.named("extra")).toInstance(INTEGER.incrementAndGet());
    }
  }

  static class OuterClassModule extends AbstractModule {

    @Override
    protected void configure() {
      bind(String.class).annotatedWith(Names.named("base")).toInstance("base");
    }
  }

  static class InnerClassModule extends AbstractModule {

    @Override
    protected void configure() {
      bind(String.class).annotatedWith(Names.named("inner")).toInstance("inner");
    }
  }

  static class TestCaseModule extends AbstractModule {

    @Override
    protected void configure() {
      bind(String.class).annotatedWith(Names.named("test")).toInstance("test");
    }
  }

  static final class CachedModule extends AbstractModule {

    static final AtomicLong ATOMIC_LONG = new AtomicLong(0);

    @Override
    protected void configure() {
      bind(long.class).toInstance(ATOMIC_LONG.incrementAndGet());
    }
  }

  static final class NonCachedModule extends AbstractModule {

    static final AtomicLong ATOMIC_LONG = new AtomicLong(0);
    //depend on which test executed first
    static final AtomicBoolean FIRST_EXECUTED = new AtomicBoolean(false);
    static final AtomicBoolean SECOND_EXECUTED = new AtomicBoolean(false);

    @Override
    protected void configure() {
      bind(long.class).toInstance(ATOMIC_LONG.incrementAndGet());
    }
  }
}
