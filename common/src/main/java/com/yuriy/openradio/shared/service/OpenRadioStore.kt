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

package com.yuriy.openradio.shared.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import com.yuriy.openradio.shared.model.media.MediaId
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.IntentUtils
import com.yuriy.openradio.shared.vo.RadioStation

/**
 * [OpenRadioStore] is the object that provides ability to perform one way communication with [OpenRadioService].
 */
object OpenRadioStore {

    const val KEY_NAME_COMMAND_NAME = "KEY_NAME_COMMAND_NAME"

    const val VALUE_NAME_GET_RADIO_STATION_COMMAND = "VALUE_NAME_GET_RADIO_STATION_COMMAND"
    const val VALUE_NAME_UPDATE_SORT_IDS = "VALUE_NAME_UPDATE_SORT_IDS"
    const val VALUE_NAME_STOP_SERVICE = "VALUE_NAME_STOP_SERVICE"
    const val VALUE_NAME_TOGGLE_LAST_PLAYED_ITEM = "VALUE_NAME_TOGGLE_LAST_PLAYED_ITEM"
    const val VALUE_NAME_NETWORK_SETTINGS_CHANGED = "VALUE_NAME_NETWORK_SETTINGS_CHANGED"
    const val VALUE_NAME_CLEAR_CACHE = "VALUE_NAME_CLEAR_CACHE"
    const val VALUE_NAME_MASTER_VOLUME_CHANGED = "VALUE_NAME_MASTER_VOLUME_CHANGED"
    const val VALUE_NAME_NOTIFY_CHILDREN_CHANGED = "VALUE_NAME_NOTIFY_CHILDREN_CHANGED"
    const val VALUE_NAME_REMOVE_BY_ID = "VALUE_NAME_REMOVE_BY_ID"
    const val VALUE_NAME_UPDATE_TREE = "VALUE_NAME_UPDATE_TREE"

    private const val EXTRA_KEY_MEDIA_DESCRIPTION = "EXTRA_KEY_MEDIA_DESCRIPTION"
    private const val EXTRA_KEY_IS_FAVORITE = "EXTRA_KEY_IS_FAVORITE"
    const val EXTRA_KEY_MEDIA_ID = "EXTRA_KEY_MEDIA_ID"
    const val EXTRA_KEY_MEDIA_IDS = "EXTRA_KEY_MEDIA_IDS"
    const val EXTRA_KEY_SORT_IDS = "EXTRA_KEY_SORT_IDS"
    const val EXTRA_KEY_MASTER_VOLUME = "EXTRA_KEY_MASTER_VOLUME"
    const val EXTRA_KEY_PARENT_ID = "EXTRA_KEY_PARENT_ID"

    private const val BUNDLE_ARG_CATALOGUE_ID = "BUNDLE_ARG_CATALOGUE_ID"
    private const val BUNDLE_ARG_CURRENT_PLAYBACK_STATE = "BUNDLE_ARG_CURRENT_PLAYBACK_STATE"
    private const val BUNDLE_ARG_IS_RESTORE_STATE = "BUNDLE_ARG_IS_RESTORE_STATE"

    fun makeNetworkSettingsChangedIntent(context: Context): Intent {
        val intent = makeStartServiceIntent(context)
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_NETWORK_SETTINGS_CHANGED)
        return intent
    }

    fun makeClearCacheIntent(context: Context): Intent {
        val intent = makeStartServiceIntent(context)
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_CLEAR_CACHE)
        return intent
    }

    fun makeUpdateTreeIntent(context: Context): Intent {
        val intent = makeStartServiceIntent(context)
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_UPDATE_TREE)
        return intent
    }

    fun makeMasterVolumeChangedIntent(context: Context, masterVolume: Int): Intent {
        val intent = makeStartServiceIntent(context)
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_MASTER_VOLUME_CHANGED)
        intent.putExtra(EXTRA_KEY_MASTER_VOLUME, masterVolume)
        return intent
    }

    fun makeNotifyChildrenChangedIntent(context: Context, parentId: String): Intent {
        val intent = makeStartServiceIntent(context)
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_NOTIFY_CHILDREN_CHANGED)
        intent.putExtra(EXTRA_KEY_PARENT_ID, parentId)
        return intent
    }

    fun makeRemoveByMediaIdIntent(context: Context, mediaId: String): Intent {
        val intent = makeStartServiceIntent(context)
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_REMOVE_BY_ID)
        intent.putExtra(EXTRA_KEY_MEDIA_ID, mediaId)
        return intent
    }

    /**
     * Factory method to make Intent to update Sort Ids of the Radio Stations.
     *
     * @param context               Application context.
     * @param mediaId               Array of the Media Ids (of the Radio Stations).
     * @param sortId                Array of the corresponded Sort Ids.
     * @param parentCategoryMediaId ID of the current category ([etc ...][MediaId.MEDIA_ID_FAVORITES_LIST]).
     * @return [Intent].
     */
    fun makeUpdateSortIdsIntent(
        context: Context, mediaId: String, sortId: Int, parentCategoryMediaId: String
    ): Intent {
        val intent = makeStartServiceIntent(context)
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_UPDATE_SORT_IDS)
        intent.putExtra(EXTRA_KEY_MEDIA_IDS, mediaId)
        intent.putExtra(EXTRA_KEY_SORT_IDS, sortId)
        intent.putExtra(EXTRA_KEY_MEDIA_ID, parentCategoryMediaId)
        return intent
    }

    /**
     * Make intent to stop service.
     *
     * @param context Context of the callee.
     * @return [Intent].
     */
    fun makeStopServiceIntent(context: Context): Intent {
        val intent = makeStartServiceIntent(context)
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_STOP_SERVICE)
        return intent
    }

    /**
     * Factory method to make [Intent] to update whether [RadioStation] is Favorite.
     *
     * @param context          Context of the callee.
     * @param mediaDescription [MediaDescriptionCompat] of the [RadioStation].
     * @param isFavorite       Whether Radio station is Favorite or not.
     * @return [Intent].
     */
    fun makeUpdateIsFavoriteIntent(
        context: Context, mediaDescription: MediaDescriptionCompat?, isFavorite: Boolean
    ): Intent {
        val intent = makeStartServiceIntent(context)
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_GET_RADIO_STATION_COMMAND)
        intent.putExtra(EXTRA_KEY_MEDIA_DESCRIPTION, mediaDescription)
        intent.putExtra(EXTRA_KEY_IS_FAVORITE, isFavorite)
        return intent
    }

    /**
     * @param context
     * @return
     */
    fun makeToggleLastPlayedItemIntent(context: Context): Intent {
        val intent = makeStartServiceIntent(context)
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_TOGGLE_LAST_PLAYED_ITEM)
        return intent
    }
    
    private fun makeStartServiceIntent(context: Context): Intent {
        return Intent(context, OpenRadioService::class.java)
    }

    /**
     * Extract [.EXTRA_KEY_IS_FAVORITE] value from the [Intent].
     *
     * @param intent [Intent].
     * @return True in case of the key exists and it's value is True, False otherwise.
     */
    fun getIsFavoriteFromIntent(intent: Intent): Boolean {
        return (intent.hasExtra(EXTRA_KEY_IS_FAVORITE)
                && intent.getBooleanExtra(EXTRA_KEY_IS_FAVORITE, false))
    }

    fun extractMediaDescription(intent: Intent): MediaDescriptionCompat? {
        return IntentUtils.getParcelableExtra<MediaDescriptionCompat>(EXTRA_KEY_MEDIA_DESCRIPTION, intent)
            ?: return MediaDescriptionCompat.Builder().build()
    }

    fun putCurrentParentId(bundle: Bundle?, currentParentId: String?) {
        if (bundle == null) {
            return
        }
        bundle.putString(BUNDLE_ARG_CATALOGUE_ID, currentParentId)
    }

    fun getCurrentParentId(bundle: Bundle?): String {
        return if (bundle == null) {
            AppUtils.EMPTY_STRING
        } else bundle.getString(BUNDLE_ARG_CATALOGUE_ID, AppUtils.EMPTY_STRING)
    }

    fun putCurrentPlaybackState(bundle: Bundle?, value: Int) {
        if (bundle == null) {
            return
        }
        bundle.putInt(BUNDLE_ARG_CURRENT_PLAYBACK_STATE, value)
    }

    fun putRestoreState(bundle: Bundle?, value: Boolean) {
        if (bundle == null) {
            return
        }
        bundle.putBoolean(BUNDLE_ARG_IS_RESTORE_STATE, value)
    }

    fun getRestoreState(bundle: Bundle?): Boolean {
        return bundle?.getBoolean(BUNDLE_ARG_IS_RESTORE_STATE, false) ?: false
    }
}
