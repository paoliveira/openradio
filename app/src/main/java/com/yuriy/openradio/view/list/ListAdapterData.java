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
public class ListAdapterData<T> implements Serializable {

    private final List<T> mItems;

    /**
     * Constructor
     * @param comparator {@link java.util.Comparator} to implement sorting
     */
    public ListAdapterData(Comparator<T> comparator) {
        if (comparator != null) {
            // TODO: Implement sorting algorithm here
            mItems = new ArrayList<T>();
        } else {
            mItems = new ArrayList<T>();
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
