/*
 * Copyright 2019 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.gabor.view.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;

import com.yuriy.openradio.gabor.R;
import com.yuriy.openradio.gabor.shared.permission.PermissionChecker;
import com.yuriy.openradio.gabor.shared.utils.AppLogger;
import com.yuriy.openradio.gabor.shared.view.SafeToast;
import com.yuriy.openradio.gabor.view.activity.TvSearchActivity;

/*
 * This class demonstrates how to do in-app search
 */
public class TvSearchFragment extends SearchSupportFragment
        implements SearchSupportFragment.SearchResultProvider {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = TvSearchFragment.class.getSimpleName();

    private static final int REQUEST_SPEECH = 0x00000010;

    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getContext();

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        setSearchResultProvider(this);

        if (!PermissionChecker.isGranted(context, Manifest.permission.RECORD_AUDIO)) {
            SafeToast.showAnyThread(
                    context, context.getString(R.string.record_audio_permission_not_granted)
            );
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        AppLogger.i(CLASS_NAME + " On activity result:" + resultCode);
        switch (requestCode) {
            case REQUEST_SPEECH:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        setSearchQuery(data, true);
                        break;
                    default:
                        // If recognizer is canceled or failed, keep focus on the search orb
//                        if (FINISH_ON_RECOGNIZER_CANCELED) {
//                            if (!hasResults()) {
//                                Log.v(CLASS_NAME, "Voice search canceled");
//                                getView().findViewById(R.id.lb_search_bar_speech_orb).requestFocus();
//                            }
//                        }
                        break;
                }
                break;
        }
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(final String query) {
        AppLogger.i(CLASS_NAME + String.format(" Search text submitted: %s", query));
        if (getActivity() instanceof TvSearchActivity) {
            final TvSearchActivity activity = (TvSearchActivity) getActivity();

            if (activity != null) {
                activity.onSearchDialogClick(query);
            }
        }
        return true;
    }

    public void focusOnSearch() {
        getView().findViewById(R.id.lb_search_bar).requestFocus();
    }
}
