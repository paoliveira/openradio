/*
 * Copyright 2017 The "Open Radio" Project. Author: Chernyshov Yuriy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yuriy.openradio.net;

import android.app.Application;
import android.net.Uri;
import android.test.ApplicationTestCase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 29/12/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class UrlBuilderTest extends ApplicationTestCase<Application> {

    public UrlBuilderTest() {
        super(Application.class);
    }

    public void testPreProcessIconUrl() throws Exception {
        final String oldUrl = "https://" + UrlBuilder.OLD_IMG_BASE_URL + "/station/49314/c175.png";
        final String newUrl = UrlBuilder.preProcessIconUrl(oldUrl);

        assertThat(newUrl.contains(UrlBuilder.NEW_IMG_BASE_URL), is(true));
        assertThat(newUrl.contains(UrlBuilder.OLD_IMG_BASE_URL), is(false));
    }

    public void testPreProcessIconUrlNullInput() throws Exception {
        final String newUrl = UrlBuilder.preProcessIconUrl(null);

        assertThat(newUrl, nullValue());
    }

    public void testPreProcessIconUrlEmptyInput() throws Exception {
        final String newUrl = UrlBuilder.preProcessIconUrl("");

        assertThat(newUrl, is(""));
    }

    public void testPreProcessIconUri() throws Exception {
        final String oldUrl = "https://" + UrlBuilder.OLD_IMG_BASE_URL + "/station/49314/c175.png";
        final Uri oldUri = Uri.parse(oldUrl);
        final Uri newUri = UrlBuilder.preProcessIconUri(oldUri);

        assertThat(newUri.toString().contains(UrlBuilder.NEW_IMG_BASE_URL), is(true));
        assertThat(newUri.toString().contains(UrlBuilder.OLD_IMG_BASE_URL), is(false));
    }

    public void testPreProcessIconUriNullInput() throws Exception {
        final Uri newUri = UrlBuilder.preProcessIconUri(null);

        assertThat(newUri, nullValue());
    }
}
