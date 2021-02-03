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

package com.yuriy.openradio.gabor.view.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yuriy.openradio.gabor.R;
import com.yuriy.openradio.gabor.shared.view.BaseDialogFragment;
import com.yuriy.openradio.gabor.view.activity.MainActivity;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class RSSettingsDialog extends BaseDialogFragment {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = RSSettingsDialog.class.getSimpleName();

    /**
     * Tag string to use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

    private static final String KEY_MEDIA_ITEM = CLASS_NAME + "_KEY_MEDIA_ITEM";

    @Override
    @NonNull
    public Dialog onCreateDialog(@Nullable final Bundle savedInstance) {
        final MainActivity activity = (MainActivity) getActivity();

        final View view = getInflater().inflate(
                R.layout.dialog_rs_settings,
                activity.findViewById(R.id.dialog_rs_settings_root)
        );

        setWindowDimensions(view, 0.8f, 0.4f);

        final Bundle args = getArguments();
        final MediaBrowserCompat.MediaItem item = extractMediaItem(args);
        if (item == null) {
            view.findViewById(R.id.dialog_rs_settings_coming_soon).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.dialog_rs_settings_edit_remove).setVisibility(View.VISIBLE);
            view.findViewById(R.id.dialog_rs_settings_edit_btn).setTag(item);
            view.findViewById(R.id.dialog_rs_settings_remove_btn).setTag(item);
            final TextView name = view.findViewById(R.id.dialog_rs_settings_rs_name);
            name.setText(item.getDescription().getTitle());
        }

        return createAlertDialog(view);
    }

    public static void provideMediaItem(@NonNull final Bundle bundle,
                                        @NonNull final MediaBrowserCompat.MediaItem mediaItem) {
        bundle.putParcelable(KEY_MEDIA_ITEM, mediaItem);
    }

    @Nullable
    private static MediaBrowserCompat.MediaItem extractMediaItem(@Nullable final Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        if (!bundle.containsKey(KEY_MEDIA_ITEM)) {
            return null;
        }
        return (MediaBrowserCompat.MediaItem) bundle.getParcelable(KEY_MEDIA_ITEM);
    }
}
