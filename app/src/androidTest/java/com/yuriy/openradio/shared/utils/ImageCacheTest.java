package com.yuriy.openradio.shared.utils;

import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 7/16/16
 * E-Mail: chernyshov.yuriy@gmail.com
 */
@RunWith(AndroidJUnit4.class)
public class ImageCacheTest {

    @Test
    public void testConstructExternalCacheDir() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        final File file = ImageCache.constructExternalCacheDir(context);

        assertThat(file, notNullValue());
        assertThat(file.getPath(), notNullValue());
    }
}
