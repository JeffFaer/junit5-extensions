# disable-extension

[![Maven Central][mvn-img]][mvn-link]

Fine-grained control over disabling tests. Can be used to disable
arbitrarily nested, inherited tests.

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
public class DerivedTest extends BaseTest {
  @Test
  void otherTest() {}
}
````

[mvn-img]: https://maven-badges.herokuapp.com/maven-central/name.falgout.jeffrey.testing.junit5/disable-extension/badge.svg
[mvn-link]: https://maven-badges.herokuapp.com/maven-central/name.falgout.jeffrey.testing.junit5/disable-extension
