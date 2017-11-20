package com.yuriy.openradio.vo;

import android.support.annotation.NonNull;

import com.yuriy.openradio.service.LocationService;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 19/11/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */

public final class GoogleGeoLocation {

    private CountryVO mCountry;

    public GoogleGeoLocation() {
        super();
        mCountry = new CountryVO(
                LocationService.COUNTRY_NAME_DEFAULT,
                LocationService.COUNTRY_CODE_DEFAULT
        );
    }

    public void setCountry(@NonNull final CountryVO value) {
        mCountry = new CountryVO(value.getName(), value.getCode());
    }

    public CountryVO getCountry() {
        return mCountry;
    }

    @Override
    public String toString() {
        return "GoogleGeoLocation{" +
                "country=" + mCountry +
                "}";
    }
}
