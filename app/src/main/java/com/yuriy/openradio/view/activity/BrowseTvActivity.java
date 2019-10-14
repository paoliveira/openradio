package com.yuriy.openradio.view.activity;

import android.os.Bundle;

import com.yuriy.openradio.R;

/*
 * Browse TV Activity class that loads Radio Stations categories TV fragment.
 */
public final class BrowseTvActivity extends LeanbackActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browse_tv);
        getWindow().setBackgroundDrawableResource(R.drawable.main_tv_bg);
    }
}
