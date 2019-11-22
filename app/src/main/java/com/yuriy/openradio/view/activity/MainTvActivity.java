package com.yuriy.openradio.view.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.yuriy.openradio.R;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.view.fragment.MainTvFragment;

import java.util.List;

/*
 * Main TV Activity class that loads main TV fragment.
 */
public final class MainTvActivity extends FragmentActivity {

    private ImageView mBackBtn;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_tv);

        setUpBackBtn();
        setUpSettingsBtn();
    }

    public void onDataLoaded() {
        int numItemsInStack = 0;
        final FragmentManager manager = getSupportFragmentManager();
        final List<Fragment> list = manager.getFragments();
        for (final Fragment fragment : list) {
            if (!(fragment instanceof MainTvFragment)) {
                continue;
            }
            numItemsInStack = ((MainTvFragment)fragment).getNumItemsInStack();
            break;
        }
        if (numItemsInStack > 1) {
            showBackBtn();
        } else {
            hideBackBtn();
        }
    }

    private void showBackBtn() {
        if (mBackBtn == null) {
            return;
        }
        mBackBtn.setVisibility(View.VISIBLE);
    }

    private void hideBackBtn() {
        if (mBackBtn == null) {
            return;
        }
        mBackBtn.setVisibility(View.INVISIBLE);
    }

    private void setUpBackBtn() {
        mBackBtn = findViewById(R.id.tv_back_btn);
        if (mBackBtn == null) {
            return;
        }
        mBackBtn.setOnClickListener(
                v -> {
                    final FragmentManager manager = getSupportFragmentManager();
                    final List<Fragment> list = manager.getFragments();
                    for (final Fragment fragment : list) {
                        if (!(fragment instanceof MainTvFragment)) {
                            continue;
                        }
                        ((MainTvFragment)fragment).handleBackButton();
                    }
                }
        );
    }

    private void setUpSettingsBtn() {
        final ImageView button = findViewById(R.id.tv_settings_btn);
        if (button == null) {
            return;
        }
        button.setOnClickListener(
                v -> {
                    AppLogger.e("CLICKED");
                }
        );
    }
}
