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

package com.yuriy.openradio.shared.broadcast

import android.content.Intent

object AppLocalBroadcast {

    /**
     * Action name for event when current location of device is changed.
     */
    private const val ACTION_LOCATION_CHANGED = "ACTION_LOCATION_CHANGED"
    private const val ACTION_SLEEP_TIMER = "ACTION_SLEEP_TIMER"
    private const val ACTION_SORT_ID_CHANGED = "ACTION_SORT_ID_CHANGED"
    /**
     * Action name for the "Current index on queue" changed,
     * when currently selected Radio Station was changed.
     */
    private const val ACTION_CURRENT_INDEX_ON_QUEUE_CHANGED = "ACTION_CURRENT_INDEX_ON_QUEUE_CHANGED"
    /**
     * Action name for the "Master Volume Changed" event,
     * when volume of application's player was changed.
     */
    private const val ACTION_MASTER_VOLUME_CHANGED = "ACTION_MASTER_VOLUME_CHANGED"
    private const val ACTION_VALIDATE_OF_RS_FAILED = "ACTION_VALIDATE_OF_RS_FAILED"
    private const val ACTION_VALIDATE_OF_RS_SUCCESS = "ACTION_VALIDATE_OF_RS_SUCCESS"
    private const val ACTION_CLEAR_CACHE = "ACTION_CLEAR_CACHE"
    private const val ACTION_EQUALIZED_APPLIED = "ACTION_EQUALIZED_APPLIED"

    /**
     * Key value for the Currently selected index in the Intent's bundles.
     */
    private const val KEY_CURRENT_INDEX_ON_QUEUE = "KEY_CURRENT_INDEX_ON_QUEUE"

    /**
     * Key value for the Currently selected Media Id in the Intent's bundles.
     */
    private const val KEY_CURRENT_MEDIA_ID_ON_QUEUE = "KEY_CURRENT_MEDIA_ID_ON_QUEUE"
    private const val KEY_VALIDATED_RS_FAIL_REASON = "KEY_VALIDATED_RS_FAIL_REASON"
    private const val KEY_VALIDATED_RS_SUCCESS_MESSAGE = "KEY_VALIDATED_RS_SUCCESS_MESSAGE"
    private const val KEY_SORT_ID = "KEY_SORT_ID"

    fun getSortId(intent: Intent): Int {
        return intent.getIntExtra(KEY_SORT_ID, 0)
    }

    fun getCurrentIndexOnQueue(intent: Intent): Int {
        return intent.getIntExtra(KEY_CURRENT_INDEX_ON_QUEUE, 0)
    }

    fun getCurrentMediaIdOnQueue(intent: Intent): String? {
        return intent.getStringExtra(KEY_CURRENT_MEDIA_ID_ON_QUEUE)
    }

    fun getActionValidateOfRSFailedReason(intent: Intent?): String {
        if (intent == null) {
            return ""
        }
        return if (!intent.hasExtra(KEY_VALIDATED_RS_FAIL_REASON)) {
            ""
        } else intent.getStringExtra(KEY_VALIDATED_RS_FAIL_REASON) ?: ""
    }

    fun getActionValidateOfRSSuccessMessage(intent: Intent?): String {
        if (intent == null) {
            return ""
        }
        return if (!intent.hasExtra(KEY_VALIDATED_RS_SUCCESS_MESSAGE)) {
            ""
        } else intent.getStringExtra(KEY_VALIDATED_RS_SUCCESS_MESSAGE) ?: ""
    }

    /**
     * @return Instance of the [Intent] that indicates about changed Location.
     */
    fun createIntentLocationChanged(): Intent {
        return Intent(ACTION_LOCATION_CHANGED)
    }

    fun createIntentSleepTimer(): Intent {
        return Intent(ACTION_SLEEP_TIMER)
    }

    fun createIntentSortIdChanged(sortId: Int): Intent {
        return Intent(ACTION_SORT_ID_CHANGED).putExtra(KEY_SORT_ID, sortId)
    }

    /**
     * @return Instance of the [Intent] that indicates Current Index of the queue item.
     *
     * @param currentIndex Index of the current selected item in the queue.
     * @param mediaId      Id of the Media Item.
     */
    fun createIntentCurrentIndexOnQueue(currentIndex: Int,
                                        mediaId: String?): Intent {
        val intent = Intent(ACTION_CURRENT_INDEX_ON_QUEUE_CHANGED)
        intent.putExtra(KEY_CURRENT_INDEX_ON_QUEUE, currentIndex)
        intent.putExtra(KEY_CURRENT_MEDIA_ID_ON_QUEUE, mediaId)
        return intent
    }

    /**
     * @return Intent to associate with master volume changed event.
     */
    fun createIntentMasterVolumeChanged(): Intent {
        return Intent(ACTION_MASTER_VOLUME_CHANGED)
    }

    fun createIntentClearCache(): Intent {
        return Intent(ACTION_CLEAR_CACHE)
    }

    fun createIntentEqualizerApplied(): Intent {
        return Intent(ACTION_EQUALIZED_APPLIED)
    }

    fun createIntentValidateOfRSFailed(reason: String?): Intent {
        val intent = Intent(ACTION_VALIDATE_OF_RS_FAILED)
        intent.putExtra(KEY_VALIDATED_RS_FAIL_REASON, reason)
        return intent
    }

    fun createIntentValidateOfRSSuccess(message: String?): Intent {
        val intent = Intent(ACTION_VALIDATE_OF_RS_SUCCESS)
        intent.putExtra(KEY_VALIDATED_RS_SUCCESS_MESSAGE, message)
        return intent
    }

    fun getActionLocationChanged(): String {
        return ACTION_LOCATION_CHANGED
    }

    fun getActionSleepTimer(): String {
        return ACTION_SLEEP_TIMER
    }

    fun getActionSortIdChanged(): String {
        return ACTION_SORT_ID_CHANGED
    }

    fun getActionCurrentIndexOnQueueChanged(): String {
        return ACTION_CURRENT_INDEX_ON_QUEUE_CHANGED
    }

    fun getActionClearCache(): String {
        return ACTION_CLEAR_CACHE
    }

    fun getActionEqualizerApplied(): String {
        return ACTION_EQUALIZED_APPLIED
    }

    fun getActionValidateOfRSFailed(): String {
        return ACTION_VALIDATE_OF_RS_FAILED
    }

    fun getActionValidateOfRSSuccess(): String {
        return ACTION_VALIDATE_OF_RS_SUCCESS
    }

    fun getActionMasterVolumeChanged(): String {
        return ACTION_MASTER_VOLUME_CHANGED
    }
}