package com.yuriy.openradio.api;

import android.support.annotation.NonNull;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 01/11/16
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public class CountryVO {

    @NonNull
    private final String mName;

    @NonNull
    private final String mCode;

    CountryVO(@NonNull final String name, @NonNull final String code) {
        super();
        mName = name;
        mCode = code;
    }

    public String getName() {
        return mName;
    }

    public String getCode() {
        return mCode;
    }

    @Override
    public String toString() {
        return "CountryVO{" +
                "mName='" + mName + '\'' +
                ", mCode='" + mCode + '\'' +
                '}';
    }
}
