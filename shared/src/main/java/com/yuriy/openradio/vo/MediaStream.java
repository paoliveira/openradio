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

package com.yuriy.openradio.vo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 4/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 * <p>
 * {@link MediaStream} is a value object that holds data associated with Radio Station's stream.
 */
public final class MediaStream {

    private final List<Variant> mVariants;

    public void setVariant(final int bitrate, @NonNull final String url) {
        mVariants.add(new Variant(bitrate, url));
    }

    public int getVariantsNumber() {
        return mVariants.size();
    }

    boolean isEmpty() {
        return mVariants.isEmpty();
    }

    public void clear() {
        mVariants.clear();
    }

    @Nullable
    public Variant getVariant(final int position) {
        if (position < 0) {
            return null;
        }
        if (position >= getVariantsNumber()) {
            return null;
        }
        return mVariants.get(position).copy();
    }

    @Override
    public String toString() {
        return "MediaStream{variants=" + mVariants + "}";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MediaStream that = (MediaStream) o;
        return mVariants.equals(that.mVariants);
    }

    @Override
    public int hashCode() {
        return mVariants.hashCode();
    }

    /**
     * Private constructor.
     * Disallow instantiation of this helper class.
     */
    private MediaStream() {
        super();
        mVariants = new ArrayList<>();
    }

    /**
     * Copy constructor.
     *
     * @param mediaStream Object to be copied.
     */
    private MediaStream(final MediaStream mediaStream) {
        this();
        for (final Variant variant : mediaStream.mVariants) {
            mVariants.add(new Variant(variant.mBitrate, variant.mUrl));
        }
    }

    public static final class Variant {

        @NonNull
        private final String mUrl;

        // TODO: Convert to enum
        private final int mBitrate;

        private Variant(final int bitrate, @NonNull final String url) {
            super();
            mBitrate = bitrate;
            mUrl = url;
        }

        public String getUrl() {
            return mUrl;
        }

        public int getBitrate() {
            return mBitrate;
        }

        @Override
        public String toString() {
            return "Variant{" +
                    "url='" + mUrl + '\'' +
                    ", bitrate=" + mBitrate +
                    '}';
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final Variant variant = (Variant) o;

            if (mBitrate != variant.mBitrate) return false;
            return mUrl.equals(variant.mUrl);
        }

        @Override
        public int hashCode() {
            int result = mUrl.hashCode();
            result = 31 * result + mBitrate;
            return result;
        }

        private Variant copy() {
            return new Variant(mBitrate, mUrl);
        }
    }

    /**
     * Factory method to create instance of the {@link MediaStream}.
     *
     * @return Instance of the {@link MediaStream}.
     */
    public static MediaStream makeDefaultInstance() {
        return new MediaStream();
    }

    /**
     * Factory method to create copy-instance of the {@link MediaStream}.
     *
     * @param mediaStream Object to be copied.
     * @return Copied instance of {@link MediaStream}.
     */
    public static MediaStream makeCopyInstance(final MediaStream mediaStream) {
        return new MediaStream(mediaStream);
    }
}
