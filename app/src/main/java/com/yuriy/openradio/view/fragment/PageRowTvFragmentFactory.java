package com.yuriy.openradio.view.fragment;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.PageRow;
import androidx.leanback.widget.Row;

import com.yuriy.openradio.R;

public final class PageRowTvFragmentFactory extends BrowseSupportFragment.FragmentFactory {

    private static final byte RADIO_STATIONS_HEADER_ID = 1;
    private static final byte SETTINGS_HEADER_ID = 2;
    private static final byte ADD_RADIO_STATION_HEADER_ID = 3;
    private static final byte MUSIC_PLAYER_HEADER_ID = 4;

    private final BackgroundManager mBackgroundManager;

    PageRowTvFragmentFactory(final BackgroundManager backgroundManager) {
        super();
        mBackgroundManager = backgroundManager;
    }

    @Override
    public final Fragment createFragment(final Object rowObj) {
        final Row row = (Row)rowObj;
        if (mBackgroundManager != null) {
            mBackgroundManager.setDrawable(null);
        }
        switch ((byte) row.getHeaderItem().getId()) {
            case RADIO_STATIONS_HEADER_ID:
                return new RadioStationsTvFragment();
            case SETTINGS_HEADER_ID:
                return new SettingsTvFragment();
            case ADD_RADIO_STATION_HEADER_ID:
                return new AddRadioStationTvFragment();
            case MUSIC_PLAYER_HEADER_ID:
                return new MusicPlayerTvFragment();
        }

        throw new IllegalArgumentException(String.format("Invalid row %s", rowObj));
    }

    static PageRow createRadioStationsRageRow(final Context context) {
        return GridTvFragment.createPageRow(
                RADIO_STATIONS_HEADER_ID,
                context.getString(R.string.tv_header_menu_radio_stations)
        );
    }

    static PageRow createSettingsRageRow(final Context context) {
        return GridTvFragment.createPageRow(
                SETTINGS_HEADER_ID,
                context.getString(R.string.app_settings_title)
        );
    }

    static PageRow createAddRadioStationRageRow(final Context context) {
        return GridTvFragment.createPageRow(
                ADD_RADIO_STATION_HEADER_ID,
                context.getString(R.string.add_station_dialog_title)
        );
    }

    static PageRow createMusicPlayerRageRow(final Context context) {
        return MusicPlayerTvFragment.createPageRow(
                MUSIC_PLAYER_HEADER_ID,
                context.getString(R.string.tv_header_menu_music_player)
        );
    }
}
