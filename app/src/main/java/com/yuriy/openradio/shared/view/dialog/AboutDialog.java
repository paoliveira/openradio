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

package com.yuriy.openradio.shared.view.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.utils.AppUtils;
import com.yuriy.openradio.shared.utils.IntentUtils;
import com.yuriy.openradio.shared.view.BaseDialogFragment;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class AboutDialog extends BaseDialogFragment {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = AboutDialog.class.getSimpleName();

    /**
     * Tag string to use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

    /**
     * My profile's url
     */
    private static final String AUTHOR_PROFILE_URL
            = "https://www.linkedin.com/in/yurii-chernyshov/";

    /**
     * Project's url
     */
    private static final String PROJECT_HOME_URL
            = "https://bitbucket.org/ChernyshovYuriy/openradio";

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        final View view = getInflater().inflate(
                R.layout.dialog_about,
                getActivity().findViewById(R.id.dialog_about_root)
        );

        setWindowDimensions(view, 0.9f, 0.9f);

        final Context context = getContext();
        final String titleText = context.getString(R.string.app_name);
        final TextView title = view.findViewById(R.id.dialog_about_title_view);
        title.setText(titleText);

        final TextView authorLink = view.findViewById(R.id.about_author_link_view);
        authorLink.setOnClickListener(
                v -> {
                    final Intent intent = IntentUtils.makeUrlBrowsableIntent(AUTHOR_PROFILE_URL);
                    AppUtils.startActivitySafe(context, intent);
                }
        );

        final TextView projectHomeLink = view.findViewById(R.id.about_project_link_view);
        projectHomeLink.setOnClickListener(
                v -> {
                    final Intent intent = IntentUtils.makeUrlBrowsableIntent(PROJECT_HOME_URL);
                    AppUtils.startActivitySafe(context, intent);
                }
        );

        final TextView exoPlayerVersion = view.findViewById(R.id.exo_player_version_view);
        final String exoPlayerVersionText
                = getString(R.string.about_exo_text) + " " + ExoPlayerLibraryInfo.VERSION;
        exoPlayerVersion.setText(exoPlayerVersionText);

        return createAlertDialog(view);
    }
}
