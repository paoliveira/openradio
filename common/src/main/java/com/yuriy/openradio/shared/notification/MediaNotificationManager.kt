/*
 * Copyright 2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.utils.AppUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * A wrapper class for ExoPlayer's PlayerNotificationManager. It sets up the notification shown to
 * the user during audio playback and provides track metadata, such as track title and icon image.
 */
class MediaNotificationManager(
    private val mContext: Context,
    sessionToken: MediaSessionCompat.Token,
    notificationListener: PlayerNotificationManager.NotificationListener,
    private val mListener:Listener
) {

    interface Listener {

        fun onCloseApp()
    }

    private val mServiceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mNotificationManager: PlayerNotificationManager

    init {
        val mediaController = MediaControllerCompat(mContext, sessionToken)
        val builder = PlayerNotificationManager.Builder(mContext, NOW_PLAYING_NOTIFICATION_ID, NOW_PLAYING_CHANNEL_ID)
        with(builder) {
            setMediaDescriptionAdapter(DescriptionAdapter(mediaController))
            setNotificationListener(notificationListener)
            setChannelNameResourceId(R.string.notification_channel)
            setChannelDescriptionResourceId(R.string.notification_channel_description)
            setCustomActionReceiver(ActionsReceiver())
        }
        mNotificationManager = builder.build()
        mNotificationManager.setMediaSessionToken(sessionToken)
        val smallIcon =
            if (AppUtils.hasVersionLollipop()) R.drawable.ic_notification else R.drawable.ic_notification_drawable
        mNotificationManager.setSmallIcon(smallIcon)
        mNotificationManager.setUseRewindAction(false)
        mNotificationManager.setUseFastForwardAction(false)
    }

    fun hideNotification() {
        mNotificationManager.setPlayer(null)
    }

    fun showNotificationForPlayer(player: Player) {
        mNotificationManager.setPlayer(player)
    }

    private inner class ActionsReceiver : PlayerNotificationManager.CustomActionReceiver {

        override fun createCustomActions(
            context: Context,
            instanceId: Int
        ): MutableMap<String, NotificationCompat.Action> {
            val map = HashMap<String, NotificationCompat.Action>()
            map[ACTION_CLOSE_APP] = getCloseAppAction()
            return map
        }

        override fun getCustomActions(player: Player): MutableList<String> {
            val list = ArrayList<String>()
            list.add(ACTION_CLOSE_APP)
            return list
        }

        override fun onCustomAction(player: Player, action: String, intent: Intent) {
            when (action) {
                ACTION_CLOSE_APP -> mListener.onCloseApp()
            }
        }

        private fun getCloseAppAction(): NotificationCompat.Action {
            return NotificationCompat.Action(
                R.drawable.ic_close_app,
                mContext.getString(R.string.notif_close_app_label),
                PendingIntent.getBroadcast(
                    mContext, 100,
                    Intent(ACTION_CLOSE_APP).setPackage(mContext.packageName),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
    }

    private inner class DescriptionAdapter(private val mController: MediaControllerCompat) :
        PlayerNotificationManager.MediaDescriptionAdapter {

        var mIconUri: Uri? = null
        var mBitmap: Bitmap? = null

        override fun createCurrentContentIntent(player: Player): PendingIntent? =
            mController.sessionActivity

        override fun getCurrentContentText(player: Player) =
            mController.metadata?.description?.subtitle.toString()

        override fun getCurrentContentTitle(player: Player) =
            mController.metadata?.description?.title.toString()

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            val iconUri = mController.metadata?.description?.iconUri
            return if (mIconUri != iconUri || mBitmap == null) {

                // Cache the bitmap for the current song so that successive calls to
                // `getCurrentLargeIcon` don't cause the bitmap to be recreated.
                mIconUri = iconUri
                mServiceScope.launch {
                    mBitmap = iconUri?.let {
                        // TODO: ?
                        //resolveUriAsBitmap(it)
                        null
                    }
                    mBitmap?.let { callback.onBitmap(it) }
                }
                null
            } else {
                mBitmap
            }
        }
    }

    companion object {
        private const val NOW_PLAYING_CHANNEL_ID = "com.yuriy.openradio.media.NOW_PLAYING"

        // Arbitrary number used to identify our notification
        private const val NOW_PLAYING_NOTIFICATION_ID = 0x0012

        private const val ACTION_CLOSE_APP = "com.yuriy.openradio.action.CLOSE_APP"
    }
}
