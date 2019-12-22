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

package com.yuriy.openradio.view.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.utils.IntentsHelper;
import com.yuriy.openradio.shared.view.BaseDialogFragment;
import com.yuriy.openradio.shared.view.SafeToast;
import com.yuriy.openradio.view.activity.MainActivity;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/3/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link UseLocationDialog} is a Dialog to inform user that there is a profit of using Location
 * service of the device.
 */
public final class UseLocationDialog extends BaseDialogFragment {

    /**
     * Tag string to use in the debugging messages.
     */
    private static final String LOG_TAG = UseLocationDialog.class.getSimpleName();

    /**
     * The tag for this fragment, as per {@link android.app.FragmentTransaction#add}.
     */
    public final static String DIALOG_TAG = LOG_TAG + "Tag";

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Activity activity = getActivity();
        final View view = getInflater().inflate(
                R.layout.use_location_dialog,
                activity.findViewById(R.id.use_location_dialog_root)
        );

        final Button enableLocationServiceBtn
                = view.findViewById(R.id.uld_enable_location_service_btn_view);
        enableLocationServiceBtn.setOnClickListener(
                v -> {
                    final Intent intent = IntentsHelper.makeOpenLocationSettingsIntent();
                    // Verify that the intent will resolve to an activity
                    if (intent.resolveActivity(activity.getPackageManager()) != null) {
                        startActivityForResult(intent, IntentsHelper.REQUEST_CODE_LOCATION_SETTINGS);
                    } else {
                        SafeToast.showAnyThread(
                                activity.getApplicationContext(),
                                getString(R.string.no_location_setting_desc)
                        );
                    }
                }
        );

        setWindowDimensions(view, 0.9f, 0.9f);

        final AlertDialog.Builder builder = createAlertDialogBuilderWithOkButton(activity);
        builder.setTitle(getString(R.string.location_service));
        builder.setView(view);
        return builder.create();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IntentsHelper.REQUEST_CODE_LOCATION_SETTINGS) {
            ((MainActivity) getActivity()).processLocationCallback();
        }
    }
}
