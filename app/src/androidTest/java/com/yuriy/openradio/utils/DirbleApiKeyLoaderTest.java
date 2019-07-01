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

package com.yuriy.openradio.utils;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class DirbleApiKeyLoaderTest {

    public DirbleApiKeyLoaderTest() {
        super();
    }

    @Before
    public void setUp() throws Exception {
        DirbleApiKeyLoader.init(InstrumentationRegistry.getTargetContext());
    }

    @Test
    public void getApiKeyTest() {
        int num = 100;
        int c = 0;
        for (int i = 0; i <= num; i++) {
            DirbleApiKeyLoader.getApiKey();
            c = i;
        }

        assertThat(c, is(num));
    }
}
