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
package com.google.android.exoplayer2.trackselection;

import android.text.TextUtils;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link MappingTrackSelector} that allows configuration of common parameters. It is safe to call
 * the methods of this class from the application thread. See {@link Parameters#Parameters()} for
 * default selection parameters.
 */
public class DefaultTrackSelector extends MappingTrackSelector {

  /**
   * Holder for available configurations for the {@link DefaultTrackSelector}.
   */
  public static final class Parameters {

    // Audio.
    public final String preferredAudioLanguage;

    // Text.
    public final String preferredTextLanguage;

    /**
     * Constructor with default selection parameters:
     * <ul>
     *   <li>No preferred audio language is set.</li>
     *   <li>No preferred text language is set.</li>
     * </ul>
     */
    public Parameters() {
      this(null, null);
    }

    /**
     * @param preferredAudioLanguage The preferred language for audio, as well as for forced text
     *     tracks as defined by RFC 5646. {@code null} to select the default track, or first track
     *     if there's no default.
     * @param preferredTextLanguage The preferred language for text tracks as defined by RFC 5646.
     *     {@code null} to select the default track, or first track if there's no default.
     */
    public Parameters(String preferredAudioLanguage, String preferredTextLanguage) {
      this.preferredAudioLanguage = preferredAudioLanguage;
      this.preferredTextLanguage = preferredTextLanguage;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      Parameters other = (Parameters) obj;
      return TextUtils.equals(preferredAudioLanguage, other.preferredAudioLanguage)
          && TextUtils.equals(preferredTextLanguage, other.preferredTextLanguage);
    }

    @Override
    public int hashCode() {
      int result = preferredAudioLanguage.hashCode();
      result = 31 * result + preferredTextLanguage.hashCode();
      return result;
    }

  }

  /**
   * If a dimension (i.e. width or height) of a video is greater or equal to this fraction of the
   * corresponding viewport dimension, then the video is considered as filling the viewport (in that
   * dimension).
   */
  private static final int[] NO_TRACKS = new int[0];
  private static final int WITHIN_RENDERER_CAPABILITIES_BONUS = 1000;

  private final TrackSelection.Factory adaptiveTrackSelectionFactory;
  private final AtomicReference<Parameters> paramsReference;

  /**
   * Constructs an instance that does not support adaptive track selection.
   */
  public DefaultTrackSelector() {
    this((TrackSelection.Factory) null);
  }

  /**
   * Constructs an instance that uses a factory to create adaptive track selections.
   *
   * @param adaptiveTrackSelectionFactory A factory for adaptive {@link TrackSelection}s, or null if
   *     the selector should not support adaptive tracks.
   */
  public DefaultTrackSelector(TrackSelection.Factory adaptiveTrackSelectionFactory) {
    this.adaptiveTrackSelectionFactory = adaptiveTrackSelectionFactory;
    paramsReference = new AtomicReference<>(new Parameters());
  }

  /**
   * Atomically sets the provided parameters for track selection.
   *
   * @param params The parameters for track selection.
   */
  public void setParameters(Parameters params) {
    Assertions.checkNotNull(params);
    if (!paramsReference.getAndSet(params).equals(params)) {
      invalidate();
    }
  }

  /**
   * Gets the current selection parameters.
   *
   * @return The current selection parameters.
   */
  public Parameters getParameters() {
    return paramsReference.get();
  }

  // MappingTrackSelector implementation.

  @Override
  protected TrackSelection[] selectTracks(RendererCapabilities[] rendererCapabilities,
      TrackGroupArray[] rendererTrackGroupArrays, int[][][] rendererFormatSupports)
      throws ExoPlaybackException {
    // Make a track selection for each renderer.
    int rendererCount = rendererCapabilities.length;
    TrackSelection[] rendererTrackSelections = new TrackSelection[rendererCount];
    Parameters params = paramsReference.get();

    boolean seenVideoRendererWithMappedTracks = false;

    boolean selectedAudioTracks = false;
    for (int i = 0; i < rendererCount; i++) {
      switch (rendererCapabilities[i].getTrackType()) {
        case C.TRACK_TYPE_VIDEO:
          // Already done. Do nothing.
          break;
        case C.TRACK_TYPE_AUDIO:
          if (!selectedAudioTracks) {
            rendererTrackSelections[i] = selectAudioTrack(rendererTrackGroupArrays[i],
                rendererFormatSupports[i], params.preferredAudioLanguage,
                false, true,
                seenVideoRendererWithMappedTracks ? null : adaptiveTrackSelectionFactory);
            selectedAudioTracks = rendererTrackSelections[i] != null;
          }
          break;
        case C.TRACK_TYPE_TEXT:

          break;
        default:
          rendererTrackSelections[i] = selectOtherTrack(rendererCapabilities[i].getTrackType(),
              rendererTrackGroupArrays[i], rendererFormatSupports[i],
              false);
          break;
      }
    }
    return rendererTrackSelections;
  }

  // Audio track selection implementation.

  protected TrackSelection selectAudioTrack(TrackGroupArray groups, int[][] formatSupport,
      String preferredAudioLanguage, boolean exceedRendererCapabilitiesIfNecessary,
      boolean allowMixedMimeAdaptiveness, TrackSelection.Factory adaptiveTrackSelectionFactory) {
    int selectedGroupIndex = C.INDEX_UNSET;
    int selectedTrackIndex = C.INDEX_UNSET;
    int selectedTrackScore = 0;
    for (int groupIndex = 0; groupIndex < groups.length; groupIndex++) {
      TrackGroup trackGroup = groups.get(groupIndex);
      int[] trackFormatSupport = formatSupport[groupIndex];
      for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
        if (isSupported(trackFormatSupport[trackIndex], exceedRendererCapabilitiesIfNecessary)) {
          Format format = trackGroup.getFormat(trackIndex);
          int trackScore = getAudioTrackScore(trackFormatSupport[trackIndex],
              preferredAudioLanguage, format);
          if (trackScore > selectedTrackScore) {
            selectedGroupIndex = groupIndex;
            selectedTrackIndex = trackIndex;
            selectedTrackScore = trackScore;
          }
        }
      }
    }

    if (selectedGroupIndex == C.INDEX_UNSET) {
      return null;
    }

    TrackGroup selectedGroup = groups.get(selectedGroupIndex);
    if (adaptiveTrackSelectionFactory != null) {
      // If the group of the track with the highest score allows it, try to enable adaptation.
      int[] adaptiveTracks = getAdaptiveAudioTracks(selectedGroup,
          formatSupport[selectedGroupIndex], allowMixedMimeAdaptiveness);
      if (adaptiveTracks.length > 0) {
        return adaptiveTrackSelectionFactory.createTrackSelection(selectedGroup,
            adaptiveTracks);
      }
    }
    return new FixedTrackSelection(selectedGroup, selectedTrackIndex);
  }

  private static int getAudioTrackScore(int formatSupport, String preferredLanguage,
      Format format) {
    boolean isDefault = (format.selectionFlags & C.SELECTION_FLAG_DEFAULT) != 0;
    int trackScore;
    if (formatHasLanguage(format, preferredLanguage)) {
      if (isDefault) {
        trackScore = 4;
      } else {
        trackScore = 3;
      }
    } else if (isDefault) {
      trackScore = 2;
    } else {
      trackScore = 1;
    }
    if (isSupported(formatSupport, false)) {
      trackScore += WITHIN_RENDERER_CAPABILITIES_BONUS;
    }
    return trackScore;
  }

  private static int[] getAdaptiveAudioTracks(TrackGroup group, int[] formatSupport,
      boolean allowMixedMimeTypes) {
    int selectedConfigurationTrackCount = 0;
    AudioConfigurationTuple selectedConfiguration = null;
    HashSet<AudioConfigurationTuple> seenConfigurationTuples = new HashSet<>();
    for (int i = 0; i < group.length; i++) {
      Format format = group.getFormat(i);
      AudioConfigurationTuple configuration = new AudioConfigurationTuple(
          format.channelCount, format.sampleRate,
          allowMixedMimeTypes ? null : format.sampleMimeType);
      if (seenConfigurationTuples.add(configuration)) {
        int configurationCount = getAdaptiveAudioTrackCount(group, formatSupport, configuration);
        if (configurationCount > selectedConfigurationTrackCount) {
          selectedConfiguration = configuration;
          selectedConfigurationTrackCount = configurationCount;
        }
      }
    }

    if (selectedConfigurationTrackCount > 1) {
      int[] adaptiveIndices = new int[selectedConfigurationTrackCount];
      int index = 0;
      for (int i = 0; i < group.length; i++) {
        if (isSupportedAdaptiveAudioTrack(group.getFormat(i), formatSupport[i],
            selectedConfiguration)) {
          adaptiveIndices[index++] = i;
        }
      }
      return adaptiveIndices;
    }
    return NO_TRACKS;
  }

  private static int getAdaptiveAudioTrackCount(TrackGroup group, int[] formatSupport,
      AudioConfigurationTuple configuration) {
    int count = 0;
    for (int i = 0; i < group.length; i++) {
      if (isSupportedAdaptiveAudioTrack(group.getFormat(i), formatSupport[i], configuration)) {
        count++;
      }
    }
    return count;
  }

  private static boolean isSupportedAdaptiveAudioTrack(Format format, int formatSupport,
      AudioConfigurationTuple configuration) {
    return isSupported(formatSupport, false) && format.channelCount == configuration.channelCount
        && format.sampleRate == configuration.sampleRate
        && (configuration.mimeType == null
            || TextUtils.equals(configuration.mimeType, format.sampleMimeType));
  }

  // General track selection methods.

  protected TrackSelection selectOtherTrack(int trackType, TrackGroupArray groups,
      int[][] formatSupport, boolean exceedRendererCapabilitiesIfNecessary) {
    TrackGroup selectedGroup = null;
    int selectedTrackIndex = 0;
    int selectedTrackScore = 0;
    for (int groupIndex = 0; groupIndex < groups.length; groupIndex++) {
      TrackGroup trackGroup = groups.get(groupIndex);
      int[] trackFormatSupport = formatSupport[groupIndex];
      for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
        if (isSupported(trackFormatSupport[trackIndex], exceedRendererCapabilitiesIfNecessary)) {
          Format format = trackGroup.getFormat(trackIndex);
          boolean isDefault = (format.selectionFlags & C.SELECTION_FLAG_DEFAULT) != 0;
          int trackScore = isDefault ? 2 : 1;
          if (isSupported(trackFormatSupport[trackIndex], false)) {
            trackScore += WITHIN_RENDERER_CAPABILITIES_BONUS;
          }
          if (trackScore > selectedTrackScore) {
            selectedGroup = trackGroup;
            selectedTrackIndex = trackIndex;
            selectedTrackScore = trackScore;
          }
        }
      }
    }
    return selectedGroup == null ? null
        : new FixedTrackSelection(selectedGroup, selectedTrackIndex);
  }

  protected static boolean isSupported(int formatSupport, boolean allowExceedsCapabilities) {
    int maskedSupport = formatSupport & RendererCapabilities.FORMAT_SUPPORT_MASK;
    return maskedSupport == RendererCapabilities.FORMAT_HANDLED || (allowExceedsCapabilities
        && maskedSupport == RendererCapabilities.FORMAT_EXCEEDS_CAPABILITIES);
  }

  protected static boolean formatHasLanguage(Format format, String language) {
    return language != null
        && TextUtils.equals(language, Util.normalizeLanguageCode(format.language));
  }

  private static final class AudioConfigurationTuple {

    public final int channelCount;
    public final int sampleRate;
    public final String mimeType;

    public AudioConfigurationTuple(int channelCount, int sampleRate, String mimeType) {
      this.channelCount = channelCount;
      this.sampleRate = sampleRate;
      this.mimeType = mimeType;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      AudioConfigurationTuple other = (AudioConfigurationTuple) obj;
      return channelCount == other.channelCount && sampleRate == other.sampleRate
          && TextUtils.equals(mimeType, other.mimeType);
    }

    @Override
    public int hashCode() {
      int result = channelCount;
      result = 31 * result + sampleRate;
      result = 31 * result + (mimeType != null ? mimeType.hashCode() : 0);
      return result;
    }

  }

}
