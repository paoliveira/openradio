/*
 * Copyright 2017-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.model.media

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.yuriy.openradio.shared.service.OpenRadioService
import com.yuriy.openradio.shared.utils.AppLogger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 29/06/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class MediaResourcesManager(context: Context, className: String) {
    /**
     * Tag string to use in logging message.
     */
    private val mClassName = "MdRsrcsMgr $className "

    /**
     * Browses media content offered by a [android.service.media.MediaBrowserService].
     */
    private val mMediaBrowser: MediaBrowserCompat

    private val mIsConnectInvoked = AtomicBoolean(false)

    /**
     * Controller of media content offered by a [android.service.media.MediaBrowserService].
     */
    private var mMediaController: MediaControllerCompat? = null

    /**
     * Listener of the media Controllers callbacks.
     */
    private val mMediaSessionCallback = MediaSessionCallback()

    /**
     * Transport controls of the Media Controller.
     */
    private var mTransportControls: MediaControllerCompat.TransportControls? = null

    /**
     * Callee [Activity].
     */
    private var mActivity: Activity? = null

    /**
     * Listener for the media resources events. Acts as proxy between this manager and callee Activity.
     */
    private var mListener: MediaResourceManagerListener? = null
    private val mSubscribed = HashSet<String>()

    /**
     * Constructor.
     */
    init {
        // Initialize Media Browser
        val callback = MediaBrowserConnectionCallback()
        mMediaBrowser = MediaBrowserCompat(
            context,
            ComponentName(context, OpenRadioService::class.java),
            callback,
            null
        )
    }

    /**
     * Creates Media Browser, assigns listener.
     */
    fun init(activity: Activity, listener: MediaResourceManagerListener) {
        mActivity = activity
        mListener = listener
    }

    /**
     * Connects to the Media Browse service.
     */
    fun connect() {
        if (mMediaBrowser.isConnected) {
            AppLogger.w("$mClassName connect aborted, already connected")
            // Register callbacks
            mMediaController?.registerCallback(mMediaSessionCallback)
            // Set actual media controller
            MediaControllerCompat.setMediaController(mActivity!!, mMediaController)
            // To update Play/Pause btn of the Currently Playing station. By default it shows spinner.
            mMediaSessionCallback.dispatchLatestState()
            return
        }
        if (mIsConnectInvoked.get()) {
            return
        }
        try {
            mMediaBrowser.connect()
            mIsConnectInvoked.set(true)
            AppLogger.i("$mClassName connected")
        } catch (e: IllegalStateException) {
            AppLogger.e("$mClassName can not connect", e)
        }
    }

    /**
     * Disconnects from the Media Browse service. After this, no more callbacks will be received.
     */
    fun disconnect() {
        if (!mMediaBrowser.isConnected) {
            AppLogger.w("$mClassName disconnect aborted, already disconnected")
            return
        }
        if (!mIsConnectInvoked.get()) {
            return
        }
        mMediaBrowser.disconnect()
        mIsConnectInvoked.set(false)
        AppLogger.i("$mClassName disconnected")
    }

    fun clean() {
        mMediaController?.unregisterCallback(mMediaSessionCallback)
        if (mActivity != null) {
            MediaControllerCompat.setMediaController(mActivity!!, null)
        }
        mActivity = null
        mListener = null
    }

    /**
     * Queries for information about the media items that are contained within the specified id and subscribes to
     * receive updates when they change.
     *
     * @param parentId The id of the parent media item whose list of children will be subscribed.
     * @param callback The callback to receive the list of children.
     * @param options Options to pass to Media Browser when do subscribe.
     */
    fun subscribe(parentId: String,
                  callback: MediaBrowserCompat.SubscriptionCallback?,
                  options: Bundle = Bundle()) {
        AppLogger.i("$mClassName subscribe:$parentId")
        if (callback == null) {
            AppLogger.e("$mClassName subscribe listener is null")
            return
        }
        if (mSubscribed.contains(parentId)) {
            AppLogger.w("$mClassName already subscribed")
            return
        }
        mSubscribed.add(parentId)
        mMediaBrowser.subscribe(parentId, options, callback)
    }

    /**
     * Unsubscribe for changes to the children of the specified media id.
     *
     * @param parentId The id of the parent media item whose list of children will be unsubscribed.
     */
    fun unsubscribe(parentId: String) {
        if (!mSubscribed.contains(parentId)) {
            return
        }
        AppLogger.i("$mClassName unsubscribe:$parentId, $mMediaBrowser")
        mSubscribed.remove(parentId)
        mMediaBrowser.unsubscribe(parentId)
    }

    /**
     * Gets the root id.<br></br>
     * Note that the root id may become invalid or change when when the browser is disconnected.
     *
     * @return Root Id.
     */
    val root: String
        get() = mMediaBrowser.root

    /**
     * @return Metadata.
     */
    val mediaMetadata: MediaMetadataCompat?
        get() = if (mMediaController != null) mMediaController?.metadata else null

    /**
     * @param mediaId media id of the item to play.
     */
    fun playFromMediaId(mediaId: String?) {
        mTransportControls?.playFromMediaId(mediaId, null)
    }

    private fun handleMediaBrowserConnected() {
        if (mActivity == null) {
            AppLogger.e("$mClassName media browser connected when context is null, disconnect")
            disconnect()
            return
        }

        // Initialize Media Controller
        mMediaController = try {
            MediaControllerCompat(
                mActivity?.applicationContext,
                mMediaBrowser.sessionToken
            )
        } catch (e: RemoteException) {
            AppLogger.e("handleMediaBrowserConnected", e)
            return
        }

        // Initialize Transport Controls
        mTransportControls = mMediaController?.transportControls
        // Register callbacks
        mMediaController?.registerCallback(mMediaSessionCallback)

        // Set actual media controller
        MediaControllerCompat.setMediaController(mActivity!!, mMediaController)
        mListener?.onConnected()
    }

    /**
     * Callback object for the Media Browser connection events.
     */
    private inner class MediaBrowserConnectionCallback : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            AppLogger.i("$mClassName connected")
            handleMediaBrowserConnected()
        }

        override fun onConnectionSuspended() {
            AppLogger.w("$mClassName connection Suspended")
            val manager = this@MediaResourcesManager
            manager.mMediaController?.unregisterCallback(manager.mMediaSessionCallback)
            manager.mTransportControls = null
            manager.mMediaController = null
            if (manager.mActivity != null) {
                MediaControllerCompat.setMediaController(manager.mActivity!!, null)
            }
        }

        override fun onConnectionFailed() {
            AppLogger.e("$mClassName connection failed")
        }
    }

    /**
     * Receive callbacks from the [MediaControllerCompat].<br></br>
     * Here we update our state such as which queue is being shown,
     * the current title and description and the [PlaybackStateCompat].
     */
    private inner class MediaSessionCallback : MediaControllerCompat.Callback() {

        private var mCurrentState: PlaybackStateCompat? = null

        override fun onSessionDestroyed() {
            AppLogger.i("$mClassName session destroyed. Need to fetch a new Media Session")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            if (state == null) {
                AppLogger.e("$mClassName playbackStateChanged to null state")
                return
            }
            mCurrentState = state
            mListener?.onPlaybackStateChanged(state)
        }

        override fun onQueueChanged(queue: List<MediaSessionCompat.QueueItem>) {
            mListener?.onQueueChanged(queue)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            if (metadata == null) {
                AppLogger.e("$mClassName metadata changed null")
                return
            }
            mListener?.onMetadataChanged(metadata)
        }

        fun dispatchLatestState() {
            if (mCurrentState == null) {
                return
            }
            onPlaybackStateChanged(mCurrentState)
        }
    }
}
