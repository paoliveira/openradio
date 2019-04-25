/*
 * Copyright 2019 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UrlBuilderTest {

    public UrlBuilderTest() {
        super();
    }

    @Test
    public void excludeApiKeyTestWithApiKey() {
        final String token = "3243242k3h42jg2j41";
        final String url = "http://www.errewrwe.com?" + UrlBuilder.TOKEN_KEY;

        assertThat(UrlBuilder.excludeApiToken(url + token), is(url));
    }

    @Test
    public void excludeApiKeyTestWithEmptyApiKey() {
        final String url = "http://www.errewrwe.com?" + UrlBuilder.TOKEN_KEY;

        assertThat(UrlBuilder.excludeApiToken(url), is(url));
    }

    @Test
    public void excludeApiKeyTestWithoutApiKey() {
        final String url = "http://www.errewrwe.com?";

        assertThat(UrlBuilder.excludeApiToken(url), is(url));
    }
}
