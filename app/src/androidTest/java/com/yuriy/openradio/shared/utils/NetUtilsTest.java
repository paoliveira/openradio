package com.yuriy.openradio.shared.utils;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class NetUtilsTest {

    public NetUtilsTest() {
        super();
    }

    @Test
    public void checkImage() {
        final String url = "https://djradio.gr/wp-content/uploads/2019/05/djradio-logo-smaller-white-1.png";
        final boolean result = NetUtils.checkResource(url);

        MatcherAssert.assertThat(result, CoreMatchers.is(true));
    }
}
