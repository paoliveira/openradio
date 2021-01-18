package com.xenione.libs.swipemaker

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import com.xenione.libs.swipemaker.orientation.HorizontalOrientationStrategy
import com.xenione.libs.swipemaker.orientation.OrientationStrategy
import com.xenione.libs.swipemaker.orientation.OrientationStrategyFactory

/**
 * Created by Eugeni on 10/04/2016.
 */
class SwipeLayout @JvmOverloads constructor(context: Context?,
                                            attrs: AttributeSet? = null,
                                            defStyleAttr: Int = 0) : RelativeLayout(context, attrs, defStyleAttr) {

    enum class Orientation {
        HORIZONTAL;

        fun get(): OrientationStrategyFactory {
            return HorizontalOrientationStrategyFactory()
        }
    }

    interface OnTranslateChangeListener {
        fun onTranslateChange(globalPercent: Float, index: Int, relativePercent: Float)
    }

    private val mOrientationStrategy: OrientationStrategy?

    fun anchor(vararg points: Int?) {
        mOrientationStrategy!!.setAnchor(*points)
    }

    fun setOnTranslateChangeListener(listener: OnTranslateChangeListener?) {
        mOrientationStrategy!!.setOnTranslateChangeListener(listener)
    }

    fun isDragDisabled(value: Boolean) {
        mOrientationStrategy!!.isDragDisabled(value)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return mOrientationStrategy!!.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!mOrientationStrategy!!.onTouchEvent(event)) {
            super.onTouchEvent(event)
        }
        return true
    }

    fun translateTo(position: Int) {
        mOrientationStrategy!!.translateTo(position)
    }

    private class HorizontalOrientationStrategyFactory : OrientationStrategyFactory {
        override fun make(view: View?): OrientationStrategy {
            return HorizontalOrientationStrategy(view!!)
        }
    }

    init {
        mOrientationStrategy = Orientation.HORIZONTAL.get().make(this)
    }
}