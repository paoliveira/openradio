/*
 * Copyright 2017-2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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
     * Action name for the "Current index on queue" changed,
     * when currently selected Radio Station was changed.
     */
    private const val ACTION_CURRENT_INDEX_ON_QUEUE_CHANGED = "ACTION_CURRENT_INDEX_ON_QUEUE_CHANGED"

    /**
     * Key value for the Currently selected index in the Intent's bundles.
     */
    private const val KEY_CURRENT_INDEX_ON_QUEUE = "KEY_CURRENT_INDEX_ON_QUEUE"

    fun getCurrentIndexOnQueue(intent: Intent): Int {
        return intent.getIntExtra(KEY_CURRENT_INDEX_ON_QUEUE, 0)
    }

    /**
     * @return Instance of the [Intent] that indicates Current Index of the queue item.
     *
     * @param currentIndex Index of the current selected item in the queue.
     */
    fun createIntentCurrentIndexOnQueue(currentIndex: Int): Intent {
        val intent = Intent(ACTION_CURRENT_INDEX_ON_QUEUE_CHANGED)
        intent.putExtra(KEY_CURRENT_INDEX_ON_QUEUE, currentIndex)
        return intent
    }

    fun getActionCurrentIndexOnQueueChanged(): String {
        return ACTION_CURRENT_INDEX_ON_QUEUE_CHANGED
    }
}
