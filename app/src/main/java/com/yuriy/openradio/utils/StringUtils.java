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

package com.yuriy.openradio.utils;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/20/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */

/**
 * <p>Operations on {@link java.lang.String} that are {@code null} safe.</p>
 */
public final class StringUtils {

    /**
      * <p>Checks if a CharSequence is empty ("") or null.</p>
      *
      * <pre>
      * StringUtils.isEmpty(null)      = true
      * StringUtils.isEmpty("")        = true
      * StringUtils.isEmpty(" ")       = false
      * StringUtils.isEmpty("bob")     = false
      * StringUtils.isEmpty("  bob  ") = false
      * </pre>
      *
      * <p>NOTE: This method changed in Lang version 2.0.
      * It no longer trims the CharSequence.
      * That functionality is available in isBlank().</p>
      *
      * @param cs  the CharSequence to check, may be null
      * @return {@code true} if the CharSequence is empty or null
      * @since 3.0 Changed signature from isEmpty(String) to isEmpty(CharSequence)
      */
    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }
}
