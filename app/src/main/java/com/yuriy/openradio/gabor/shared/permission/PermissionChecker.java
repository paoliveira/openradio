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

package com.yuriy.openradio.gabor.shared.permission;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import androidx.core.app.ActivityCompat;

import com.yuriy.openradio.gabor.shared.utils.AppLogger;
import com.yuriy.openradio.gabor.shared.utils.AppUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link PermissionChecker} is a helper class that designed to manage permissions changes
 * introduced in API 23.
 */
// TODO: Move to Android's class
public final class PermissionChecker {

    /**
     * Tag string to use in the logg messages.
     */
    private static final String LOG_TAG = "PermissionChecker";

    /**
     * Collection of the listeners which observe changes of the permissions that has been
     * introduced in API 23.
     */
    private static final List<WeakReference<PermissionStatusListener>> PERMISSION_STATUS_LISTENERS
            = new ArrayList<>();

    /**
     * Checks whether provided permission is granted or not.
     *
     * @param context    Application's context.
     * @param permission Permission name to check.
     * @return <b>TRUE</b> in case of provided permission is granted,
     * <b>FALSE</b> otherwise.
     */
    public static boolean isGranted(final Context context, final String permission) {
        if (!AppUtils.hasVersionM()) {
            return true;
        }
        final boolean result = context != null
                && !TextUtils.isEmpty(permission)
                && ActivityCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
        if (!result) {
            dispatch(permission);
        }
        return result;
    }

    /**
     * Checks whether provided permission is granted or not.
     *
     * @param context    Weak reference to the Application's context.
     * @param permission Permission name to check.
     * @return <b>TRUE</b> in case of provided permission is granted,
     * <b>FALSE</b> otherwise.
     */
    public static boolean isGranted(final WeakReference<Context> context, final String permission) {
        if (!AppUtils.hasVersionM()) {
            return true;
        }
        final boolean result = context != null
                && context.get() != null
                && !TextUtils.isEmpty(permission)
                && ActivityCompat.checkSelfPermission(context.get(), permission)
                == PackageManager.PERMISSION_GRANTED;
        if (!result) {
            dispatch(permission);
        }
        return result;
    }

    /**
     * Add implementation of the {@link PermissionStatusListener}.<br>
     * Observe permission status change over this listener.
     *
     * @param listener Implementation of the {@link PermissionStatusListener}.
     */
    public static void addPermissionStatusListener(final PermissionStatusListener listener) {
        synchronized (PERMISSION_STATUS_LISTENERS) {
            AppLogger.i(LOG_TAG + " Add listener:" + listener);
            PERMISSION_STATUS_LISTENERS.add(new WeakReference<>(listener));
        }
    }

    /**
     * Remove implementation of the {@link PermissionStatusListener}.<br>
     *
     * @param listener Implementation of the {@link PermissionStatusListener}.
     */
    public static void removePermissionStatusListener(final PermissionStatusListener listener) {
        synchronized (PERMISSION_STATUS_LISTENERS) {
            for (WeakReference<PermissionStatusListener> reference : PERMISSION_STATUS_LISTENERS) {
                final PermissionStatusListener statusListener = reference.get();
                if (listener == null) {
                    continue;
                }
                if (listener.equals(statusListener)) {
                    AppLogger.i(LOG_TAG + " Remove listener:" + listener);
                    PERMISSION_STATUS_LISTENERS.remove(reference);
                    return;
                }
            }
        }
    }

    private static void dispatch(final String permissionName) {
        AppLogger.e(LOG_TAG + " '" + permissionName + "' not granted, "
                + PERMISSION_STATUS_LISTENERS.size() + " listeners");
        synchronized (PERMISSION_STATUS_LISTENERS) {
            for (final WeakReference<PermissionStatusListener> reference : PERMISSION_STATUS_LISTENERS) {
                final PermissionStatusListener callback = reference.get();
                if (callback != null) {
                    AppLogger.i(LOG_TAG + " Dispatch to:" + callback);
                    callback.onPermissionRequired(permissionName);
                } else {
                    AppLogger.w(LOG_TAG + " Listener is null");
                }
            }
        }
    }
}
