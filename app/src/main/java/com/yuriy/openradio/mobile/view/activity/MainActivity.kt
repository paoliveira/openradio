/*
 * Copyright 2017-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
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
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.yuriy.openradio.R
import com.yuriy.openradio.mobile.view.list.MobileMediaItemsAdapter
import com.yuriy.openradio.shared.broadcast.AppLocalReceiverCallback
import com.yuriy.openradio.shared.model.storage.FavoritesStorage.isFavorite
import com.yuriy.openradio.shared.model.storage.LatestRadioStationStorage
import com.yuriy.openradio.shared.permission.PermissionChecker.isLocationGranted
import com.yuriy.openradio.shared.permission.PermissionChecker.requestLocationPermission
import com.yuriy.openradio.shared.presenter.MediaPresenter
import com.yuriy.openradio.shared.presenter.MediaPresenterListener
import com.yuriy.openradio.shared.service.LocationService.Companion.doEnqueueWork
import com.yuriy.openradio.shared.service.OpenRadioService.Companion.makeEditRadioStationIntent
import com.yuriy.openradio.shared.service.OpenRadioService.Companion.makeRemoveRadioStationIntent
import com.yuriy.openradio.shared.service.OpenRadioService.Companion.makeStopServiceIntent
import com.yuriy.openradio.shared.service.OpenRadioService.Companion.putCurrentPlaybackState
import com.yuriy.openradio.shared.service.OpenRadioService.Companion.putRestoreState
import com.yuriy.openradio.shared.utils.AppLogger.d
import com.yuriy.openradio.shared.utils.AppLogger.i
import com.yuriy.openradio.shared.utils.AppLogger.w
import com.yuriy.openradio.shared.utils.AppUtils.getApplicationVersion
import com.yuriy.openradio.shared.utils.AppUtils.getApplicationVersionCode
import com.yuriy.openradio.shared.utils.AppUtils.hasLocation
import com.yuriy.openradio.shared.utils.AppUtils.searchQuery
import com.yuriy.openradio.shared.utils.IntentUtils.intentBundleToString
import com.yuriy.openradio.shared.utils.MediaIdHelper
import com.yuriy.openradio.shared.utils.MediaItemHelper.buildMediaDescriptionFromRadioStation
import com.yuriy.openradio.shared.utils.MediaItemHelper.getDisplayDescription
import com.yuriy.openradio.shared.utils.MediaItemHelper.isEndOfList
import com.yuriy.openradio.shared.utils.MediaItemHelper.updateFavoriteField
import com.yuriy.openradio.shared.utils.UiUtils.clearDialogs
import com.yuriy.openradio.shared.view.BaseDialogFragment.Companion.newInstance
import com.yuriy.openradio.shared.view.SafeToast.showAnyThread
import com.yuriy.openradio.shared.view.dialog.AboutDialog
import com.yuriy.openradio.shared.view.dialog.AddStationDialog
import com.yuriy.openradio.shared.view.dialog.EditStationDialog
import com.yuriy.openradio.shared.view.dialog.EditStationDialog.Companion.getBundleWithMediaKey
import com.yuriy.openradio.shared.view.dialog.EqualizerDialog
import com.yuriy.openradio.shared.view.dialog.GeneralSettingsDialog
import com.yuriy.openradio.shared.view.dialog.GoogleDriveDialog
import com.yuriy.openradio.shared.view.dialog.GoogleDriveDialog.Companion.findGoogleDriveDialog
import com.yuriy.openradio.shared.view.dialog.LogsDialog
import com.yuriy.openradio.shared.view.dialog.LogsDialog.Companion.findLogsDialog
import com.yuriy.openradio.shared.view.dialog.RSSettingsDialog
import com.yuriy.openradio.shared.view.dialog.RSSettingsDialog.Companion.provideMediaItem
import com.yuriy.openradio.shared.view.dialog.RemoveStationDialog
import com.yuriy.openradio.shared.view.dialog.RemoveStationDialog.Companion.createBundle
import com.yuriy.openradio.shared.view.dialog.SearchDialog
import com.yuriy.openradio.shared.view.dialog.StreamBufferingDialog
import com.yuriy.openradio.shared.view.list.MediaItemsAdapter
import com.yuriy.openradio.shared.vo.RadioStation
import com.yuriy.openradio.shared.vo.RadioStationToAdd
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import java.util.concurrent.atomic.*
import javax.inject.Inject

/**
 * Created with Android Studio.
 * Author: Chernyshov Yuriy - Mobile Development
 * Date: 19.12.14
 * Time: 15:13
 *
 *
 * Main Activity class with represents the list of the categories: All, By Genre, Favorites, etc ...
 */
@AndroidEntryPoint
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
    private var mCurrentPlaybackState = PlaybackStateCompat.STATE_NONE

    /**
     * Listener for the List view click event.
     */
    private val mMediaItemListener: MediaItemsAdapter.Listener

    /**
     * Guardian field to prevent UI operation after addToLocals instance passed.
     */
    private val mIsOnSaveInstancePassed: AtomicBoolean
    private var mBufferedTextView: TextView? = null
    private var mPlayBtn: View? = null
    private var mPauseBtn: View? = null
    private var mProgressBarCrs: ProgressBar? = null

    @JvmField
    @Inject
    var mMediaPresenter: MediaPresenter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        d(CLASS_NAME + "OnCreate:" + savedInstanceState)
        val context = applicationContext
        initUi(context)
        mIsOnSaveInstancePassed.set(false)
        hideProgressBar()
        updateBufferedTime(0)

        // Register local receivers.
        mMediaPresenter!!.registerReceivers(applicationContext, mLocalBroadcastReceiverCb)
        val medSubscriptionCb: MediaBrowserCompat.SubscriptionCallback = MediaBrowserSubscriptionCallback()
        val mediaPresenterLstnr: MediaPresenterListener = MediaPresenterListenerImpl()
        mMediaPresenter!!.init(
                this, savedInstanceState, findViewById(R.id.list_view),
                findViewById(R.id.current_radio_station_view), MobileMediaItemsAdapter(this),
                mMediaItemListener, medSubscriptionCb, mediaPresenterLstnr
        )
        mMediaPresenter!!.restoreState(savedInstanceState)
        if (hasLocation(context)) {
            if (isLocationGranted(context)) {
                connectToMediaBrowser()
                doEnqueueWork(applicationContext)
            } else {
                requestLocationPermission(
                        this, findViewById(R.id.main_layout), 1234
                )
            }
        } else {
            connectToMediaBrowser()
        }
    }

    override fun onResume() {
        // Set OnSaveInstanceState to false
        mIsOnSaveInstancePassed.set(false)
        i(CLASS_NAME + "OnResume")
        super.onResume()

        // Hide any progress bar
        hideProgressBar()
        connectToMediaBrowser()
    }

    override fun onDestroy() {
        super.onDestroy()
        i(CLASS_NAME + "OnDestroy")
        mMediaPresenter!!.clean()
        if (!mIsOnSaveInstancePassed.get()) {
            mMediaPresenter!!.destroy()
            ContextCompat.startForegroundService(
                    applicationContext,
                    makeStopServiceIntent(applicationContext)
            )
        }

        // Unregister local receivers
        mMediaPresenter!!.unregisterReceivers(applicationContext)
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
        clearDialogs(this, transaction)
        return when (id) {
            R.id.action_search -> {

                // Show Search Dialog
                val dialog = newInstance(
                        SearchDialog::class.java.name
                )
                dialog!!.show(transaction, SearchDialog.DIALOG_TAG)
                true
            }
            R.id.action_eq -> {

                // Show Equalizer Dialog
                val dialog = newInstance(
                        EqualizerDialog::class.java.name
                )
                dialog!!.show(transaction, EqualizerDialog.DIALOG_TAG)
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        d(CLASS_NAME + "OnSaveInstance:" + outState)
        // Track OnSaveInstanceState passed
        mIsOnSaveInstancePassed.set(true)
        putRestoreState(outState, true)
        putCurrentPlaybackState(outState, mCurrentPlaybackState)
        mMediaPresenter!!.handleSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        hideNoDataMessage()
        hideProgressBar()
        if (mMediaPresenter!!.handleBackPressed(applicationContext)) {
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
        mBufferedTextView = findViewById(R.id.crs_buffered_view)
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
            clearDialogs(this, transaction)
            menuItem.isChecked = false
            // Handle navigation view item clicks here.
            when (menuItem.itemId) {
                R.id.nav_general -> {
                    // Show Search Dialog
                    val settingsDialog = newInstance(
                            GeneralSettingsDialog::class.java.name
                    )
                    settingsDialog!!.show(transaction, GeneralSettingsDialog.DIALOG_TAG)
                }
                R.id.nav_buffering -> {
                    // Show Stream Buffering Dialog
                    val streamBufferingDialog = newInstance(
                            StreamBufferingDialog::class.java.name
                    )
                    streamBufferingDialog!!.show(transaction, StreamBufferingDialog.DIALOG_TAG)
                }
                R.id.nav_google_drive -> {
                    // Show Google Drive Dialog
                    val googleDriveDialog = newInstance(
                            GoogleDriveDialog::class.java.name
                    )
                    googleDriveDialog!!.show(transaction, GoogleDriveDialog.DIALOG_TAG)
                }
                R.id.nav_logs -> {
                    // Show Application Logs Dialog
                    val applicationLogsDialog = newInstance(
                            LogsDialog::class.java.name
                    )
                    applicationLogsDialog!!.show(transaction, LogsDialog.DIALOG_TAG)
                }
                R.id.nav_about -> {
                    // Show About Dialog
                    val aboutDialog = newInstance(
                            AboutDialog::class.java.name
                    )
                    aboutDialog!!.show(transaction, AboutDialog.DIALOG_TAG)
                }
                else -> {
                }
            }
            drawer.closeDrawer(GravityCompat.START)
            true
        }
        val versionText = getApplicationVersion(context) + "." +
                getApplicationVersionCode(context)
        val versionView = navigationView.getHeaderView(0).findViewById<TextView>(
                R.id.drawer_ver_code_view
        )
        versionView.text = versionText

        // Handle Add Radio Station button.
        addBtn.setOnClickListener { view: View? ->
            // Show Add Station Dialog
            val transaction = supportFragmentManager.beginTransaction()
            val dialog = newInstance(
                    AddStationDialog::class.java.name
            )
            dialog!!.show(transaction, AddStationDialog.DIALOG_TAG)
        }
    }

    private fun connectToMediaBrowser() {
        mMediaPresenter!!.connect()
    }

    /**
     * Process user's input in order to edit custom [RadioStation].
     */
    fun processEditStationCallback(mediaId: String?, radioStationToAdd: RadioStationToAdd?) {
        startService(makeEditRadioStationIntent(
                this, mediaId, radioStationToAdd!!
        ))
    }

    /**
     * Process user's input in order to remove custom [RadioStation].
     */
    fun processRemoveStationCallback(mediaId: String?) {
        startService(makeRemoveRadioStationIntent(this, mediaId))
    }

    /**
     * Process call back from the Search Dialog.
     *
     * @param queryString String to query for.
     */
    fun onSearchDialogClick(queryString: String?) {
        unsubscribeFromItem(MediaIdHelper.MEDIA_ID_SEARCH_FROM_APP)
        // Save search query string, retrieve it later in the service
        searchQuery = queryString
        mMediaPresenter!!.addMediaItemToStack(MediaIdHelper.MEDIA_ID_SEARCH_FROM_APP)
    }

    /**
     * Remove provided Media Id from the collection. Reconnect [MediaBrowserCompat].
     *
     * @param mediaItemId Media Id.
     */
    private fun unsubscribeFromItem(mediaItemId: String) {
        hideNoDataMessage()
        hideProgressBar()
        mMediaPresenter!!.unsubscribeFromItem(mediaItemId)
    }

    /**
     * Updates root view is there was changes in collection.
     * Should be call only if current media id is [MediaIdHelper.MEDIA_ID_ROOT].
     */
    private fun updateRootView() {
        unsubscribeFromItem(MediaIdHelper.MEDIA_ID_ROOT)
        mMediaPresenter!!.addMediaItemToStack(MediaIdHelper.MEDIA_ID_ROOT)
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
        var name = ""
        if (item.description.title != null) {
            name = item.description.title.toString()
        }
        if (mIsOnSaveInstancePassed.get()) {
            w(CLASS_NAME + "Can not show Dialog after OnSaveInstanceState")
            return
        }
        val transaction = supportFragmentManager.beginTransaction()
        clearDialogs(this, transaction)

        // Show Remove Station Dialog
        val bundle = createBundle(item.mediaId, name)
        val dialog = newInstance(
                RemoveStationDialog::class.java.name, bundle
        )
        dialog!!.show(transaction, RemoveStationDialog.DIALOG_TAG)
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
        if (mIsOnSaveInstancePassed.get()) {
            w(CLASS_NAME + "Can not show Dialog after OnSaveInstanceState")
            return
        }
        val transaction = supportFragmentManager.beginTransaction()
        clearDialogs(this, transaction)

        // Show Edit Station Dialog
        val dialog = newInstance(
                EditStationDialog::class.java.name, getBundleWithMediaKey(item.mediaId)
        )
        dialog!!.show(transaction, EditStationDialog.DIALOG_TAG)
    }

    @MainThread
    private fun handlePlaybackStateChanged(state: PlaybackStateCompat) {
        mCurrentPlaybackState = state.state
        when (mCurrentPlaybackState) {
            PlaybackStateCompat.STATE_PLAYING -> {
                mPlayBtn!!.visibility = View.GONE
                mPauseBtn!!.visibility = View.VISIBLE
            }
            PlaybackStateCompat.STATE_STOPPED, PlaybackStateCompat.STATE_PAUSED -> {
                mPlayBtn!!.visibility = View.VISIBLE
                mPauseBtn!!.visibility = View.GONE
            }
        }
        mProgressBarCrs!!.visibility = View.GONE
        hideProgressBar()
        val bufferedDuration = (state.bufferedPosition - state.position) / 1000
        updateBufferedTime(bufferedDuration)
    }

    /**
     * Updates buffered value of the currently playing radio station.
     *
     * @param value Buffered time in seconds.
     */
    private fun updateBufferedTime(value: Long) {
        var valueCpy = value
        if (mBufferedTextView == null) {
            return
        }
        if (valueCpy < 0) {
            valueCpy = 0
        }
        mBufferedTextView!!.visibility = if (valueCpy > 0) View.VISIBLE else View.INVISIBLE
        mBufferedTextView!!.text = String.format(Locale.getDefault(), "Buffered %d sec", valueCpy)
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
        val descriptionView = findViewById<TextView>(R.id.crs_description_view)
        if (descriptionView != null) {
            descriptionView.text = getDisplayDescription(description, getString(R.string.media_description_default))
        }
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
                    buildMediaDescriptionFromRadioStation(context, radioStation),
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
            updateFavoriteField(
                    mediaItem,
                    isFavorite(radioStation, context)
            )
            MediaItemsAdapter.handleFavoriteAction(favoriteCheckView, description, mediaItem, context)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        d(
                CLASS_NAME + " permissions:" + permissions.contentToString()
                        + ", results:" + grantResults.contentToString()
        )
        for (i in permissions.indices) {
            val permission = permissions[i]
            if (permission == Manifest.permission.ACCESS_FINE_LOCATION && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                doEnqueueWork(applicationContext)
            }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        d(
                CLASS_NAME + "OnActivityResult: request:" + requestCode
                        + " result:" + resultCode
                        + " intent:" + data
                        + " data:" + intentBundleToString(data)
        )
        val gDriveDialog = findGoogleDriveDialog(supportFragmentManager)
        gDriveDialog?.onActivityResult(requestCode, resultCode, data)
        val logsDialog = findLogsDialog(supportFragmentManager)
        logsDialog?.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * Callback receiver of the local application's event.
     */
    private inner class LocalBroadcastReceiverCallback : AppLocalReceiverCallback {
        override fun onLocationChanged() {
            if (mIsOnSaveInstancePassed.get()) {
                w(CLASS_NAME + "Can not do Location Changed after OnSaveInstanceState")
                return
            }
            d(CLASS_NAME + "Location Changed received")
            if (mMediaPresenter != null) {
                return
            }
            if (TextUtils.equals(mMediaPresenter!!.currentParentId, MediaIdHelper.MEDIA_ID_ROOT)) {
                updateRootView()
            }
        }

        override fun onCurrentIndexOnQueueChanged(index: Int, mediaId: String?) {
            if (mMediaPresenter != null) {
                mMediaPresenter!!.handleCurrentIndexOnQueueChanged(mediaId)
            }
        }
    }

    private inner class MediaBrowserSubscriptionCallback : MediaBrowserCompat.SubscriptionCallback() {
        @SuppressLint("RestrictedApi")
        override fun onChildrenLoaded(parentId: String,
                                      children: List<MediaBrowserCompat.MediaItem>) {
            i(
                    CLASS_NAME + "Children loaded:" + parentId + ", children:" + children.size
            )
            if (mIsOnSaveInstancePassed.get()) {
                w(CLASS_NAME + "Can not perform on children loaded after OnSaveInstanceState")
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
            if (isEndOfList(children)) {
                return
            }
            if (mMediaPresenter != null) {
                mMediaPresenter!!.handleChildrenLoaded(parentId, children)
            }
        }

        override fun onError(id: String) {
            hideProgressBar()
            showAnyThread(
                    this@MainActivity, this@MainActivity.getString(R.string.error_loading_media)
            )
        }
    }

    /**
     * Listener of the List Item events.
     */
    private inner class MediaItemListenerImpl : MediaItemsAdapter.Listener {
        override fun onItemSettings(item: MediaBrowserCompat.MediaItem, position: Int) {
            val transaction = supportFragmentManager.beginTransaction()
            clearDialogs(this@MainActivity, transaction)
            val bundle = Bundle()
            var currentParentId = ""
            if (mMediaPresenter != null) {
                currentParentId = mMediaPresenter!!.currentParentId
            }
            if (MediaIdHelper.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST == currentParentId) {
                provideMediaItem(bundle, item)
            }
            val fragment = newInstance(RSSettingsDialog::class.java.name, bundle)
            fragment!!.show(transaction, RSSettingsDialog.DIALOG_TAG)
        }

        override fun onItemSelected(item: MediaBrowserCompat.MediaItem, position: Int) {
            if (mMediaPresenter == null) {
                return
            }
            mMediaPresenter!!.setActiveItem(position)
            mMediaPresenter!!.handleItemClick(item, position)
        }
    }

    private inner class MediaPresenterListenerImpl : MediaPresenterListener {
        override fun showProgressBar() {
            this@MainActivity.showProgressBar()
        }

        override fun handleMetadataChanged(metadata: MediaMetadataCompat?) {
            this@MainActivity.handleMetadataChanged(metadata!!)
        }

        override fun handlePlaybackStateChanged(state: PlaybackStateCompat?) {
            this@MainActivity.handlePlaybackStateChanged(state!!)
        }
    }

    /**
     * Default constructor.
     */
    init {
        mLocalBroadcastReceiverCb = LocalBroadcastReceiverCallback()
        mMediaItemListener = MediaItemListenerImpl()
        mIsOnSaveInstancePassed = AtomicBoolean(false)
    }
}