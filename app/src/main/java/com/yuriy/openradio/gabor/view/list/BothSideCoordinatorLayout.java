package com.yuriy.openradio.gabor.view.list;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.xenione.libs.swipemaker.AbsCoordinatorLayout;
import com.xenione.libs.swipemaker.SwipeLayout;
import com.yuriy.openradio.gabor.R;

/**
 * Created on 06/04/16.
 */
public class BothSideCoordinatorLayout extends AbsCoordinatorLayout {

    public BothSideCoordinatorLayout(final Context context) {
        super(context);
    }

    public BothSideCoordinatorLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public BothSideCoordinatorLayout(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BothSideCoordinatorLayout(final Context context, final AttributeSet attrs,
                                     final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void doInitialViewsLocation() {
        final SwipeLayout foregroundView = findViewById(R.id.foreground_view);
        final View settings = findViewById(R.id.settings_btn_view);
        final View favorite = findViewById(R.id.favorite_view);
        foregroundView.anchor(-favorite.getWidth(), 0, settings.getRight());
    }

    @Override
    public void onTranslateChange(float global, int index, float relative) {
    }
}
