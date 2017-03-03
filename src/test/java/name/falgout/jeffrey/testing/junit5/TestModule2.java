package name.falgout.jeffrey.testing.junit5;

import com.google.inject.AbstractModule;

final class TestModule2 extends AbstractModule {
  static final byte BYTE = 1;

  @Override
  protected void configure() {
    bind(byte.class).toInstance(BYTE);
  }
}
