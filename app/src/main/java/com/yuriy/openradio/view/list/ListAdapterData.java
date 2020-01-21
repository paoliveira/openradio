/*
 * Copyright 2017 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.view.list;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 11/29/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
final class ListAdapterData<T> implements Serializable {

    /**
     * Collection of the data items.
     */
    private final List<T> mItems;

    /**
     * Main constructor.
     */
    ListAdapterData() {
        super();
        mItems = new ArrayList<>();
    }

    /**
     * Add items to the adapter.
     *
     * @param items The items to add to collection.
     */
    public void addAll(final List<T> items) {
        mItems.addAll(items);
    }

    /**
     * Add provided item to the specified position.
     *
     * @param position Position to add item at.
     * @param item     Item to add.
     */
    void addAt(final int position, final T item) {
        mItems.add(position, item);
    }

    /**
     * Remove provided item from collection.
     *
     * @param item Item to be removed.
     */
    public void remove(final T item) {
        mItems.remove(item);
    }

    /**
     * Get item at the specified position.
     *
     * @param position The position of the item.
     * @return Item at the specified position or {@code null} if index is out of bounds.
     */
    @Nullable
    T getItem(final int position) {
        if (position < 0 || position >= getItemsCount()) {
            return null;
        }
        return mItems.get(position);
    }

    /**
     * Get the count of the items in the collection.
     *
     * @return The count of the items in the collection.
     */
    int getItemsCount() {
        return mItems.size();
    }

    /**
     * Clear collection of items.
     */
    public void clear() {
        mItems.clear();
    }
}
