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

import android.content.Context;

import com.yuriy.openradio.net.Downloader;
import com.yuriy.openradio.net.HTTPDownloaderImpl;
import com.yuriy.openradio.net.UrlBuilder;
import com.yuriy.openradio.utils.AppUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/2/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public class FlagLoaderImpl implements FlagLoader {

    private static final String FLAGS_DIR = "/img/flags";

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    /**
     *
     */
    private FlagLoaderImpl() { }

    /**
     *
     * @return
     */
    public static FlagLoader getInstance() {
        return new FlagLoaderImpl();
    }

    @Override
    public void loadFlag(final Context context, final String countryCode) {
        EXECUTOR_SERVICE.submit(
                new Runnable() {

                    @Override
                    public void run() {
                        final Downloader downloader = new HTTPDownloaderImpl();
                        final byte[] data = downloader.downloadDataFromUri(
                                UrlBuilder.getCountryFlagSmall(countryCode)
                        );
                        AppUtils.saveDataToFile(
                                data,
                                AppUtils.getExternalStorageDir(context)
                                        + FLAGS_DIR, "flag-" + countryCode + ".jpg"
                        );
                    }
                }
        );
    }

    @Override
    public boolean isFlagLoaded(final Context context, final String countryCode) {
        return AppUtils.isFileExist(
                AppUtils.getExternalStorageDir(context) + FLAGS_DIR + "flag-" + countryCode + ".jpg"
        );
    }
}
