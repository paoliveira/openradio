package com.yuriy.openradio.utils;

import junit.framework.TestCase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/13/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public class MediaIDHelperTest extends TestCase {

    public void testGetId() {
        final String id = "__PARENT_CATEGORIES__";
        final String startsWith = "__PARENT_CATEGORIES__11";

        assertThat(MediaIDHelper.getId(startsWith), is(id));
    }
}
