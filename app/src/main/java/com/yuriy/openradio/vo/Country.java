package com.yuriy.openradio.vo;

import android.support.annotation.NonNull;

import com.yuriy.openradio.service.LocationService;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 01/11/16
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class Country {

    @NonNull
    private final String mName;

    @NonNull
    private final String mCode;

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
                LocationService.COUNTRY_NAME_DEFAULT,
                LocationService.COUNTRY_CODE_DEFAULT
        );
    }

    public String getName() {
        return mName;
    }

    public String getCode() {
        return mCode;
    }

    @Override
    public String toString() {
        return "Country{" +
                "name='" + mName + '\'' +
                ", code='" + mCode + '\'' +
                "}";
    }
}
