/*
 * Copyright 2017-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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
package com.yuriy.openradio.shared.view

import android.app.AlertDialog
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.DialogFragment

/**
 * Created with Android Studio.
 * Author: Chernyshov Yuriy - Mobile Development
 * Date: 28.05.14
 * Time: 13:32
 *
 *
 * [BaseDialogFragment] is a base class to display Dialog.
 */
abstract class BaseDialogFragment : DialogFragment() {
    /**
     *
     * @param view
     * @return
     */
    protected fun createAlertDialog(view: View?): AlertDialog {
        val builder = createAlertDialogBuilder(activity)
        builder.setView(view)
        return builder.create()
    }

    /**
     * Factory method to return [LayoutInflater] for the Dialog.
     *
     * @return [LayoutInflater].
     */
    protected val inflater: LayoutInflater
        get() = LayoutInflater.from(activity)

    /**
     *
     * @param view
     * @param widthPercentage
     * @param heightPercentage
     */
    protected fun setWindowDimensions(view: View, widthPercentage: Float, heightPercentage: Float) {
        val displayRectangle = Rect()
        val window = activity!!.window
        window.decorView.getWindowVisibleDisplayFrame(displayRectangle)
        view.minimumWidth = (displayRectangle.width() * widthPercentage).toInt()
        view.minimumHeight = (displayRectangle.height() * heightPercentage).toInt()
    }

    companion object {
        /**
         * Factory method to create instance of the provided class.
         *
         * @param className Name of the class to have instance of.
         * @return Instance of the class.
         */
        fun newInstance(className: String, bundle: Bundle? = null): DialogFragment {
            val dialogFragment = Class.forName(className).getConstructor().newInstance() as BaseDialogFragment
            dialogFragment.arguments = bundle
            return dialogFragment
        }

        /**
         * Help method to return builder of the [AlertDialog].
         *
         * @param context Context of the place where dialog must be shown.
         * @return builder of the [AlertDialog].
         */
        protected fun createAlertDialogBuilder(context: Context?): AlertDialog.Builder {
            return AlertDialog.Builder(context)
        }
    }
}
