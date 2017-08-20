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
package com.google.android.exoplayer2;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.view.TextureView;

import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataRenderer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.util.Util;

import java.util.List;

/**
 * An {@link ExoPlayer} implementation that uses default {@link Renderer} components.
 */
@TargetApi(16)
public class SimpleExoPlayer implements ExoPlayer {

  protected final Renderer[] renderers;

  private final ExoPlayer player;
  private final ComponentListener componentListener;
  private final int audioRendererCount;

  protected SimpleExoPlayer(RenderersFactory renderersFactory, TrackSelector trackSelector,
      LoadControl loadControl) {
    componentListener = new ComponentListener();
    Looper eventLooper = Looper.myLooper() != null ? Looper.myLooper() : Looper.getMainLooper();
    Handler eventHandler = new Handler(eventLooper);
    renderers = renderersFactory.createRenderers(eventHandler, componentListener,
        componentListener, componentListener);

    // Obtain counts of audio renderers.
    int audioRendererCount = 0;
    for (Renderer renderer : renderers) {
      switch (renderer.getTrackType()) {
        case C.TRACK_TYPE_AUDIO:
          audioRendererCount++;
          break;
      }
    }
    this.audioRendererCount = audioRendererCount;

    // Build the player and associated objects.
    player = new ExoPlayerImpl(renderers, trackSelector, loadControl);
  }

  /**
   * Sets the attributes for audio playback, used by the underlying audio track. If not set, the
   * default audio attributes will be used. They are suitable for general media playback.
   * <p>
   * Setting the audio attributes during playback may introduce a short gap in audio output as the
   * audio track is recreated. A new audio session id will also be generated.
   * <p>
   * If tunneling is enabled by the track selector, the specified audio attributes will be ignored,
   * but they will take effect if audio is later played without tunneling.
   * <p>
   * If the device is running a build before platform API version 21, audio attributes cannot be set
   * directly on the underlying audio track. In this case, the usage will be mapped onto an
   * equivalent stream type using {@link Util#getStreamTypeForAudioUsage(int)}.
   *
   * @param audioAttributes The attributes to use for audio playback.
   */
  public void setAudioAttributes(AudioAttributes audioAttributes) {
    ExoPlayerMessage[] messages = new ExoPlayerMessage[audioRendererCount];
    int count = 0;
    for (Renderer renderer : renderers) {
      if (renderer.getTrackType() == C.TRACK_TYPE_AUDIO) {
        messages[count++] = new ExoPlayerMessage(renderer, C.MSG_SET_AUDIO_ATTRIBUTES,
            audioAttributes);
      }
    }
    player.sendMessages(messages);
  }

  // ExoPlayer implementation

  @Override
  public Looper getPlaybackLooper() {
    return player.getPlaybackLooper();
  }

  @Override
  public void addListener(Player.EventListener listener) {
    player.addListener(listener);
  }

  @Override
  public void removeListener(Player.EventListener listener) {
    player.removeListener(listener);
  }

  @Override
  public int getPlaybackState() {
    return player.getPlaybackState();
  }

  @Override
  public void prepare(MediaSource mediaSource) {
    player.prepare(mediaSource);
  }

  @Override
  public void prepare(MediaSource mediaSource, boolean resetPosition, boolean resetState) {
    player.prepare(mediaSource, resetPosition, resetState);
  }

  @Override
  public void setPlayWhenReady(boolean playWhenReady) {
    player.setPlayWhenReady(playWhenReady);
  }

  @Override
  public boolean getPlayWhenReady() {
    return player.getPlayWhenReady();
  }

  @Override
  public @RepeatMode int getRepeatMode() {
    return player.getRepeatMode();
  }

  @Override
  public void setRepeatMode(@RepeatMode int repeatMode) {
    player.setRepeatMode(repeatMode);
  }

  @Override
  public boolean isLoading() {
    return player.isLoading();
  }

  @Override
  public void seekToDefaultPosition() {
    player.seekToDefaultPosition();
  }

  @Override
  public void seekToDefaultPosition(int windowIndex) {
    player.seekToDefaultPosition(windowIndex);
  }

  @Override
  public void seekTo(long positionMs) {
    player.seekTo(positionMs);
  }

  @Override
  public void seekTo(int windowIndex, long positionMs) {
    player.seekTo(windowIndex, positionMs);
  }

  @Override
  public void setPlaybackParameters(PlaybackParameters playbackParameters) {
    player.setPlaybackParameters(playbackParameters);
  }

  @Override
  public PlaybackParameters getPlaybackParameters() {
    return player.getPlaybackParameters();
  }

  @Override
  public void stop() {
    player.stop();
  }

  @Override
  public void release() {
    player.release();
  }

  @Override
  public void sendMessages(ExoPlayerMessage... messages) {
    player.sendMessages(messages);
  }

  @Override
  public void blockingSendMessages(ExoPlayerMessage... messages) {
    player.blockingSendMessages(messages);
  }

  @Override
  public int getRendererCount() {
    return player.getRendererCount();
  }

  @Override
  public int getRendererType(int index) {
    return player.getRendererType(index);
  }

  @Override
  public TrackGroupArray getCurrentTrackGroups() {
    return player.getCurrentTrackGroups();
  }

  @Override
  public TrackSelectionArray getCurrentTrackSelections() {
    return player.getCurrentTrackSelections();
  }

  @Override
  public Timeline getCurrentTimeline() {
    return player.getCurrentTimeline();
  }

  @Override
  public Object getCurrentManifest() {
    return player.getCurrentManifest();
  }

  @Override
  public int getCurrentPeriodIndex() {
    return player.getCurrentPeriodIndex();
  }

  @Override
  public int getCurrentWindowIndex() {
    return player.getCurrentWindowIndex();
  }

  @Override
  public long getDuration() {
    return player.getDuration();
  }

  @Override
  public long getCurrentPosition() {
    return player.getCurrentPosition();
  }

  @Override
  public long getBufferedPosition() {
    return player.getBufferedPosition();
  }

  @Override
  public int getBufferedPercentage() {
    return player.getBufferedPercentage();
  }

  @Override
  public boolean isCurrentWindowDynamic() {
    return player.isCurrentWindowDynamic();
  }

  @Override
  public boolean isCurrentWindowSeekable() {
    return player.isCurrentWindowSeekable();
  }

  @Override
  public boolean isPlayingAd() {
    return player.isPlayingAd();
  }

  @Override
  public int getCurrentAdGroupIndex() {
    return player.getCurrentAdGroupIndex();
  }

  @Override
  public int getCurrentAdIndexInAdGroup() {
    return player.getCurrentAdIndexInAdGroup();
  }

  @Override
  public long getContentPosition() {
    return player.getContentPosition();
  }

  // Internal methods.

  private final class ComponentListener implements
      AudioRendererEventListener, TextRenderer.Output, MetadataRenderer.Output,
      SurfaceHolder.Callback, TextureView.SurfaceTextureListener {

    // AudioRendererEventListener implementation

    @Override
    public void onAudioEnabled(DecoderCounters counters) {

    }

    @Override
    public void onAudioSessionId(int sessionId) {

    }

    @Override
    public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs,
        long initializationDurationMs) {
    }

    @Override
    public void onAudioInputFormatChanged(Format format) {

    }

    @Override
    public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs,
        long elapsedSinceLastFeedMs) {
    }

    @Override
    public void onAudioDisabled(DecoderCounters counters) {

    }

    // TextRenderer.Output implementation

    @Override
    public void onCues(List<Cue> cues) {

    }

    // MetadataRenderer.Output implementation

    @Override
    public void onMetadata(Metadata metadata) {

    }

    // SurfaceHolder.Callback implementation

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
      // Do nothing.
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    // TextureView.SurfaceTextureListener implementation

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
      // Do nothing.
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {

      return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
      // Do nothing.
    }

  }

}
