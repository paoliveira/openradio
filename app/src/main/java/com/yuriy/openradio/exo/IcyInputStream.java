/*
** AACDecoder - Freeware Advanced Audio (AAC) Decoder for Android
** Copyright (C) 2012 Spolecne s.r.o., http://www.spoledge.com
**  
** This file is a part of AACDecoder.
**
** AACDecoder is free software; you can redistribute it and/or modify
** it under the terms of the GNU Lesser General Public License as published
** by the Free Software Foundation; either version 3 of the License,
** or (at your option) any later version.
** 
** This program is distributed in the hope that it will be useful,
** but WITHOUT ANY WARRANTY; without even the implied warranty of
** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
** GNU Lesser General Public License for more details.
** 
** You should have received a copy of the GNU Lesser General Public License
** along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.yuriy.openradio.exo;

import android.support.annotation.NonNull;

import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.FabricUtils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

/**
 * This is an InputStream which allows to fetch Icecast/Shoutcast metadata from.
 */
final class IcyInputStream extends FilterInputStream {

    private static final String CLASS_NAME = IcyInputStream.class.getSimpleName();

    ////////////////////////////////////////////////////////////////////////////
    // Attributes
    ////////////////////////////////////////////////////////////////////////////

    /**
     * The mPeriod of metadata frame in bytes.
     */
    private final int mPeriod;

    /**
     * The actual number of mRemaining bytes before the metadata.
     */
    private int mRemaining;

    /**
     * This is a temporary buffer used for fetching metadata bytes.
     */
    private byte[] mBuffer;

    /**
     * The callback.
     */
    private final IcyInputStreamListener mIcyInputStreamListener;

    /**
     * The character encoding of the metadata.
     */
    private final String mCharacterEncoding;

    ////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new input stream.
     *
     * @param inputStream            The underlying input stream.
     * @param period                 The mPeriod of metadata frame is repeating (in bytes).
     * @param icyInputStreamListener The callback.
     * @param characterEncoding      The encoding used for metadata strings - may be null,
     *                               default is UTF-8.
     */
    IcyInputStream(final InputStream inputStream,
                          final int period,
                          final IcyInputStreamListener icyInputStreamListener,
                          final String characterEncoding) {
        super(inputStream);
        mPeriod = period;
        mIcyInputStreamListener = icyInputStreamListener;
        mCharacterEncoding = characterEncoding != null ? characterEncoding : "UTF-8";
        mRemaining = period;
        mBuffer = new byte[128];
    }

    ////////////////////////////////////////////////////////////////////////////
    // InputStream
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public int read() throws IOException {
        final int ret = super.read();
        if (--mRemaining == 0) {
            fetchMetadata();
        }
        return ret;
    }

    @Override
    public int read(@NonNull final byte[] buffer,
                    final int offset, final int len) throws IOException {
        final int ret = in.read(buffer, offset, mRemaining < len ? mRemaining : len);
        if (mRemaining == ret) {
            fetchMetadata();
        } else {
            mRemaining -= ret;
        }
        return ret;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Private
    ////////////////////////////////////////////////////////////////////////////

    /**
     * This method reads the metadata string.
     * Actually it calls the method parseMetadata().
     */
    private void fetchMetadata() throws IOException {
        mRemaining = mPeriod;

        int size = in.read();

        // either no metadata or eof:
        if (size < 1) {
            return;
        }

        // size *= 16:
        size <<= 4;

        if (mBuffer.length < size) {
            mBuffer = null;
            mBuffer = new byte[size];
            AppLogger.d(CLASS_NAME + " Enlarged metadata buffer to " + size + " bytes");
        }

        size = readFully(mBuffer, 0, size);

        // find the string end:
        for (int i = 0; i < size; i++) {
            if (mBuffer[i] == 0) {
                size = i;
                break;
            }
        }

        String metadataString;

        try {
            metadataString = new String(mBuffer, 0, size, mCharacterEncoding);
        } catch (final Exception e) {
            FabricUtils.logException(e);
            return;
        }

        AppLogger.d(CLASS_NAME + " Metadata string: " + metadataString);

        parseMetadata(metadataString);
    }

    /**
     * Parses the metadata and sends them to IcyInputStreamListener.
     *
     * @param string the metadata string like: StreamTitle='...';StreamUrl='...';
     */
    private void parseMetadata(final String string) {
        final Map<String, String> metadata = new TreeMap<>();
        final String[] kvs = string.split(";");
        int n;
        boolean isString;
        String key;
        String value;
        for (final String kv : kvs) {
            n = kv.indexOf('=');
            if (n < 1) {
                continue;
            }
            isString = n + 1 < kv.length()
                    && kv.charAt(kv.length() - 1) == '\''
                    && kv.charAt(n + 1) == '\'';

            key = kv.substring(0, n);
            value = isString ?
                    kv.substring(n + 2, kv.length() - 1) :
                    n + 1 < kv.length() ?
                            kv.substring(n + 1) : "";

            metadata.put(key, value);
        }
        mIcyInputStreamListener.onMetadata(metadata);
    }

    /**
     * Tries to read all bytes into the target buffer.
     *
     * @param size the requested size
     * @return the number of really bytes read; if less than requested, then eof detected
     */
    private int readFully(final byte[] buffer, int offset, int size) throws IOException {
        int n;
        int oo = offset;
        while (size > 0 && (n = in.read(buffer, offset, size)) != -1) {
            offset += n;
            size -= n;
        }
        return offset - oo;
    }

}
