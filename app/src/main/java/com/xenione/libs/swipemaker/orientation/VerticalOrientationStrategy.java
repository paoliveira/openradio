package com.xenione.libs.swipemaker.orientation;

import android.view.MotionEvent;
import android.view.View;

import androidx.core.view.MotionEventCompat;

/**
 * Created by Eugeni on 28/09/2016.
 */
public class VerticalOrientationStrategy extends OrientationStrategy {

    private int mLastTouchY;
    private boolean mIsDragging;
    private final View mView;

    public VerticalOrientationStrategy(final View view) {
        super(view);
        mView = view;
    }

    public VerticalOrientationStrategy(final View view, final int slopTouch) {
        super(view, slopTouch);
        mView = view;
    }

    @Override
    public boolean onInterceptTouchEvent(final MotionEvent event) {
        final int action = event.getAction();
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            fling();
            mIsDragging = false;
            return false;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mLastTouchY = (int) event.getY();
                mHelperScroller.finish();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                int deltaY = Math.abs((int) event.getY() - mLastTouchY);
                mIsDragging = deltaY > mTouchSlop;
                if (mIsDragging) {
                    disallowParentInterceptTouchEvent(true);
                    mLastTouchY = (int) event.getY();
                }
            }
        }

        return mIsDragging;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        final int action = event.getAction();
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
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
                mLastTouchY = (int) event.getY();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                int deltaY = (int) event.getY() - mLastTouchY;
                if (mIsDragging) {
                    translateBy(deltaY);
                } else if (Math.abs(deltaY) > mTouchSlop) {
                    disallowParentInterceptTouchEvent(true);
                    mLastTouchY = (int) event.getY();
                    mIsDragging = true;
                }
                break;
            }
        }

        return mIsDragging;
    }

    @Override
    int getDelta() {
        return (int) mView.getTranslationY();
    }

    @Override
    void setDelta(final int delta) {
        mView.setTranslationY(delta);
    }
}
