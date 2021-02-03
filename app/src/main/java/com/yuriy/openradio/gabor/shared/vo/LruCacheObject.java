/*
 * Copyright 2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.gabor.shared.vo;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class LruCacheObject {

    public enum State {
        NONE, STARTED
    }

    private State mState;
    private Bitmap mBitmap;

    public LruCacheObject() {
        this(State.STARTED, null);
    }

    public LruCacheObject(@NonNull final State state, @Nullable final Bitmap bitmap) {
        super();
        mState = state;
        mBitmap = bitmap;
    }

    @NonNull
    public State getState() {
        return mState;
    }

    public void setState(@NonNull final State value) {
        mState = value;
    }

    @Nullable
    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(@Nullable final Bitmap value) {
        mBitmap = value;
    }
}
