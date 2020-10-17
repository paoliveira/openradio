package com.xenione.libs.swipemaker;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.xenione.libs.swipemaker.orientation.HorizontalOrientationStrategy;
import com.xenione.libs.swipemaker.orientation.OrientationStrategy;
import com.xenione.libs.swipemaker.orientation.OrientationStrategyFactory;

/**
 * Created by Eugeni on 10/04/2016.
 */
public final class SwipeLayout extends RelativeLayout {

    public enum Orientation {
        HORIZONTAL;

        Orientation() {
        }

        private OrientationStrategyFactory get() {
            return new HorizontalOrientationStrategyFactory();
        }
    }

    public interface OnTranslateChangeListener {
        void onTranslateChange(float globalPercent, int index, float relativePercent);
    }

    private OrientationStrategy mOrientationStrategy;

    public SwipeLayout(final Context context) {
        this(context, null);
    }

    public SwipeLayout(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeLayout(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mOrientationStrategy = Orientation.HORIZONTAL.get().make(this);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SwipeLayout(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void startWith(int position) {
        mOrientationStrategy.startWith(position);
    }

    public void anchor(final Integer... points) {
        mOrientationStrategy.setAnchor(points);
    }

    public void setOnTranslateChangeListener(final OnTranslateChangeListener listener) {
        mOrientationStrategy.setOnTranslateChangeListener(listener);
    }

    public void isDragDisabled(final boolean value) {
        mOrientationStrategy.isDragDisabled(value);
    }

    @Override
    public boolean onInterceptTouchEvent(final MotionEvent event) {
        return mOrientationStrategy.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (!mOrientationStrategy.onTouchEvent(event)) {
            super.onTouchEvent(event);
        }
        return true;
    }

    public void translateTo(final int position) {
        mOrientationStrategy.translateTo(position);
    }

    private static class HorizontalOrientationStrategyFactory implements OrientationStrategyFactory {

        private HorizontalOrientationStrategyFactory() {
            super();
        }

        @Override
        public OrientationStrategy make(final View view) {
            return new HorizontalOrientationStrategy(view);
        }
    }
}
