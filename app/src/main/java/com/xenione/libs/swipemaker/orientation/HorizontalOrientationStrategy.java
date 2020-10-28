package com.xenione.libs.swipemaker.orientation;

import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Eugeni on 28/09/2016.
 */
public class HorizontalOrientationStrategy extends OrientationStrategy {

    private int mLastTouchX;
    private boolean mIsDragging;
    private final View mView;

    public HorizontalOrientationStrategy(final View view) {
        super(view);
        mView = view;
    }

    @Override
    public boolean onInterceptTouchEvent(final MotionEvent event) {
        if (isDragDisabled()) {
            return false;
        }
        final int action = event.getAction();
        final int eventX = (int) event.getX();
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            fling();
            return false;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mLastTouchX = eventX;
                mHelperScroller.finish();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                int deltaX = Math.abs(eventX - mLastTouchX);
                mIsDragging = deltaX > mTouchSlop;
                if (mIsDragging) {
                    disallowParentInterceptTouchEvent(true);
                    mLastTouchX = eventX;
                }
            }
        }

        return mIsDragging;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        final int action = event.getAction();
        final int eventX = (int) event.getX();
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            if (isDragDisabled()) {
                return Math.abs(mLastTouchX - eventX) > 50;
            }

            if (mIsDragging) {
                disallowParentInterceptTouchEvent(false);
            }
            boolean isFling = fling();
            boolean handled = mIsDragging | isFling;
            mIsDragging = false;
            return handled;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mLastTouchX = eventX;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (isDragDisabled()) {
                    break;
                }
                final int deltaX = eventX - mLastTouchX;
                if (mIsDragging) {
                    translateBy(deltaX);
                } else if (Math.abs(deltaX) > mTouchSlop) {
                    disallowParentInterceptTouchEvent(true);
                    mLastTouchX = eventX;
                    mIsDragging = true;
                }
                break;
            }
        }

        return mIsDragging;
    }

    @Override
    int getDelta() {
        return (int) mView.getTranslationX();
    }

    @Override
    void setDelta(int delta) {
        mView.setTranslationX(delta);
    }
}
