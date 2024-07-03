package com.github.altfatterz.joinexamples;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StreamToStreamJoinValueJoinerTest {

    // under test
    private StreamToStreamValueJoiner joiner = new StreamToStreamValueJoiner();

    @Test
    public void adImpressionValueAndClickValue() {
        String result = joiner.apply("readOnlyKey", "shown", "clicked");
        assertThat(result).isEqualTo("shown/clicked");
    }

    @Test
    public void adClickValueNull() {
        String result = joiner.apply("readOnlyKey", "shown", null);
        assertThat(result).isEqualTo("shown/not-clicked-yet");
    }

}
