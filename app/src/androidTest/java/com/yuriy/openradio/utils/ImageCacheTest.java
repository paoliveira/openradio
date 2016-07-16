package com.yuriy.openradio.utils;

import android.test.InstrumentationTestCase;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 7/16/16
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public class ImageCacheTest extends InstrumentationTestCase {

    public void testConstructExternalCacheDir() throws Exception {

        final File file = ImageCache.constructExternalCacheDir(
                getInstrumentation().getTargetContext()
        );

        assertThat(file, notNullValue());
        assertThat(file.getPath(), notNullValue());
    }
}
