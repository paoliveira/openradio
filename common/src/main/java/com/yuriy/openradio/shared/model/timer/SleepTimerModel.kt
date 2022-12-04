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

package com.yuriy.openradio.shared.model.timer

import java.util.*

interface SleepTimerModel {

    fun init()

    fun isEnabled(): Boolean

    fun setEnabled(value: Boolean)

    fun updateTime(enabled: Boolean)

    fun updateTimer(time: Long, enabled: Boolean)

    fun setDate(year: Int, month: Int, day: Int)

    fun setTime(hourOfDay: Int, minute: Int)

    fun getTime(): Date

    fun getTimestamp(): Long

    fun isTimestampNotValid(value: Long): Boolean

    fun addSleepTimerListener(listener: SleepTimerListener)

    fun removeSleepTimerListener(listener: SleepTimerListener)
}
