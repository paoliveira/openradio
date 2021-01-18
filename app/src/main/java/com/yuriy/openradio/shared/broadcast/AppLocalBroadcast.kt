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
package com.yuriy.openradio.shared.broadcast

import android.content.Intent

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/3/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
object AppLocalBroadcast {
    /**
     * @return Name for the Location Changed action.
     */
    /**
     * Action name for event when current location of device is changed.
     */
    private const val actionLocationChanged = "ACTION_LOCATION_CHANGED"

    private const val actionSleepTimer = "ACTION_SLEEP_TIMER"
    /**
     * @return Name for the Current Index on Queue Changed action.
     */
    /**
     * Action name for the "Current index on queue" changed,
     * when currently selected Radio Station was changed.
     */
    private const val actionCurrentIndexOnQueueChanged = "ACTION_CURRENT_INDEX_ON_QUEUE_CHANGED"
    /**
     * @return Name for the Master Volume Changed action.
     */
    /**
     * Action name for the "Master Volume Changed" event,
     * when volume of application's player was changed.
     */
    private const val actionMasterVolumeChanged = "ACTION_MASTER_VOLUME_CHANGED"
    private const val actionValidateOfRSFailed = "ACTION_VALIDATE_OF_RS_FAILED"
    private const val actionValidateOfRSSuccess = "ACTION_VALIDATE_OF_RS_SUCCESS"

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
    private const val actionClearCache = "ACTION_CLEAR_CACHE"
    @JvmStatic
    fun getCurrentIndexOnQueue(intent: Intent): Int {
        return intent.getIntExtra(KEY_CURRENT_INDEX_ON_QUEUE, 0)
    }

    @JvmStatic
    fun getCurrentMediaIdOnQueue(intent: Intent): String? {
        return intent.getStringExtra(KEY_CURRENT_MEDIA_ID_ON_QUEUE)
    }

    @JvmStatic
    fun getActionValidateOfRSFailedReason(intent: Intent?): String {
        if (intent == null) {
            return ""
        }
        return if (!intent.hasExtra(KEY_VALIDATED_RS_FAIL_REASON)) {
            ""
        } else intent.getStringExtra(KEY_VALIDATED_RS_FAIL_REASON) ?: ""
    }

    @JvmStatic
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
        return Intent(actionLocationChanged)
    }

    fun createIntentSleepTimer(): Intent {
        return Intent(actionSleepTimer)
    }

    /**
     * @return Instance of the [Intent] that indicates Current Index of the queue item.
     *
     * @param currentIndex Index of the current selected item in the queue.
     * @param mediaId      Id of the Media Item.
     */
    fun createIntentCurrentIndexOnQueue(currentIndex: Int,
                                        mediaId: String?): Intent {
        val intent = Intent(actionCurrentIndexOnQueueChanged)
        intent.putExtra(KEY_CURRENT_INDEX_ON_QUEUE, currentIndex)
        intent.putExtra(KEY_CURRENT_MEDIA_ID_ON_QUEUE, mediaId)
        return intent
    }

    /**
     * @return Intent to associate with master volume changed event.
     */
    fun createIntentMasterVolumeChanged(): Intent {
        return Intent(actionMasterVolumeChanged)
    }

    fun createIntentClearCache(): Intent {
        return Intent(actionClearCache)
    }

    fun createIntentValidateOfRSFailed(reason: String?): Intent {
        val intent = Intent(actionValidateOfRSFailed)
        intent.putExtra(KEY_VALIDATED_RS_FAIL_REASON, reason)
        return intent
    }

    fun createIntentValidateOfRSSuccess(message: String?): Intent {
        val intent = Intent(actionValidateOfRSSuccess)
        intent.putExtra(KEY_VALIDATED_RS_SUCCESS_MESSAGE, message)
        return intent
    }

    fun getActionLocationChanged(): String {
        return actionLocationChanged
    }

    fun getActionSleepTimer(): String {
        return actionSleepTimer
    }

    fun getActionCurrentIndexOnQueueChanged(): String {
        return actionCurrentIndexOnQueueChanged
    }

    fun getActionClearCache(): String {
        return actionClearCache
    }

    @JvmStatic
    fun getActionValidateOfRSFailed(): String {
        return actionValidateOfRSFailed
    }

    @JvmStatic
    fun getActionValidateOfRSSuccess(): String {
        return actionValidateOfRSSuccess
    }

    @JvmStatic
    fun getActionMasterVolumeChanged(): String {
        return actionMasterVolumeChanged
    }
}