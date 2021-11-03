/*
 * Copyright (C) 2016-2021 The Android Open Source Project
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

package com.yuriy.openradio.shared.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.RemoteException
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.service.OpenRadioService
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.MediaItemHelper
import com.yuriy.openradio.shared.vo.RadioStation
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Keeps track of a notification and updates it automatically for a given
 * MediaSession. Maintaining a visible notification (usually) guarantees that the music service
 * won't be killed during playback.
 *
 * @param mContext Application context.
 * @param mService Open Radio Service.
 */
class MediaNotification(private val mContext: Context, private val mService: OpenRadioService) : BroadcastReceiver() {

    private lateinit var mSessionToken: MediaSessionCompat.Token
    private lateinit var mController: MediaControllerCompat
    private var mTransportControls: MediaControllerCompat.TransportControls? = null
    private var mMetadata: MediaMetadataCompat? = null
    private val mCb: MediaControllerCompatCallback
    private val mNotificationManager: NotificationManagerCompat
    private val mPauseIntent: PendingIntent
    private val mPlayIntent: PendingIntent
    private val mPreviousIntent: PendingIntent
    private val mNextIntent: PendingIntent
    private val mCloseAppIntent: PendingIntent
    private val mNotificationColor: Int
    private val mStarted = AtomicBoolean(false)
    private val mNotificationChannelFactory: NotificationChannelFactory
    private val mUseNavigationActionsInCompactView = false
    private val notificationColor: Int
        get() {
            var notificationColor = 0
            val packageName = mContext.packageName
            try {
                val packageContext = mContext.createPackageContext(packageName, 0)
                val applicationInfo = mContext.packageManager.getApplicationInfo(packageName, 0)
                packageContext.setTheme(applicationInfo.theme)
                val theme = packageContext.theme
                if (AppUtils.hasVersionLollipop()) {
                    val ta = theme.obtainStyledAttributes(intArrayOf(R.attr.colorPrimary))
                    notificationColor = ta.getColor(0, Color.DKGRAY)
                    ta.recycle()
                } else {
                    notificationColor = -0xbbbbbc
                }
            } catch (e: Exception) {
                AppLogger.e("Get notification color", e)
            }
            return notificationColor
        }

    init {
        mCb = MediaControllerCompatCallback()
        mNotificationChannelFactory = NotificationChannelFactory(mContext)
        updateSessionToken()

        mNotificationColor = notificationColor
        mNotificationManager = NotificationManagerCompat.from(mContext)
        val pkg = mContext.packageName
        mPauseIntent = PendingIntent.getBroadcast(
            mContext, 100,
            Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_IMMUTABLE
        )
        mPlayIntent = PendingIntent.getBroadcast(
            mContext, 100,
            Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_IMMUTABLE
        )
        mPreviousIntent = PendingIntent.getBroadcast(
            mContext, 100,
            Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_IMMUTABLE
        )
        mNextIntent = PendingIntent.getBroadcast(
            mContext, 100,
            Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_IMMUTABLE
        )
        mCloseAppIntent = PendingIntent.getBroadcast(
            mContext, 100,
            Intent(ACTION_CLOSE_APP).setPackage(pkg), PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before [.stopNotification] is called.
     */
    fun startNotification(context: Context, radioStation: RadioStation) {
        if (mStarted.get()) {
            return
        }
        mController.registerCallback(mCb)
        val filter = IntentFilter()
        filter.addAction(ACTION_NEXT)
        filter.addAction(ACTION_PAUSE)
        filter.addAction(ACTION_PLAY)
        filter.addAction(ACTION_PREV)
        filter.addAction(ACTION_CLOSE_APP)
        context.registerReceiver(this, filter)
        val metadata = mController.metadata
        mMetadata = metadata ?: MediaItemHelper.metadataFromRadioStation(context, radioStation)
        mStarted.set(true)
        // The notification must be updated after setting started to true
        handleNotification(mController.playbackState)
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    fun stopNotification(context: Context) {
        if (!mStarted.get()) {
            return
        }
        mController.unregisterCallback(mCb)
        mNotificationManager.cancelAll()
        try {
            context.unregisterReceiver(this)
        } catch (ex: IllegalArgumentException) {
            AppLogger.e("$CLASS_NAME error while unregister", ex)
        }
        mStarted.set(false)
    }

    override fun onReceive(context: Context, intent: Intent) {
        AppLogger.d("$CLASS_NAME received intent $intent")
        when (intent.action) {
            ACTION_PAUSE -> mTransportControls!!.pause()
            ACTION_PLAY -> mTransportControls!!.play()
            ACTION_NEXT -> mTransportControls!!.skipToNext()
            ACTION_PREV -> mTransportControls!!.skipToPrevious()
            ACTION_CLOSE_APP -> {
                AppLogger.i("$CLASS_NAME close App from Notification")
                mService.closeService()
            }
        }
    }

    /**
     * Update the state based on a change on the session token. Called either when
     * we are running for the first time or when the media session owner has destroyed the session
     * (see [android.media.session.MediaController.Callback.onSessionDestroyed])
     */
    private fun updateSessionToken() {
        val freshToken = mService.sessionToken
        AppLogger.d("$CLASS_NAME fresh token:$freshToken")
        if (!this::mSessionToken.isInitialized || mSessionToken != freshToken) {
            if (this::mController.isInitialized) {
                mController.unregisterCallback(mCb)
            }
            if (freshToken != null) {
                mSessionToken = freshToken
            }
            mController = try {
                MediaControllerCompat(mContext, mSessionToken)
            } catch (e: RemoteException) {
                AppLogger.e("UpdateSessionToken", e)
                return
            }
            mTransportControls = mController.transportControls
            if (mStarted.get()) {
                mController.registerCallback(mCb)
            }
        }
    }

    private inner class MediaControllerCompatCallback : MediaControllerCompat.Callback() {

        private var mPlaybackState: PlaybackStateCompat? = null

        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            AppLogger.d("$CLASS_NAME Received new playback state:$state")
            val doNotify = if (mPlaybackState == null) {
                true
            } else {
                doHandleState(mPlaybackState!!.state, state.state)
            }
            if (doNotify) {
                handleNotification(state)
            }
            mPlaybackState = state
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat) {
            AppLogger.d("$CLASS_NAME Received new metadata:$metadata")
            mMetadata = metadata
            handleNotification(mPlaybackState)
        }

        override fun onSessionDestroyed() {
            AppLogger.d("$CLASS_NAME Session was destroyed, resetting to the new session token")
            updateSessionToken()
        }

        private fun doHandleState(curState: Int, newState: Int): Boolean {
            return newState != curState
        }
    }

    fun handleNotification(playbackState: PlaybackStateCompat?) {
        if (mMetadata == null) {
            return
        }
        if (playbackState == null) {
            return
        }
        val builder = NotificationCompat.Builder(
            mContext, MediaNotificationData.CHANNEL_ID
        )

        // Create/Retrieve Notification Channel for O and beyond devices (26+).
        mNotificationChannelFactory.createChannel(MediaNotificationData(mContext, mMetadata!!))
        // If skip to previous action is enabled
        var enablePrevious = false
        if (playbackState.actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L) {
            builder.addAction(
                R.drawable.ic_skip_prev,
                mContext.getString(R.string.label_previous),
                mPreviousIntent
            )
            enablePrevious = true
        }

        val description = mMetadata!!.description
        var art = description.iconBitmap
        if (art == null && description.iconUri != null) {
            //art = description.iconUri
        } else if (art == null) {
            // use a placeholder art while the remote art is being downloaded
            art = BitmapFactory.decodeResource(mContext.resources, R.drawable.ic_radio_station)
        } else {
            AppLogger.d("Art bitmap:$art")
        }

        builder.addAction(getPlayPauseAction(playbackState))

        // If skip to next action is enabled
        var enableNext = false
        if (playbackState.actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L) {
            builder.addAction(
                R.drawable.ic_skip_next,
                mContext.getString(R.string.label_next),
                mNextIntent
            )
            enableNext = true
        }
        val smallIcon =
            if (AppUtils.hasVersionLollipop()) R.drawable.ic_notification else R.drawable.ic_notification_drawable
        // Build the style.
        val actionsToShowInCompact = getActionIndicesForCompactView(
            getActions(enableNext, enablePrevious, playbackState), playbackState
        )
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(*actionsToShowInCompact)
            .setMediaSession(mSessionToken)
        builder
            .setContentIntent(makePendingIntent())
            .setStyle(mediaStyle)
            .setColor(mNotificationColor)
            .setLargeIcon(art)
            .setSmallIcon(smallIcon)
            .setOngoing(true)
            .setDefaults(0)
            .setSound(null)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentTitle(description.title)
            .setContentText(description.subtitle)
        AppLogger.d(
            CLASS_NAME + " Update Notification for ${description.mediaId} " +
                "state:" + playbackState +
                " title:" + description.title +
                " subtitle:" + description.subtitle
        )

        builder.addAction(getCloseAppAction())

        mService.startForeground(NOTIFICATION_ID, builder.build())
    }

    fun notifyService(message: String) {
        val art = BitmapFactory.decodeResource(
            mContext.resources, R.drawable.ic_radio_station
        )
        // Build the style.
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0)
            .setMediaSession(mSessionToken)
        // Create/Retrieve Notification Channel for O and beyond devices (26+).
        mNotificationChannelFactory.createChannel(ServiceStartedNotificationData(mContext))
        val builder = NotificationCompat.Builder(
            mContext, ServiceStartedNotificationData.CHANNEL_ID
        )
        val smallIcon =
            if (AppUtils.hasVersionLollipop()) R.drawable.ic_notification else R.drawable.ic_notification_drawable
        builder
            .setContentIntent(makePendingIntent())
            .setStyle(mediaStyle)
            .setColor(mNotificationColor)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentTitle(mContext.getString(R.string.app_name))
            .setContentText(message)
            .setSmallIcon(smallIcon)
            .setLargeIcon(art)

        builder.addAction(getCloseAppAction())

        AppLogger.d("$CLASS_NAME show notification '$message'")
        mService.startForeground(NOTIFICATION_ID, builder.build())
    }

    /**
     * Gets the names and order of the actions to be included in the notification at the current player state.
     *
     * The playback and custom actions are combined and placed in the following order if not omitted:
     *
     * +---------------------------------------------------------------------------------+
     *
     * | prev | &lt;&lt; | play/pause | &gt;&gt; | next | custom actions | stop |
     *
     * +---------------------------------------------------------------------------------+
     *
     * The names must be of the playback actions
     * [ACTION_PAUSE], [ACTION_PLAY], [ACTION_NEXT] or [ACTION_PREV].
     * Otherwise the action name is ignored.
     */
    private fun getActions(
        enableNext: Boolean, enablePrevious: Boolean, playbackState: PlaybackStateCompat
    ): List<String> {
        val stringActions: MutableList<String> = ArrayList()
        if (enablePrevious) {
            stringActions.add(ACTION_PREV)
        }
        if (shouldShowPauseButton(playbackState)) {
            stringActions.add(ACTION_PAUSE)
        } else {
            stringActions.add(ACTION_PLAY)
        }
        if (enableNext) {
            stringActions.add(ACTION_NEXT)
        }
        stringActions.add(ACTION_CLOSE_APP)
        return stringActions
    }

    private fun getCloseAppAction(): NotificationCompat.Action {
        return NotificationCompat.Action(
            R.drawable.ic_close_app,
            mContext.getString(R.string.notif_close_app_label),
            mCloseAppIntent
        )
    }

    /**
     * Gets an array with the indices of the buttons to be shown in compact mode.
     *
     * The indices must refer to the list of actions passed as the first parameter.
     *
     * @param actionNames The names of the actions included in the notification.
     * @param playbackState State of the current playback.
     */
    private fun getActionIndicesForCompactView(
        actionNames: List<String>,
        playbackState: PlaybackStateCompat
    ): IntArray {
        val pauseActionIndex = actionNames.indexOf(ACTION_PAUSE)
        val playActionIndex = actionNames.indexOf(ACTION_PLAY)
        val skipPreviousActionIndex = if (mUseNavigationActionsInCompactView) actionNames.indexOf(ACTION_PREV) else -1
        val skipNextActionIndex = if (mUseNavigationActionsInCompactView) actionNames.indexOf(ACTION_NEXT) else -1
        val actionIndices = IntArray(4)
        var actionCounter = 0
        if (skipPreviousActionIndex != -1) {
            actionIndices[actionCounter++] = skipPreviousActionIndex
        }
        val shouldShowPauseButton = shouldShowPauseButton(playbackState)
        if (pauseActionIndex != -1 && shouldShowPauseButton) {
            actionIndices[actionCounter++] = pauseActionIndex
        } else if (playActionIndex != -1 && !shouldShowPauseButton) {
            actionIndices[actionCounter++] = playActionIndex
        }
        if (skipNextActionIndex != -1) {
            actionIndices[actionCounter++] = skipNextActionIndex
        }
        actionIndices[actionCounter++] = actionNames.indexOf(ACTION_CLOSE_APP)
        return actionIndices.copyOf(actionCounter)
    }

    private fun shouldShowPauseButton(playbackState: PlaybackStateCompat): Boolean {
        return playbackState.state != PlaybackStateCompat.STATE_PLAYING
    }

    private fun makePendingIntent(): PendingIntent? {

        // TODO: FIX ME
        val className = if (mService.isTv) "com.yuriy.openradio.tv.view.activity.TvMainActivity"
        else "com.yuriy.openradio.mobile.view.activity.MainActivity"

        val componentName = ComponentName(mContext, className)
        val intent = Intent()
        intent.component = componentName
        return PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun getPlayPauseAction(playbackState: PlaybackStateCompat): NotificationCompat.Action {
        val label: String
        val icon: Int
        val intent: PendingIntent
        if (playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
            label = mContext.getString(R.string.label_pause)
            icon = R.drawable.ic_pause
            intent = mPauseIntent
        } else {
            label = mContext.getString(R.string.label_play)
            icon = R.drawable.ic_play_arrow
            intent = mPlayIntent
        }
        return NotificationCompat.Action(icon, label, intent)
    }

    companion object {
        private val CLASS_NAME = MediaNotification::class.java.simpleName
        private const val NOTIFICATION_ID = 412
        private const val ACTION_PAUSE = "com.yuriy.openradio.pause"
        private const val ACTION_PLAY = "com.yuriy.openradio.play"
        private const val ACTION_PREV = "com.yuriy.openradio.prev"
        private const val ACTION_NEXT = "com.yuriy.openradio.next"
        const val ACTION_CLOSE_APP = "com.yuriy.openradio.close_app"
    }
}
