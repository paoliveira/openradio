package com.yuriy.openradio.service;

import androidx.leanback.media.PlayerAdapter;

import com.yuriy.openradio.utils.AppLogger;

public class ServicePlayerTvAdapter extends PlayerAdapter {

    private static final String CLASS_NAME = ServicePlayerTvAdapter.class.getSimpleName();

    public ServicePlayerTvAdapter() {
        super();
    }

    @Override
    public void play() {
        AppLogger.d(CLASS_NAME + " play");
    }

    @Override
    public void pause() {
        AppLogger.d(CLASS_NAME + " pause");
    }
}
