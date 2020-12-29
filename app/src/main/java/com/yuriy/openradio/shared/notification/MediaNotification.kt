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
package com.yuriy.openradio.shared.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
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
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yuriy.openradio.R
import com.yuriy.openradio.mobile.view.activity.MainActivity
import com.yuriy.openradio.shared.service.OpenRadioService
import com.yuriy.openradio.shared.utils.AnalyticsUtils.logException
import com.yuriy.openradio.shared.utils.AppLogger.d
import com.yuriy.openradio.shared.utils.AppLogger.e
import com.yuriy.openradio.shared.utils.AppUtils.hasVersionLollipop
import com.yuriy.openradio.shared.utils.MediaItemHelper.metadataFromRadioStation
import com.yuriy.openradio.shared.vo.RadioStation
import com.yuriy.openradio.tv.view.activity.TvMainActivity
import java.util.concurrent.atomic.*


/**
 * Keeps track of a notification and updates it automatically for a given
 * MediaSession. Maintaining a visible notification (usually) guarantees that the music service
 * won't be killed during playback.
 */
class MediaNotification(service: OpenRadioService) : BroadcastReceiver() {
    private val mService: OpenRadioService
    private var mSessionToken: MediaSessionCompat.Token? = null
    private var mController: MediaControllerCompat? = null
    private var mTransportControls: MediaControllerCompat.TransportControls? = null
    private var mMetadata: MediaMetadataCompat? = null
    private val mCb: MediaControllerCompatCallback
    private val mNotificationManager: NotificationManagerCompat
    private val mPauseIntent: PendingIntent
    private val mPlayIntent: PendingIntent
    private val mPreviousIntent: PendingIntent
    private val mNextIntent: PendingIntent
    private val mNotificationColor: Int
    private val mStarted = AtomicBoolean(false)
    private val mNotificationChannelFactory: NotificationChannelFactory
    private val notificationColor: Int
        get() {
            var notificationColor = 0
            val packageName = mService.packageName
            try {
                val packageContext = mService.createPackageContext(packageName, 0)
                val applicationInfo = mService.packageManager.getApplicationInfo(packageName, 0)
                packageContext.setTheme(applicationInfo.theme)
                val theme = packageContext.theme
                if (hasVersionLollipop()) {
                    val ta = theme.obtainStyledAttributes(intArrayOf(R.attr.colorPrimary))
                    notificationColor = ta.getColor(0, Color.DKGRAY)
                    ta.recycle()
                } else {
                    notificationColor = -0xbbbbbc
                }
            } catch (e: Exception) {
                logException(e)
            }
            return notificationColor
        }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before [.stopNotification] is called.
     */
    fun startNotification(context: Context?, radioStation: RadioStation?) {
        if (mStarted.get()) {
            return
        }
        mController!!.registerCallback(mCb)
        val filter = IntentFilter()
        filter.addAction(ACTION_NEXT)
        filter.addAction(ACTION_PAUSE)
        filter.addAction(ACTION_PLAY)
        filter.addAction(ACTION_PREV)
        mService.registerReceiver(this, filter)
        val metadata = mController!!.metadata
        mMetadata = metadata ?: metadataFromRadioStation(context, radioStation)
        mStarted.set(true)
        // The notification must be updated after setting started to true
        handleNotification(mController!!.playbackState)
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    fun stopNotification() {
        d(CLASS_NAME + " stop, ORS[" + mService.hashCode() + "]")
        mStarted.set(false)
        mController!!.unregisterCallback(mCb)
        mNotificationManager.cancelAll()
        try {
            mService.unregisterReceiver(this)
        } catch (ex: IllegalArgumentException) {
            e("$CLASS_NAME error while unregister:$ex")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        d("$CLASS_NAME Received intent with action $action")
        if (TextUtils.isEmpty(action)) {
            return
        }
        when (action) {
            ACTION_PAUSE -> mTransportControls!!.pause()
            ACTION_PLAY -> mTransportControls!!.play()
            ACTION_NEXT -> mTransportControls!!.skipToNext()
            ACTION_PREV -> mTransportControls!!.skipToPrevious()
        }
    }

    /**
     * Update the state based on a change on the session token. Called either when
     * we are running for the first time or when the media session owner has destroyed the session
     * (see [android.media.session.MediaController.Callback.onSessionDestroyed])
     */
    private fun updateSessionToken() {
        val freshToken = mService.sessionToken
        if (mSessionToken == null || mSessionToken != freshToken) {
            if (mController != null) {
                mController!!.unregisterCallback(mCb)
            }
            mSessionToken = freshToken
            mController = try {
                MediaControllerCompat(mService, mSessionToken!!)
            } catch (e: RemoteException) {
                logException(e)
                return
            }
            mTransportControls = mController!!.transportControls
            if (mStarted.get()) {
                mController!!.registerCallback(mCb)
            }
        }
    }

    private inner class MediaControllerCompatCallback: MediaControllerCompat.Callback() {
        private var mPlaybackState: PlaybackStateCompat? = null
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            d("$CLASS_NAME Received new playback state:$state")
            val doNotify: Boolean = if (mPlaybackState == null) {
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
            d("$CLASS_NAME Received new metadata:$metadata")
            mMetadata = metadata
            handleNotification(mPlaybackState)
        }

        override fun onSessionDestroyed() {
            d("$CLASS_NAME Session was destroyed, resetting to the new session token")
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
                mService, MediaNotificationData.CHANNEL_ID
        )

        // Create/Retrieve Notification Channel for O and beyond devices (26+).
        mNotificationChannelFactory.createChannel(MediaNotificationData(mService, mMetadata!!))
        var playPauseActionIndex = 0
        // If skip to previous action is enabled
        if (playbackState.actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L) {
            builder.addAction(
                    R.drawable.ic_skip_prev,
                    mService.getString(R.string.label_previous),
                    mPreviousIntent
            )
            playPauseActionIndex = 1
        }

        // Build the style.
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle() // only show play/pause in compact view
                .setShowActionsInCompactView(playPauseActionIndex)
                .setMediaSession(mSessionToken)
        val description = mMetadata!!.description
        var art = description.iconBitmap
        if (art == null) {
            // use a placeholder art while the remote art is being downloaded
            art = BitmapFactory.decodeResource(mService.resources, R.drawable.ic_radio_station)
        }
        // TODO:
        //if (art == null && description.iconUri != null) {
            // This sample assumes the iconUri will be a valid URL formatted String, but
            // it can actually be any valid Android Uri formatted String.
            // async fetch the album art icon
            // val artUrl = UrlBuilder.preProcessIconUrl(description.iconUri.toString())
        //}

        // If skip to next action is enabled
        if (playbackState.actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L) {
            builder.addAction(
                    R.drawable.ic_skip_next,
                    mService.getString(R.string.label_next),
                    mNextIntent
            )
        }
        val smallIcon = if (hasVersionLollipop()) R.drawable.ic_notification else R.drawable.ic_notification_drawable
        builder
                .setContentIntent(makePendingIntent())
                .addAction(getPlayPauseAction(playbackState))
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
        d(
                CLASS_NAME + " Update Notification " +
                        "state:" + playbackState +
                        "title:" + description.title +
                        "subtitle:" + description.subtitle
        )
        d(CLASS_NAME + " update, ORS[" + mService.hashCode() + "]")
        //        mNotificationChannelFactory.updateChannel(NOTIFICATION_ID, builder.build());
        mService.startForeground(NOTIFICATION_ID, builder.build())
        // TODO: Fetch and update Notification.
//        if (fetchArtUrl != null && !BitmapUtils.isUrlLocalResource(fetchArtUrl)) {
//            if (cacheObject == null) {
//                fetchBitmapFromURLAsync(fetchArtUrl);
//            }
//        }
    }

    fun notifyService(message: String?) {
        val art = BitmapFactory.decodeResource(
                mService.resources, R.drawable.ic_radio_station
        )
        // Build the style.
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mSessionToken)
        // Create/Retrieve Notification Channel for O and beyond devices (26+).
        mNotificationChannelFactory.createChannel(ServiceStartedNotificationData(mService))
        val builder = NotificationCompat.Builder(
                mService, ServiceStartedNotificationData.CHANNEL_ID
        )
        val smallIcon = if (hasVersionLollipop()) R.drawable.ic_notification else R.drawable.ic_notification_drawable
        builder
                .setContentIntent(makePendingIntent())
                .setStyle(mediaStyle)
                .setColor(mNotificationColor)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(mService.getString(com.yuriy.openradio.R.string.app_name))
                .setContentText(message)
                .setSmallIcon(smallIcon)
                .setLargeIcon(art)
        d(CLASS_NAME + " show Just Started notification ORS[" + mService.hashCode() + "]")
        mService.startForeground(NOTIFICATION_ID, builder.build())
    }

    private fun makePendingIntent(): PendingIntent? {
        return PendingIntent.getActivity(
                mService, 0,
                Intent(mService, if (mService.isTv) TvMainActivity::class.java else MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getPlayPauseAction(playbackState: PlaybackStateCompat): NotificationCompat.Action {
        val label: String
        val icon: Int
        val intent: PendingIntent
        if (playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
            label = mService.getString(R.string.label_pause)
            icon = R.drawable.ic_pause
            intent = mPauseIntent
        } else {
            label = mService.getString(R.string.label_play)
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
        private const val MAX_ALBUM_ART_CACHE_SIZE = 1024 * 1024
    }

    init {
        mCb = MediaControllerCompatCallback()
        mService = service
        mNotificationChannelFactory = NotificationChannelFactory(mService)
        updateSessionToken()

        mNotificationColor = notificationColor
        mNotificationManager = NotificationManagerCompat.from(mService)
        val pkg = mService.packageName
        mPauseIntent = PendingIntent.getBroadcast(mService, 100,
                Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)
        mPlayIntent = PendingIntent.getBroadcast(mService, 100,
                Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)
        mPreviousIntent = PendingIntent.getBroadcast(mService, 100,
                Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)
        mNextIntent = PendingIntent.getBroadcast(mService, 100,
                Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)
    }
}
