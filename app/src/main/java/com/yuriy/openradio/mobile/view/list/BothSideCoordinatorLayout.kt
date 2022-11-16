package com.yuriy.openradio.mobile.view.list

import android.content.Context
import android.util.AttributeSet
import com.xenione.libs.swipemaker.AbsCoordinatorLayout
import com.xenione.libs.swipemaker.SwipeLayout
import com.yuriy.openradio.mobile.R
import com.yuriy.openradio.shared.utils.findView

/**
 * Created on 06/04/16.
 */
class BothSideCoordinatorLayout : AbsCoordinatorLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun doInitialViewsLocation() {
        val foregroundView = findViewById<SwipeLayout>(R.id.foreground_view)
        val settings = findView(R.id.settings_btn_view)
        val favorite = findView(R.id.favorite_view)
        foregroundView.anchor(-favorite.width, 0, settings.right)
    }

    override fun onTranslateChange(globalPercent: Float, index: Int, relativePercent: Float) {}
}
