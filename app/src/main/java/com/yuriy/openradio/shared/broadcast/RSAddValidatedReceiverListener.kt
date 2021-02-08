/*
 * Copyright 2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 26/08/18
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * Listener interface for new radio station to add validation event.
 */
interface RSAddValidatedReceiverListener {
    /**
     * Dispatches when validation was successful.
     *
     * @param message Message associated with success.
     */
    fun onSuccess(message: String)

    /**
     * Dispatches when validation was failed.
     *
     * @param reason Reason of failure.
     */
    fun onFailure(reason: String)
}