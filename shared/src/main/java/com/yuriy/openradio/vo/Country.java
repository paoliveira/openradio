package com.yuriy.openradio.vo;

import androidx.annotation.NonNull;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 01/11/16
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class Country {

    /**
     * Default value of the Country Code.
     */
    public static final String COUNTRY_CODE_DEFAULT = "CA";

    public static final String COUNTRY_NAME_DEFAULT = "Canada";

    @NonNull
    private final String mName;

    @NonNull
    private final String mCode;

    private double mLatitude;

    private double mLongitude;

    public Country(@NonNull final String name, @NonNull final String code) {
        super();
        mName = name;
        mCode = code;
    }

    /**
     * Factory method to return default country.
     *
     * @return Default country.
     */
    public static Country getDefaultCountry() {
        return new Country(
                COUNTRY_NAME_DEFAULT,
                COUNTRY_CODE_DEFAULT
        );
    }

    public String getName() {
        return mName;
    }

    public String getCode() {
        return mCode;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(final double value) {
        mLatitude = value;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(final double value) {
        mLongitude = value;
    }

    @Override
    public String toString() {
        return "Country{" +
                "name='" + mName + '\'' +
                ", code='" + mCode + '\'' +
                ", lat='" + mLatitude + '\'' +
                ", lng='" + mLongitude + '\'' +
                "}";
    }
}
