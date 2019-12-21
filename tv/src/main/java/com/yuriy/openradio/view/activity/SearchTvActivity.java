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

package com.yuriy.openradio.view.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.fragment.app.FragmentActivity;

import com.yuriy.openradio.R;
import com.yuriy.openradio.utils.AppUtils;
import com.yuriy.openradio.view.fragment.SearchTvFragment;

/**
 *
 */
public final class SearchTvActivity extends FragmentActivity {

    public static final int SEARCH_TV_ACTIVITY_REQUEST_CODE = 5839;
    private SearchTvFragment mFragment;

    public static Intent makeStartIntent(final Context context) {
        return new Intent(context, SearchTvActivity.class);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv_search);
        mFragment = (SearchTvFragment) getSupportFragmentManager().findFragmentById(R.id.search_fragment);
    }

    @Override
    public boolean onSearchRequested() {
        mFragment.startRecognition();
        return true;
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        // If there are no results found, press the left key to reselect the microphone
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            mFragment.focusOnSearch();
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onSearchDialogClick(final String queryString) {
        // Save search query string, retrieve it later in the service
        AppUtils.setSearchQuery(queryString);
        setResult(RESULT_OK, new Intent());
        finish();
    }
}
