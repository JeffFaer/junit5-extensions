# extension-testing

[![Maven Central][mvn-img]][mvn-link]

Utilities for testing JUnit 5 extensions.

For usage examples, take a look at the other project's tests in this
repository.

### `ExtensionTester`
Runs all of the tests in a given `Class` and generates a report that is
relatively easy to assert on.

### `@ExpectFailure`
Ensures that a test throws an exception. It's possible verify message
and arbitrarily nested causes on the thrown exception.

[mvn-img]: https://maven-badges.herokuapp.com/maven-central/name.falgout.jeffrey.testing.junit5/extension-testing/badge.svg
[mvn-link]: https://maven-badges.herokuapp.com/maven-central/name.falgout.jeffrey.testing.junit5/extension-testing
