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
import com.xenione.libs.swipemaker.orientation.VerticalOrientationStrategy;

/**
 * Created by Eugeni on 10/04/2016.
 */
public final class SwipeLayout extends RelativeLayout {

    public enum Orientation {
        HORIZONTAL,
        VERTICAL;

        Orientation() {
        }

        private OrientationStrategyFactory get() {
            switch (this) {
                case HORIZONTAL: {
                    return new HorizontalOrientationStrategyFactory();
                }
                default:
                case VERTICAL: {
                    return new VerticalOrientationStrategyFactory();
                }
            }
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
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SwipeLayout(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setOrientation(final Orientation orientation) {
        mOrientationStrategy = orientation.get().make(this);
    }

    public void setOrientation(final OrientationStrategyFactory factory) {
        mOrientationStrategy = factory.make(this);
    }

    public void startWith(int position) {
        makeSureOrientationStrategy();
        mOrientationStrategy.startWith(position);
    }

    public void anchor(final Integer... points) {
        makeSureOrientationStrategy();
        mOrientationStrategy.setAnchor(points);
    }

    public void setOnTranslateChangeListener(final OnTranslateChangeListener listener) {
        makeSureOrientationStrategy();
        mOrientationStrategy.setOnTranslateChangeListener(listener);
    }

    @Override
    public boolean onInterceptTouchEvent(final MotionEvent event) {
        makeSureOrientationStrategy();
        return mOrientationStrategy.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        makeSureOrientationStrategy();
        final boolean handled = mOrientationStrategy.onTouchEvent(event);
        if (!handled) {
            super.onTouchEvent(event);
        }
        return true;
    }

    public void translateTo(final int position) {
        mOrientationStrategy.translateTo(position);
    }

    private void makeSureOrientationStrategy() {
        if (mOrientationStrategy != null) {
            return;
        }
        mOrientationStrategy = Orientation.HORIZONTAL.get().make(this);
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

    private static class VerticalOrientationStrategyFactory implements OrientationStrategyFactory {

        private VerticalOrientationStrategyFactory() {
            super();
        }

        @Override
        public OrientationStrategy make(final View view) {
            return new VerticalOrientationStrategy(view);
        }
    }
}
