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
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.yuriy.openradio.mobile.R
import com.yuriy.openradio.mobile.view.list.MobileMediaItemsAdapter
import com.yuriy.openradio.shared.broadcast.AppLocalReceiverCallback
import com.yuriy.openradio.shared.model.storage.FavoritesStorage
import com.yuriy.openradio.shared.model.storage.LatestRadioStationStorage
import com.yuriy.openradio.shared.presenter.MediaPresenter
import com.yuriy.openradio.shared.presenter.MediaPresenterListener
import com.yuriy.openradio.shared.service.LocationService
import com.yuriy.openradio.shared.service.OpenRadioService
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.IntentUtils
import com.yuriy.openradio.shared.utils.MediaIdHelper
import com.yuriy.openradio.shared.utils.MediaItemHelper
import com.yuriy.openradio.shared.utils.UiUtils
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.view.dialog.AboutDialog
import com.yuriy.openradio.shared.view.dialog.AddStationDialog
import com.yuriy.openradio.shared.view.dialog.BaseAddEditStationDialog
import com.yuriy.openradio.shared.view.dialog.EditStationDialog
import com.yuriy.openradio.shared.view.dialog.EqualizerDialog
import com.yuriy.openradio.shared.view.dialog.GeneralSettingsDialog
import com.yuriy.openradio.shared.view.dialog.GoogleDriveDialog
import com.yuriy.openradio.shared.view.dialog.LogsDialog
import com.yuriy.openradio.shared.view.dialog.NetworkDialog
import com.yuriy.openradio.shared.view.dialog.RSSettingsDialog
import com.yuriy.openradio.shared.view.dialog.RemoveStationDialog
import com.yuriy.openradio.shared.view.dialog.SearchDialog
import com.yuriy.openradio.shared.view.dialog.SleepTimerDialog
import com.yuriy.openradio.shared.view.dialog.StreamBufferingDialog
import com.yuriy.openradio.shared.view.list.MediaItemsAdapter
import com.yuriy.openradio.shared.vo.RadioStation
import com.yuriy.openradio.shared.vo.RadioStationToAdd
import java.util.*
import java.util.concurrent.atomic.*

/**
 * Created with Android Studio.
 * Author: Chernyshov Yuriy - Mobile Development
 * Date: 19.12.14
 * Time: 15:13
 *
 * Main Activity class with represents the list of the categories: All, By Genre, Favorites, etc ...
 */
class MainActivity : AppCompatActivity() {

    companion object {
        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME: String = MainActivity::class.java.simpleName + " "
    }

    /**
     * Progress Bar view to indicate that data is loading.
     */
    private var mProgressBar: ProgressBar? = null

    /**
     * Text View to display that data has not been loaded.
     */
    private var mNoDataView: TextView? = null

    /**
     * Member field to keep reference to the Local broadcast receiver.
     */
    private val mLocalBroadcastReceiverCb: LocalBroadcastReceiverCallback

    /**
     * Listener for the List view click event.
     */
    private val mMediaItemListener: MediaItemsAdapter.Listener

    private var mPlayBtn: View? = null
    private var mPauseBtn: View? = null
    private var mProgressBarCrs: ProgressBar? = null

    private lateinit var mMediaPresenter: MediaPresenter

    init {
        mLocalBroadcastReceiverCb = LocalBroadcastReceiverCallback()
        mMediaItemListener = MediaItemListenerImpl()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.d("$CLASS_NAME OnCreate:$savedInstanceState")

        mMediaPresenter = MediaPresenter.getInstance(applicationContext)

        initUi(applicationContext)
        hideProgressBar()

        // Register local receivers.
        mMediaPresenter.registerReceivers(applicationContext, mLocalBroadcastReceiverCb)
        val medSubscriptionCb = MediaBrowserSubscriptionCallback()
        val mediaPresenterLstnr = MediaPresenterListenerImpl()
        mMediaPresenter.init(
                this, savedInstanceState, findViewById(R.id.list_view),
                findViewById(R.id.current_radio_station_view), MobileMediaItemsAdapter(this),
                mMediaItemListener, medSubscriptionCb, mediaPresenterLstnr
        )
        mMediaPresenter.restoreState(savedInstanceState)
        mMediaPresenter.connect()
    }

    override fun onResume() {
        super.onResume()
        AppLogger.i("$CLASS_NAME OnResume")
        mMediaPresenter.handleResume()
        hideProgressBar()
        LocationService.checkCountry(this, findViewById(R.id.main_layout))
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.i("$CLASS_NAME OnDestroy")
        mMediaPresenter.handleDestroy(applicationContext)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    @SuppressLint("NonConstantResourceId")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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

                val bundle = SearchDialog.makeNewInstanceBundle(
                        object : SearchDialog.Listener {

                            override fun onSuccess(queryBundle: Bundle) {
                                onSearchDialogClick(queryBundle)
                            }
                        }
                )
                // Show Search Dialog
                val dialog = BaseDialogFragment.newInstance(SearchDialog::class.java.name, bundle)
                dialog?.show(transaction, SearchDialog.DIALOG_TAG)
                true
            }
            R.id.action_eq -> {

                // Show Equalizer Dialog
                val dialog = BaseDialogFragment.newInstance(EqualizerDialog::class.java.name)
                dialog?.show(transaction, EqualizerDialog.DIALOG_TAG)
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
        if (mMediaPresenter.handleBackPressed(applicationContext)) {
            // perform android frameworks lifecycle
            super.onBackPressed()
        }
    }

    /**
     * Initialize UI components.
     */
    private fun initUi(context: Context) {
        // Set content.
        setContentView(R.layout.main_drawer)
        mPlayBtn = findViewById(R.id.crs_play_btn_view)
        mPauseBtn = findViewById(R.id.crs_pause_btn_view)
        mProgressBarCrs = findViewById(R.id.crs_progress_view)
        // Initialize progress bar
        mProgressBar = findViewById(R.id.progress_bar_view)
        // Initialize No Data text view
        mNoDataView = findViewById(R.id.no_data_view)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val addBtn = findViewById<FloatingActionButton>(R.id.add_station_btn)
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
                    // Show Search Dialog
                    val dialog = BaseDialogFragment.newInstance(GeneralSettingsDialog::class.java.name)
                    dialog!!.show(transaction, GeneralSettingsDialog.DIALOG_TAG)
                }
                R.id.nav_buffering -> {
                    // Show Stream Buffering Dialog
                    val dialog = BaseDialogFragment.newInstance(StreamBufferingDialog::class.java.name)
                    dialog!!.show(transaction, StreamBufferingDialog.DIALOG_TAG)
                }
                R.id.nav_sleep_timer -> {
                    // Show Sleep Timer Dialog
                    val dialog = BaseDialogFragment.newInstance(SleepTimerDialog::class.java.name)
                    dialog!!.show(transaction, SleepTimerDialog.DIALOG_TAG)
                }
                R.id.nav_google_drive -> {
                    // Show Google Drive Dialog
                    val dialog = BaseDialogFragment.newInstance(GoogleDriveDialog::class.java.name)
                    dialog!!.show(transaction, GoogleDriveDialog.DIALOG_TAG)
                }
                R.id.nav_logs -> {
                    // Show Application Logs Dialog
                    val dialog = BaseDialogFragment.newInstance(LogsDialog::class.java.name)
                    dialog!!.show(transaction, LogsDialog.DIALOG_TAG)
                }
                R.id.nav_about -> {
                    // Show About Dialog
                    val dialog = BaseDialogFragment.newInstance(AboutDialog::class.java.name)
                    dialog!!.show(transaction, AboutDialog.DIALOG_TAG)
                }
                R.id.nav_network -> {
                    // Show Network Dialog
                    val dialog = BaseDialogFragment.newInstance(NetworkDialog::class.java.name)
                    dialog!!.show(transaction, NetworkDialog.DIALOG_TAG)
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
        val versionView = navigationView.getHeaderView(0).findViewById<TextView>(
                R.id.drawer_ver_code_view
        )
        versionView.text = versionText

        // Handle Add Radio Station button.
        addBtn.setOnClickListener {
            // Show Add Station Dialog
            val transaction = supportFragmentManager.beginTransaction()
            val dialog = BaseDialogFragment.newInstance(AddStationDialog::class.java.name)
            dialog!!.show(transaction, AddStationDialog.DIALOG_TAG)
        }
    }

    /**
     * Process user's input in order to edit custom [RadioStation].
     */
    fun processEditStationCallback(mediaId: String?, radioStationToAdd: RadioStationToAdd?) {
        startService(OpenRadioService.makeEditRadioStationIntent(
                this, mediaId, radioStationToAdd!!
        ))
    }

    /**
     * Process user's input in order to remove custom [RadioStation].
     */
    fun processRemoveStationCallback(mediaId: String?) {
        startService(OpenRadioService.makeRemoveRadioStationIntent(this, mediaId))
    }

    /**
     * Process call back from the Search Dialog.
     *
     * @param queryBundle Bundle with information to query for.
     */
    fun onSearchDialogClick(queryBundle: Bundle) {
        unsubscribeFromItem(MediaIdHelper.MEDIA_ID_SEARCH_FROM_APP)
        mMediaPresenter.addMediaItemToStack(MediaIdHelper.MEDIA_ID_SEARCH_FROM_APP, queryBundle)
    }

    /**
     * Remove provided Media Id from the collection. Reconnect [MediaBrowserCompat].
     *
     * @param mediaItemId Media Id.
     */
    private fun unsubscribeFromItem(mediaItemId: String) {
        hideNoDataMessage()
        hideProgressBar()
        mMediaPresenter.unsubscribeFromItem(mediaItemId)
    }

    /**
     * Updates root view is there was changes in collection.
     * Should be call only if current media id is [MediaIdHelper.MEDIA_ID_ROOT].
     */
    private fun updateRootView() {
        unsubscribeFromItem(MediaIdHelper.MEDIA_ID_ROOT)
        mMediaPresenter.addMediaItemToStack(MediaIdHelper.MEDIA_ID_ROOT)
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

    /**
     * Show "No data" text view.
     */
    private fun showNoDataMessage() {
        if (mNoDataView == null) {
            return
        }
        mNoDataView!!.visibility = View.VISIBLE
    }

    /**
     * Hide "No data" text view.
     */
    private fun hideNoDataMessage() {
        if (mNoDataView == null) {
            return
        }
        mNoDataView!!.visibility = View.GONE
    }

    fun onRemoveRSClick(view: View) {
        val item = view.tag as MediaBrowserCompat.MediaItem
        handleRemoveRadioStationMenu(item)
    }

    /**
     * Handles action of the Radio Station deletion.
     *
     * @param item Media item related to the Radio Station to be deleted.
     */
    private fun handleRemoveRadioStationMenu(item: MediaBrowserCompat.MediaItem) {
        var name = AppUtils.EMPTY_STRING
        if (item.description.title != null) {
            name = item.description.title.toString()
        }
        if (mMediaPresenter.getOnSaveInstancePassed()) {
            AppLogger.w(CLASS_NAME + "Can not show Dialog after OnSaveInstanceState")
            return
        }
        val transaction = supportFragmentManager.beginTransaction()
        UiUtils.clearDialogs(this, transaction)

        // Show Remove Station Dialog
        val bundle = RemoveStationDialog.makeBundle(
                item.mediaId,
                name,
                object : RemoveStationDialog.Listener {

                    override fun onSuccess(mediaId: String?) {
                        processRemoveStationCallback(mediaId)
                    }
                }
        )
        val dialog = BaseDialogFragment.newInstance(RemoveStationDialog::class.java.name, bundle)
        dialog?.show(transaction, RemoveStationDialog.DIALOG_TAG)
    }

    fun onEditRSClick(view: View) {
        val item = view.tag as MediaBrowserCompat.MediaItem
        handleEditRadioStationMenu(item)
    }

    /**
     * Handles edit of the Radio Station action.
     *
     * @param item Media item related to the Radio Station to be edited.
     */
    private fun handleEditRadioStationMenu(item: MediaBrowserCompat.MediaItem) {
        if (mMediaPresenter.getOnSaveInstancePassed()) {
            AppLogger.w(CLASS_NAME + "Can not show Dialog after OnSaveInstanceState")
            return
        }
        val transaction = supportFragmentManager.beginTransaction()
        UiUtils.clearDialogs(this, transaction)

        val bundle = EditStationDialog.makeBundle(
                item.mediaId,
                object : EditStationDialog.Listener {

                    override fun onSuccess(mediaId: String?, radioStation: RadioStationToAdd?) {
                        processEditStationCallback(mediaId, radioStation)
                    }
                }
        )
        // Show Edit Station Dialog
        val dialog = BaseDialogFragment.newInstance(EditStationDialog::class.java.name, bundle)
        dialog!!.show(transaction, EditStationDialog.DIALOG_TAG)
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
            PlaybackStateCompat.STATE_ERROR,
            PlaybackStateCompat.STATE_BUFFERING, PlaybackStateCompat.STATE_CONNECTING,
            PlaybackStateCompat.STATE_FAST_FORWARDING, PlaybackStateCompat.STATE_NONE,
            PlaybackStateCompat.STATE_REWINDING, PlaybackStateCompat.STATE_SKIPPING_TO_NEXT,
            PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS, PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM -> {
                //Empty
            }
        }
        mProgressBarCrs!!.visibility = View.GONE
        hideProgressBar()
    }

    /**
     * Handles event of Metadata updated.
     * Updates UI related to the currently playing Radio Station.
     *
     * @param metadata Metadata related to currently playing Radio Station.
     */
    private fun handleMetadataChanged(metadata: MediaMetadataCompat) {
        val context: Context = this
        val radioStation = LatestRadioStationStorage[context] ?: return
        val description = metadata.description
        val nameView = findViewById<TextView>(R.id.crs_name_view)
        if (nameView != null) {
            nameView.text = description.title
        }
        mMediaPresenter.updateDescription(
                applicationContext, findViewById(R.id.crs_description_view), description
        )
        findViewById<ProgressBar>(R.id.crs_img_progress_view)?.visibility = View.GONE
        val imgView = findViewById<ImageView>(R.id.crs_img_view)
        // Show placeholder before load an image.
        imgView.setImageResource(R.drawable.ic_radio_station)
        MediaItemsAdapter.updateImage(description, imgView)
        MediaItemsAdapter.updateBitrateView(
                radioStation.mediaStream.getVariant(0)!!.bitrate,
                findViewById(R.id.crs_bitrate_view),
                true
        )
        val favoriteCheckView = findViewById<CheckBox>(R.id.crs_favorite_check_view)
        if (favoriteCheckView != null) {
            favoriteCheckView.buttonDrawable = AppCompatResources.getDrawable(this, R.drawable.src_favorite)
            favoriteCheckView.isChecked = false
            val mediaItem = MediaBrowserCompat.MediaItem(
                    MediaItemHelper.buildMediaDescriptionFromRadioStation(
                        radioStation, isFavorite = FavoritesStorage.isFavorite(radioStation, context)
                    ),
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
            MediaItemsAdapter.handleFavoriteAction(favoriteCheckView, description, mediaItem, context)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        AppLogger.d(
                CLASS_NAME + " permissions:" + permissions.contentToString()
                        + ", results:" + grantResults.contentToString()
        )
        mMediaPresenter.handlePermissionsResult(applicationContext, requestCode, permissions, grantResults)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        AppLogger.d(
                CLASS_NAME + "OnActivityResult: request:" + requestCode
                        + " result:" + resultCode
                        + " intent:" + data
                        + " data:" + IntentUtils.intentBundleToString(data)
        )
        val gDriveDialog = GoogleDriveDialog.findDialog(supportFragmentManager)
        gDriveDialog?.onActivityResult(requestCode, resultCode, data)
        val logsDialog = LogsDialog.findDialog(supportFragmentManager)
        logsDialog?.onActivityResult(requestCode, resultCode, data)
        val addEditStationDialog = BaseAddEditStationDialog.findDialog(supportFragmentManager)
        addEditStationDialog?.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * Callback receiver of the local application's event.
     */
    private inner class LocalBroadcastReceiverCallback : AppLocalReceiverCallback {

        override fun onLocationChanged() {
            if (mMediaPresenter.getOnSaveInstancePassed()) {
                AppLogger.w(CLASS_NAME + "Can not do Location Changed after OnSaveInstanceState")
                return
            }
            if (MediaIdHelper.MEDIA_ID_ROOT == mMediaPresenter.currentParentId) {
                LocationService.checkCountry(
                        this@MainActivity, this@MainActivity.findViewById(R.id.main_layout)
                )
                updateRootView()
            }
        }

        override fun onCurrentIndexOnQueueChanged(index: Int, mediaId: String?) {
            mMediaPresenter.handleCurrentIndexOnQueueChanged(mediaId)
        }

        override fun onSleepTimer() {
            hideNoDataMessage()
            hideProgressBar()
            mMediaPresenter.exitFromUi()
        }

        override fun onSortIdChanged(mediaId: String, sortId: Int) {
            mMediaPresenter.handleCurrentIndexOnQueueChanged(mMediaPresenter.getCurrentMediaId())
        }

        override fun onGoogleDriveDownloaded() {
            if (mMediaPresenter.getOnSaveInstancePassed()) {
                AppLogger.w(CLASS_NAME + "Can not do GoogleDriveDownloaded after OnSaveInstanceState")
                return
            }
            if (MediaIdHelper.MEDIA_ID_ROOT == mMediaPresenter.currentParentId) {
                updateRootView()
            }
        }
    }

    private inner class MediaBrowserSubscriptionCallback : MediaBrowserCompat.SubscriptionCallback() {

        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>,
                                      options: Bundle) {
            AppLogger.i(
                "$CLASS_NAME children loaded:$parentId, children:${children.size}," +
                    " options:${IntentUtils.bundleToString(options)}"
            )
            if (mMediaPresenter.getOnSaveInstancePassed()) {
                AppLogger.w("$CLASS_NAME can not perform on children loaded after OnSaveInstanceState")
                return
            }
            hideProgressBar()
            val addBtn = findViewById<FloatingActionButton>(R.id.add_station_btn)
            if (parentId == MediaIdHelper.MEDIA_ID_ROOT) {
                addBtn.visibility = View.VISIBLE
            } else {
                addBtn.visibility = View.GONE
            }
            if (children.isEmpty()) {
                showNoDataMessage()
            }

            // No need to go on if indexed list ended with last item.
            if (MediaItemHelper.isEndOfList(children)) {
                return
            }
            mMediaPresenter.handleChildrenLoaded(parentId, children)
        }

        override fun onError(id: String) {
            hideProgressBar()
            com.yuriy.openradio.shared.view.SafeToast.showAnyThread(this@MainActivity, this@MainActivity.getString(R.string.error_loading_media))
        }
    }

    /**
     * Listener of the List Item events.
     */
    private inner class MediaItemListenerImpl : MediaItemsAdapter.Listener {

        override fun onItemSettings(item: MediaBrowserCompat.MediaItem, position: Int) {
            val transaction = supportFragmentManager.beginTransaction()
            UiUtils.clearDialogs(this@MainActivity, transaction)
            val bundle = Bundle()
            RSSettingsDialog.provideMediaItem(
                bundle, item, mMediaPresenter.currentParentId, mMediaPresenter.itemsCount()
            )
            val fragment = BaseDialogFragment.newInstance(RSSettingsDialog::class.java.name, bundle)
            fragment!!.show(transaction, RSSettingsDialog.DIALOG_TAG)
        }

        override fun onItemSelected(item: MediaBrowserCompat.MediaItem, position: Int) {
            mMediaPresenter.setActiveItem(position)
            mMediaPresenter.handleItemClick(item, position)
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