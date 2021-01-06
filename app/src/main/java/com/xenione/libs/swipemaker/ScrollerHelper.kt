package com.xenione.libs.swipemaker

import android.widget.OverScroller

/**
 * Created by Eugeni on 10/04/2016.
 */
class ScrollerHelper(private val mScroller: OverScroller) {

    fun startScroll(start: Int, end: Int): Boolean {
        if (start == end) {
            return false
        }
        val delta = end - start
        mScroller.startScroll(start, 0, delta, 0)
        return true
    }

    fun finish() {
        if (!mScroller.isFinished) {
            mScroller.forceFinished(true)
        }
    }

    val isFinished: Boolean
        get() = mScroller.isFinished

    fun computeScrollOffset(): Boolean {
        return mScroller.computeScrollOffset()
    }

    val currX: Int
        get() = mScroller.currX
}