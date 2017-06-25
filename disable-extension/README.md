# disable-extension

Finer grained control for disabling tests. Can be used to disable arbitrarily
nested, inherited tests.

````java
public abstract class BaseTest {
  @Test
  void badTest() {
    fail("I'm a bad test!");
  }

  @Nested
  class NestedTests {
    @Test
    void badNestedTest() {
      fail("Another bad test!");
    }
  }
}

@Disable("badTest()")
@Disable({"NestedTests", "badNestedTest()"})
public class DerivedTest {
  @Test
  void otherTest() {}
}
````