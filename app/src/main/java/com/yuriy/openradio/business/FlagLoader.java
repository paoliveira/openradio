/*
 * Copyright 2015 The "Open Radio" Project. Author: Chernyshov Yuriy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yuriy.openradio.business;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/2/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */

/**
 * {@link FlagLoader} is an interface that provides methods to load flag's images.
 */
public interface FlagLoader {

    /**
     * Load bitmap of the flag of the provided country.
     *
     * @param countryCode Country code.
     */
    void loadFlag(final String countryCode);

    /**
     * Check whether flag of the specified country has been loaded.
     *
     * @param countryCode Country code.
     * @return True in case of the flag has been loaded before, False - otherwise.
     */
    boolean isFlagLoaded(final String countryCode);
}
