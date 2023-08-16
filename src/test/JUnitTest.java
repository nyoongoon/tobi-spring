package test;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class JUnitTest {
    static JUnitTest testObject;

    @Test
    public void test1(){
        assertThat(this, is(not(sameInstance(testObject))));
    }

    @Test
    public void test2(){
        assertThat(this, is(not(sameInstance(testObject))));
    }

    @Test
    public void test3(){
        assertThat(this, is(not(sameInstance(testObject))));
    }
}
