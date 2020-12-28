/*
 * Copyright 2019-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.app.Activity
import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.broadcast.AbstractReceiver
import com.yuriy.openradio.shared.broadcast.AppLocalBroadcast
import com.yuriy.openradio.shared.broadcast.AppLocalReceiver
import com.yuriy.openradio.shared.broadcast.AppLocalReceiverCallback
import com.yuriy.openradio.shared.broadcast.ConnectivityReceiver
import com.yuriy.openradio.shared.broadcast.ScreenReceiver
import com.yuriy.openradio.shared.model.media.MediaResourceManagerListener
import com.yuriy.openradio.shared.model.media.MediaResourcesManager
import com.yuriy.openradio.shared.service.OpenRadioService.Companion.getCurrentParentId
import com.yuriy.openradio.shared.service.OpenRadioService.Companion.makeStopServiceIntent
import com.yuriy.openradio.shared.service.OpenRadioService.Companion.makeToggleLastPlayedItemIntent
import com.yuriy.openradio.shared.service.OpenRadioService.Companion.putCurrentParentId
import com.yuriy.openradio.shared.utils.AppLogger.d
import com.yuriy.openradio.shared.utils.AppLogger.e
import com.yuriy.openradio.shared.utils.AppLogger.i
import com.yuriy.openradio.shared.utils.AppLogger.w
import com.yuriy.openradio.shared.utils.MediaIdHelper.isMediaIdRefreshable
import com.yuriy.openradio.shared.utils.MediaItemHelper.isEndOfList
import com.yuriy.openradio.shared.view.SafeToast.showAnyThread
import com.yuriy.openradio.shared.view.list.MediaItemsAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaPresenter @Inject constructor(@ApplicationContext context: Context?) {
    /**
     * Manager object that acts as interface between Media Resources and current Activity.
     */
    private val mMediaRsrMgr: MediaResourcesManager?

    /**
     * Stack of the media items.
     * It is used when navigating back and forth via list.
     */
    private val mMediaItemsStack: MutableList<String?> = LinkedList()

    /**
     * Map of the selected and clicked positions for lists of the media items.
     * Contract is - array of integer has 2 elements {selected position, clicked position}.
     */
    private val mPositions: MutableMap<String?, IntArray?> = Hashtable()
    private var mListLastVisiblePosition = 0

    /**
     * ID of the parent of current item (whether it is directory or Radio Station).
     */
    var currentParentId = ""
    private var mCallback: MediaBrowserCompat.SubscriptionCallback? = null
    private var mActivity: Activity? = null
    private var mListener: MediaPresenterListener? = null
    private var mListView: RecyclerView? = null
    private val mScrollListener: RecyclerView.OnScrollListener
    private var mLastKnownMetadata: MediaMetadataCompat? = null

    /**
     * Adapter for the representing media items in the list.
     */
    private var mAdapter: MediaItemsAdapter? = null

    /**
     * Receiver for the local application;s events
     */
    private val mAppLocalBroadcastRcvr: AppLocalReceiver

    /**
     * Receiver for the Screen OF/ON events.
     */
    private val mScreenBroadcastRcvr: AbstractReceiver
    private var mCurrentRadioStationView: View? = null
    fun init(activity: Activity, bundle: Bundle?, listView: RecyclerView,
             currentRadioStationView: View,
             adapter: MediaItemsAdapter, itemAdapterListener: MediaItemsAdapter.Listener?,
             mediaSubscriptionCallback: MediaBrowserCompat.SubscriptionCallback,
             listener: MediaPresenterListener?) {
        d("$CLASS_NAME init")
        mCallback = mediaSubscriptionCallback
        mActivity = activity
        mListener = listener
        mListView = listView
        mAdapter = adapter
        mCurrentRadioStationView = currentRadioStationView
        // Listener of events provided by Media Resource Manager.
        val mediaRsrMgrLst: MediaResourceManagerListener = MediaResourceManagerListenerImpl()
        mMediaRsrMgr!!.init(activity, bundle, mediaRsrMgrLst)
        val layoutManager = LinearLayoutManager(activity)
        mListView!!.layoutManager = layoutManager
        // Set adapter
        mListView!!.adapter = mAdapter
        mListView!!.addOnScrollListener(mScrollListener)
        mAdapter!!.listener = itemAdapterListener
        mCurrentRadioStationView!!.setOnClickListener { v: View? -> activity.startService(makeToggleLastPlayedItemIntent(activity)) }
        if (!mMediaItemsStack.isEmpty()) {
            val mediaId = mMediaItemsStack[mMediaItemsStack.size - 1]
            d("$CLASS_NAME current media id:$mediaId")
            unsubscribeFromItem(mediaId)
            addMediaItemToStack(mediaId)
        }
    }

    fun clean() {
        d("$CLASS_NAME clean")
        mMediaRsrMgr!!.clean()
        mCallback = null
        mActivity = null
        mListener = null
        mAdapter!!.clear()
        mAdapter!!.removeListener()
    }

    fun destroy() {
        d("$CLASS_NAME destroy")
        if (mListView != null) {
            mListView!!.removeOnScrollListener(mScrollListener)
        }
        // Disconnect Media Browser
        mMediaRsrMgr!!.disconnect()
    }

    fun handleBackPressed(context: Context): Boolean {
        d(CLASS_NAME + " back pressed start:" + mMediaItemsStack.size)

        // If there is root category - close activity
        if (mMediaItemsStack.size == 1) {

            // Un-subscribe from item
            mMediaRsrMgr!!.unsubscribe(mMediaItemsStack.removeAt(mMediaItemsStack.size - 1)!!)
            // Clear stack
            mMediaItemsStack.clear()
            context.startService(makeStopServiceIntent(context))
            d("$CLASS_NAME back pressed return true, stop service")
            return true
        }
        var index = mMediaItemsStack.size - 1
        if (index >= 0) {
            // Get current media item and un-subscribe.
            val currentMediaId = mMediaItemsStack.removeAt(index)
            mMediaRsrMgr!!.unsubscribe(currentMediaId!!)
        }

        // Un-subscribe from all items.
        for (mediaItemId in mMediaItemsStack) {
            mMediaRsrMgr!!.unsubscribe(mediaItemId!!)
        }

        // Subscribe to the previous item.
        index = mMediaItemsStack.size - 1
        if (index >= 0) {
            val previousMediaId = mMediaItemsStack[index]
            if (!TextUtils.isEmpty(previousMediaId)) {
                if (mListener != null) {
                    mListener!!.showProgressBar()
                }
                mMediaRsrMgr!!.subscribe(previousMediaId!!, mCallback)
            }
        } else {
            d("$CLASS_NAME back pressed return true")
            return true
        }
        d(CLASS_NAME + " back pressed end:" + mMediaItemsStack.size)
        return false
    }

    fun unsubscribeFromItem(mediaId: String?) {
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
        mMediaRsrMgr!!.unsubscribe(mediaId!!)
    }

    fun addMediaItemToStack(mediaId: String?) {
        if (mCallback == null) {
            e("$CLASS_NAME add media id to stack, callback null")
            return
        }
        if (TextUtils.isEmpty(mediaId)) {
            e("$CLASS_NAME add empty media id to stack")
            return
        }
        if (!mMediaItemsStack.contains(mediaId)) {
            mMediaItemsStack.add(mediaId)
        }
        if (mListener != null) {
            mListener!!.showProgressBar()
        }
        mMediaRsrMgr!!.subscribe(mediaId!!, mCallback)
    }

    /**
     * Sets the item on the provided index as active.
     *
     * @param position Position of the item in the list.
     */
    fun setActiveItem(position: Int) {
        if (mListView == null) {
            return
        }
        mAdapter!!.activeItemId = position
        mAdapter!!.notifyDataSetChanged()
    }

    fun updateListPositions(clickPosition: Int) {
        val layoutManager = mListView!!.layoutManager as LinearLayoutManager? ?: return
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

    fun handleItemClick(item: MediaBrowserCompat.MediaItem?, clickPosition: Int) {
        if (mActivity == null) {
            return
        }
        if (!ConnectivityReceiver.checkConnectivityAndNotify(mActivity!!)) {
            return
        }

        // Current selected media item
        if (item == null) {
            //TODO: Improve message
            showAnyThread(mActivity, mActivity!!.getString(R.string.can_not_play_station))
            return
        }
        if (item.isBrowsable) {
            if (item.description.title != null
                    && item.description.title == mActivity!!.getString(R.string.category_empty)) {
                return
            }
        }
        updateListPositions(clickPosition)
        val mediaId = item.mediaId

        // If it is browsable - then we navigate to the next category
        if (item.isBrowsable) {
            addMediaItemToStack(mediaId)
        } else if (item.isPlayable) {
            // Else - we play an item
            mMediaRsrMgr!!.playFromMediaId(mediaId)
        }
    }

    fun connect() {
        if (mMediaRsrMgr == null) {
            return
        }
        mMediaRsrMgr.connect()
    }

    fun getPositions(mediaItem: String?): IntArray {
        // Restore clicked position for the Catalogue list.
        return if (!TextUtils.isEmpty(mediaItem) && mPositions.containsKey(mediaItem)) {
            mPositions[mediaItem] ?: return createInitPositionEntry()
        } else createInitPositionEntry()
    }

    private fun createInitPositionEntry(): IntArray {
        return intArrayOf(0, MediaSessionCompat.QueueItem.UNKNOWN_ID)
    }

    fun handleChildrenLoaded(parentId: String,
                             children: List<MediaBrowserCompat.MediaItem>) {
        currentParentId = parentId

        // No need to go on if indexed list ended with last item.
        if (isEndOfList(children)) {
            return
        }
        mAdapter!!.parentId = parentId
        mAdapter!!.clearData()
        mAdapter!!.addAll(children)
        mAdapter!!.notifyDataSetChanged()
        restoreSelectedPosition()
    }

    fun restoreSelectedPosition() {
        // Restore positions for the Catalogue list.
        val positions = getPositions(currentParentId)
        val clickedPosition = positions[1]
        val selectedPosition = positions[0]
        // This will make selected item highlighted.
        setActiveItem(clickedPosition)
        // This will do scroll to the position.
        mListView!!.scrollToPosition(selectedPosition.coerceAtLeast(0))
        Handler().postDelayed(
                { mListView!!.smoothScrollToPosition(Math.max(selectedPosition, 0)) }, 50
        )
    }

    fun handleSaveInstanceState(outState: Bundle) {
        putCurrentParentId(outState, currentParentId)
        if (mLastKnownMetadata != null) {
            outState.putParcelable(BUNDLE_ARG_LAST_KNOWN_METADATA, mLastKnownMetadata)
        }
        //        outState.putInt(BUNDLE_ARG_LIST_1_VISIBLE_ID, mListFirstVisiblePosition);
//        outState.putInt(BUNDLE_ARG_LIST_CLICKED_ID, mAdapter.getActiveItemId());
//        updateListPositions(mAdapter.getActiveItemId());
    }

    fun handleRestoreInstanceState(savedInstanceState: Bundle) {
//        mListFirstVisiblePosition = savedInstanceState.getInt(BUNDLE_ARG_LIST_1_VISIBLE_ID);
//        mListSavedClickedPosition = savedInstanceState.getInt(BUNDLE_ARG_LIST_CLICKED_ID);
    }

    fun handleCurrentIndexOnQueueChanged(mediaId: String?) {
        setActiveItem(mAdapter!!.getIndexForMediaId(mediaId))
    }

    fun restoreState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            // Nothing to restore
            return
        }
        currentParentId = getCurrentParentId(savedInstanceState)
        handleRestoreInstanceState(savedInstanceState)
        restoreSelectedPosition()
        handleMetadataChanged(savedInstanceState.getParcelable(BUNDLE_ARG_LAST_KNOWN_METADATA))
    }

    /**
     * Register receiver for the application's local events.
     */
    fun registerReceivers(context: Context?, callback: AppLocalReceiverCallback?) {
        mAppLocalBroadcastRcvr.registerListener(callback)

        // Create filter and add actions
        val intentFilter = IntentFilter()
        intentFilter.addAction(AppLocalBroadcast.getActionLocationChanged())
        intentFilter.addAction(AppLocalBroadcast.getActionCurrentIndexOnQueueChanged())
        // Register receiver
        LocalBroadcastManager.getInstance(context!!).registerReceiver(
                mAppLocalBroadcastRcvr,
                intentFilter
        )
        mScreenBroadcastRcvr.register(context)
    }

    /**
     * Unregister receiver for the application's local events.
     */
    fun unregisterReceivers(context: Context?) {
        mAppLocalBroadcastRcvr.unregisterListener()
        LocalBroadcastManager.getInstance(context!!).unregisterReceiver(
                mAppLocalBroadcastRcvr
        )
        mScreenBroadcastRcvr.unregister(context)
    }

    private fun handleMediaResourceManagerConnected() {
        val mediaId = if (mMediaItemsStack.isEmpty()) mMediaRsrMgr!!.root else mMediaItemsStack[mMediaItemsStack.size - 1]!!
        addMediaItemToStack(mediaId)
        // Update metadata in case of UI started on and media service was already created and stream played.
        handleMetadataChanged(mMediaRsrMgr!!.mediaMetadata)
    }

    private fun handleMetadataChanged(metadata: MediaMetadataCompat?) {
        if (mListener == null) {
            return
        }
        if (metadata == null) {
            return
        }
        if (mCurrentRadioStationView != null && mCurrentRadioStationView!!.visibility != View.VISIBLE) {
            mCurrentRadioStationView!!.visibility = View.VISIBLE
        }
        mLastKnownMetadata = metadata
        mListener!!.handleMetadataChanged(metadata)
    }

    private fun onScrolledToEnd() {
        if (isMediaIdRefreshable(currentParentId)) {
            unsubscribeFromItem(currentParentId)
            addMediaItemToStack(currentParentId)
        } else {
            w(CLASS_NAME + "Category " + currentParentId + " is not refreshable")
        }
    }

    /**
     * Listener for the Media Resources related events.
     */
    private inner class MediaResourceManagerListenerImpl: MediaResourceManagerListener {
        override fun onConnected() {
            i("$CLASS_NAME Connected")
            handleMediaResourceManagerConnected()
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            d("$CLASS_NAME psc:$state")
            val activity = this@MediaPresenter
            if (activity.mListener != null) {
                activity.mListener!!.handlePlaybackStateChanged(state)
            }
        }

        override fun onQueueChanged(queue: List<MediaSessionCompat.QueueItem>) {
            d("$CLASS_NAME qc:$queue")
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat,
                                       queue: List<MediaSessionCompat.QueueItem>?) {
            handleMetadataChanged(metadata)
        }
    }

    private inner class ScrollListener : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                return
            }
            updateListPositions(mAdapter!!.activeItemId)
            if (mListLastVisiblePosition == mAdapter!!.itemCount - 1) {
                onScrolledToEnd()
            }
        }
    }

    companion object {
        private val CLASS_NAME = MediaPresenter::class.java.simpleName

        /**
         * Key value for the first visible ID in the List for the store Bundle
         */
        private const val BUNDLE_ARG_LIST_1_VISIBLE_ID = "BUNDLE_ARG_LIST_1_VISIBLE_ID"
        private const val BUNDLE_ARG_LIST_CLICKED_ID = "BUNDLE_ARG_LIST_CLICKED_ID"
        private const val BUNDLE_ARG_LAST_KNOWN_METADATA = "BUNDLE_ARG_LAST_KNOWN_METADATA"
    }

    init {
        mScrollListener = ScrollListener()
        mScreenBroadcastRcvr = ScreenReceiver()
        mAppLocalBroadcastRcvr = AppLocalReceiver.getInstance()
        mMediaRsrMgr = MediaResourcesManager(context!!, javaClass.simpleName)
    }
}
