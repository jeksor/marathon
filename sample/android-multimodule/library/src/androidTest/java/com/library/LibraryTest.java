package com.library;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LibraryTest {
    @Test
    public void test1() throws Exception {
        Thread.sleep(1000);
    }

    @Test
    public void test2() throws Exception {
        Thread.sleep(1000);
    }

    @Test
    public void test3() throws Exception {
        Thread.sleep(1000);
    }

    @Test
    public void test4() throws Exception {
        Thread.sleep(1000);
    }

    @Test
    public void test5Failed() throws Exception {
        Thread.sleep(1000);
        throw new AssertionError();
    }
}
