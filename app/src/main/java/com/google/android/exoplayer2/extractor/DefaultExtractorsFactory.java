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
import com.google.android.exoplayer2.extractor.mp4.FragmentedMp4Extractor;
import com.google.android.exoplayer2.extractor.mp4.Mp4Extractor;
import com.google.android.exoplayer2.extractor.ogg.OggExtractor;
import com.google.android.exoplayer2.extractor.ts.Ac3Extractor;
import com.google.android.exoplayer2.extractor.ts.AdtsExtractor;
import com.google.android.exoplayer2.extractor.ts.PsExtractor;
import com.google.android.exoplayer2.extractor.ts.TsExtractor;
import com.google.android.exoplayer2.extractor.wav.WavExtractor;

import java.lang.reflect.Constructor;

/**
 * An {@link ExtractorsFactory} that provides an array of extractors for the following formats:
 *
 * <ul>
 * <li>MP4, including M4A ({@link Mp4Extractor})</li>
 * <li>fMP4 ({@link FragmentedMp4Extractor})</li>
 * <li>Ogg Vorbis/FLAC ({@link OggExtractor}</li>
 * <li>MP3 ({@link Mp3Extractor})</li>
 * <li>AAC ({@link AdtsExtractor})</li>
 * <li>MPEG TS ({@link TsExtractor})</li>
 * <li>MPEG PS ({@link PsExtractor})</li>
 * <li>WAV ({@link WavExtractor})</li>
 * <li>AC3 ({@link Ac3Extractor})</li>
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

  private @TsExtractor.Mode int tsMode;

  public DefaultExtractorsFactory() {
    tsMode = TsExtractor.MODE_SINGLE_PMT;
  }

  @Override
  public synchronized Extractor[] createExtractors() {
    Extractor[] extractors = new Extractor[FLAC_EXTRACTOR_CONSTRUCTOR == null ? 9 : 10];
    extractors[0] = new FragmentedMp4Extractor(0);
    extractors[1] = new Mp4Extractor();
    extractors[2] = new Mp3Extractor(0);
    extractors[3] = new AdtsExtractor();
    extractors[4] = new Ac3Extractor();
    extractors[5] = new TsExtractor(tsMode, 0);
    extractors[6] = new OggExtractor();
    extractors[7] = new PsExtractor();
    extractors[8] = new WavExtractor();
    if (FLAC_EXTRACTOR_CONSTRUCTOR != null) {
      try {
        extractors[9] = FLAC_EXTRACTOR_CONSTRUCTOR.newInstance();
      } catch (Exception e) {
        // Should never happen.
        throw new IllegalStateException("Unexpected error creating FLAC extractor", e);
      }
    }
    return extractors;
  }

}
