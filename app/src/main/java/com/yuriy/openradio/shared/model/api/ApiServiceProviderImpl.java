/*
 * Copyright 2017 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.model.api;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.yuriy.openradio.shared.broadcast.ConnectivityReceiver;
import com.yuriy.openradio.shared.model.net.Downloader;
import com.yuriy.openradio.shared.model.parser.DataParser;
import com.yuriy.openradio.shared.model.parser.JsonDataParserImpl;
import com.yuriy.openradio.shared.model.storage.cache.CacheType;
import com.yuriy.openradio.shared.model.storage.cache.api.ApiCache;
import com.yuriy.openradio.shared.model.storage.cache.api.InMemoryApiCache;
import com.yuriy.openradio.shared.model.storage.cache.api.PersistentAPIDbHelper;
import com.yuriy.openradio.shared.model.storage.cache.api.PersistentApiCache;
import com.yuriy.openradio.shared.service.LocationService;
import com.yuriy.openradio.shared.utils.AnalyticsUtils;
import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.AppUtils;
import com.yuriy.openradio.shared.utils.NetUtils;
import com.yuriy.openradio.shared.vo.Category;
import com.yuriy.openradio.shared.vo.Country;
import com.yuriy.openradio.shared.vo.MediaStream;
import com.yuriy.openradio.shared.vo.RadioStation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 * <p>
 * {@link ApiServiceProviderImpl} is the implementation of the
 * {@link ApiServiceProvider} interface.
 */
public final class ApiServiceProviderImpl implements ApiServiceProvider {

    /**
     * Tag string to use in logging messages.
     */
    private static final String CLASS_NAME = ApiServiceProviderImpl.class.getSimpleName() + " ";

    /**
     * Implementation of the {@link DataParser} which allows to
     * parse raw response of the data into different formats.
     */
    private final DataParser mDataParser;

    /**
     *
     */
    @NonNull
    private final Context mContext;

    /**
     *
     */
    @NonNull
    private final ApiCache mApiCachePersistent;

    private final ApiCache mApiCacheInMemory;

    /**
     * Constructor.
     *
     * @param context    Context of a callee.
     * @param dataParser Implementation of the {@link DataParser}
     */
    public ApiServiceProviderImpl(@NonNull final Context context, final DataParser dataParser) {
        super();
        mApiCachePersistent = new PersistentApiCache(context, PersistentAPIDbHelper.DATABASE_NAME);
        mApiCacheInMemory = new InMemoryApiCache();
        mContext = context;
        mDataParser = dataParser;
    }

    @Override
    public void close() {
        if (mApiCachePersistent instanceof PersistentApiCache) {
            ((PersistentApiCache) mApiCachePersistent).close();
        }
        if (mApiCacheInMemory instanceof InMemoryApiCache) {
            (mApiCacheInMemory).clear();
        }
    }

    @Override
    public void clear() {
        mApiCachePersistent.clear();
        mApiCacheInMemory.clear();
    }

    @Override
    public List<Category> getCategories(final Downloader downloader, final Uri uri, final CacheType cacheType) {

        final List<Category> allCategories = new ArrayList<>();

        if (mDataParser == null) {
            AppLogger.w(CLASS_NAME + "Can not parse data, parser is null");
            return allCategories;
        }

        final JSONArray array = downloadJsonArray(downloader, uri, cacheType);

        JSONObject object;
        Category category;
        for (int i = 0; i < array.length(); i++) {
            try {
                object = (JSONObject) array.get(i);

                category = Category.makeDefaultInstance();

                // TODO: Use data parser to parse JSON to value object

                if (object.has(JsonDataParserImpl.KEY_NAME)) {
                    category.setId(object.getString(JsonDataParserImpl.KEY_NAME));
                    category.setTitle(
                            AppUtils.capitalize(object.getString(JsonDataParserImpl.KEY_NAME))
                    );
                    if (object.has(JsonDataParserImpl.KEY_STATIONS_COUNT)) {
                        category.setStationsCount(object.getInt(JsonDataParserImpl.KEY_STATIONS_COUNT));
                    }
                }

                allCategories.add(category);

            } catch (JSONException e) {
                AnalyticsUtils.logException(e);
            }
        }

        return allCategories;
    }

    @Override
    public List<Country> getCountries(final Downloader downloader, final Uri uri, final CacheType cacheType) {

        final List<Country> allCountries = new ArrayList<>();

        if (mDataParser == null) {
            AppLogger.w(CLASS_NAME + "Can not parse data, parser is null");
            return allCountries;
        }

        for (final String countryName : LocationService.COUNTRY_NAME_TO_CODE.keySet()) {
            allCountries.add(
                    new Country(
                            countryName,
                            Objects.requireNonNull(LocationService.COUNTRY_NAME_TO_CODE.get(countryName))
                    )
            );
        }

        return allCountries;
    }

    @Override
    public List<RadioStation> getStations(final Downloader downloader, final Uri uri, final CacheType cacheType) {
        return getStations(downloader, uri, new ArrayList<>(), cacheType);
    }

    @Override
    public List<RadioStation> getStations(final Downloader downloader,
                                          final Uri uri,
                                          final List<Pair<String, String>> parameters,
                                          final CacheType cacheType) {

        final List<RadioStation> radioStations = new ArrayList<>();

        if (mDataParser == null) {
            AppLogger.w(CLASS_NAME + "Can not parse data, parser is null");
            return radioStations;
        }

        final JSONArray array = downloadJsonArray(downloader, uri, parameters, cacheType);

        JSONObject object;
        for (int i = 0; i < array.length(); i++) {
            try {
                object = (JSONObject) array.get(i);

                final RadioStation radioStation = getRadioStation(mContext, object);
                if (radioStation == null) {
                    continue;
                }
                // TODO: Move this check point into Radio Station
                if (radioStation.isMediaStreamEmpty()) {
                    continue;
                }

                radioStations.add(radioStation);

            } catch (JSONException e) {
                AnalyticsUtils.logException(e);
            }
        }

        return radioStations;
    }

    @Override
    public boolean addStation(final Downloader downloader,
                              final Uri uri,
                              final List<Pair<String, String>> parameters,
                              final CacheType cacheType) {
        // Post data to the server.
        final String response = new String(downloader.downloadDataFromUri(mContext, uri, parameters));
        AppLogger.i("Add station response:" + response);
        if (TextUtils.isEmpty(response)) {
            return false;
        }
        boolean value = false;
        try {
            // {"ok":false,"message":"AddStationError 'url is empty'","uuid":""}
            // {"ok":true,"message":"added station successfully","uuid":"3516ff35-14b9-4845-8624-4e6b0a7a3ab9"}
            final JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.has("ok")) {
                final String str = jsonObject.getString("ok");
                if (!TextUtils.isEmpty(str)) {
                    value = str.equalsIgnoreCase("true");
                }
            }
        } catch (final JSONException e) {
            AnalyticsUtils.logException(e);
        }
        return value;
    }

    @Override
    @Nullable
    public RadioStation getStation(final Downloader downloader, final Uri uri, final CacheType cacheType) {
        // Download response from the server.
        final String response = new String(downloader.downloadDataFromUri(mContext, uri));
        AppLogger.i(CLASS_NAME + "Response:" + response);

        // Ignore empty response.
        if (response.isEmpty()) {
            AppLogger.e(CLASS_NAME + "Can not parse data, response is empty");
            return null;
        }

        JSONObject object;

        try {
            object = new JSONObject(response);
        } catch (JSONException e) {
            AnalyticsUtils.logException(e);
            return null;
        }

        try {
            return getRadioStation(mContext, object);
        } catch (JSONException e) {
            AnalyticsUtils.logException(e);
        }

        return null;
    }

    /**
     * Download data as {@link JSONArray}.
     *
     * @param downloader Implementation of the {@link Downloader}.
     * @param uri        Uri to download from.
     * @return {@link JSONArray}
     */
    private JSONArray downloadJsonArray(final Downloader downloader,
                                        final Uri uri,
                                        final CacheType cacheType) {
        return downloadJsonArray(downloader, uri, new ArrayList<>(), cacheType);
    }

    /**
     * Download data as {@link JSONArray}.
     *
     * @param downloader Implementation of the {@link Downloader}.
     * @param uri        Uri to download from.
     * @param parameters List of parameters to attach to connection.
     * @return {@link JSONArray}
     */
    // TODO: Refactor this method to download raw response. Then Use parser to get data.
    private JSONArray downloadJsonArray(final Downloader downloader,
                                        final Uri uri,
                                        final List<Pair<String, String>> parameters,
                                        final CacheType cacheType) {
        JSONArray array = new JSONArray();

        if (!ConnectivityReceiver.checkConnectivityAndNotify(mContext)) {
            return array;
        }

        // Create key to associate response with.
        String responsesMapKey = uri.toString();
        try {
            responsesMapKey += NetUtils.getPostParametersQuery(parameters);
        } catch (final UnsupportedEncodingException e) {
            AnalyticsUtils.logException(e);
            responsesMapKey = null;
        }

        // Fetch RAM memory first.
        array = mApiCacheInMemory.get(responsesMapKey);
        if (array != null) {
            return array;
        }

        // Then look up data in the DB.
        array = mApiCachePersistent.get(responsesMapKey);
        if (array != null) {
            mApiCacheInMemory.remove(responsesMapKey);
            mApiCacheInMemory.put(responsesMapKey, array);
            return array;
        }
        // Finally, go to internet.

        // Declare and initialize variable for response.
        final String response = new String(downloader.downloadDataFromUri(mContext, uri, parameters));
        // Ignore empty response finally.
        if (response.isEmpty()) {
            array = new JSONArray();
            AppLogger.w(CLASS_NAME + "Can not parse data, response is empty");
            return array;
        }

        boolean isSuccess = false;
        try {
            array = new JSONArray(response);
            isSuccess = true;
        } catch (final JSONException e) {
            AnalyticsUtils.logException(e);
        }

        if (isSuccess) {
            // Remove previous record.
            mApiCachePersistent.remove(responsesMapKey);
            mApiCacheInMemory.remove(responsesMapKey);
            // Finally, cache new response.
            mApiCachePersistent.put(responsesMapKey, array);
            mApiCacheInMemory.put(responsesMapKey, array);
        }

        return array;
    }

    /**
     * Updates {@link RadioStation} with the values extracted from the JSOn Object.
     *
     * @param object JSON object that holds informational parameters.
     * @return RadioStation or null.
     * @throws JSONException
     */
    private RadioStation getRadioStation(final Context context, final JSONObject object) throws JSONException {

        final RadioStation radioStation = RadioStation.makeDefaultInstance(
                context, object.getString(JsonDataParserImpl.KEY_STATION_UUID)
        );

        if (object.has(JsonDataParserImpl.KEY_STATUS)) {
            radioStation.setStatus(object.getInt(JsonDataParserImpl.KEY_STATUS));
        }
        if (object.has(JsonDataParserImpl.KEY_NAME)) {
            radioStation.setName(object.getString(JsonDataParserImpl.KEY_NAME));
        }
        if (object.has(JsonDataParserImpl.KEY_HOME_PAGE)) {
            radioStation.setHomePage(object.getString(JsonDataParserImpl.KEY_HOME_PAGE));
        }
        if (object.has(JsonDataParserImpl.KEY_COUNTRY)) {
            radioStation.setCountry(object.getString(JsonDataParserImpl.KEY_COUNTRY));
        }
        if (object.has(JsonDataParserImpl.KEY_COUNTRY_CODE)) {
            radioStation.setCountryCode(object.getString(JsonDataParserImpl.KEY_COUNTRY_CODE));
        }

        if (object.has(JsonDataParserImpl.KEY_URL)) {
            int bitrate = 0;
            if (object.has(JsonDataParserImpl.KEY_BIT_RATE)) {
                bitrate = object.getInt(JsonDataParserImpl.KEY_BIT_RATE);
            }
            final MediaStream mediaStream = MediaStream.makeDefaultInstance();
            mediaStream.setVariant(bitrate, object.getString(JsonDataParserImpl.KEY_URL));
            radioStation.setMediaStream(mediaStream);
        }

        if (object.has(JsonDataParserImpl.KEY_IMAGE)) {
            // TODO : Encapsulate Image in the same way as Stream.
            final JSONObject imageObject = object.getJSONObject(JsonDataParserImpl.KEY_IMAGE);

            if (imageObject.has(JsonDataParserImpl.KEY_URL)) {
                radioStation.setImageUrl(imageObject.getString(JsonDataParserImpl.KEY_URL));
            }

            if (imageObject.has(JsonDataParserImpl.KEY_THUMB)) {
                final JSONObject imageThumbObject = imageObject.getJSONObject(
                        JsonDataParserImpl.KEY_THUMB
                );
                if (imageThumbObject.has(JsonDataParserImpl.KEY_URL)) {
                    radioStation.setThumbUrl(imageThumbObject.getString(JsonDataParserImpl.KEY_URL));
                }
            }
        }

        if (object.has(JsonDataParserImpl.KEY_FAV_ICON)) {
            radioStation.setImageUrl(object.getString(JsonDataParserImpl.KEY_FAV_ICON));
            radioStation.setThumbUrl(object.getString(JsonDataParserImpl.KEY_FAV_ICON));
        }

        return radioStation;
    }
}
