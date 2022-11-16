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

package com.yuriy.openradio.shared.presenter

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.yuriy.openradio.shared.broadcast.AppLocalReceiverCallback
import com.yuriy.openradio.shared.view.list.MediaItemsAdapter

interface MediaPresenter {

    fun addMediaItemToStack(mediaId: String, options: Bundle = Bundle())

    fun connect()

    fun destroy()

    fun getOnSaveInstancePassed(): Boolean

    fun handleBackPressed(): Boolean

    fun handleChildrenLoaded(
        parentId: String,
        children: List<MediaBrowserCompat.MediaItem>
    )

    fun handleClosePresenter()

    fun handleCurrentIndexOnQueueChanged(index: Int)

    fun handleEditRadioStationMenu(view: View)

    fun handleItemSelected(item: MediaBrowserCompat.MediaItem?, clickPosition: Int)

    fun handleItemSettings(item: MediaBrowserCompat.MediaItem)

    fun handlePermissionsResult(permissions: Array<String>, grantResults: IntArray)

    fun handleRemoveRadioStationMenu(view: View)

    fun handleResume()

    fun handleSaveInstanceState(outState: Bundle)

    fun init(
        activity: FragmentActivity, mainLayout: View,
        bundle: Bundle, listView: RecyclerView, currentRadioStationView: View,
        adapter: MediaItemsAdapter,
        mediaSubscriptionCallback: MediaBrowserCompat.SubscriptionCallback,
        listener: MediaPresenterListener
    )

    fun onLocationChanged(countryCode: String)

    fun getCountryCode(): String

    fun registerReceivers(callback: AppLocalReceiverCallback)

    fun setActiveItem(position: Int)

    fun unsubscribeFromItem(mediaId: String?)

    fun updateDescription(descriptionView: TextView?, description: MediaDescriptionCompat)

    fun updateListPositions(clickPosition: Int)

    fun updateRootView()
}
