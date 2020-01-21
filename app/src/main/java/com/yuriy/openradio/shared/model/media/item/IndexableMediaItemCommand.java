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

package com.yuriy.openradio.shared.model.media.item;

import androidx.annotation.NonNull;

import com.yuriy.openradio.shared.model.net.UrlBuilder;
import com.yuriy.openradio.shared.utils.AppLogger;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 14/01/18
 * E-Mail: chernyshov.yuriy@gmail.com
 */

public abstract class IndexableMediaItemCommand extends MediaItemCommandImpl {

    private static final String CLASS_NAME = IndexableMediaItemCommand.class.getSimpleName();

    /**
     * Index of the current page of the Radio Stations List.
     */
    private AtomicInteger mPageIndex;

    IndexableMediaItemCommand() {
        super();
        mPageIndex = new AtomicInteger(UrlBuilder.FIRST_PAGE_INDEX);
    }

    @Override
    public void execute(final IUpdatePlaybackState playbackStateListener,
                        @NonNull final MediaItemCommandDependencies dependencies) {
        super.execute(playbackStateListener, dependencies);
        AppLogger.d(CLASS_NAME + " invoked");
        if (!dependencies.isSameCatalogue()) {
            mPageIndex.set(UrlBuilder.FIRST_PAGE_INDEX);
        }
    }

    @Override
    public boolean doLoadNoDataReceived() {
        return mPageIndex.get() == UrlBuilder.FIRST_PAGE_INDEX + 1;
    }

    int getPageNumber() {
        return mPageIndex.getAndIncrement();
    }
}
