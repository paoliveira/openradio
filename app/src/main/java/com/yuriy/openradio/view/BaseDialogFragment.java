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

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;

import com.yuriy.openradio.R;

import java.lang.reflect.InvocationTargetException;

/**
 * Created with Android Studio.
 * Author: Chernyshov Yuriy - Mobile Development
 * Date: 28.05.14
 * Time: 13:32
 */

/**
 * {@link BaseDialogFragment} is a base class to display Dialog.
 */
public class BaseDialogFragment extends DialogFragment {

    /**
     * String tag to use in the logging message.
     */
    private static final String LOG_TAG = BaseDialogFragment.class.getSimpleName();

    /**
     * Factory method to create instance of the provided class.
     *
     * @param className Name of the class to have instance of.
     * @return Instance of the class.
     */
    public static BaseDialogFragment newInstance(final String className) {

        BaseDialogFragment baseDialogFragment = null;
        try {
            baseDialogFragment = (BaseDialogFragment) Class.forName(className).getConstructor()
                    .newInstance();
        } catch (java.lang.InstantiationException e) {
            Log.e(LOG_TAG, "New instance InstantiationException:" + e.getMessage());
        } catch (IllegalAccessException e) {
            Log.e(LOG_TAG, "New instance IllegalAccessException:" + e.getMessage());
        } catch (InvocationTargetException e) {
            Log.e(LOG_TAG, "New instance InvocationTargetException:" + e.getMessage());
        } catch (NoSuchMethodException e) {
            Log.e(LOG_TAG, "New instance NoSuchMethodException:" + e.getMessage());
        } catch (ClassNotFoundException e) {
            Log.e(LOG_TAG, "New instance ClassNotFoundException:" + e.getMessage());
        }
        return baseDialogFragment;
    }

    /**
     * Help method to return builder of the {@link AlertDialog} with Cancel button.
     *
     * @param context Context of the place where dialog must be shown.
     * @return builder of the {@link AlertDialog}.
     */
    protected static AlertDialog.Builder createAlertDialogWithCancelButton(final Context context) {
        final AlertDialog.Builder builder = createAlertDialog(context);
        builder.setNegativeButton(R.string.close_label, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        return builder;
    }

    /**
     * Help method to return builder of the {@link AlertDialog} with Cancel button.
     *
     * @param context Context of the place where dialog must be shown.
     * @return builder of the {@link AlertDialog}.
     */
    protected static AlertDialog.Builder createAlertDialogWithOkButton(final Context context) {
        final AlertDialog.Builder builder = createAlertDialog(context);
        builder.setPositiveButton(R.string.ok_label, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        return builder;
    }

    /**
     * Help method to return builder of the {@link AlertDialog}.
     *
     * @param context Context of the place where dialog must be shown.
     * @return builder of the {@link AlertDialog}.
     */
    protected static AlertDialog.Builder createAlertDialog(final Context context) {
        return new AlertDialog.Builder(context);
    }

    /**
     * Factory method to return {@link LayoutInflater} for the Dialog.
     *
     * @return {@link LayoutInflater}.
     */
    protected LayoutInflater getInflater() {
        return LayoutInflater.from(getActivity());
    }
}
