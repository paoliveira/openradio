package com.xenione.libs.swipemaker;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.core.view.ViewCompat;

import com.yuriy.openradio.R;

/**
 * Created on 06/04/16.
 */
public abstract class AbsCoordinatorLayout extends FrameLayout implements SwipeLayout.OnTranslateChangeListener {

    private SwipeLayout mForegroundView;
    private int mStartPosition = 0;

    public AbsCoordinatorLayout(final Context context) {
        super(context);
    }

    public AbsCoordinatorLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public AbsCoordinatorLayout(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AbsCoordinatorLayout(final Context context, final AttributeSet attrs,
                                final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        doInitialViewsLocation();
    }

    public abstract void doInitialViewsLocation();

    public void startWith(final int position) {
        mStartPosition = position;
        mForegroundView.startWith(position);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mForegroundView = findViewById(R.id.foregroundView);
        mForegroundView.setOnTranslateChangeListener(this);
    }

    public void sync() {
        if (!isInEditMode()) {
            ViewCompat.postOnAnimation(this, () -> mForegroundView.translateTo(mStartPosition));
        }
    }
}
