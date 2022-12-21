/*
 * Copyright 2017-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package wseemann.media.jplaylistparser.parser

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.NetUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.hamcrest.MatcherAssert
import org.hamcrest.core.Is
import org.junit.Test
import org.junit.runner.RunWith
import wseemann.media.jplaylistparser.parser.pls.PLSPlaylistParser
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 25/11/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
@RunWith(AndroidJUnit4::class)
class AutoDetectParserAndroidTest {

    private val parser = AutoDetectParser(AppUtils.TIME_OUT)

    @Test
    fun testUnrecognizedPlsStream() {
        val url = "https://streaming.galaxywebsolutions.com/tunein/9068/stream/pls"
        var ext = AppUtils.EMPTY_STRING
        val latch = CountDownLatch(1)
        GlobalScope.launch(Dispatchers.IO) {
            kotlin.run {
                ext = parser.getStreamExtension(url, false)
                latch.countDown()
            }
        }
        latch.await((AppUtils.TIME_OUT + 1000).toLong(), TimeUnit.SECONDS)
        // Requires internet connection!
        MatcherAssert.assertThat(ext, Is.`is`(PLSPlaylistParser.EXTENSION))
    }

    @Test
    fun testUnrecognizedPlsStream2() {
        val url = "https://www.jesusbitte.com/doc/tv2/joda.m3u"
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val latch = CountDownLatch(1)
        GlobalScope.launch(Dispatchers.IO) {
            kotlin.run {
                NetUtils.extractUrlsFromPlaylist(context, url)
                latch.countDown()
            }
        }
        latch.await((AppUtils.TIME_OUT + 1000).toLong(), TimeUnit.SECONDS)
        // Requires internet connection!
    }
}
