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

package com.yuriy.openradio.shared.model.storage

import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/3/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * [ServiceLifecycleManager] is the object to monitor lifecycle of the
 * [com.yuriy.openradio.shared.service.OpenRadioService].
 */
object ServiceLifecycleManager {

    private val sCounter = AtomicInteger(0)

    /**
     * @return True if Open Radio Service is inactive. False - otherwise.
     */
    fun isInactive(): Boolean {
        return sCounter.get() == 0
    }

    /**
     * Update state to indicate that the service is active (created).
     */
    fun setActive() {
        sCounter.incrementAndGet()
    }

    /**
     * Update state to indicate that the service is inactive (destroyed).
     */
    fun setInactive() {
        sCounter.decrementAndGet()
    }
}
