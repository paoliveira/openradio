package com.xenione.libs.swipemaker.orientation;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.OverScroller;

import androidx.core.view.ViewCompat;

import com.xenione.libs.swipemaker.Anchors;
import com.xenione.libs.swipemaker.Position;
import com.xenione.libs.swipemaker.ScrollerHelper;
import com.xenione.libs.swipemaker.SwipeLayout;

/**
 * Created by Eugeni on 28/09/2016.
 */
public abstract class OrientationStrategy implements Runnable {

    private final Position mPositionInfo;
    private final View mView;
    private SwipeLayout.OnTranslateChangeListener mOnTranslateChangeListener;
    final int mTouchSlop;
    ScrollerHelper mHelperScroller;

    public OrientationStrategy(final View view) {
        this(view, ViewConfiguration.get(view.getContext()).getScaledTouchSlop());
    }

    public OrientationStrategy(final View view, final int touchSlop) {
        mView = view;
        mTouchSlop = touchSlop;
        mHelperScroller = new ScrollerHelper(new OverScroller(mView.getContext()));
        mPositionInfo = new Position();
    }

    public void setAnchor(final Integer... points) {
        mPositionInfo.setAnchors(Anchors.make(points));
    }

    public void setOnTranslateChangeListener(final SwipeLayout.OnTranslateChangeListener listener) {
        mOnTranslateChangeListener = listener;
    }

    public abstract boolean onTouchEvent(final MotionEvent event);

    public abstract boolean onInterceptTouchEvent(final MotionEvent event);

    abstract int getDelta();

    abstract void setDelta(final int delta);

    public void translateBy(final int delta){
        translateTo(getDelta() + delta);
    }

    public void translateTo(final int distance) {
        final int cropped = ensureInsideBounds(distance);
        if (getDelta() == cropped) {
            return;
        }
        setDelta(cropped);
        updatePosition(cropped);
    }

    private void updatePosition(final int newPosition) {
        mPositionInfo.updatePosition(newPosition);
        notifyListener();
    }
    private void notifyListener() {
        if (mOnTranslateChangeListener != null) {
            mOnTranslateChangeListener.onTranslateChange(
                    mPositionInfo.mGlobal, mPositionInfo.mSection, mPositionInfo.mRelative
            );
        }
    }

    private int ensureInsideBounds(final int x) {
        return mPositionInfo.cropInLimits(x);
    }

    boolean fling() {
        final int start = getDelta();
        final int end = endPositionFrom(start);
        final boolean started = mHelperScroller.startScroll(start, end);
        ViewCompat.postOnAnimation(mView, this);
        return started;
    }

    boolean isFling() {
        return !mHelperScroller.isFinished();
    }

    @Override
    public void run() {
        if (mHelperScroller.computeScrollOffset()) {
            translateTo(mHelperScroller.getCurrX());
            ViewCompat.postOnAnimation(mView, this);
        }
    }

    private int endPositionFrom(final int currPosition) {
        return mPositionInfo.closeTo(currPosition);
    }

    void disallowParentInterceptTouchEvent(final boolean disallow) {
        final ViewParent parent = mView.getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
        }
    }

    public void startWith(final int position) {
        mPositionInfo.setCurrPos(position);
    }
}
