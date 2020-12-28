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
import com.yuriy.openradio.shared.model.media.MediaResourcesManager
import com.yuriy.openradio.shared.service.OpenRadioService
import com.yuriy.openradio.shared.service.OpenRadioService.Companion.getCurrentParentId
import com.yuriy.openradio.shared.service.OpenRadioService.Companion.getCurrentPlaybackState
import com.yuriy.openradio.shared.service.OpenRadioService.Companion.getRestoreState
import com.yuriy.openradio.shared.utils.AnalyticsUtils.logException
import com.yuriy.openradio.shared.utils.AppLogger.d
import com.yuriy.openradio.shared.utils.AppLogger.e
import com.yuriy.openradio.shared.utils.AppLogger.i
import com.yuriy.openradio.shared.utils.AppLogger.w
import com.yuriy.openradio.shared.utils.MediaItemHelper.playbackStateToString
import java.util.*

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
    private val CLASS_NAME: String = "MdRsrcsMgr $className "

    /**
     * Browses media content offered by a [android.service.media.MediaBrowserService].
     */
    private val mMediaBrowser: MediaBrowserCompat?

    /**
     * Controller of media content offered by a [android.service.media.MediaBrowserService].
     */
    private var mMediaController: MediaControllerCompat? = null

    /**
     * Listener of the media Controllers callbacks.
     */
    private val mMediaSessionCallback: MediaSessionCallback

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
    private val mSubscribed: MutableSet<String>

    /**
     * Creates Media Browser, assigns listener.
     */
    fun init(activity: Activity, bundle: Bundle?,
             listener: MediaResourceManagerListener) {
        mActivity = activity
        mListener = listener
        //TODO: Simple solution that needs to be revised.
        OpenRadioService.mCurrentParentId = getCurrentParentId(bundle)
        OpenRadioService.mIsRestoreState = getRestoreState(bundle)
        val state = getCurrentPlaybackState(bundle)
        // Do not assign unknown state.
        if (state != PlaybackStateCompat.STATE_NONE) {
            OpenRadioService.mState = state
        }
    }

    /**
     * Connects to the Media Browse service.
     */
    fun connect() {
        if (mMediaBrowser!!.isConnected) {
            w(CLASS_NAME + "Connect aborted, already connected")
            // Register callbacks
            mMediaController!!.registerCallback(mMediaSessionCallback)
            // Set actual media controller
            MediaControllerCompat.setMediaController(mActivity!!, mMediaController)
            // To update Play/Pause btn of the Currently Playing station. By default it shows spinner.
            mMediaSessionCallback.dispatchLatestState()
            return
        }
        try {
            mMediaBrowser.connect()
            i(CLASS_NAME + "Connected")
        } catch (e: IllegalStateException) {
            e(CLASS_NAME + "Can not connect:" + e)
        }
    }

    /**
     * Disconnects from the Media Browse service. After this, no more callbacks will be received.
     */
    fun disconnect() {
        if (!mMediaBrowser!!.isConnected) {
            w(CLASS_NAME + "Disconnect aborted, already disconnected")
            return
        }
        mMediaBrowser.disconnect()
        i(CLASS_NAME + "Disconnected")
    }

    fun clean() {
        if (mMediaController != null) {
            mMediaController!!.unregisterCallback(mMediaSessionCallback)
        }
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
     */
    fun subscribe(parentId: String,
                  callback: MediaBrowserCompat.SubscriptionCallback?) {
        i(CLASS_NAME + "Subscribe:" + parentId)
        if (callback == null) {
            e("$CLASS_NAME subscribe listener is null")
            return
        }
        if (mSubscribed.contains(parentId)) {
            w(CLASS_NAME + "already subscribed")
            return
        }
        mSubscribed.add(parentId)
        mMediaBrowser!!.subscribe(parentId, callback)
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
        i(CLASS_NAME + "Unsubscribe:" + parentId + ", " + mMediaBrowser)
        if (mMediaBrowser != null) {
            mSubscribed.remove(parentId)
            mMediaBrowser.unsubscribe(parentId)
        }
    }

    /**
     * Gets the root id.<br></br>
     * Note that the root id may become invalid or change when when the browser is disconnected.
     *
     * @return Root Id.
     */
    val root: String
        get() = mMediaBrowser?.root ?: ""

    /**
     * @return Metadata.
     */
    val mediaMetadata: MediaMetadataCompat?
        get() = if (mMediaController != null) mMediaController!!.metadata else null

    /**
     * @param mediaId media id of the item to play.
     */
    fun playFromMediaId(mediaId: String?) {
        if (mTransportControls != null) {
            mTransportControls!!.playFromMediaId(mediaId, null)
        }
    }

    private fun handleMediaBrowserConnected() {
        d(CLASS_NAME + "Session token " + mMediaBrowser!!.sessionToken)
        if (mActivity == null) {
            e("$CLASS_NAME media browser connected when context is null, disconnect")
            disconnect()
            return
        }

        // Initialize Media Controller
        mMediaController = try {
            MediaControllerCompat(
                    mActivity,
                    mMediaBrowser.sessionToken
            )
        } catch (e: RemoteException) {
            logException(e)
            return
        }

        // Initialize Transport Controls
        mTransportControls = mMediaController!!.transportControls
        // Register callbacks
        mMediaController!!.registerCallback(mMediaSessionCallback)

        // Set actual media controller
        MediaControllerCompat.setMediaController(mActivity!!, mMediaController)
        if (mListener != null) {
            mListener!!.onConnected()
        } else {
            e("$CLASS_NAME handle media browser connected, listener is null")
        }
    }

    /**
     * Callback object for the Media Browser connection events.
     */
    private inner class MediaBrowserConnectionCallback: MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            i(CLASS_NAME + "Connected")
            handleMediaBrowserConnected()
        }

        override fun onConnectionSuspended() {
            w(CLASS_NAME + "Connection Suspended")
            val manager = this@MediaResourcesManager
            manager.mMediaController!!.unregisterCallback(manager.mMediaSessionCallback)
            manager.mTransportControls = null
            manager.mMediaController = null
            if (manager.mActivity != null) {
                MediaControllerCompat.setMediaController(manager.mActivity!!, null)
            }
        }

        override fun onConnectionFailed() {
            e(CLASS_NAME + "Connection Failed")
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
            i(CLASS_NAME + "Session destroyed. Need to fetch a new Media Session")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            if (state == null) {
                e(CLASS_NAME + "PlaybackStateChanged to null state")
                return
            }
            d(
                    CLASS_NAME + "psc:["
                            + playbackStateToString(state) + "]" + state
            )
            mCurrentState = state
            if (mListener == null) {
                e(CLASS_NAME + "PlaybackStateChanged listener null")
                return
            }
            mListener!!.onPlaybackStateChanged(state)
        }

        override fun onQueueChanged(queue: List<MediaSessionCompat.QueueItem>) {
            d(CLASS_NAME + "Queue changed:" + queue)
            if (mListener == null) {
                e(CLASS_NAME + "Queue changed listener null")
                return
            }
            mListener!!.onQueueChanged(queue)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            d(CLASS_NAME + "Metadata changed:" + metadata)
            if (metadata == null) {
                e(CLASS_NAME + "Metadata changed null")
                return
            }
            if (mListener == null) {
                e(CLASS_NAME + "Metadata changed listener null")
                return
            }
            if (mMediaController == null) {
                e(CLASS_NAME + "Metadata changed media controller null")
                return
            }
            mListener!!.onMetadataChanged(
                    metadata, mMediaController!!.queue
            )
        }

        fun dispatchLatestState() {
            if (mCurrentState == null) {
                return
            }
            onPlaybackStateChanged(mCurrentState)
        }
    }

    /**
     * Constructor.
     */
    init {
        mMediaSessionCallback = MediaSessionCallback()
        mSubscribed = HashSet()
        // Initialize Media Browser
        val callback: MediaBrowserCompat.ConnectionCallback = MediaBrowserConnectionCallback()
        mMediaBrowser = MediaBrowserCompat(
                context,
                ComponentName(context, OpenRadioService::class.java),
                callback,
                null
        )
    }
}
