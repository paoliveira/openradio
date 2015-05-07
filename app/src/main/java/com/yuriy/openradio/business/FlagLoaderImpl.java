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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.yuriy.openradio.net.Downloader;
import com.yuriy.openradio.net.HTTPDownloaderImpl;
import com.yuriy.openradio.net.UrlBuilder;
import com.yuriy.openradio.utils.AppUtils;

import java.io.File;
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
    public void getFlag(final Context context, final String countryCode) {
        final File file = new File(getFlagUrl(context, countryCode));
        if (file.exists() && file.isFile()) {
            final Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            return;
        }
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
                                getFlagUrl(context, countryCode)
                        );
                    }
                }
        );
    }

    /**
     * Build url to the county's flag.
     *
     * @param context     Callee context.
     * @param countryCode Country code.
     * @return Url to the flag image of the country.
     */
    private String getFlagUrl(final Context context, final String countryCode) {
        return AppUtils.getExternalStorageDir(context) + FLAGS_DIR + "flag-" + countryCode + ".jpg";
    }
}
