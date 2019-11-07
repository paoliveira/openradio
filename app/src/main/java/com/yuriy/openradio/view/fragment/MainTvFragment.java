package com.yuriy.openradio.view.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.PageRow;

import com.yuriy.openradio.R;
import com.yuriy.openradio.view.activity.SearchTvActivity;

public final class MainTvFragment extends BrowseSupportFragment {

    private static final String CLASS_NAME = MainTvFragment.class.getSimpleName() + " ";

    public MainTvFragment() {
        super();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context context = getContext();

        setupUiElements(context);
        setupEventListeners(context);
        createRows(context);

        getMainFragmentRegistry().registerFragment(
                PageRow.class,
                new PageRowTvFragmentFactory(null)
        );

        prepareEntranceTransition();
        startEntranceTransition();
    }

    private void createRows(final Context context) {
        // This Adapter is used to render the Main TV Fragment sidebar labels.
        final ArrayObjectAdapter adapter = new ArrayObjectAdapter(new ListRowPresenter());
        adapter.add(PageRowTvFragmentFactory.createRadioStationsRageRow(context));
        adapter.add(PageRowTvFragmentFactory.createAddRadioStationRageRow(context));
        adapter.add(PageRowTvFragmentFactory.createSettingsRageRow(context));
        setAdapter(adapter);
    }

    private void setupUiElements(final Context context) {
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
        setBrandColor(getResources().getColor(R.color.primary_color));
        setSearchAffordanceColor(ContextCompat.getColor(context, R.color.blue_light_color));
        setTitle(getString(R.string.app_name));
//        setHeaderPresenterSelector(new PresenterSelector() {
//            @Override
//            public Presenter getPresenter(Object o) {
//                return new IconHeaderItemPresenter();
//            }
//        });
    }

    private void setupEventListeners(final Context context) {
        setOnSearchClickedListener(view -> {
            Intent intent = new Intent(context, SearchTvActivity.class);
            startActivity(intent);
        });
    }
}
