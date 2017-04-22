package com.yuriy.openradio.exo;

import java.util.Map;

/**
 * Callback from player to GUI.
 */
public interface IcyInputStreamListener {

    /**
     * This method is called when the stream receives a metadata information.
     * It can be either before starting the stream (from HTTP header - all header pairs)
     * or during playback (metadata frame info).
     * <p>
     *
     * @param metadata Map of the key values representing metadata.
     *                 The metadata key is from HTTP header: "icy-genre", "icy-url", "content-type",
     *                 or from the dynamic metadata frame: e.g. "StreamTitle" or "StreamUrl".
     *                 The value is metadata value related to the key.
     */
    void onMetadata(final Map<String, String> metadata);
}
