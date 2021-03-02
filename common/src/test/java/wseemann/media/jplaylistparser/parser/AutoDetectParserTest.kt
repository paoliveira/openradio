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

import org.hamcrest.MatcherAssert
import org.hamcrest.core.Is
import org.junit.Test
import wseemann.media.jplaylistparser.parser.m3u.M3UPlaylistParser
import wseemann.media.jplaylistparser.parser.m3u8.M3U8PlaylistParser
import wseemann.media.jplaylistparser.parser.pls.PLSPlaylistParser

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 25/11/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class AutoDetectParserTest {

    @Test
    fun testFileExtension() {
        val parser = AutoDetectParser(0)
        var url = "http://s06.hktoolbar.com/radio-HTTP/cr2-hd.3gp/chunklist.m3u8?nimblesessionid=41472102"
        var ext = parser.getFileExtension(url)
        MatcherAssert.assertThat(ext, Is.`is`(M3U8PlaylistParser.EXTENSION))

        url = "http://s06.hktoolbar.com/radio-HTTP/cr2-hd.3gp/chunklist.m3u?nimblesessionid=41472102"
        ext = parser.getFileExtension(url)
        MatcherAssert.assertThat(ext, Is.`is`(M3UPlaylistParser.EXTENSION))

        url = "http://s06.hktoolbar.com/radio-HTTP/cr2-hd.3gp/chunklist.pls?nimblesessionid=41472102"
        ext = parser.getFileExtension(url)
        MatcherAssert.assertThat(ext, Is.`is`(PLSPlaylistParser.EXTENSION))
    }

    @Test
    fun testExtractFileNameFromHeader() {
        val param1 = "attachment; filename=playlist_9068.pls"
        MatcherAssert.assertThat(
                AutoDetectParser.getFileExtFromHeaderParam(param1),
                Is.`is`("playlist_9068.pls")
        )

        val param2 = "filename=playlist_9068.pls"
        MatcherAssert.assertThat(
                AutoDetectParser.getFileExtFromHeaderParam(param2),
                Is.`is`("playlist_9068.pls")
        )
    }
}
