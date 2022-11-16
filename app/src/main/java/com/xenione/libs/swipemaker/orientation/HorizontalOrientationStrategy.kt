package com.xenione.libs.swipemaker.orientation

import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * Created by Eugeni on 28/09/2016.
 */
class HorizontalOrientationStrategy(private val mView: View) : OrientationStrategy(mView) {

    private var mLastTouchX = 0
    private var mIsDragging = false

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (isDragDisabled) {
            return false
        }
        val action = event.action
        val eventX = event.x.toInt()
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            fling()
            return false
        }
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mLastTouchX = eventX
                mHelperScroller.finish()
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = abs(eventX - mLastTouchX)
                mIsDragging = deltaX > mTouchSlop
                if (mIsDragging) {
                    disallowParentInterceptTouchEvent(true)
                    mLastTouchX = eventX
                }
            }
        }
        return mIsDragging
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        val eventX = event.x.toInt()
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            if (isDragDisabled) {
                return abs(mLastTouchX - eventX) > 50
            }
            if (mIsDragging) {
                disallowParentInterceptTouchEvent(false)
            }
            val isFling = fling()
            val handled = mIsDragging or isFling
            mIsDragging = false
            return handled
        }
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mLastTouchX = eventX
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isDragDisabled) {
                    val deltaX = eventX - mLastTouchX
                    if (mIsDragging) {
                        translateBy(deltaX)
                    } else if (abs(deltaX) > mTouchSlop) {
                        disallowParentInterceptTouchEvent(true)
                        mLastTouchX = eventX
                        mIsDragging = true
                    }
                }
            }
        }
        return mIsDragging
    }

    override fun getDelta(): Int {
        return mView.translationX.toInt()
    }

    override fun setDelta(value: Int) {
        mView.translationX = value.toFloat()
    }
}