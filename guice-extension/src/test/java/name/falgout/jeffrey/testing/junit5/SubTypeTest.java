package name.falgout.jeffrey.testing.junit5;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

@IncludeModule(TestModule2.class)
public class SubTypeTest extends BaseTypeTest {
  @Test
  void subTypeCanIncludeModule(byte parameter) {
    assertThat(parameter).isEqualTo(TestModule2.BYTE);
  }
}
