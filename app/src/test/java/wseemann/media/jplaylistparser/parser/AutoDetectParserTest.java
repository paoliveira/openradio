package wseemann.media.jplaylistparser.parser;

import org.junit.Test;

import wseemann.media.jplaylistparser.parser.m3u.M3UPlaylistParser;
import wseemann.media.jplaylistparser.parser.m3u8.M3U8PlaylistParser;
import wseemann.media.jplaylistparser.parser.pls.PLSPlaylistParser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 25/11/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */

public final class AutoDetectParserTest {

    public AutoDetectParserTest() {
        super();
    }

    @Test
    public void getFileExtension() throws Exception {
        final AutoDetectParser parser = new AutoDetectParser(0);
        String url = "http://s06.hktoolbar.com/radio-HTTP/cr2-hd.3gp/chunklist.m3u8?nimblesessionid=41472102";

        String ext = parser.getFileExtension(url);
        assertThat(ext, is(M3U8PlaylistParser.EXTENSION));

        url = "http://s06.hktoolbar.com/radio-HTTP/cr2-hd.3gp/chunklist.m3u?nimblesessionid=41472102";

        ext = parser.getFileExtension(url);
        assertThat(ext, is(M3UPlaylistParser.EXTENSION));

        url = "http://s06.hktoolbar.com/radio-HTTP/cr2-hd.3gp/chunklist.pls?nimblesessionid=41472102";

        ext = parser.getFileExtension(url);
        assertThat(ext, is(PLSPlaylistParser.EXTENSION));
    }
}
