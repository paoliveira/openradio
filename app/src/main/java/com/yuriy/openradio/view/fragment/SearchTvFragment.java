package com.yuriy.openradio.view.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.yuriy.openradio.R;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.view.SafeToast;

/*
 * This class demonstrates how to do in-app search
 */
public class SearchTvFragment extends SearchSupportFragment
        implements SearchSupportFragment.SearchResultProvider {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = SearchTvFragment.class.getSimpleName();

    /**
     * Tag string to use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

    private static final String TAG = SearchTvFragment.class.getSimpleName();
    private static final boolean FINISH_ON_RECOGNIZER_CANCELED = true;
    private static final int REQUEST_SPEECH = 0x00000010;

    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;
    private String mQuery;

    private boolean mResultsFound = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        setSearchResultProvider(this);
        setOnItemViewClickedListener(new ItemViewClickedListener());
        Log.d(TAG, "User is initiating a search. Do we have RECORD_AUDIO permission? " +
                hasPermission(Manifest.permission.RECORD_AUDIO));
        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            Log.d(TAG, "Does not have RECORD_AUDIO, using SpeechRecognitionCallback");
            // SpeechRecognitionCallback is not required and if not provided recognition will be
            // handled using internal speech recognizer, in which case you must have RECORD_AUDIO
            // permission
            setSpeechRecognitionCallback(() -> {
                try {
                    startActivityForResult(getRecognizerIntent(), REQUEST_SPEECH);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "Cannot find activity for speech recognizer", e);
                }
            });
        } else {
            Log.d(TAG, "We DO have RECORD_AUDIO");
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
                        if (FINISH_ON_RECOGNIZER_CANCELED) {
                            if (!hasResults()) {
                                Log.v(TAG, "Voice search canceled");
                                getView().findViewById(R.id.lb_search_bar_speech_orb).requestFocus();
                            }
                        }
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
        Log.i(TAG, String.format("Search text changed: %s", newQuery));
        loadQuery(newQuery);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.i(TAG, String.format("Search text submitted: %s", query));
        loadQuery(query);
        return true;
    }

    public boolean hasResults() {
        return mRowsAdapter.size() > 0 && mResultsFound;
    }

    private boolean hasPermission(final String permission) {
        final Context context = getActivity();
        return PackageManager.PERMISSION_GRANTED == context.getPackageManager().checkPermission(
                permission, context.getPackageName());
    }

    private void loadQuery(String query) {
        if (!TextUtils.isEmpty(query) && !query.equals("nil")) {
            mQuery = query;
            AppLogger.d("Search for:" + query);
        }
    }

    public void focusOnSearch() {
        getView().findViewById(R.id.lb_search_bar).requestFocus();
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {

        private ItemViewClickedListener() {
            super();
        }

        @Override
        public void onItemClicked(final Presenter.ViewHolder itemViewHolder,
                                  final Object item,
                                  final RowPresenter.ViewHolder rowViewHolder,
                                  final Row row) {
            SafeToast.showAnyThread(getContext().getApplicationContext(), ((String) item));
        }
    }

}
