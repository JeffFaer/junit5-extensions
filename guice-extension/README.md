# guice-extension

[![Maven Central][mvn-img]][mvn-link]

Injects fields marked with `@Inject` and attempts to resolve any method
parameters in your tests.

````java
@ExtendWith(GuiceExtension.class)
@IncludeModule(YourModule.class)
@IncludeModule(YourOtherModule.class)
public class YourTest {
  @Inject YourInjectableObject aField;
  @Inject @SomeBindingAnnotation String specialString;
  @Inject static GlobalState staticInjection;
  
  @BeforeEach
  void setUp(ThisToo willBeInjected) {}
  
  @Test
  void yourFirstTest(@AnotherBindingAnnotation AnotherInjectableObject thisArgumentWillBeInjected) {}
  
  @Test
  @IncludeModule(AThirdModule.class)
  void anotherTest(ObjectInThirdModule whoa) {}
  
  @Nested
  @IncludeModule(AFourthModule.class)
  class NestedClass {
    /* This class will have access to
     *   - YourModule
     *   - YourOtherModule
     *   - AFourthModule
     *
     * AThirdModule is not included since it is declared on the method above, not this class.
     */
  }
}
````

[mvn-img]: https://maven-badges.herokuapp.com/maven-central/name.falgout.jeffrey.testing.junit5/guice-extension/badge.svg
[mvn-link]: https://maven-badges.herokuapp.com/maven-central/name.falgout.jeffrey.testing.junit5/guice-extension
