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
package com.yuriy.openradio.shared.view.list

import java.io.Serializable

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 11/29/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class ListAdapterData<T> : Serializable {
    /**
     * Collection of the data items.
     */
    private val mItems = ArrayList<T>()

    /**
     * Add items to the adapter.
     *
     * @param items The items to add to collection.
     */
    fun addAll(items: List<T>) {
        mItems.addAll(items)
    }

    /**
     * Remove provided item from collection.
     *
     * @param item Item to be removed.
     */
    fun remove(item: T) {
        mItems.remove(item)
    }

    /**
     * Get item at the specified position.
     *
     * @param position The position of the item.
     * @return Item at the specified position or `null` if index is out of bounds.
     */
    fun getItem(position: Int): T? {
        return if (position < 0 || position >= itemsCount) {
            null
        } else mItems[position]
    }

    /**
     * Get the count of the items in the collection.
     *
     * @return The count of the items in the collection.
     */
    val itemsCount: Int
        get() = mItems.size

    /**
     * Clear collection of items.
     */
    fun clear() {
        mItems.clear()
    }
}
