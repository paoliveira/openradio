package com.yuriy.openradio.view.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;

import com.yuriy.openradio.R;
import com.yuriy.openradio.utils.AppLogger;

/*
 * This class demonstrates how to do in-app search
 */
public class SearchTvFragment extends SearchSupportFragment
        implements SearchSupportFragment.SearchResultProvider {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = SearchTvFragment.class.getSimpleName();

    private static final int REQUEST_SPEECH = 0x00000010;

    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        setSearchResultProvider(this);
        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            AppLogger.d(CLASS_NAME + " Does not have RECORD_AUDIO, using SpeechRecognitionCallback");
            // SpeechRecognitionCallback is not required and if not provided recognition will be
            // handled using internal speech recognizer, in which case you must have RECORD_AUDIO
            // permission
            setSpeechRecognitionCallback(() -> {
                try {
                    startActivityForResult(getRecognizerIntent(), REQUEST_SPEECH);
                } catch (ActivityNotFoundException e) {
                    Log.e(CLASS_NAME, "Cannot find activity for speech recognizer", e);
                }
            });
        } else {
            Log.d(CLASS_NAME, "We DO have RECORD_AUDIO");
        }
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SPEECH:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        setSearchQuery(data, true);
                        break;
                    default:
                        // If recognizer is canceled or failed, keep focus on the search orb
//                        if (FINISH_ON_RECOGNIZER_CANCELED) {
//                            if (!hasResults()) {
//                                Log.v(CLASS_NAME, "Voice search canceled");
//                                getView().findViewById(R.id.lb_search_bar_speech_orb).requestFocus();
//                            }
//                        }
                        break;
                }
                break;
        }
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        AppLogger.i(CLASS_NAME + String.format(" Search text submitted: %s", query));
        return true;
    }

    private boolean hasPermission(final String permission) {
        final Context context = getActivity();
        return PackageManager.PERMISSION_GRANTED == context.getPackageManager().checkPermission(
                permission, context.getPackageName());
    }

    public void focusOnSearch() {
        getView().findViewById(R.id.lb_search_bar).requestFocus();
    }
}
