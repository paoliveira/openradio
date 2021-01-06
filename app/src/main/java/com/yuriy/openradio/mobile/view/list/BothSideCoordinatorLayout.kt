package com.yuriy.openradio.mobile.view.list

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.xenione.libs.swipemaker.AbsCoordinatorLayout
import com.xenione.libs.swipemaker.SwipeLayout
import com.yuriy.openradio.R

/**
 * Created on 06/04/16.
 */
class BothSideCoordinatorLayout : AbsCoordinatorLayout {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun doInitialViewsLocation() {
        val foregroundView: SwipeLayout = findViewById(R.id.foreground_view)
        val settings = findViewById<View>(R.id.settings_btn_view)
        val favorite = findViewById<View>(R.id.favorite_view)
        foregroundView.anchor(-favorite.width, 0, settings.right)
    }

    override fun onTranslateChange(globalPercent: Float, index: Int, relativePercent: Float) {}
}