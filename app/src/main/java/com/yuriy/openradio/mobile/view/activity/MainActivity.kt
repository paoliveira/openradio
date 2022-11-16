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

package com.yuriy.openradio.mobile.view.activity

import android.annotation.SuppressLint
import android.app.assist.AssistContent
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.material.navigation.NavigationView
import com.yuriy.openradio.mobile.R
import com.yuriy.openradio.mobile.dependencies.DependencyRegistry
import com.yuriy.openradio.mobile.view.list.MobileMediaItemsAdapter
import com.yuriy.openradio.shared.broadcast.AppLocalReceiverCallback
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommon
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommonUi
import com.yuriy.openradio.shared.dependencies.MediaPresenterDependency
import com.yuriy.openradio.shared.model.media.MediaId
import com.yuriy.openradio.shared.presenter.MediaPresenter
import com.yuriy.openradio.shared.presenter.MediaPresenterListener
import com.yuriy.openradio.shared.utils.*
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.view.dialog.*
import com.yuriy.openradio.shared.view.list.MediaItemsAdapter
import com.yuriy.openradio.shared.vo.isInvalid

/**
 * Created with Android Studio.
 * Author: Chernyshov Yuriy - Mobile Development
 * Date: 19.12.14
 * Time: 15:13
 *
 * Main Activity class with represents the list of the categories: All, By Genre, Favorites, etc ...
 */
class MainActivity : AppCompatActivity(), MediaPresenterDependency {

    companion object {
        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME = MainActivity::class.java.simpleName
    }

    /**
     * Progress Bar view to indicate that data is loading.
     */
    private lateinit var mProgressBar: ProgressBar

    /**
     * Text View to display that data has not been loaded.
     */
    private lateinit var mNoDataView: TextView

    /**
     * Member field to keep reference to the Local broadcast receiver.
     */
    private val mLocalBroadcastReceiverCb: LocalBroadcastReceiverCallback

    private lateinit var mPlayBtn: View
    private lateinit var mPauseBtn: View
    private lateinit var mProgressBarCrs: ProgressBar
    private lateinit var mMediaPresenter: MediaPresenter
    private lateinit var mMainActivityPresenter: MainActivityPresenter
    private lateinit var mCastContext: CastContext
    private var mSavedInstanceState = Bundle()

    init {
        mLocalBroadcastReceiverCb = LocalBroadcastReceiverCallback()
    }

    fun configureWith(presenter: MainActivityPresenter) {
        mMainActivityPresenter = presenter
    }

    override fun configureWith(mediaPresenter: MediaPresenter) {
        mMediaPresenter = mediaPresenter
        // Register local receivers.
        mMediaPresenter.registerReceivers(mLocalBroadcastReceiverCb)
        val mediaItemsAdapter = MobileMediaItemsAdapter(this)
        val mediaSubscriptionCb = MediaBrowserSubscriptionCallback()
        val mediaPresenterImpl = MediaPresenterListenerImpl()
        mMediaPresenter.init(
            this, findView(R.id.main_layout), mSavedInstanceState, findViewById(R.id.list_view),
            findViewById(R.id.current_radio_station_view), mediaItemsAdapter,
            mediaSubscriptionCb, mediaPresenterImpl
        )
        mMediaPresenter.connect()
    }

    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)
        AppLogger.i("$CLASS_NAME assistant content $outContent")
    }

    override fun onProvideAssistData(data: Bundle?) {
        super.onProvideAssistData(data)
        AppLogger.i("$CLASS_NAME assistant data $data")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.d("$CLASS_NAME OnCreate:$savedInstanceState")

        DependencyRegistry.inject(this)

        if (savedInstanceState != null) {
            mSavedInstanceState = Bundle(savedInstanceState)
        }

        initUi(applicationContext)
        hideProgressBar()

        DependencyRegistryCommonUi.inject(this)

        // Initialize the Cast context. This is required so that the media route button can be
        // created in the AppBar.
        if (DependencyRegistryCommon.isCastAvailable) {
            mCastContext = CastContext.getSharedInstance(applicationContext)
        }
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        // Set up a MediaRouteButton to allow the user to control the current media playback route.
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.action_cast)
        return true
    }

    @SuppressLint("NonConstantResourceId")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mMediaPresenter.getOnSaveInstancePassed()) {
            return super.onOptionsItemSelected(item)
        }
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        val transaction = supportFragmentManager.beginTransaction()
        UiUtils.clearDialogs(this, transaction)
        return when (id) {
            R.id.action_search -> {
                // Show Search Dialog
                val dialog = BaseDialogFragment.newInstance(SearchDialog::class.java.name)
                dialog.show(transaction, SearchDialog.DIALOG_TAG)
                true
            }
            R.id.action_eq -> {
                // Show Equalizer Dialog
                val dialog = BaseDialogFragment.newInstance(EqualizerDialog::class.java.name)
                dialog.show(transaction, EqualizerDialog.DIALOG_TAG)
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        AppLogger.d("$CLASS_NAME OnSaveInstance:$outState")
        mMediaPresenter.handleSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        hideNoDataMessage()
        hideProgressBar()
        if (mMediaPresenter.handleBackPressed()) {
            // Perform Android's framework lifecycle.
            super.onBackPressed()
            // Indicate that the activity is finished.
            finish()
        }
    }

    /**
     * Initialize UI components.
     */
    private fun initUi(context: Context) {
        // Set content.
        setContentView(R.layout.main_drawer)
        mPlayBtn = findView(R.id.crs_play_btn_view)
        mPauseBtn = findView(R.id.crs_pause_btn_view)
        mProgressBarCrs = findViewById(R.id.crs_progress_view)
        // Initialize progress bar
        mProgressBar = findViewById(R.id.progress_bar_view)
        // Initialize No Data text view
        mNoDataView = findTextView(R.id.no_data_view)
        val toolbar = findToolbar(R.id.toolbar)
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val addBtn = findFloatingActionButton(R.id.add_station_btn)
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        navigationView.setNavigationItemSelectedListener { menuItem: MenuItem ->
            val transaction = supportFragmentManager.beginTransaction()
            UiUtils.clearDialogs(this, transaction)
            menuItem.isChecked = false
            // Handle navigation view item clicks here.
            when (menuItem.itemId) {
                R.id.nav_general -> {
                    // Show General Settings Dialog
                    val dialog = BaseDialogFragment.newInstance(GeneralSettingsDialog::class.java.name)
                    dialog.show(transaction, GeneralSettingsDialog.DIALOG_TAG)
                }
                R.id.nav_buffering -> {
                    // Show Stream Buffering Dialog
                    val dialog = BaseDialogFragment.newInstance(StreamBufferingDialog::class.java.name)
                    dialog.show(transaction, StreamBufferingDialog.DIALOG_TAG)
                }
                R.id.nav_sleep_timer -> {
                    // Show Sleep Timer Dialog
                    val dialog = BaseDialogFragment.newInstance(SleepTimerDialog::class.java.name)
                    dialog.show(transaction, SleepTimerDialog.DIALOG_TAG)
                }
                R.id.nav_google_drive -> {
                    // Show Google Drive Dialog
                    val dialog = BaseDialogFragment.newInstance(GoogleDriveDialog::class.java.name)
                    dialog.show(transaction, GoogleDriveDialog.DIALOG_TAG)
                }
                R.id.nav_about -> {
                    // Show About Dialog
                    val dialog = BaseDialogFragment.newInstance(AboutDialog::class.java.name)
                    dialog.show(transaction, AboutDialog.DIALOG_TAG)
                }
                R.id.nav_network -> {
                    // Show Network Dialog
                    val dialog = BaseDialogFragment.newInstance(NetworkDialog::class.java.name)
                    dialog.show(transaction, NetworkDialog.DIALOG_TAG)
                }
                else -> {
                    // No dialog found.
                }
            }
            drawer.closeDrawer(GravityCompat.START)
            true
        }
        val versionText = AppUtils.getApplicationVersion(context) + "." +
                AppUtils.getApplicationVersionCode(context)
        val versionView = navigationView.getHeaderView(0).findTextView(R.id.drawer_ver_code_view)
        versionView.text = versionText

        // Handle Add Radio Station button.
        addBtn.setOnClickListener {
            // Show Add Station Dialog
            val transaction = supportFragmentManager.beginTransaction()
            val dialog = BaseDialogFragment.newInstance(AddStationDialog::class.java.name)
            dialog.show(transaction, AddStationDialog.DIALOG_TAG)
        }
    }

    /**
     * Process call back from the Search Dialog.
     *
     * @param queryBundle Bundle with information to query for.
     */
    fun onSearchDialogClick(queryBundle: Bundle) {
        mMediaPresenter.unsubscribeFromItem(MediaId.MEDIA_ID_SEARCH_FROM_APP)
        mMediaPresenter.addMediaItemToStack(MediaId.MEDIA_ID_SEARCH_FROM_APP, queryBundle)
    }

    /**
     * Show progress bar.
     */
    private fun showProgressBar() {
        if (!this::mProgressBar.isInitialized) {
            return
        }
        mProgressBar.visible()
    }

    /**
     * Hide progress bar.
     */
    private fun hideProgressBar() {
        if (!this::mProgressBar.isInitialized) {
            return
        }
        mProgressBar.gone()
    }

    /**
     * Show "No data" text view.
     */
    private fun showNoDataMessage() {
        if (!this::mNoDataView.isInitialized) {
            return
        }
        mNoDataView.visible()
    }

    /**
     * Hide "No data" text view.
     */
    private fun hideNoDataMessage() {
        if (!this::mNoDataView.isInitialized) {
            return
        }
        mNoDataView.gone()
    }

    fun onRemoveRSClick(view: View) {
        mMediaPresenter.handleRemoveRadioStationMenu(view)
    }

    fun onEditRSClick(view: View) {
        mMediaPresenter.handleEditRadioStationMenu(view)
    }

    @MainThread
    private fun handlePlaybackStateChanged(state: PlaybackStateCompat) {
        when (state.state) {
            PlaybackStateCompat.STATE_BUFFERING, PlaybackStateCompat.STATE_PLAYING -> {
                mPlayBtn.gone()
                mPauseBtn.visible()
            }
            PlaybackStateCompat.STATE_NONE, PlaybackStateCompat.STATE_ERROR,
            PlaybackStateCompat.STATE_STOPPED, PlaybackStateCompat.STATE_PAUSED -> {
                mPlayBtn.visible()
                mPauseBtn.gone()
            }
            PlaybackStateCompat.STATE_CONNECTING, PlaybackStateCompat.STATE_FAST_FORWARDING,
            PlaybackStateCompat.STATE_REWINDING, PlaybackStateCompat.STATE_SKIPPING_TO_NEXT,
            PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS, PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM -> {
                //Empty
            }
        }
        mProgressBarCrs.gone()
        hideProgressBar()
    }

    /**
     * Handles event of Metadata updated.
     * Updates UI related to the currently playing Radio Station.
     *
     * @param metadata Metadata related to currently playing Radio Station.
     */
    private fun handleMetadataChanged(metadata: MediaMetadataCompat) {
        val radioStation = mMainActivityPresenter.getLastRadioStation()
        if (radioStation.isInvalid()) {
            // TODO: Improve this.
            return
        }
        val description = metadata.description
        val nameView = findTextView(R.id.crs_name_view)
        nameView.text = description.title
        mMediaPresenter.updateDescription(
            findTextView(R.id.crs_description_view), description
        )
        findProgressBar(R.id.crs_img_progress_view).gone()
        val imgView = findImageView(R.id.crs_img_view)
        // Show placeholder before load an image.
        imgView.setImageResource(R.drawable.ic_radio_station)
        MediaItemsAdapter.updateImage(applicationContext, description, imgView)
        MediaItemsAdapter.updateBitrateView(
            radioStation.mediaStream.getVariant(0).bitrate, findTextView(R.id.crs_bitrate_view), true
        )
        val favoriteCheckView = findCheckBox(R.id.crs_favorite_check_view)
        favoriteCheckView.buttonDrawable = AppCompatResources.getDrawable(this, R.drawable.src_favorite)
        favoriteCheckView.isChecked = false
        val mediaItem = mMainActivityPresenter.getLastMediaItem()
        MediaItemsAdapter.handleFavoriteAction(favoriteCheckView, description, mediaItem, applicationContext)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mMediaPresenter.handlePermissionsResult(permissions, grantResults)
    }

    /**
     * Callback receiver of the local application's event.
     */
    private inner class LocalBroadcastReceiverCallback : AppLocalReceiverCallback {

        override fun onCurrentIndexOnQueueChanged(index: Int) {
            mMediaPresenter.handleCurrentIndexOnQueueChanged(index)
        }
    }

    private inner class MediaBrowserSubscriptionCallback : MediaBrowserCompat.SubscriptionCallback() {

        override fun onChildrenLoaded(
            parentId: String, children: List<MediaBrowserCompat.MediaItem>,
            options: Bundle
        ) {
            AppLogger.i(
                "$CLASS_NAME children loaded:$parentId, children:${children.size}," +
                        " options:${IntentUtils.bundleToString(options)}"
            )
            if (mMediaPresenter.getOnSaveInstancePassed()) {
                AppLogger.w("$CLASS_NAME can not perform on children loaded after OnSaveInstanceState")
                return
            }
            hideProgressBar()
            val addBtn = findFloatingActionButton(R.id.add_station_btn)
            if (parentId == MediaId.MEDIA_ID_ROOT) {
                addBtn.visible()
            } else {
                addBtn.gone()
            }
            if (children.isEmpty()) {
                showNoDataMessage()
            }

            // No need to go on if indexed list ended with last item.
            if (PlayerUtils.isEndOfList(children)) {
                return
            }
            mMediaPresenter.handleChildrenLoaded(parentId, children)
        }

        override fun onError(id: String) {
            hideProgressBar()
            com.yuriy.openradio.shared.view.SafeToast.showAnyThread(
                this@MainActivity,
                this@MainActivity.getString(R.string.error_loading_media)
            )
        }
    }

    private inner class MediaPresenterListenerImpl : MediaPresenterListener {

        override fun showProgressBar() {
            this@MainActivity.showProgressBar()
        }

        override fun handleMetadataChanged(metadata: MediaMetadataCompat) {
            this@MainActivity.handleMetadataChanged(metadata)
        }

        override fun handlePlaybackStateChanged(state: PlaybackStateCompat) {
            this@MainActivity.handlePlaybackStateChanged(state)
        }
    }
}
