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

package com.yuriy.openradio.shared.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.utils.AppUtils.hasVersionM

/**
 * [PermissionChecker] is a helper class that designed to manage permissions changes
 * introduced in API 23.
 */
object PermissionChecker {

    private const val REQUEST_CODE_LOCATION = 1000
    private const val REQUEST_CODE_EXT_STORAGE = 1001
    private const val REQUEST_CODE_REC_AUDIO = 1002
    private const val REQUEST_CODE_BLUETOOTH = 1003

    private const val KEY_PERMISSION_REQUESTED = "KEY_PERMISSION_REQUESTED"
    private const val VALUE_PERMISSION_REQUESTED = "VALUE_PERMISSION_REQUESTED"

    fun isLocationGranted(context: Context): Boolean {
        return isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    fun isExternalStorageGranted(context: Context): Boolean {
        return isGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun isRecordAudioGranted(context: Context): Boolean {
        return isGranted(context, Manifest.permission.RECORD_AUDIO)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun isBluetoothConnectGranted(context: Context): Boolean {
        return isGranted(context, Manifest.permission.BLUETOOTH_CONNECT)
    }

    fun requestLocationPermission(activity: Activity, layout: View) {
        requestPermission(
            activity, layout,
            Manifest.permission.ACCESS_COARSE_LOCATION, activity.getString(R.string.location_access_proposed),
            REQUEST_CODE_LOCATION
        )
    }

    fun requestExternalStoragePermission(activity: Activity, layout: View) {
        requestPermission(
            activity, layout,
            Manifest.permission.READ_EXTERNAL_STORAGE, activity.getString(R.string.storage_permission_proposed),
            REQUEST_CODE_EXT_STORAGE
        )
    }

    fun requestRecordAudioPermission(activity: Activity, layout: View) {
        requestPermission(
            activity, layout,
            Manifest.permission.RECORD_AUDIO, activity.getString(R.string.record_audio_permission_proposed),
            REQUEST_CODE_REC_AUDIO
        )
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun requestBluetoothPermission(activity: Activity, layout: View) {
        requestPermission(
            activity, layout,
            Manifest.permission.BLUETOOTH_CONNECT, activity.getString(R.string.bluetooth_permission_proposed),
            REQUEST_CODE_BLUETOOTH
        )
    }

    /**
     *
     */
    private fun requestPermission(
        activity: Activity, layout: View,
        permissionName: String, permissionMessage: String,
        requestCode: Int
    ) {
        if (activity.intent != null && activity.intent.hasExtra(KEY_PERMISSION_REQUESTED)) {
            // This is legal case. ActivityCompat.requestPermissions will invoke this activity over and over.
            // To avoid recursion, use extras to return.
            return
        }
        activity.intent.putExtra(KEY_PERMISSION_REQUESTED, VALUE_PERMISSION_REQUESTED)
        // Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permissionName)) {
            Snackbar.make(layout, permissionMessage, Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok_label) {
                // Request the permission
                ActivityCompat.requestPermissions(activity, arrayOf(permissionName), requestCode)
            }.show()
        } else {
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(activity, arrayOf(permissionName), requestCode)
        }
    }

    /**
     * Checks whether provided permission is granted or not.
     *
     * @param context    Application's context.
     * @param permission Permission name to check.
     * @return **TRUE** in case of provided permission is granted,
     * **FALSE** otherwise.
     */
    private fun isGranted(context: Context?, permission: String): Boolean {
        return if (!hasVersionM()) {
            true
        } else context != null && permission.isNotEmpty()
                && (ActivityCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED)
    }
}
