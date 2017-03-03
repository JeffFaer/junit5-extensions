# junit5-guice-extension

A [JUnit5 extension](src/main/java/name/falgout/jeffrey/testing/junit5/GuiceExtension.java) which will inject fields marked with `@Inject` and will attempt to resolve any method parameters in your tests.

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

## How can I use this library in my project?
- Clone the repository and use Maven to create a .jar file:
````
git clone https://github.com/JeffreyFalgout/junit5-guice-extension
cd junit5-guice-extension/
mvn package
````

- <strike>Maven Central</strike> Coming soon!
