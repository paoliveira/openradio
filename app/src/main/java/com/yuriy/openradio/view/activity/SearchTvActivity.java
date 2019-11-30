package com.yuriy.openradio.view.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.fragment.app.FragmentActivity;

import com.yuriy.openradio.R;
import com.yuriy.openradio.view.fragment.SearchTvFragment;

/**
 *
 */
public final class SearchTvActivity extends FragmentActivity {

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
        if (mFragment.hasResults()) {
            startActivity(new Intent(this, SearchTvActivity.class));
        } else {
            mFragment.startRecognition();
        }
        return true;
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        // If there are no results found, press the left key to reselect the microphone
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && !mFragment.hasResults()) {
            mFragment.focusOnSearch();
        }
        return super.onKeyDown(keyCode, event);
    }
}
