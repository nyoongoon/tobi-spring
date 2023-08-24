package org.learningtest.template;

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CalSumTest {
    @Test
    public void sumOfNumbers() throws IOException{
        Calculator calculator = new Calculator();
        int sum = calculator.calcSum(getClass().getResource(
                "numbers.text").getPath());
        assertThat(sum, is(10));
    }
}
