# mockito-extension

[![Maven Central][mvn-img]][mvn-link]

A temporary Mockito extension. Will be removed once
[Mockito creates their own](https://github.com/mockito/mockito/issues/445).

 - Supports `@Mock`, `@Spy`, and `@Captor` annotated fields.
 - Supports `@Mock` and `ArgumentCaptor` typed parameters.

````java
@ExtendWith(MockitoExtension.class)
public class SomeTest {
  @Mock Function<String, String> stringTransform; // Will be instantiated.

  @Test
  void testTransform(ArgumentCaptor<String> stringCaptor /* Will be instantiated. */) {
  }
}
````

[mvn-img]: https://maven-badges.herokuapp.com/maven-central/name.falgout.jeffrey.testing.junit5/mockito-extension/badge.svg
[mvn-link]: https://maven-badges.herokuapp.com/maven-central/name.falgout.jeffrey.testing.junit5/mockito-extension
