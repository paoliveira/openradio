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
