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
package com.yuriy.openradio.tv.view.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ObjectAdapter
import com.yuriy.openradio.shared.permission.PermissionChecker
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.findView
import com.yuriy.openradio.tv.R
import com.yuriy.openradio.tv.view.activity.TvSearchActivity

/*
 * This class demonstrates how to do in-app search
 */
class TvSearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {

    private val mHandler = Handler(Looper.getMainLooper())

    private var mRowsAdapter: ArrayObjectAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        setSearchResultProvider(this)
    }

    override fun onResume() {
        super.onResume()
        val context = context
        if (context == null) {
            AppLogger.e("Can't do resume pause on null context")
            return
        }
        val view = view
        if (view == null) {
            AppLogger.e("Can't do resume pause on null view")
            return
        }
        val frame = view.findView(R.id.lb_search_frame)
        if (!PermissionChecker.isRecordAudioGranted(context)) {
            PermissionChecker.requestRecordAudioPermission(requireActivity(), frame)
        }
    }

    override fun onPause() {
        mHandler.removeCallbacksAndMessages(null)
        try {
            super.onPause()
        } catch (e: Exception) {
            AppLogger.e("Can't do normal pause of TV activity", e)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        AppLogger.i("$CLASS_NAME On activity result:$resultCode")
        when (requestCode) {
            REQUEST_SPEECH -> when (resultCode) {
                Activity.RESULT_OK -> {
                    var searchQuery = AppUtils.EMPTY_STRING
                    if (data != null && data.extras != null) {
                        searchQuery = AppUtils.getSearchQueryFromBundle(data.extras!!)
                    }
                    setSearchQuery(searchQuery, true)
                }
            }
        }
    }

    override fun getResultsAdapter(): ObjectAdapter {
        return mRowsAdapter!!
    }

    override fun onQueryTextChange(newQuery: String): Boolean {
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        AppLogger.i(CLASS_NAME + String.format(" Search text submitted: %s", query))
        if (activity is TvSearchActivity) {
            val activity = activity as TvSearchActivity?
            activity?.onSearchDialogClick(query)
        }
        return true
    }

    fun focusOnSearch() {
        requireView().findView(R.id.lb_search_bar).requestFocus()
    }

    companion object {
        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME = TvSearchFragment::class.java.simpleName
        private const val REQUEST_SPEECH = 0x00000010
    }
}
