package com.xenione.libs.swipemaker

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import com.xenione.libs.swipemaker.SwipeLayout.OnTranslateChangeListener
import com.yuriy.openradio.R

/**
 * Created on 06/04/16.
 */
abstract class AbsCoordinatorLayout : FrameLayout, OnTranslateChangeListener {

    private var mForegroundView: SwipeLayout? = null
    private var mStartPosition = 0

    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context!!, attrs, defStyleAttr)

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        doInitialViewsLocation()
    }

    abstract fun doInitialViewsLocation()

    override fun onFinishInflate() {
        super.onFinishInflate()
        mForegroundView = findViewById(R.id.foreground_view)
        mForegroundView?.setOnTranslateChangeListener(this)
    }

    fun sync() {
        if (!isInEditMode) {
            ViewCompat.postOnAnimation(this) { mForegroundView!!.translateTo(mStartPosition) }
        }
    }
}