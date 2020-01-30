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

package com.yuriy.openradio.shared.model.storage;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yuriy.openradio.shared.vo.RadioStation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class RadioStationsStorage {

    public static final int UNKNOWN_INDEX = -1;

    @SuppressWarnings("unused")
    private static final String CLASS_NAME = RadioStationsStorage.class.getSimpleName();

    /**
     * Collection of the Radio Stations.
     */
    private final List<RadioStation> mRadioStations;

    public RadioStationsStorage() {
        super();
        mRadioStations = Collections.synchronizedList(new ArrayList<>());
    }

    public void sort(final Comparator<RadioStation> comparator) {
        synchronized (mRadioStations) {
            Collections.sort(mRadioStations, comparator);
        }
    }

    public void addAll(@NonNull final List<RadioStation> list) {
        synchronized (mRadioStations) {
            mRadioStations.addAll(list);
        }
    }

    public void clear() {
        synchronized (mRadioStations) {
            mRadioStations.clear();
        }
    }

    public boolean isEmpty() {
        boolean result;
        synchronized (mRadioStations) {
            result = mRadioStations.isEmpty();
        }
        return result;
    }

    public int size() {
        int result;
        synchronized (mRadioStations) {
            result = mRadioStations.size();
        }
        return result;
    }

    /**
     * Method return index of the {@link RadioStation} in the collection.
     *
     * @param mediaId Id of the Radio Station.
     * @return Index of the Radio Station in the collection.
     */
    public int getIndex(final String mediaId) {
        if (TextUtils.isEmpty(mediaId)) {
            return UNKNOWN_INDEX;
        }
        int index = 0;
        synchronized (mRadioStations) {
            for (final RadioStation item : mRadioStations) {
                if (mediaId.equals(item.getId())) {
                    return index;
                }
                index++;
            }
        }
        return UNKNOWN_INDEX;
    }

    /**
     * @param id
     * @return
     */
    @Nullable
    public RadioStation getById(final String id) {
        RadioStation result = null;
        if (TextUtils.isEmpty(id)) {
            return result;
        }
        synchronized (mRadioStations) {
            for (final RadioStation item : mRadioStations) {
                if (item == null) {
                    continue;
                }
                if (TextUtils.equals(item.getId(), id)) {
                    result = item;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * @param mediaId
     * @return
     */
    @Nullable
    public RadioStation remove(final String mediaId) {
        RadioStation result = null;
        if (TextUtils.isEmpty(mediaId)) {
            return result;
        }
        synchronized (mRadioStations) {
            for (final RadioStation radioStation : mRadioStations) {
                if (radioStation == null) {
                    continue;
                }
                if (TextUtils.equals(radioStation.getId(), mediaId)) {
                    mRadioStations.remove(radioStation);
                    result = radioStation;
                    break;
                }
            }
        }
        return result;
    }

    @Nullable
    public RadioStation getAt(final int index) {
        if (index < 0) {
            return null;
        }
        if (index >= size()) {
            return null;
        }
        RadioStation result;
        synchronized (mRadioStations) {
            result = mRadioStations.get(index);
        }
        return result;
    }

    /**
     * @param index
     * @return
     */
    public boolean isIndexPlayable(final int index) {
        return (index >= 0 && index < size());
    }

    /**
     * Clear destination and copy collection from source.
     *
     * @param source Source collection.
     */
    public void clearAndCopy(@NonNull final List<RadioStation> source) {
        clear();
        addAll(source);
    }

    @NonNull
    public List<RadioStation> getAll() {
        synchronized (mRadioStations) {
            return new ArrayList<>(mRadioStations);
        }
    }

    /**
     * Merge Radio Stations from listB to listA.
     *
     * @param listA
     * @param listB
     */
    public static void merge(final List<RadioStation> listA, final List<RadioStation> listB) {
        if (listA == null || listB == null) {
            return;
        }
        for (final RadioStation radioStation : listB) {
            if (listA.contains(radioStation)) {
                continue;
            }
            listA.add(radioStation);
        }
    }
}
