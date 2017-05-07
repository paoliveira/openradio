/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.google.android.exoplayer2.extractor;

import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.extractor.ogg.OggExtractor;
import com.google.android.exoplayer2.extractor.wav.WavExtractor;

import java.lang.reflect.Constructor;

/**
 * An {@link ExtractorsFactory} that provides an array of extractors for the following formats:
 *
 * <ul>
 * <li>Ogg Vorbis/FLAC ({@link OggExtractor}</li>
 * <li>MP3 ({@link Mp3Extractor})</li>
 * <li>WAV ({@link WavExtractor})</li>
 * <li>FLAC (only available if the FLAC extension is built and included)</li>
 * </ul>
 */
public final class DefaultExtractorsFactory implements ExtractorsFactory {

  private static final Constructor<? extends Extractor> FLAC_EXTRACTOR_CONSTRUCTOR;
  static {
    Constructor<? extends Extractor> flacExtractorConstructor = null;
    try {
      flacExtractorConstructor =
          Class.forName("com.google.android.exoplayer2.ext.flac.FlacExtractor")
              .asSubclass(Extractor.class).getConstructor();
    } catch (ClassNotFoundException e) {
      // Extractor not found.
    } catch (NoSuchMethodException e) {
      // Constructor not found.
    }
    FLAC_EXTRACTOR_CONSTRUCTOR = flacExtractorConstructor;
  }

  private @Mp3Extractor.Flags int mp3Flags;

  /**
   * Sets flags for {@link Mp3Extractor} instances created by the factory.
   *
   * @see Mp3Extractor#Mp3Extractor(int)
   * @param flags The flags to use.
   * @return The factory, for convenience.
   */
  public synchronized DefaultExtractorsFactory setMp3ExtractorFlags(@Mp3Extractor.Flags int flags) {
    mp3Flags = flags;
    return this;
  }

  @Override
  public synchronized Extractor[] createExtractors() {
    Extractor[] extractors = new Extractor[FLAC_EXTRACTOR_CONSTRUCTOR == null ? 11 : 12];
    extractors[3] = new Mp3Extractor(mp3Flags);
    extractors[8] = new OggExtractor();
    extractors[10] = new WavExtractor();
    if (FLAC_EXTRACTOR_CONSTRUCTOR != null) {
      try {
        extractors[11] = FLAC_EXTRACTOR_CONSTRUCTOR.newInstance();
      } catch (Exception e) {
        // Should never happen.
        throw new IllegalStateException("Unexpected error creating FLAC extractor", e);
      }
    }
    return extractors;
  }

}
