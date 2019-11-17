package com.yuriy.openradio.view.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.app.PlaybackSupportFragment;
import androidx.leanback.app.PlaybackSupportFragmentGlueHost;
import androidx.leanback.media.PlaybackBannerControlGlue;
import androidx.leanback.widget.BaseOnItemViewClickedListener;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.PageRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.RowPresenter;

import com.yuriy.openradio.service.ServicePlayerTvAdapter;

public class PlayerTvFragment
        extends PlaybackSupportFragment
        implements BaseOnItemViewClickedListener, BrowseSupportFragment.MainFragmentAdapterProvider {

    private PlaybackBannerControlGlue<ServicePlayerTvAdapter> mMediaPlayerGlue;

    private final BrowseSupportFragment.MainFragmentAdapter<Fragment> mMainFragmentAdapter =

            new BrowseSupportFragment.MainFragmentAdapter<Fragment>(this) {
                @Override
                public void setEntranceTransitionState(boolean state) {
                    //PlayerTvFragment.this.setEntranceTransitionState(state);
                }
            };

    public PlayerTvFragment() {
        super();
        setControlsOverlayAutoHideEnabled(false);
    }

    static PageRow createPageRow(final byte id, final String name) {
        return new PageRow(new HeaderItem(id, name));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMediaPlayerGlue = new PlaybackBannerControlGlue<>(
                getActivity(),
                new int[]{0, 1},
                new ServicePlayerTvAdapter()
        );
        mMediaPlayerGlue.setHost(new PlaybackSupportFragmentGlueHost(this));

        mMediaPlayerGlue.setTitle("Title");
        mMediaPlayerGlue.setSubtitle("SubTitle");
//        mMediaPlayerGlue.setArt(getResources().getDrawable(R.drawable.ic_launcher));
        String uriPath = "android.resource://com.example.android.leanback/raw/video";
//        mMediaPlayerGlue.getPlayerAdapter().setDataSource(Uri.parse(uriPath));
//        mMediaPlayerGlue.playWhenPrepared();
    }

    @Override
    public void onResume() {
        super.onResume();
        showControlsOverlay(true);
    }

    @Override
    public void setControlsOverlayAutoHideEnabled(boolean enabled) {
        super.setControlsOverlayAutoHideEnabled(false);
    }

    @Override
    public void onItemClicked(final Presenter.ViewHolder itemViewHolder,
                              final Object item,
                              final RowPresenter.ViewHolder rowViewHolder,
                              final Object row) {

    }

    @Override
    public BrowseSupportFragment.MainFragmentAdapter getMainFragmentAdapter() {
        return mMainFragmentAdapter;
    }
}
