package com.yuriy.openradio.view.activity;

import android.os.Bundle;

import com.yuriy.openradio.R;

/*
 * Main TV Activity class that loads main TV fragment.
 */
public final class MainTvActivity extends LeanbackActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_tv);
    }
}
