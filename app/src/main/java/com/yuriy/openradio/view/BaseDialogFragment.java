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

package com.yuriy.openradio.view;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import androidx.fragment.app.DialogFragment;

import com.yuriy.openradio.R;
import com.yuriy.openradio.utils.FabricUtils;

/**
 * Created with Android Studio.
 * Author: Chernyshov Yuriy - Mobile Development
 * Date: 28.05.14
 * Time: 13:32
 * <p>
 * {@link BaseDialogFragment} is a base class to display Dialog.
 */
public abstract class BaseDialogFragment extends DialogFragment {

    /**
     * Factory method to create instance of the provided class.
     *
     * @param className Name of the class to have instance of.
     * @return Instance of the class.
     */
    public static DialogFragment newInstance(final String className) {
        return newInstance(className, null);
    }

    /**
     *
     * @param className
     * @param bundle
     * @return
     */
    public static DialogFragment newInstance(final String className, final Bundle bundle) {

        DialogFragment dialogFragment = null;
        try {
            dialogFragment = (BaseDialogFragment) Class.forName(className).getConstructor().newInstance();
            dialogFragment.setArguments(bundle);
        } catch (final Exception e) {
            FabricUtils.logException(e);
        }
        return dialogFragment;
    }

    /**
     * Help method to return builder of the {@link AlertDialog} with Cancel button.
     *
     * @param context Context of the place where dialog must be shown.
     * @return builder of the {@link AlertDialog}.
     */
    protected static AlertDialog.Builder createAlertDialogBuilderWithCancelButton(final Context context) {
        final AlertDialog.Builder builder = createAlertDialogBuilder(context);
        builder.setNegativeButton(R.string.close_label, (dialog, id) -> dialog.cancel());
        return builder;
    }

    /**
     * Help method to return builder of the {@link AlertDialog} with Cancel button.
     *
     * @param context Context of the place where dialog must be shown.
     * @return builder of the {@link AlertDialog}.
     */
    protected static AlertDialog.Builder createAlertDialogBuilderWithOkButton(final Context context) {
        final AlertDialog.Builder builder = createAlertDialogBuilder(context);
        builder.setPositiveButton(R.string.ok_label, (dialog, id) -> dialog.cancel());
        return builder;
    }

    /**
     * Help method to return builder of the {@link AlertDialog}.
     *
     * @param context Context of the place where dialog must be shown.
     * @return builder of the {@link AlertDialog}.
     */
    protected static AlertDialog.Builder createAlertDialogBuilder(final Context context) {
        return new AlertDialog.Builder(context);
    }

    /**
     *
     * @param view
     * @return
     */
    protected AlertDialog createAlertDialog(final View view) {
        final AlertDialog.Builder builder = createAlertDialogBuilder(getActivity());
        builder.setView(view);
        return builder.create();
    }

    /**
     * Factory method to return {@link LayoutInflater} for the Dialog.
     *
     * @return {@link LayoutInflater}.
     */
    protected LayoutInflater getInflater() {
        return LayoutInflater.from(getActivity());
    }

    /**
     *
     * @param view
     * @param widthPercentage
     * @param heightPercentage
     */
    protected void setWindowDimensions(final View view, final float widthPercentage, final float heightPercentage) {
        final Rect displayRectangle = new Rect();
        final Window window = getActivity().getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
        view.setMinimumWidth((int) (displayRectangle.width() * widthPercentage));
        view.setMinimumHeight((int) (displayRectangle.height() * heightPercentage));
    }
}
