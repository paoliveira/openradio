/*
 * Copyright 2019 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.gabor.shared.permission;

import android.content.Context;

import com.yuriy.openradio.gabor.shared.utils.AppUtils;
import com.yuriy.openradio.gabor.shared.view.activity.PermissionsDialogActivity;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionListener implements PermissionStatusListener {

    /**
     * Reference to the enclosing class.
     */
    private final WeakReference<Context> mReference;
    private final Map<String, Double> mMap = new ConcurrentHashMap<>();
    private static final int DELTA = 2000;

    /**
     * Main constructor.
     *
     * @param reference Reference to the enclosing class.
     */
    public PermissionListener(final Context reference) {
        super();
        mReference = new WeakReference<>(reference);
    }

    @Override
    public void onPermissionRequired(final String permissionName) {
        if (mReference.get() == null) {
            return;
        }

        final double currentTime = System.currentTimeMillis();

        if (mMap.containsKey(permissionName)) {
            if (currentTime - mMap.get(permissionName) < DELTA) {
                return;
            }
        }

        mMap.put(permissionName, currentTime);

        AppUtils.startActivitySafe(
                mReference.get(),
                PermissionsDialogActivity.getIntent(mReference.get(), permissionName)
        );
    }
}
