package com.yuriy.openradio.business;

import com.yuriy.openradio.vo.GoogleGeoLocation;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 19/11/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public interface GoogleGeoDataParser {

    GoogleGeoLocation getLocation(final byte[] data);
}
