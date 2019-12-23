package com.jeppeman.locallydynamic;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;


public class ObjectsTest {
    @Test
    public void whenObj1AndObj2AreNull_equals_shouldReturnTrue() {
        boolean equals = Objects.equals(null, null);

        assertThat(equals).isTrue();
    }

    @Test
    public void whenObj1AndObj2AreNotEqual_equals_shouldReturnFalse() {
        boolean equals = Objects.equals(null, "hey");

        assertThat(equals).isFalse();
    }

    @Test
    public void whenObj1AndObj2AreEqual_equals_shouldReturnTrue() {
        boolean equals = Objects.equals("hey", "hey");

        assertThat(equals).isTrue();
    }
}