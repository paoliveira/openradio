/*
 * Copyright 2019 The "Open Radio" Project. Author: Chernyshov Yuriy
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
package com.yuriy.openradio.tv.view.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.fragment.app.FragmentActivity
import com.yuriy.openradio.R
import com.yuriy.openradio.mobile.view.activity.MainActivity
import com.yuriy.openradio.shared.broadcast.AppLocalReceiverCallback
import com.yuriy.openradio.shared.model.storage.LatestRadioStationStorage
import com.yuriy.openradio.shared.presenter.MediaPresenter
import com.yuriy.openradio.shared.presenter.MediaPresenterListener
import com.yuriy.openradio.shared.service.LocationService
import com.yuriy.openradio.shared.utils.AppLogger.d
import com.yuriy.openradio.shared.utils.AppLogger.e
import com.yuriy.openradio.shared.utils.AppLogger.i
import com.yuriy.openradio.shared.utils.AppLogger.w
import com.yuriy.openradio.shared.utils.AppUtils.startActivityForResultSafe
import com.yuriy.openradio.shared.utils.MediaIdHelper
import com.yuriy.openradio.shared.utils.MediaItemHelper
import com.yuriy.openradio.shared.utils.UiUtils.clearDialogs
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.view.SafeToast.showAnyThread
import com.yuriy.openradio.shared.view.dialog.AddStationDialog
import com.yuriy.openradio.shared.view.dialog.BaseAddEditStationDialog
import com.yuriy.openradio.shared.view.dialog.EqualizerDialog
import com.yuriy.openradio.shared.view.dialog.GoogleDriveDialog
import com.yuriy.openradio.shared.view.dialog.LogsDialog
import com.yuriy.openradio.shared.view.list.MediaItemsAdapter
import com.yuriy.openradio.tv.view.dialog.TvSettingsDialog
import com.yuriy.openradio.tv.view.list.TvMediaItemsAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import java.util.concurrent.atomic.*
import javax.inject.Inject

/*
 * Main TV Activity class that loads main TV fragment.
 */
@AndroidEntryPoint
class TvMainActivity : FragmentActivity() {
    /**
     * Progress Bar view to indicate that data is loading.
     */
    private var mProgressBar: ProgressBar? = null

    @JvmField
    @Inject
    var mMediaPresenter: MediaPresenter? = null
    private val mListener: MediaItemsAdapter.Listener
    private var mPlayBtn: View? = null
    private var mPauseBtn: View? = null

    /**
     * Member field to keep reference to the Local broadcast receiver.
     */
    private val mLocalBroadcastReceiverCb: LocalBroadcastReceiverCallback
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tv_main)
        setUpAddBtn()
        setUpSearchBtn()
        setUpSettingsBtn()
        setUpEqualizerBtn()
        val context = applicationContext
        mProgressBar = findViewById(R.id.progress_bar_tv_view)
        mPlayBtn = findViewById(R.id.tv_crs_play_btn_view)
        mPauseBtn = findViewById(R.id.tv_crs_pause_btn_view)

        // Register local receivers.
        mMediaPresenter!!.registerReceivers(applicationContext, mLocalBroadcastReceiverCb)
        val subscriptionCb: MediaBrowserCompat.SubscriptionCallback = MediaBrowserSubscriptionCallback()
        val listener: MediaPresenterListener = MediaPresenterListenerImpl()
        mMediaPresenter!!.init(
                this, savedInstanceState, findViewById(R.id.tv_list_view),
                findViewById(R.id.tv_current_radio_station_view), TvMediaItemsAdapter(context), mListener,
                subscriptionCb, listener
        )
        mMediaPresenter!!.restoreState(savedInstanceState)
        mMediaPresenter!!.connect()
    }

    override fun onResume() {
        super.onResume()
        mMediaPresenter!!.handleResume()
        hideProgressBar()
        LocationService.checkCountry(this, findViewById(R.id.tv_main_layout))
    }

    override fun onDestroy() {
        super.onDestroy()
        mMediaPresenter!!.handleDestroy(applicationContext)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mMediaPresenter!!.handleSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        d(
                CLASS_NAME + " permissions:" + permissions.contentToString()
                        + ", results:" + grantResults.contentToString()
        )
        mMediaPresenter?.handlePermissionsResult(applicationContext, requestCode, permissions, grantResults)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        d(CLASS_NAME + "on activity result, rqst:" + requestCode + " rslt:" + resultCode)
        val gDriveDialog = GoogleDriveDialog.findDialog(supportFragmentManager)
        gDriveDialog?.onActivityResult(requestCode, resultCode, data)
        val logsDialog = LogsDialog.findDialog(supportFragmentManager)
        logsDialog?.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TvSearchActivity.SEARCH_TV_ACTIVITY_REQUEST_CODE) {
            onSearchDialogClick()
        }
        val addEditStationDialog = BaseAddEditStationDialog.findDialog(supportFragmentManager)
        addEditStationDialog?.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * Process call back from the Search Dialog.
     */
    private fun onSearchDialogClick() {
        if (mMediaPresenter == null) {
            return
        }
        mMediaPresenter!!.unsubscribeFromItem(MediaIdHelper.MEDIA_ID_SEARCH_FROM_APP)
        mMediaPresenter!!.addMediaItemToStack(MediaIdHelper.MEDIA_ID_SEARCH_FROM_APP)
    }

    /**
     * Show progress bar.
     */
    private fun showProgressBar() {
        if (mProgressBar == null) {
            return
        }
        mProgressBar!!.visibility = View.VISIBLE
    }

    /**
     * Hide progress bar.
     */
    private fun hideProgressBar() {
        if (mProgressBar == null) {
            return
        }
        mProgressBar!!.visibility = View.GONE
    }

    override fun onBackPressed() {
        hideProgressBar()
        if (mMediaPresenter!!.handleBackPressed(applicationContext)) {
            // perform android frameworks lifecycle
            super.onBackPressed()
        }
    }

    @MainThread
    private fun handlePlaybackStateChanged(state: PlaybackStateCompat) {
        when (state.state) {
            PlaybackStateCompat.STATE_PLAYING -> {
                mPlayBtn!!.visibility = View.GONE
                mPauseBtn!!.visibility = View.VISIBLE
            }
            PlaybackStateCompat.STATE_STOPPED, PlaybackStateCompat.STATE_PAUSED -> {
                mPlayBtn!!.visibility = View.VISIBLE
                mPauseBtn!!.visibility = View.GONE
            }
            PlaybackStateCompat.STATE_BUFFERING, PlaybackStateCompat.STATE_CONNECTING,
            PlaybackStateCompat.STATE_ERROR, PlaybackStateCompat.STATE_FAST_FORWARDING,
            PlaybackStateCompat.STATE_NONE, PlaybackStateCompat.STATE_REWINDING,
            PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS,
            PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM -> {
                //Empty
            }
        }
        hideProgressBar()
    }

    private fun setUpSettingsBtn() {
        val button = findViewById<ImageView>(R.id.tv_settings_btn) ?: return
        button.setOnClickListener { showTvSettings() }
    }

    private fun setUpSearchBtn() {
        val button = findViewById<ImageView>(R.id.tv_search_btn) ?: return
        button.setOnClickListener {
            startActivityForResultSafe(
                    this,
                    TvSearchActivity.makeStartIntent(this),
                    TvSearchActivity.SEARCH_TV_ACTIVITY_REQUEST_CODE
            )
        }
    }

    private fun setUpEqualizerBtn() {
        val button = findViewById<ImageView>(R.id.tv_eq_btn) ?: return
        button.setOnClickListener {
            // Show Equalizer Dialog
            val transaction = supportFragmentManager.beginTransaction()
            val dialog = BaseDialogFragment.newInstance(
                    EqualizerDialog::class.java.name
            )
            dialog!!.show(transaction, EqualizerDialog.DIALOG_TAG)
        }
    }

    private fun setUpAddBtn() {
        val button = findViewById<ImageView>(R.id.tv_add_btn) ?: return
        button.setOnClickListener {
            // Show Add Station Dialog
            val transaction = supportFragmentManager.beginTransaction()
            val dialog = BaseDialogFragment.newInstance(
                    AddStationDialog::class.java.name
            )
            dialog!!.show(transaction, AddStationDialog.DIALOG_TAG)
        }
    }

    private fun showTvSettings() {
        val transaction = supportFragmentManager.beginTransaction()
        clearDialogs(this, transaction)
        // Show Settings Dialog
        val dialogFragment = BaseDialogFragment.newInstance(
                TvSettingsDialog::class.java.name
        )
        dialogFragment!!.show(transaction, TvSettingsDialog.DIALOG_TAG)
    }

    /**
     * Handles event of Metadata updated.
     * Updates UI related to the currently playing Radio Station.
     *
     * @param metadata Metadata related to currently playing Radio Station.
     */
    private fun handleMetadataChanged(metadata: MediaMetadataCompat) {
        val context: Context = this
        val radioStation = LatestRadioStationStorage[context]
        if (radioStation == null) {
            e("Handle metadata changed, rs is null")
            return
        }
        val description = metadata.description
        val nameView = findViewById<TextView>(R.id.tv_crs_name_view)
        if (nameView != null) {
            nameView.text = description.title
        }
        val descriptionView = findViewById<TextView>(R.id.tv_crs_description_view)
        if (descriptionView != null) {
            descriptionView.text = MediaItemHelper.getDisplayDescription(description, getString(R.string.media_description_default))
        }
        val imgView = findViewById<ImageView>(R.id.tv_crs_img_view)
        // Show placeholder before load an image.
        imgView.setImageResource(R.drawable.ic_radio_station)
        MediaItemsAdapter.updateImage(description, imgView)
        MediaItemsAdapter.updateBitrateView(
                radioStation.mediaStream.getVariant(0)!!.bitrate,
                findViewById(R.id.tv_crs_bitrate_view),
                true
        )
        //        final CheckBox favoriteCheckView = findViewById(R.id.tv_crs_favorite_check_view);
//        if (favoriteCheckView != null) {
//            favoriteCheckView.setButtonDrawable(
//                    AppCompatResources.getDrawable(this, R.drawable.src_favorite)
//            );
//            favoriteCheckView.setChecked(false);
//            final MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
//                    MediaItemHelper.buildMediaDescriptionFromRadioStation(context, radioStation),
//                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
//            );
//            MediaItemHelper.updateFavoriteField(
//                    mediaItem,
//                    FavoritesStorage.isFavorite(radioStation, context)
//            );
//            MediaItemsAdapter.handleFavoriteAction(favoriteCheckView, description, mediaItem, context);
//        }
    }

    private fun handleChildrenLoaded(parentId: String,
                                     children: List<MediaBrowserCompat.MediaItem>) {
        if (mMediaPresenter!!.getOnSaveInstancePassed()) {
            w(CLASS_NAME + "Can not perform on children loaded after OnSaveInstanceState")
            return
        }
        hideProgressBar()
        if (mMediaPresenter != null) {
            mMediaPresenter!!.handleChildrenLoaded(parentId, children)
        }
    }

    /**
     * Updates root view is there was changes in collection.
     * Should be call only if current media id is [MediaIdHelper.MEDIA_ID_ROOT].
     */
    private fun updateRootView() {
        mMediaPresenter!!.unsubscribeFromItem(MediaIdHelper.MEDIA_ID_ROOT)
        mMediaPresenter!!.addMediaItemToStack(MediaIdHelper.MEDIA_ID_ROOT)
    }

    private inner class MediaBrowserSubscriptionCallback : MediaBrowserCompat.SubscriptionCallback() {
        @SuppressLint("RestrictedApi")
        override fun onChildrenLoaded(parentId: String,
                                      children: List<MediaBrowserCompat.MediaItem>) {
            i(
                    CLASS_NAME + " Children loaded:" + parentId + ", children:" + children.size
            )
            handleChildrenLoaded(parentId, children)
        }

        override fun onError(id: String) {
            showAnyThread(
                    applicationContext,
                    getString(R.string.error_loading_media)
            )
        }
    }

    private inner class MediaPresenterListenerImpl : MediaPresenterListener {
        override fun showProgressBar() {
            this@TvMainActivity.showProgressBar()
        }

        override fun handleMetadataChanged(metadata: MediaMetadataCompat) {
            this@TvMainActivity.handleMetadataChanged(metadata)
        }

        override fun handlePlaybackStateChanged(state: PlaybackStateCompat) {
            this@TvMainActivity.handlePlaybackStateChanged(state)
        }
    }

    private inner class TvMediaItemsAdapterListenerImpl : MediaItemsAdapter.Listener {
        override fun onItemSettings(item: MediaBrowserCompat.MediaItem, position: Int) {
            //TODO:
        }

        override fun onItemSelected(item: MediaBrowserCompat.MediaItem, position: Int) {
            if (mMediaPresenter == null) {
                return
            }
            mMediaPresenter!!.setActiveItem(position)
            mMediaPresenter!!.handleItemClick(item, position)
        }
    }

    /**
     * Callback receiver of the local application's event.
     */
    private inner class LocalBroadcastReceiverCallback : AppLocalReceiverCallback {

        override fun onLocationChanged() {
            if (mMediaPresenter == null) {
                return
            }
            if (mMediaPresenter!!.getOnSaveInstancePassed()) {
                w(CLASS_NAME + "Can not do Location Changed after OnSaveInstanceState")
                return
            }
            if (MediaIdHelper.MEDIA_ID_ROOT == mMediaPresenter!!.currentParentId) {
                LocationService.checkCountry(
                        this@TvMainActivity, this@TvMainActivity.findViewById(R.id.tv_main_layout)
                )
                updateRootView()
            }
        }

        override fun onCurrentIndexOnQueueChanged(index: Int, mediaId: String?) {
            if (mMediaPresenter != null) {
                mMediaPresenter!!.handleCurrentIndexOnQueueChanged(mediaId)
            }
        }

        override fun onSleepTimer() {
            hideProgressBar()
            mMediaPresenter?.clearMediaItems()
            finish()
        }

        override fun onSortIdChanged(sortId: Int) {
            mMediaPresenter?.setActiveItem(sortId)
        }

        override fun onGoogleDriveDownloaded() {
            if (mMediaPresenter == null) {
                return
            }
            if (mMediaPresenter!!.getOnSaveInstancePassed()) {
                w(CLASS_NAME + "Can not do GoogleDriveDownloaded after OnSaveInstanceState")
                return
            }
            if (MediaIdHelper.MEDIA_ID_ROOT == mMediaPresenter!!.currentParentId) {
                updateRootView()
            }
        }
    }

    companion object {
        private val CLASS_NAME = TvMainActivity::class.java.simpleName + " "
    }

    init {
        mListener = TvMediaItemsAdapterListenerImpl()
        mLocalBroadcastReceiverCb = LocalBroadcastReceiverCallback()
    }
}
