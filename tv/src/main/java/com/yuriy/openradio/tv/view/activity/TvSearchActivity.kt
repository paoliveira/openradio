/*
 * Copyright 2019-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.tv.view.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.tv.R
import com.yuriy.openradio.tv.view.fragment.TvSearchFragment

/**
 *
 */
class TvSearchActivity : FragmentActivity() {

    private var mFragment: TvSearchFragment? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tv_search)
        mFragment = supportFragmentManager.findFragmentById(R.id.search_fragment) as TvSearchFragment?
    }

    override fun onSearchRequested(): Boolean {
        mFragment!!.startRecognition()
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // If there are no results found, press the left key to reselect the microphone
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            mFragment!!.focusOnSearch()
        }
        return super.onKeyDown(keyCode, event)
    }

    fun onSearchDialogClick(queryString: String) {
        val intent = Intent()
        intent.putExtras(AppUtils.makeSearchQueryBundle(queryString))
        setResult(RESULT_OK, intent)
        finish()
    }

    companion object {

        const val SEARCH_TV_ACTIVITY_REQUEST_CODE = 5839

        fun makeStartIntent(context: Context): Intent {
            return Intent(context, TvSearchActivity::class.java)
        }
    }
}
