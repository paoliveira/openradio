package com.yuriy.openradio.business;

import com.yuriy.openradio.api.RadioStationVO;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/9/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public interface RadioStationDeserializer {

    RadioStationVO deserialize(final String value);
}
