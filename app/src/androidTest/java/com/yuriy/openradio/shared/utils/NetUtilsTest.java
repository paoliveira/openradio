package com.yuriy.openradio.shared.utils;

import android.content.Context;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

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
    public void testCheckImage() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final String url = "https://djradio.gr/wp-content/uploads/2019/05/djradio-logo-smaller-white-1.png";
        final boolean result = NetUtils.checkResource(context, url);

        MatcherAssert.assertThat(result, CoreMatchers.is(true));
    }

    @Test
    public void testUri() {
        final String uriStr = "https://urbanhitrapfr.ice.infomaniak.ch/urbanhitrapfr-128.mp3";
        final Uri uri = Uri.parse(uriStr);

        MatcherAssert.assertThat(uri.toString(), CoreMatchers.is(uriStr));
    }
}
