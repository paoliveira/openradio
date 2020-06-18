package com.yuriy.openradio.shared.model;

import android.content.Context;

import androidx.annotation.NonNull;

import com.yuriy.openradio.shared.presenter.MediaPresenter;

public enum Dependencies {

    INSTANCE;

    private MediaPresenter mMediaPresenter;

    Dependencies() {

    }

    public void init(@NonNull final Context context) {
        mMediaPresenter = new MediaPresenter(context);
    }

    public MediaPresenter getMediaPresenter() {
        return mMediaPresenter;
    }
}
