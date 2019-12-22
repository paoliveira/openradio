package com.yuriy.openradio.shared.utils;

import android.graphics.Bitmap;

import androidx.test.runner.AndroidJUnit4;

import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class BitmapHelperTest {

    public BitmapHelperTest() {
        super();
    }

    @Test
    public void fetch() throws Exception {
//        String url = "http://977music.com/images/logo.gif";
//        String url = "https://www1.wdr.de/radio/1live/1live-logo-schwarz-100~_v-gseagaleriexl.jpg";
        String url = "http://www.nextweb.gr/wp-content/uploads/2015/09/AthensParty_black_cm.png";

        final Bitmap bitmap = BitmapHelper.fetchAndRescaleBitmap(
                url,
                BitmapHelper.MEDIA_ART_BIG_WIDTH,
                BitmapHelper.MEDIA_ART_BIG_HEIGHT
        );

        assertThat(bitmap, IsNull.notNullValue());
    }
}
