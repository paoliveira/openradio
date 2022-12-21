/*
 * Copyright 2019-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.MainThread
import androidx.fragment.app.FragmentActivity
import com.yuriy.openradio.shared.broadcast.AppLocalReceiverCallback
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommon
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommonUi
import com.yuriy.openradio.shared.dependencies.MediaPresenterDependency
import com.yuriy.openradio.shared.model.media.MediaId
import com.yuriy.openradio.shared.presenter.MediaPresenter
import com.yuriy.openradio.shared.presenter.MediaPresenterListener
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.IntentUtils
import com.yuriy.openradio.shared.utils.UiUtils
import com.yuriy.openradio.shared.utils.findImageView
import com.yuriy.openradio.shared.utils.findProgressBar
import com.yuriy.openradio.shared.utils.findTextView
import com.yuriy.openradio.shared.utils.findView
import com.yuriy.openradio.shared.utils.gone
import com.yuriy.openradio.shared.utils.visible
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.view.SafeToast
import com.yuriy.openradio.shared.view.dialog.AddStationDialog
import com.yuriy.openradio.shared.view.dialog.EqualizerDialog
import com.yuriy.openradio.shared.view.list.MediaItemsAdapter
import com.yuriy.openradio.shared.vo.getStreamBitrate
import com.yuriy.openradio.shared.vo.isInvalid
import com.yuriy.openradio.tv.R
import com.yuriy.openradio.tv.dependencies.DependencyRegistryTv
import com.yuriy.openradio.tv.view.dialog.TvSettingsDialog
import com.yuriy.openradio.tv.view.list.TvMediaItemsAdapter

/*
 * Main TV Activity class that loads main TV fragment.
 */
class TvMainActivity : FragmentActivity(), MediaPresenterDependency {
    /**
     * Progress Bar view to indicate that data is loading.
     */
    private lateinit var mProgressBar: ProgressBar

    private lateinit var mMediaPresenter: MediaPresenter
    private lateinit var mTvMainActivityPresenter: TvMainActivityPresenter
    private lateinit var mPlayBtn: View
    private lateinit var mPauseBtn: View
    private var mSavedInstanceState = Bundle()

    /**
     * Member field to keep reference to the Local broadcast receiver.
     */
    private val mLocalBroadcastReceiverCb = LocalBroadcastReceiverCallback()
    private lateinit var mLauncher: ActivityResultLauncher<Intent>

    fun configureWith(presenter: TvMainActivityPresenter) {
        mTvMainActivityPresenter = presenter
    }

    override fun configureWith(mediaPresenter: MediaPresenter) {
        mMediaPresenter = mediaPresenter
        // Register local receivers.
        mMediaPresenter.registerReceivers(mLocalBroadcastReceiverCb)
        val mediaItemsAdapter = TvMediaItemsAdapter(applicationContext)
        val mediaSubscriptionCb = MediaBrowserSubscriptionCallback()
        val mediaPresenterImpl = MediaPresenterListenerImpl()
        mMediaPresenter.init(
            this, findView(R.id.tv_main_layout), mSavedInstanceState, findViewById(R.id.tv_list_view),
            findViewById(R.id.tv_current_radio_station_view), mediaItemsAdapter,
            mediaSubscriptionCb, mediaPresenterImpl
        )
        mMediaPresenter.connect()
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            mSavedInstanceState = Bundle(savedInstanceState)
        }
        // Someone installed TV version on a phone ... need to prevent farther actions.
        if (!DependencyRegistryCommon.isTv) {
            SafeToast.showAnyThread(applicationContext, getString(R.string.tv_on_mobile_message))
            finish()
        }

        setContentView(R.layout.tv_main)
        setUpAddBtn()
        setUpSearchBtn()
        setUpSettingsBtn()
        setUpEqualizerBtn()

        DependencyRegistryTv.inject(this)
        DependencyRegistryCommonUi.inject(this)

        mProgressBar = findViewById(R.id.progress_bar_tv_view)
        mPlayBtn = findView(R.id.tv_crs_play_btn_view)
        mPauseBtn = findView(R.id.tv_crs_pause_btn_view)

        mLauncher = IntentUtils.registerForActivityResultIntrl(
            this, ::onActivityResultCallback
        )
    }

    override fun onResume() {
        super.onResume()
        mMediaPresenter.handleResume()
        hideProgressBar()
    }

    override fun onDestroy() {
        super.onDestroy()
        mMediaPresenter.destroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mMediaPresenter.handleSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mMediaPresenter.handlePermissionsResult(permissions, grantResults)
    }

    /**
     * Process call back from the Search Dialog.
     */
    private fun onActivityResultCallback(data: Intent?) {
        var bundle = Bundle()
        if (data != null && data.extras != null) {
            bundle = Bundle(data.extras)
        }
        mMediaPresenter.unsubscribeFromItem(MediaId.MEDIA_ID_SEARCH_FROM_APP)
        mMediaPresenter.addMediaItemToStack(MediaId.MEDIA_ID_SEARCH_FROM_APP, bundle)
    }

    /**
     * Show progress bar.
     */
    private fun showProgressBar() {
        if (this::mProgressBar.isInitialized) {
            mProgressBar.visible()
        }
    }

    /**
     * Hide progress bar.
     */
    private fun hideProgressBar() {
        if (this::mProgressBar.isInitialized) {
            mProgressBar.gone()
        }
    }

    override fun onBackPressed() {
        hideProgressBar()
        if (mMediaPresenter.handleBackPressed()) {
            // Perform Android's framework lifecycle.
            super.onBackPressed()
            // Indicate that the activity is finished.
            finish()
        }
    }

    @MainThread
    private fun handlePlaybackStateChanged(state: PlaybackStateCompat) {
        when (state.state) {
            PlaybackStateCompat.STATE_BUFFERING, PlaybackStateCompat.STATE_PLAYING -> {
                if (this::mPlayBtn.isInitialized) {
                    mPlayBtn.gone()
                }
                if (this::mPauseBtn.isInitialized) {
                    mPauseBtn.visible()
                }
            }
            PlaybackStateCompat.STATE_NONE, PlaybackStateCompat.STATE_ERROR,
            PlaybackStateCompat.STATE_STOPPED, PlaybackStateCompat.STATE_PAUSED -> {
                if (this::mPlayBtn.isInitialized) {
                    mPlayBtn.visible()
                }
                if (this::mPauseBtn.isInitialized) {
                    mPauseBtn.gone()
                }
            }
            PlaybackStateCompat.STATE_CONNECTING, PlaybackStateCompat.STATE_FAST_FORWARDING,
            PlaybackStateCompat.STATE_REWINDING, PlaybackStateCompat.STATE_SKIPPING_TO_NEXT,
            PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS, PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM -> {
                //Empty
            }
        }
        hideProgressBar()
    }

    private fun setUpSettingsBtn() {
        val button = findImageView(R.id.tv_settings_btn)
        button.setOnClickListener { showTvSettings() }
    }

    private fun setUpSearchBtn() {
        val button = findImageView(R.id.tv_search_btn)
        button.setOnClickListener {
            mLauncher.launch(TvSearchActivity.makeStartIntent(this))
        }
    }

    private fun setUpEqualizerBtn() {
        val button = findImageView(R.id.tv_eq_btn)
        button.setOnClickListener {
            // Show Equalizer Dialog
            val transaction = supportFragmentManager.beginTransaction()
            val dialog = BaseDialogFragment.newInstance(
                EqualizerDialog::class.java.name
            )
            dialog.show(transaction, EqualizerDialog.DIALOG_TAG)
        }
    }

    private fun setUpAddBtn() {
        val button = findImageView(R.id.tv_add_btn)
        button.setOnClickListener {
            // Show Add Station Dialog
            val transaction = supportFragmentManager.beginTransaction()
            val dialog = BaseDialogFragment.newInstance(
                AddStationDialog::class.java.name
            )
            dialog.show(transaction, AddStationDialog.DIALOG_TAG)
        }
    }

    private fun showTvSettings() {
        val transaction = supportFragmentManager.beginTransaction()
        UiUtils.clearDialogs(supportFragmentManager, transaction)
        // Show Settings Dialog
        val dialogFragment = BaseDialogFragment.newInstance(
            TvSettingsDialog::class.java.name
        )
        dialogFragment.show(transaction, TvSettingsDialog.DIALOG_TAG)
    }

    /**
     * Handles event of Metadata updated.
     * Updates UI related to the currently playing Radio Station.
     *
     * @param metadata Metadata related to currently playing Radio Station.
     */
    private fun handleMetadataChanged(metadata: MediaMetadataCompat) {
        val radioStation = mTvMainActivityPresenter.getLastRadioStation()
        if (radioStation.isInvalid()) {
            // TODO: Improve this.
            return
        }
        val description = metadata.description
        val nameView = findTextView(R.id.tv_crs_name_view)
        nameView.text = description.title
        mMediaPresenter.updateDescription(
            findTextView(R.id.tv_crs_description_view), description
        )
        findProgressBar(R.id.tv_crs_img_progress_view).gone()
        val imgView = findImageView(R.id.tv_crs_img_view)
        // Show placeholder before load an image.
        imgView.setImageResource(R.drawable.ic_radio_station)
        MediaItemsAdapter.updateImage(applicationContext, description, imgView)
        MediaItemsAdapter.updateBitrateView(
            radioStation.getStreamBitrate(),
            findTextView(R.id.tv_crs_bitrate_view), true
        )
    }

    private fun handleChildrenLoaded(
        parentId: String,
        children: List<MediaBrowserCompat.MediaItem>
    ) {
        if (mMediaPresenter.getOnSaveInstancePassed()) {
            AppLogger.w("$CLASS_NAME can not perform on children loaded after OnSaveInstanceState")
            return
        }
        hideProgressBar()
        mMediaPresenter.handleChildrenLoaded(parentId, children)
    }

    fun onRemoveRSClick(view: View) {
        mMediaPresenter.handleRemoveRadioStationMenu(view)
    }

    fun onEditRSClick(view: View) {
        mMediaPresenter.handleEditRadioStationMenu(view)
    }

    private inner class MediaBrowserSubscriptionCallback : MediaBrowserCompat.SubscriptionCallback() {

        override fun onChildrenLoaded(
            parentId: String,
            children: List<MediaBrowserCompat.MediaItem>,
            options: Bundle
        ) {
            handleChildrenLoaded(parentId, children)
        }

        override fun onChildrenLoaded(
            parentId: String,
            children: List<MediaBrowserCompat.MediaItem>
        ) {
            handleChildrenLoaded(parentId, children)
        }

        override fun onError(id: String) {
            SafeToast.showAnyThread(
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

    /**
     * Callback receiver of the local application's event.
     */
    private inner class LocalBroadcastReceiverCallback : AppLocalReceiverCallback {

        override fun onCurrentIndexOnQueueChanged(index: Int) {
            mMediaPresenter.handleCurrentIndexOnQueueChanged(index)
        }
    }

    companion object {
        private val CLASS_NAME = TvMainActivity::class.java.simpleName
    }
}
