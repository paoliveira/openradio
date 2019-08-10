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

package com.yuriy.openradio.model.media;

import androidx.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.List;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 29/06/17
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * Interface designed to composite actions performed on {@link android.support.v4.media.MediaBrowserCompat}.
 *
 */
public interface MediaResourceManagerListener {

    void onConnected(final List<MediaSessionCompat.QueueItem> queue);

    void onPlaybackStateChanged(@NonNull final PlaybackStateCompat state);

    void onQueueChanged(final List<MediaSessionCompat.QueueItem> queue);

    void onMetadataChanged(final MediaMetadataCompat metadata, final List<MediaSessionCompat.QueueItem> queue);
}
