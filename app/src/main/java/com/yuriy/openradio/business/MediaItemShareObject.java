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

package com.yuriy.openradio.business;

import android.content.Context;
import android.support.annotation.NonNull;

import com.yuriy.openradio.api.CategoryVO;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/13/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */

/**
 * {@link MediaItemShareObject} is a class that designed to keep all necessary references that are
 * shared between implementations of the {@link MediaItemCommand} interface, includes application
 * context, name of the current category, list of all categories, etc ...
 */
public final class MediaItemShareObject {

    /**
     * Collection of the Child Categories.
     */
    private final List<CategoryVO> mChildCategories = new ArrayList<>();

    /**
     * Id of the current Category. It is used for example when back from an empty Category.
     */
    private String mCurrentCategory = "";

    /**
     * Context of the application.
     * TODO : must be final mandatory
     */
    @NonNull
    private Context mContext;

    /**
     * Private constructor.
     */
    private MediaItemShareObject() {
        super();
    }

    /**
     * @return Collection of the Child Categories.
     */
    public List<CategoryVO> getChildCategories() {
        return mChildCategories;
    }

    /**
     * @return Id of the current Category.
     */
    public String getCurrentCategory() {
        return mCurrentCategory;
    }

    /**
     * Sets the Id of the current Category.
     * @param value Id of the current Category.
     */
    public void setCurrentCategory(final String value) {
        mCurrentCategory = value;
    }

    /**
     * Gets the application's context.
     * @return the Application's context.
     */
    @NonNull
    public Context getContext() {
        return mContext;
    }

    /**
     * Sets the application's context.
     * @param value The application's context.
     */
    public void setContext(@NonNull final Context value) {
        mContext = value;
    }

    /**
     * Factory method to create default instance.
     *
     * @return Default instance of the {@link MediaItemShareObject}.
     */
    public static MediaItemShareObject getDefaultInstance() {
        return new MediaItemShareObject();
    }
}
