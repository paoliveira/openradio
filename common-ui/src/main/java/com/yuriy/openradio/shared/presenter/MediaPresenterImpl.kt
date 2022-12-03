/*
 * Copyright 2019-2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.presenter

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.*
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.broadcast.AppLocalBroadcast
import com.yuriy.openradio.shared.broadcast.AppLocalReceiver
import com.yuriy.openradio.shared.broadcast.AppLocalReceiverCallback
import com.yuriy.openradio.shared.exo.extentions.id
import com.yuriy.openradio.shared.model.media.MediaId
import com.yuriy.openradio.shared.model.media.MediaResourceManagerListener
import com.yuriy.openradio.shared.model.media.MediaResourcesManager
import com.yuriy.openradio.shared.model.net.NetworkLayer
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager
import com.yuriy.openradio.shared.model.storage.LocationStorage
import com.yuriy.openradio.shared.model.timer.SleepTimerListener
import com.yuriy.openradio.shared.model.timer.SleepTimerModel
import com.yuriy.openradio.shared.permission.PermissionChecker
import com.yuriy.openradio.shared.service.LocationService
import com.yuriy.openradio.shared.service.OpenRadioStore
import com.yuriy.openradio.shared.utils.*
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.view.SafeToast
import com.yuriy.openradio.shared.view.dialog.EditStationDialog
import com.yuriy.openradio.shared.view.dialog.RSSettingsDialog
import com.yuriy.openradio.shared.view.dialog.RemoveStationDialog
import com.yuriy.openradio.shared.view.list.MediaItemsAdapter
import com.yuriy.openradio.shared.vo.PlaybackStateError
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class MediaPresenterImpl(
    private val mContext: Context,
    private val mNetworkLayer: NetworkLayer,
    private val mLocationStorage: LocationStorage,
    private val mSleepTimerModel: SleepTimerModel
) : MediaPresenter {
    /**
     * Manager object that acts as interface between Media Resources and current Activity.
     */
    private val mMediaRsrMgr = MediaResourcesManager(mContext, javaClass.simpleName)

    /**
     * Stack of the media items.
     * It is used when navigating back and forth via list.
     */
    private val mMediaItemsStack = LinkedList<String>()

    /**
     * Map of the selected and clicked positions for lists of the media items.
     * Contract is - array of integer has 2 elements {selected position, clicked position}.
     */
    private val mPositions = Hashtable<String?, IntArray?>()
    private var mListLastVisiblePosition = 0

    /**
     * ID of the parent of current item (whether it is directory or Radio Station).
     */
    var mCurrentParentId = AppUtils.EMPTY_STRING
    private var mCallback: MediaBrowserCompat.SubscriptionCallback? = null
    private var mActivity: FragmentActivity? = null
    private var mMainLayoutView: View? = null
    private var mListener: MediaPresenterListener? = null
    private var mListView: RecyclerView? = null
    private val mScrollListener = ScrollListener()
    private var mCurrentPlaybackState = PlaybackStateCompat.STATE_NONE

    /**
     * Adapter for the representing media items in the list.
     */
    private var mAdapter: MediaItemsAdapter? = null

    /**
     * Receiver for the local application;s events
     */
    private val mAppLocalBroadcastRcvr = AppLocalReceiver.instance

    private var mCurrentRadioStationView: View? = null
    private var mCurrentMediaId = AppUtils.EMPTY_STRING

    /**
     * Guardian field to prevent UI operation after addToLocals instance passed.
     */
    private val mIsOnSaveInstancePassed = AtomicBoolean(false)
    private val mIsReceiversRegistered = AtomicBoolean(false)

    private val mLocationMessenger = Messenger(LocationHandler())
    private val mTimerListener = SleepTimerListenerImpl()

    override fun init(
        activity: FragmentActivity, mainLayout: View,
        bundle: Bundle, listView: RecyclerView, currentRadioStationView: View,
        adapter: MediaItemsAdapter,
        mediaSubscriptionCallback: MediaBrowserCompat.SubscriptionCallback,
        listener: MediaPresenterListener
    ) {
        mIsOnSaveInstancePassed.set(false)
        mCallback = mediaSubscriptionCallback
        mActivity = activity
        mMainLayoutView = mainLayout
        mListener = listener
        mListView = listView
        mAdapter = adapter
        mCurrentRadioStationView = currentRadioStationView
        mSleepTimerModel.setSleepTimerListener(mTimerListener)
        // Listener of events provided by Media Resource Manager.
        val mediaRsrMgrLst = MediaResourceManagerListenerImpl()
        mMediaRsrMgr.init(activity, mediaRsrMgrLst)
        val layoutManager = LinearLayoutManager(activity)
        mListView?.layoutManager = layoutManager
        // Set adapter
        mListView?.adapter = mAdapter
        mListView?.addOnScrollListener(mScrollListener)
        mAdapter?.listener = MediaItemsAdapterListener()
        mCurrentRadioStationView?.setOnClickListener {
            activity.startService(OpenRadioStore.makeToggleLastPlayedItemIntent(activity))
        }
        if (mMediaItemsStack.isNotEmpty()) {
            val mediaId = mMediaItemsStack[mMediaItemsStack.size - 1]
            unsubscribeFromItem(mediaId)
            addMediaItemToStack(mediaId)
        }
        restoreState(bundle)
    }

    private fun itemsCount(): Int {
        return mAdapter?.itemCount ?: 0
    }

    private fun clean() {
        AppLogger.d("$CLASS_NAME clean")
        mMediaRsrMgr.clean()
        mSleepTimerModel.clearSleepTimerListener()
        mCallback = null
        mActivity = null
        mMainLayoutView = null
        mListener = null
        mListView = null
        mCurrentRadioStationView = null
        mAdapter?.clear()
        mAdapter?.removeListener()
        mAdapter = null
    }

    override fun getOnSaveInstancePassed(): Boolean {
        return mIsOnSaveInstancePassed.get()
    }

    override fun handleResume() {
        mIsOnSaveInstancePassed.set(false)
        if (mActivity == null) {
            return
        }
        val defaultCountry = mLocationStorage.getCountryCode()
        LocationService.checkCountry(mActivity!!, mMainLayoutView!!, mLocationMessenger, defaultCountry)
        if (AppUtils.hasVersionS() && AppPreferencesManager.isBtAutoPlay(mContext)) {
            if (!PermissionChecker.isBluetoothConnectGranted(mContext)) {
                PermissionChecker.requestBluetoothPermission(mActivity!!, mMainLayoutView!!)
            }
        }
    }

    override fun destroy() {
        clean()
        if (!mIsOnSaveInstancePassed.get()) {
            disconnect()
        }
        // Unregister local receivers
        unregisterReceivers()
        LocationService.doCancelWork(mContext)
    }

    private fun disconnect() {
        mListView?.removeOnScrollListener(mScrollListener)
        // Disconnect Media Browser
        mMediaRsrMgr.disconnect()
    }

    private fun exitFromUi() {
        for (item in mMediaItemsStack) {
            mMediaRsrMgr.unsubscribe(item)
        }
        mMediaItemsStack.clear()
        mActivity?.finish()
    }

    override fun onLocationChanged(countryCode: String) {
        mLocationStorage.setCountryCode(countryCode)
        updateRootView()
    }

    override fun getCountryCode(): String {
        return mLocationStorage.getCountryCode()
    }

    /**
     * Updates root view is there was changes in collection.
     * Should be call only if current media id is [MediaId.MEDIA_ID_ROOT] or [MediaId.MEDIA_ID_BROWSE]
     * if application runs on car.
     */
    override fun updateRootView() {
        if (getOnSaveInstancePassed()) {
            AppLogger.w("$CLASS_NAME can not do Location Changed after OnSaveInstanceState")
            return
        }
        if (MediaId.MEDIA_ID_ROOT != mCurrentParentId) {
            AppLogger.w(
                "$CLASS_NAME can not do Location Changed for non root. " +
                        "Current is '$mCurrentParentId'"
            )
            return
        }
        unsubscribeFromItem(mCurrentParentId)
        addMediaItemToStack(mCurrentParentId)
    }

    override fun handleBackPressed(): Boolean {
        // If there is root category - close activity
        if (mMediaItemsStack.size == 1) {
            // Un-subscribe from item
            mMediaRsrMgr.unsubscribe(mMediaItemsStack.removeAt(0))
            // Clear stack
            mMediaItemsStack.clear()
            mContext.startService(OpenRadioStore.makeStopServiceIntent(mContext))
            return true
        }
        var index = mMediaItemsStack.size - 1
        if (index >= 0) {
            // Get current media item and un-subscribe.
            val currentMediaId = mMediaItemsStack.removeAt(index)
            mMediaRsrMgr.unsubscribe(currentMediaId)
        }

        // Un-subscribe from all items.
        for (mediaItemId in mMediaItemsStack) {
            mMediaRsrMgr.unsubscribe(mediaItemId)
        }

        // Subscribe to the previous item.
        index = mMediaItemsStack.size - 1
        if (index >= 0) {
            val previousMediaId = mMediaItemsStack[index]
            if (previousMediaId.isNotEmpty()) {
                mListener?.showProgressBar()
                mMediaRsrMgr.subscribe(previousMediaId, mCallback)
            }
        } else {
            return true
        }
        return false
    }

    override fun unsubscribeFromItem(mediaId: String?) {
        // Remove provided media item (and it's duplicates, if any)
        var i = 0
        while (i < mMediaItemsStack.size) {
            if (mMediaItemsStack[i] == mediaId) {
                mMediaItemsStack.removeAt(i)
                i--
            }
            i++
        }

        // Un-subscribe from item
        mMediaRsrMgr.unsubscribe(mediaId!!)
    }

    override fun addMediaItemToStack(mediaId: String, options: Bundle) {
        if (mCallback == null) {
            AppLogger.e("$CLASS_NAME add media id to stack, callback null")
            return
        }
        if (mediaId.isEmpty()) {
            AppLogger.e("$CLASS_NAME add empty media id to stack")
            return
        }
        if (!mMediaItemsStack.contains(mediaId)) {
            mMediaItemsStack.add(mediaId)
        }
        mListener?.showProgressBar()
        mMediaRsrMgr.subscribe(mediaId, mCallback, options)
    }

    override fun updateDescription(descriptionView: TextView?, description: MediaDescriptionCompat) {
        if (descriptionView == null) {
            return
        }
        descriptionView.text = MediaItemHelper.getDisplayDescription(
            description, mContext.getString(R.string.media_description_default)
        )
        if (PlaybackStateError.isPlaybackStateError(mContext, descriptionView.text.toString())) {
            descriptionView.setBackgroundColor(mContext.resources.getColor(R.color.or_color_red_light))
        } else {
            descriptionView.setBackgroundColor(mContext.resources.getColor(R.color.or_color_transparent))
        }
    }

    /**
     * Sets the item on the provided index as active.
     *
     * @param position Position of the item in the list.
     */
    override fun setActiveItem(position: Int) {
        if (mListView == null) {
            return
        }
        if (mCurrentParentId == MediaId.MEDIA_ID_ROOT ||
            mCurrentParentId == MediaId.MEDIA_ID_ROOT_CAR
        ) {
            // Do not process Home Screen
            return
        }
        mAdapter?.activeItemId = position
        mAdapter?.notifyDataSetChanged()
    }

    override fun updateListPositions(clickPosition: Int) {
        val layoutManager = mListView?.layoutManager as LinearLayoutManager? ?: return
        mListLastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition()
        val size = mMediaItemsStack.size
        if (size < 1) {
            return
        }
        val mediaItem = mMediaItemsStack[size - 1]
        var data = mPositions[mediaItem]
        if (data == null) {
            data = createInitPositionEntry()
            mPositions[mediaItem] = data
        }
        data[0] = layoutManager.findFirstCompletelyVisibleItemPosition()
        data[1] = clickPosition
    }

    override fun handleItemSettings(item: MediaBrowserCompat.MediaItem) {
        val transaction = getFragmentTransaction(mActivity)
        if (transaction == null) {
            AppLogger.w("$CLASS_NAME can not handle settings with invalid transaction")
            return
        }
        UiUtils.clearDialogs(mActivity!!.supportFragmentManager, transaction)
        val bundle = Bundle()
        RSSettingsDialog.provideMediaItem(
            bundle, item, mCurrentParentId, itemsCount()
        )
        val fragment = BaseDialogFragment.newInstance(RSSettingsDialog::class.java.name, bundle)
        fragment.show(transaction, RSSettingsDialog.DIALOG_TAG)
    }

    override fun handleItemSelected(item: MediaBrowserCompat.MediaItem?, clickPosition: Int) {
        if (mActivity == null) {
            return
        }
        if (!mNetworkLayer.checkConnectivityAndNotify(mActivity!!)) {
            return
        }

        // Current selected media item
        if (item == null) {
            //TODO: Improve message
            SafeToast.showAnyThread(mActivity, mActivity!!.getString(R.string.can_not_play_station))
            return
        }
        if (item.isBrowsable) {
            if (item.description.title != null
                && item.description.title == mActivity!!.getString(R.string.category_empty)
            ) {
                return
            }
        }
        updateListPositions(clickPosition)
        mCurrentMediaId = item.mediaId.toString()

        // If it is browsable - then we navigate to the next category
        if (item.isBrowsable) {
            addMediaItemToStack(mCurrentMediaId)
        } else if (item.isPlayable) {
            // Else - we play an item
            mMediaRsrMgr.playFromMediaId(mCurrentMediaId)
        }
    }

    override fun connect() {
        mMediaRsrMgr.connect()
    }

    override fun handlePermissionsResult(permissions: Array<String>, grantResults: IntArray) {
        for (i in permissions.indices) {
            val isGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED
            when (permissions[i]) {
                Manifest.permission.ACCESS_COARSE_LOCATION -> {
                    if (isGranted) {
                        LocationService.doEnqueueWork(mContext, mLocationMessenger)
                    }
                }
                Manifest.permission.BLUETOOTH_CONNECT -> {
                    AppPreferencesManager.setBtAutoPlay(mContext, isGranted)
                }
            }
        }
    }

    private fun getPositions(mediaItem: String?): IntArray {
        // Restore clicked position for the Catalogue list.
        return if (mediaItem.isNullOrEmpty().not() && mPositions.containsKey(mediaItem)) {
            mPositions[mediaItem] ?: return createInitPositionEntry()
        } else createInitPositionEntry()
    }

    private fun createInitPositionEntry(): IntArray {
        return intArrayOf(0, MediaSessionCompat.QueueItem.UNKNOWN_ID)
    }

    override fun handleChildrenLoaded(
        parentId: String,
        children: List<MediaBrowserCompat.MediaItem>
    ) {
        mCurrentParentId = parentId

        // No need to go on if indexed list ended with last item.
        if (PlayerUtils.isEndOfList(children)) {
            return
        }
        mAdapter?.parentId = parentId
        mAdapter?.clearData()
        mAdapter?.addAll(children)
        mAdapter?.notifyDataSetChanged()
        restoreSelectedPosition(parentId)
    }

    private fun restoreSelectedPosition(parentId: String) {
        if (parentId == MediaId.MEDIA_ID_ROOT ||
            parentId == MediaId.MEDIA_ID_ROOT_CAR
        ) {
            // Do not process Home Screen
            return
        }
        // Restore positions for the Catalogue list.
        val positions = getPositions(parentId)
        val clickedPosition = positions[1]
        val selectedPosition = positions[0]
        // This will make selected item highlighted.
        setActiveItem(clickedPosition)
        // This will do scroll to the position.
        mListView?.scrollToPosition(selectedPosition.coerceAtLeast(0))
        MainScope().launch {
            delay(50)
            mListView?.smoothScrollToPosition(selectedPosition.coerceAtLeast(0))
        }
    }

    override fun handleSaveInstanceState(outState: Bundle) {
        // Track OnSaveInstanceState passed
        mIsOnSaveInstancePassed.set(true)
        OpenRadioStore.putRestoreState(outState, true)
        OpenRadioStore.putCurrentPlaybackState(outState, mCurrentPlaybackState)
        OpenRadioStore.putCurrentParentId(outState, mCurrentParentId)
    }

    override fun handleCurrentIndexOnQueueChanged(index: Int) {
        setActiveItem(index)
    }

    /**
     * Handles action of the Radio Station edition.
     *
     * @param view View that contain tag associated with Media item related to the Radio Station to be edited.
     */
    override fun handleEditRadioStationMenu(view: View) {
        if (getOnSaveInstancePassed()) {
            AppLogger.w("$CLASS_NAME can not edit after OnSaveInstanceState")
            return
        }
        val transaction = getFragmentTransaction(mActivity)
        if (transaction == null) {
            AppLogger.w("$CLASS_NAME can not edit with invalid transaction")
            return
        }
        UiUtils.clearDialogs(mActivity!!.supportFragmentManager, transaction)

        val item = view.tag as MediaBrowserCompat.MediaItem
        val mediaId = item.mediaId
        if (mediaId == null) {
            AppLogger.e("$CLASS_NAME can not edit with invalid media id")
            return
        }

        val bundle = EditStationDialog.makeBundle(mediaId)
        // Show Edit Station Dialog
        val dialog = BaseDialogFragment.newInstance(EditStationDialog::class.java.name, bundle)
        dialog.show(transaction, EditStationDialog.DIALOG_TAG)
    }

    /**
     * Handles action of the Radio Station deletion.
     *
     * @param view View that contain tag associated with Media item related to the Radio Station to be deleted.
     */
    override fun handleRemoveRadioStationMenu(view: View) {
        if (getOnSaveInstancePassed()) {
            AppLogger.w("$CLASS_NAME can not show Remove RS Dialog after OnSaveInstanceState")
            return
        }
        val transaction = getFragmentTransaction(mActivity)
        if (transaction == null) {
            AppLogger.w("$CLASS_NAME can not show Remove RS Dialog with invalid transaction")
            return
        }
        UiUtils.clearDialogs(mActivity!!.supportFragmentManager, transaction)

        val item = view.tag as MediaBrowserCompat.MediaItem
        var name = AppUtils.EMPTY_STRING
        if (item.description.title != null) {
            name = item.description.title.toString()
        }

        // Show Remove Station Dialog
        val bundle = RemoveStationDialog.makeBundle(item.mediaId, name)
        val dialog = BaseDialogFragment.newInstance(RemoveStationDialog::class.java.name, bundle)
        dialog.show(transaction, RemoveStationDialog.DIALOG_TAG)
    }

    private fun restoreState(savedInstanceState: Bundle) {
        mCurrentParentId = OpenRadioStore.getCurrentParentId(savedInstanceState)
        restoreSelectedPosition(mCurrentParentId)
        handleMetadataChanged(mMediaRsrMgr.mediaMetadata)
    }

    /**
     * Register receiver for the application's local events.
     */
    override fun registerReceivers(callback: AppLocalReceiverCallback) {
        if (mIsReceiversRegistered.get()) {
            AppLogger.w("$CLASS_NAME receivers are registered")
            return
        }
        mAppLocalBroadcastRcvr.registerListener(callback)

        // Create filter and add actions
        val intentFilter = IntentFilter()
        intentFilter.addAction(AppLocalBroadcast.getActionCurrentIndexOnQueueChanged())
        // Register receiver
        LocalBroadcastManager.getInstance(mContext).registerReceiver(
            mAppLocalBroadcastRcvr,
            intentFilter
        )

        mIsReceiversRegistered.set(true)
    }

    /**
     * Unregister receiver for the application's local events.
     */
    private fun unregisterReceivers() {
        if (!mIsReceiversRegistered.get()) {
            AppLogger.w("$CLASS_NAME receivers are unregistered")
            return
        }
        mAppLocalBroadcastRcvr.unregisterListener()
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(
            mAppLocalBroadcastRcvr
        )
        mIsReceiversRegistered.set(false)
    }

    private fun handleMediaResourceManagerConnected() {
        val size = mMediaItemsStack.size
        val mediaId = if (size == 0) mMediaRsrMgr.root else mMediaItemsStack[size - 1]
        addMediaItemToStack(mediaId)
        // Update metadata in case of UI started on and media service was already created and stream played.
        handleMetadataChanged(mMediaRsrMgr.mediaMetadata)
    }

    private fun handleMetadataChanged(metadata: MediaMetadataCompat?) {
        if (mListener == null) {
            return
        }
        if (metadata == null) {
            return
        }
        val mediaId = metadata.id ?: AppUtils.EMPTY_STRING
        if (mediaId.isBlank()) {
            return
        }
        if (mCurrentRadioStationView?.visibility != View.VISIBLE) {
            mCurrentRadioStationView?.visible()
        }
        mListener?.handleMetadataChanged(metadata)
    }

    private fun onScrolledToEnd() {
        if (MediaId.isRefreshable(mCurrentParentId)) {
            unsubscribeFromItem(mCurrentParentId)
            addMediaItemToStack(mCurrentParentId)
        } else {
            AppLogger.w(CLASS_NAME + "Category $mCurrentParentId is not refreshable")
        }
    }

    override fun handleClosePresenter() {
        exitFromUi()
        disconnect()
    }

    /**
     * Listener for the Media Resources related events.
     */
    private inner class MediaResourceManagerListenerImpl : MediaResourceManagerListener {

        override fun onConnected() {
            AppLogger.i("$CLASS_NAME Connected")
            handleMediaResourceManagerConnected()
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            AppLogger.d("$CLASS_NAME psc:$state")
            mCurrentPlaybackState = state.state
            this@MediaPresenterImpl.mListener?.handlePlaybackStateChanged(state)
        }

        override fun onQueueChanged(queue: List<MediaSessionCompat.QueueItem>) {
            AppLogger.d("$CLASS_NAME qc:$queue")
        }

        override fun onMetadataChanged(
            metadata: MediaMetadataCompat,
            queue: List<MediaSessionCompat.QueueItem>?
        ) {
            handleMetadataChanged(metadata)
        }
    }

    private inner class ScrollListener : RecyclerView.OnScrollListener() {

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                return
            }
            updateListPositions(mAdapter?.activeItemId ?: 0)
            if (mListLastVisiblePosition == (mAdapter?.itemCount ?: 1) - 1) {
                onScrolledToEnd()
            }
        }
    }

    private inner class MediaItemsAdapterListener : MediaItemsAdapter.Listener {

        override fun onItemSettings(item: MediaBrowserCompat.MediaItem, position: Int) {
            handleItemSettings(item)
        }

        override fun onItemSelected(item: MediaBrowserCompat.MediaItem, position: Int) {
            setActiveItem(position)
            handleItemSelected(item, position)
        }
    }

    @SuppressLint("HandlerLeak")
    private inner class LocationHandler : Handler(Looper.getMainLooper()) {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val msgId = msg.what
            if (msgId == LocationService.MSG_ID_ON_LOCATION) {
                val countryCode = LocationService.getCountryCode(msg)
                onLocationChanged(countryCode)
            }
        }
    }

    inner class SleepTimerListenerImpl : SleepTimerListener {

        override fun onComplete() {
            handleClosePresenter()
        }
    }

    companion object {

        private const val CLASS_NAME = "MP"

        private fun getFragmentTransaction(activity: FragmentActivity?): FragmentTransaction? {
            return activity?.supportFragmentManager?.beginTransaction()
        }
    }
}
