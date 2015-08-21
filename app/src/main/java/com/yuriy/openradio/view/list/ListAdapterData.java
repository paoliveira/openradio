/*
 * Copyright 2015 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 11/29/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class ListAdapterData<T> implements Serializable {

    private final List<T> mItems;

    /**
     * Constructor
     * @param comparator {@link java.util.Comparator} to implement sorting
     */
    public ListAdapterData(Comparator<T> comparator) {
        if (comparator != null) {
            // TODO: Implement sorting algorithm here
            mItems = new ArrayList<>();
        } else {
            mItems = new ArrayList<>();
        }
    }

    /**
     * Add item to the adapter
     * @param item item
     */
    public void addItem(T item) {
        mItems.add(item);
    }

    /**
     * Get item at the specified position
     * @param position position of the item
     * @return item at the specified position
     */
    public T getItem(int position) {
        return mItems.get(position);
    }

    /**
     * @return an array of the items
     */
    public List<T> getItems() {
        return mItems;
    }

    /**
     * Get the count of the items in the collection
     * @return count of the items in the collection
     */
    public int getItemsCount() {
        return mItems.size();
    }

    /**
     * Clear collection of items
     */
    public void clear() {
        mItems.clear();
    }
}
