/*
 * Copyright 2017-2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.view.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import com.google.android.exoplayer2.ExoPlayerLibraryInfo
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.utils.IntentUtils
import com.yuriy.openradio.shared.utils.findImageView
import com.yuriy.openradio.shared.utils.findTextView
import com.yuriy.openradio.shared.view.BaseDialogFragment

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class AboutDialog : BaseDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflater.inflate(
            R.layout.dialog_about,
            requireActivity().findViewById(R.id.dialog_about_root)
        )
        setWindowDimensions(view, 0.9f, 0.9f)
        val context = context
        val titleText = context!!.getString(R.string.app_name)
        val title = view.findTextView(R.id.dialog_about_title_view)
        title.text = titleText
        val exoPlayerVersion = view.findTextView(R.id.about_exo_player_ver_link_view)
        val exoPlayerVersionText = getString(R.string.about_exo_text) + " " + ExoPlayerLibraryInfo.VERSION
        exoPlayerVersion.text = exoPlayerVersionText

        setOnClickOnText(context, view, R.id.about_author_link_view, IntentUtils.AUTHOR_PROFILE_URL)
        setOnClickOnText(context, view, R.id.about_project_link_view, IntentUtils.PROJECT_HOME_URL)
        setOnClickOnText(context, view, R.id.about_exo_player_ver_link_view, IntentUtils.EXO_PLAYER_URL)
        setOnClickOnText(context, view, R.id.about_report_issue_link_view, IntentUtils.REPORT_ISSUE_URL)
        setOnClickOnText(context, view, R.id.about_radio_browser_link_view, IntentUtils.RADIO_BROWSER_URL)
        setOnClickOnText(context, view, R.id.about_playlist_parser_name_view, IntentUtils.PLAY_LIST_PARSER_URL)
        setOnClickOnText(context, view, R.id.about_countries_boundaries_view, IntentUtils.OFFLINE_COUNTRIES_URL)
        setOnClickOnText(context, view, R.id.about_easy_swipe_name_view, IntentUtils.SWIPE_EFFECT_URL)
        setOnClickOnImage(context, view, R.id.about_item_ivan_fb_btn, IntentUtils.IVAN_FB_LINK)
        setOnClickOnImage(context, view, R.id.about_item_ivan_ig_btn, IntentUtils.IVAN_IG_LINK)

        return createAlertDialog(view)
    }

    companion object {
        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME = AboutDialog::class.java.simpleName

        /**
         * Tag string to use in dialog transactions.
         */
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"

        private fun setOnClickOnText(context: Context, view: View, viewId: Int, linkUrl: String) {
            view.findTextView(viewId).setOnClickListener {
                IntentUtils.startActivitySafe(context, IntentUtils.makeUrlBrowsableIntent(linkUrl))
            }
        }

        private fun setOnClickOnImage(context: Context, view: View, viewId: Int, linkUrl: String) {
            view.findImageView(viewId).setOnClickListener {
                IntentUtils.startActivitySafe(context, IntentUtils.makeUrlBrowsableIntent(linkUrl))
            }
        }
    }
}
