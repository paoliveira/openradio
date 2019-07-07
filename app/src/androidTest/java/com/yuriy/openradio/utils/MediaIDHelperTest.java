package com.yuriy.openradio.utils;

import junit.framework.TestCase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/13/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public class MediaIDHelperTest extends TestCase {

    public void testGetId() {
        final String id = MediaIdHelper.MEDIA_ID_CHILD_CATEGORIES;
        final String startsWith = MediaIdHelper.MEDIA_ID_CHILD_CATEGORIES + "11";

        assertThat(MediaIdHelper.getId(startsWith), is(id));
    }

    public void testGetValidCountryCode() {
        final String id = MediaIdHelper.MEDIA_ID_COUNTRIES_LIST + "BR";

        assertThat(MediaIdHelper.getCountryCode(id), is("BR"));
    }

    public void testGetInvalidCountryCodeFromValidValueSameSubName() {
        final String id = MediaIdHelper.MEDIA_ID_COUNTRIES_LIST;

        assertThat(MediaIdHelper.getCountryCode(id), nullValue());
    }

    public void testGetInvalidCountryCodeFromValidValueDifferentSubName() {
        final String id = MediaIdHelper.MEDIA_ID_SEARCH_FROM_APP;

        assertThat(MediaIdHelper.getCountryCode(id), nullValue());
    }

    public void testGetInvalidCountryCodeFromNullValue() {
        final String id = null;

        assertThat(MediaIdHelper.getCountryCode(id), nullValue());
    }

    public void testStartsWithAndEquals() {
        final String name = MediaIdHelper.MEDIA_ID_COUNTRIES_LIST;
        final String id = MediaIdHelper.MEDIA_ID_COUNTRIES_LIST;

        assertThat(name.startsWith(id), is(true));
        assertThat(name.equals(id), is(true));
    }
}
