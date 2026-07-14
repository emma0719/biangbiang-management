package com.restaurant.ops;

import static org.assertj.core.api.Assertions.assertThat;

import com.restaurant.ops.common.Normalizer;
import org.junit.jupiter.api.Test;

class NormalizerTest {
  private final Normalizer normalizer = new Normalizer();

  @Test
  void normalizesEmailAndNorthAmericanPhone() {
    assertThat(normalizer.email("  PERSON@Example.COM ")).isEqualTo("person@example.com");
    assertThat(normalizer.phone("(206) 555-0134")).isEqualTo("+12065550134");
    assertThat(normalizer.phone("1-425-555-0199")).isEqualTo("+14255550199");
  }
}
