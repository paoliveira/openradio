/*
 * Copyright 2016 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.view;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.yuriy.openradio.R;
import com.yuriy.openradio.utils.AppUtils;
import com.yuriy.openradio.utils.IntentsHelper;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public class AboutDialog extends DialogFragment {

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
            = "http://www.linkedin.com/pub/yuriy-chernyshov/1b/622/270";

    /**
     * Project's url
     */
    private static final String PROJECT_HOME_URL
            = "https://bitbucket.org/ChernyshovYuriy/openradio";

    /**
     * Create a new instance of {@link com.yuriy.openradio.view.AboutDialog}
     */
    @SuppressWarnings("all")
    public static AboutDialog newInstance() {
        final AboutDialog aboutDialog = new AboutDialog();
        // provide here an arguments, if any
        return aboutDialog;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.dialog_about, container, false);

        final String titleText = getActivity().getString(R.string.app_name) + "    v." +
                AppUtils.getApplicationVersion(getActivity());
        final TextView title = (TextView) view.findViewById(R.id.dialog_about_title_view);
        title.setText(titleText);

        final TextView authorLink = (TextView) view.findViewById(R.id.about_author_link_view);
        authorLink.setOnClickListener(
                v -> startActivity(IntentsHelper.makeUrlBrowsableIntent(AUTHOR_PROFILE_URL))
        );

        final TextView projectHomeLink = (TextView) view.findViewById(R.id.about_project_link_view);
        projectHomeLink.setOnClickListener(
                v -> startActivity(IntentsHelper.makeUrlBrowsableIntent(PROJECT_HOME_URL))
        );

        final Button closeBtn = (Button) view.findViewById(R.id.about_dialog_close_btn_view);
        closeBtn.setOnClickListener(
                v -> getDialog().dismiss()
        );

        return view;
    }
}
