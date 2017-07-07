package com.yuriy.openradio.drive;

import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */

public final class GoogleDriveRequest {

    private static final String FOLDER_NAME = "OPEN_RADIO";
    private static final String FILE_NAME_FAVORITES = "RadioStationsFavorites.txt";
    private static final String FILE_NAME_LOCALS = "RadioStationsLocals.txt";

    private final GoogleDriveAPIType mApiType;

    private String mRadioStationsFavorites;
    private String mRadioStationsLocals;

    private final GoogleApiClient mGoogleApiClient;

    public GoogleDriveRequest(final GoogleApiClient googleApiClient, final GoogleDriveAPIType apiType) {
        super();
        mGoogleApiClient = googleApiClient;
        mApiType = apiType;
    }

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    public GoogleDriveAPIType getApiType() {
        return mApiType;
    }

    public String getFolderName() {
        return FOLDER_NAME;
    }

    public String getFileNameFavorites() {
        return FILE_NAME_FAVORITES;
    }

    public String getFileNameLocals() {
        return FILE_NAME_LOCALS;
    }

    public String getRadioStationsFavorites() {
        return mRadioStationsFavorites;
    }

    public void setRadioStationsFavorites(final String value) {
        mRadioStationsFavorites = value;
    }

    public String getRadioStationsLocals() {
        return mRadioStationsLocals;
    }

    public void setRadioStationsLocals(final String value) {
        mRadioStationsLocals = value;
    }
}
