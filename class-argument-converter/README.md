# class-argument-converter

[![Maven Central][mvn-img]][mvn-link]

Convert parameterized test strings to classes in a type-safe way.

### Positive Examples
````java
    @ParameterizedTest
    @ValueSource(strings = {
        "java.lang.Object",
        "java.lang.String",
        "java.util.List",
    })
    void anything(@ConvertWith(ClassArgumentConverter.class) Class<?> anything) {
      assertThat(anything).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "java.util.List",
        "java.util.Collection",
        "java.lang.Object",
    })
    void anySuperclass(
        @ConvertWith(ClassArgumentConverter.class) Class<? super List> superclassOfList) {
      assertThat(superclassOfList).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "java.util.List",
        "java.util.ArrayList",
    })
    void anySubclass(@ConvertWith(ClassArgumentConverter.class) Class<? extends List> anything) {
      assertThat(anything).isNotNull();
    }
````

### Negative Examples
````java
    @ParameterizedTest
    @ValueSource(strings = "123ClassDoesNotExist")
    void classNotFound(@ConvertWith(ClassArgumentConverter.class) Class<?> clazz) {}
    // Will fail with ClassNotFoundException.

    @ParameterizedTest
    @ValueSource(strings = "java.lang.Object")
    void badParameterType(@ConvertWith(ClassArgumentConverter.class) String clazz) {}
    // Will fail since the parameter type is not Class.

    @ParameterizedTest
    @ValueSource(strings = "java.util.List")
    void wrongClass(@ConvertWith(ClassArgumentConverter.class) Class<Collection> clazz) {}
    // Will fail since Class<List> != Class<Collection>.

    @ParameterizedTest
    @ValueSource(strings = "java.util.List")
    void badLowerBound(@ConvertWith(ClassArgumentConverter.class) Class<? super Collection> clazz) {}
    // Will fail since Class<List> is not assignable to Class<? super Collection>.

    @ParameterizedTest
    @ValueSource(strings = "java.lang.Object")
    void badUpperBound(@ConvertWith(ClassArgumentConverter.class) Class<? extends Collection> clazz) {}
    // Will fail since Class<Object> is not assignable to Class<? extends Collection>.
````

[mvn-img]: https://maven-badges.herokuapp.com/maven-central/name.falgout.jeffrey.testing.junit5/class-argument-converter/badge.svg
[mvn-link]: https://maven-badges.herokuapp.com/maven-central/name.falgout.jeffrey.testing.junit5/class-argument-converter
