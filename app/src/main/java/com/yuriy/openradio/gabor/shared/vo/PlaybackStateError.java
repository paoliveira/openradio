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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class PlaybackStateError {

    public enum Code {
        GENERAL,
        UNRECOGNIZED_URL
    }

    private final String mMsg;
    private final Code mCode;

    public PlaybackStateError() {
        this(null, Code.GENERAL);
    }

    public PlaybackStateError(final String message) {
        this(message, Code.GENERAL);
    }

    public PlaybackStateError(final String message, @NonNull final Code code) {
        super();
        mMsg = message;
        mCode = code;
    }

    @Nullable
    public String getMsg() {
        return mMsg;
    }

    @NonNull
    public Code getCode() {
        return mCode;
    }

    @Override
    public String toString() {
        return "PlaybackStateError{" +
                "code='" + mCode + '\'' +
                ", msg=" + mMsg +
                '}';
    }
}
