package com.yuriy.openradio.view.activity;

import android.content.Intent;

import androidx.fragment.app.FragmentActivity;

/**
 * This class contains common methods that run in every activity such as search.
 */
public abstract class LeanbackActivity extends FragmentActivity {

    @Override
    public boolean onSearchRequested() {
        startActivity(new Intent(this, SearchTvActivity.class));
        return true;
    }
}
