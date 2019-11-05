package com.yuriy.openradio.view.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnChildLaidOutListener;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.PageRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.ShadowOverlayContainer;
import androidx.leanback.widget.VerticalGridPresenter;
import androidx.leanback.widget.VerticalGridView;

import com.yuriy.openradio.R;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A fragment for rendering items in a vertical grids.
 */
public class GridTvFragment
        extends Fragment
        implements BrowseSupportFragment.MainFragmentAdapterProvider {

    private static final String CLASS_NAME = GridTvFragment.class.getSimpleName();

    private ObjectAdapter mAdapter;
    private VerticalGridPresenter mGridPresenter;
    private VerticalGridPresenter.ViewHolder mGridViewHolder;
    private OnItemViewSelectedListener mOnItemViewSelectedListener;
    private OnItemViewClickedListener mOnItemViewClickedListener;
    private View.OnClickListener mOnBackClickListener;
    private int mSelectedPosition = -1;
    private AtomicBoolean mIsBackBtnInit;
    private final BrowseSupportFragment.MainFragmentAdapter<Fragment> mMainFragmentAdapter =
            new BrowseSupportFragment.MainFragmentAdapter<Fragment>(this) {
                @Override
                public void setEntranceTransitionState(boolean state) {
                    GridTvFragment.this.setEntranceTransitionState(state);
                }
            };

    static PageRow createPageRow(final byte id, final String name) {
        return new PageRow(new HeaderItem(id, name));
    }

    public GridTvFragment() {
        super();
        mIsBackBtnInit = new AtomicBoolean(false);
    }

    /**
     * Sets the grid presenter.
     */
    void setGridPresenter(final VerticalGridPresenter gridPresenter) {
        if (gridPresenter == null) {
            throw new IllegalArgumentException("Grid presenter may not be null");
        }
        mGridPresenter = gridPresenter;
        mGridPresenter.setOnItemViewSelectedListener(mViewSelectedListener);
        mGridPresenter.setOnItemViewClickedListener(mViewClickedListener);
    }

    /**
     * Sets the object adapter for the fragment.
     */
    public void setAdapter(final ObjectAdapter adapter) {
        mAdapter = adapter;
        updateAdapter();
    }

    private final OnItemViewSelectedListener mViewSelectedListener =
            new OnItemViewSelectedListener() {
                @Override
                public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                           RowPresenter.ViewHolder rowViewHolder, Row row) {
                    int position = mGridViewHolder.getGridView().getSelectedPosition();
                    gridOnItemSelected(position);
                    if (mOnItemViewSelectedListener != null) {
                        mOnItemViewSelectedListener.onItemSelected(itemViewHolder, item,
                                rowViewHolder, row);
                    }
                }
            };

    private final OnItemViewClickedListener mViewClickedListener =
            new OnItemViewClickedListener() {

                @Override
                public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                          RowPresenter.ViewHolder rowViewHolder, Row row) {
                    if (mOnItemViewClickedListener != null) {
                        mOnItemViewClickedListener.onItemClicked(itemViewHolder, item,
                                rowViewHolder, row);
                    }
                }
            };

    private final OnChildLaidOutListener mChildLaidOutListener =
            (parent, view, position, id) -> {
                if (position == 0) {
                    showOrHideTitle();
                }
            };

    /**
     * Sets an item selection listener.
     */
    public void setOnItemViewSelectedListener(final OnItemViewSelectedListener value) {
        mOnItemViewSelectedListener = value;
    }

    /**
     * Sets an item clicked listener.
     */
    public void setOnItemViewClickedListener(final OnItemViewClickedListener value) {
        mOnItemViewClickedListener = value;
    }

    public void setOnBackClickListener(final View.OnClickListener value) {
        mOnBackClickListener = value;
    }

    public void onDataLoaded() {
        if (!mIsBackBtnInit.get()) {
            new Handler().postDelayed(this::initBackButton, 1000);
        }
    }

    public void showBackButton() {
        if (getView() == null) {
            return;
        }
        final ImageView backBtn = getView().findViewById(R.id.tv_back_btn_view);
        if (backBtn == null) {
            return;
        }
        backBtn.setVisibility(View.VISIBLE);
    }

    public void hideBackButton() {
        if (getView() == null) {
            return;
        }
        final ImageView backBtn = getView().findViewById(R.id.tv_back_btn_view);
        if (backBtn == null) {
            return;
        }
        backBtn.setVisibility(View.INVISIBLE);
    }

    private void initBackButton() {
        if (getView() == null) {
            return;
        }
        final ImageView backBtn = getView().findViewById(R.id.tv_back_btn_view);
        final VerticalGridView view = getView().findViewById(R.id.browse_grid);
        ShadowOverlayContainer nextChild;
        for (int i = 0; i < view.getChildCount(); ++i) {
            nextChild = (ShadowOverlayContainer)view.getChildAt(i);
            if (nextChild == null) {
                continue;
            }
            backBtn.setX(nextChild.getX() + 5);
            backBtn.setY(nextChild.getY() - backBtn.getHeight());
            break;
        }
        backBtn.setOnClickListener(
                v -> {
                    if (mOnBackClickListener != null) {
                        mOnBackClickListener.onClick(v);
                    }
                }
        );
        mIsBackBtnInit.set(true);
    }

    private void gridOnItemSelected(int position) {
        if (position != mSelectedPosition) {
            mSelectedPosition = position;
            showOrHideTitle();
        }
    }

    private void showOrHideTitle() {
        if (mGridViewHolder.getGridView().findViewHolderForAdapterPosition(mSelectedPosition) == null) {
            return;
        }
        if (!mGridViewHolder.getGridView().hasPreviousViewInSameRow(mSelectedPosition)) {
            mMainFragmentAdapter.getFragmentHost().showTitleView(true);
        } else {
            mMainFragmentAdapter.getFragmentHost().showTitleView(false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.grid_fragment, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewGroup gridDock = view.findViewById(R.id.browse_grid_dock);
        mGridViewHolder = mGridPresenter.onCreateViewHolder(gridDock);
        gridDock.addView(mGridViewHolder.view);
        mGridViewHolder.getGridView().setOnChildLaidOutListener(mChildLaidOutListener);

        getMainFragmentAdapter().getFragmentHost().notifyViewCreated(mMainFragmentAdapter);
        updateAdapter();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mGridViewHolder = null;
    }

    @Override
    public BrowseSupportFragment.MainFragmentAdapter getMainFragmentAdapter() {
        return mMainFragmentAdapter;
    }

    /**
     * Sets the selected item position.
     */
    public void setSelectedPosition(int position) {
        mSelectedPosition = position;
        if (mGridViewHolder != null && mGridViewHolder.getGridView().getAdapter() != null) {
            mGridViewHolder.getGridView().setSelectedPositionSmooth(position);
        }
    }

    private void updateAdapter() {
        if (mGridViewHolder == null) {
            return;
        }
        mGridPresenter.onBindViewHolder(mGridViewHolder, mAdapter);
        if (mSelectedPosition != -1) {
            mGridViewHolder.getGridView().setSelectedPosition(mSelectedPosition);
        }
    }

    private void setEntranceTransitionState(boolean afterTransition) {
        mGridPresenter.setEntranceTransitionState(mGridViewHolder, afterTransition);
    }
}
