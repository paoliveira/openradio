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

package com.yuriy.openradio.api;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.yuriy.openradio.business.DataParser;
import com.yuriy.openradio.business.JSONDataParserImpl;
import com.yuriy.openradio.business.broadcast.ConnectivityReceiver;
import com.yuriy.openradio.cache.api.ApiCache;
import com.yuriy.openradio.cache.api.PersistentAPIDbHelper;
import com.yuriy.openradio.cache.api.PersistentApiCache;
import com.yuriy.openradio.net.Downloader;
import com.yuriy.openradio.net.HTTPDownloaderImpl;
import com.yuriy.openradio.net.UrlBuilder;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.FabricUtils;
import com.yuriy.openradio.utils.RadioStationChecker;
import com.yuriy.openradio.vo.Category;
import com.yuriy.openradio.vo.Country;
import com.yuriy.openradio.vo.MediaStream;
import com.yuriy.openradio.vo.RadioStation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    @SuppressWarnings("unused")
    private static final String CLASS_NAME = ApiServiceProviderImpl.class.getSimpleName() + " ";

    /**
     * Key for the search "key-value" pairs.
     */
    private static final String SEARCH_PARAMETER_KEY = "query";

    /**
     * Implementation of the {@link com.yuriy.openradio.business.DataParser} allows to
     * parse raw response of the data into different formats.
     */
    private DataParser mDataParser;

    /**
     *
     */
    @NonNull
    private final Context mContext;

    /**
     *
     */
    @NonNull
    private final ApiCache mApiCache;

    /**
     * Constructor.
     *
     * @param context    Context of a callee.
     * @param dataParser Implementation of the {@link com.yuriy.openradio.business.DataParser}
     */
    public ApiServiceProviderImpl(@NonNull final Context context, final DataParser dataParser) {
        super();
        mApiCache = new PersistentApiCache(context, PersistentAPIDbHelper.DATABASE_NAME);
        mContext = context;
        mDataParser = dataParser;
    }

    public void close() {
        if (mApiCache instanceof PersistentApiCache) {
            ((PersistentApiCache) mApiCache).close();
        }
    }

    @Override
    public List<Category> getCategories(final Downloader downloader, final Uri uri) {

        final List<Category> allCategories = new ArrayList<>();

        if (mDataParser == null) {
            AppLogger.w(CLASS_NAME + "Can not parse data, parser is null");
            return allCategories;
        }

        final JSONArray array = downloadJSONArray(downloader, uri);

        JSONObject object;
        Category category;
        for (int i = 0; i < array.length(); i++) {
            try {
                object = (JSONObject) array.get(i);

                category = Category.makeDefaultInstance();

                // TODO: Use data parser to parse JSON to value object

                if (object.has(JSONDataParserImpl.KEY_ID)) {
                    category.setId(object.getInt(JSONDataParserImpl.KEY_ID));
                }
                if (object.has(JSONDataParserImpl.KEY_TITLE)) {
                    category.setTitle(object.getString(JSONDataParserImpl.KEY_TITLE));
                }
                if (object.has(JSONDataParserImpl.KEY_DESCRIPTION)) {
                    category.setDescription(object.getString(JSONDataParserImpl.KEY_DESCRIPTION));
                }

                allCategories.add(category);

            } catch (JSONException e) {
                FabricUtils.logException(e);
            }
        }

        return allCategories;
    }

    @Override
    public List<Country> getCountries(final Downloader downloader, final Uri uri) {

        final List<Country> allCountries = new ArrayList<>();

        if (mDataParser == null) {
            AppLogger.w(CLASS_NAME + "Can not parse data, parser is null");
            return allCountries;
        }

        final JSONArray array = downloadJSONArray(downloader, uri);

        JSONObject object;
        String countryName;
        String countryCode;

        for (int i = 0; i < array.length(); i++) {
            try {
                object = (JSONObject) array.get(i);

                if (object.has(JSONDataParserImpl.KEY_COUNTRY_CODE)
                        && object.has(JSONDataParserImpl.KEY_NAME)) {
                    countryName = object.getString(JSONDataParserImpl.KEY_NAME);
                    countryCode = object.getString(JSONDataParserImpl.KEY_COUNTRY_CODE);

                    if (TextUtils.isEmpty(countryName) || TextUtils.isEmpty(countryCode)) {
                        AppLogger.w(
                                CLASS_NAME + "Can not parse Country name and or Code, " +
                                        "one or both values are not valid"
                        );
                        continue;
                    }

                    allCountries.add(new Country(countryName, countryCode));
                }
            } catch (final JSONException e) {
                FabricUtils.logException(e);
            }
        }

        return allCountries;
    }

    @Override
    public List<RadioStation> getStations(final Downloader downloader, final Uri uri) {
        return getStations(downloader, uri, new ArrayList<>());
    }

    @Override
    public List<RadioStation> getStations(final Downloader downloader,
                                          final Uri uri,
                                          final List<Pair<String, String>> parameters) {

        final List<RadioStation> radioStations = new ArrayList<>();

        if (mDataParser == null) {
            AppLogger.w(CLASS_NAME + "Can not parse data, parser is null");
            return radioStations;
        }

        final JSONArray array = downloadJSONArray(downloader, uri, parameters);

        JSONObject object;
        RadioStation radioStation;
        for (int i = 0; i < array.length(); i++) {
            try {
                object = (JSONObject) array.get(i);

                radioStation = RadioStation.makeDefaultInstance();

                updateRadioStation(radioStation, object);

                if (radioStation.isMediaStreamEmpty()) {
                    continue;
                }

                radioStations.add(radioStation);

            } catch (JSONException e) {
                FabricUtils.logException(e);
            }
        }

        //
        // Begin workaround section against dead Radio Stations
        //

        final CountDownLatch completeLatch = new CountDownLatch(radioStations.size());
        final Set<String> passedUrls = new TreeSet<>();
        final ExecutorService executor = Executors.newFixedThreadPool(5);
        for (final RadioStation radioStationVO : radioStations) {
            executor.submit(
                    new RadioStationChecker(
                            // TODO: Probably check all variants
                            radioStationVO.getMediaStream().getVariant(0).getUrl(), completeLatch, passedUrls
                    )
            );
        }
        try {
            completeLatch.await();
        } catch (final InterruptedException e) {
            /* Ignore */
        }

        // Clear "dead" Radio Stations
        for (int i = 0; i < radioStations.size(); i++) {
            radioStation = radioStations.get(i);
            // TODO: Probably check all variants
            if (!passedUrls.contains(radioStation.getMediaStream().getVariant(0).getUrl())) {
                radioStations.remove(radioStation);
                i--;
            }
        }
        passedUrls.clear();

        //
        // End workaround section against dead Radio Stations
        //

        return radioStations;
    }

    @Override
    public RadioStation getStation(Downloader downloader, Uri uri) {
        final RadioStation radioStation = RadioStation.makeDefaultInstance();

        // Download response from the server
        final String response = new String(downloader.downloadDataFromUri(uri));
        AppLogger.i(CLASS_NAME + "Response:\n" + response);

        // Ignore empty response
        if (response.isEmpty()) {
            AppLogger.w(CLASS_NAME + "Can not parse data, response is empty");
            return radioStation;
        }

        JSONObject object;

        try {
            object = new JSONObject(response);
        } catch (JSONException e) {
            FabricUtils.logException(e);
            return radioStation;
        }

        try {
            updateRadioStation(radioStation, object);
        } catch (JSONException e) {
            FabricUtils.logException(e);
        }

        return radioStation;
    }

    /**
     * Download data as {@link org.json.JSONArray}.
     *
     * @param downloader Implementation of the {@link Downloader}.
     * @param uri        Uri to download from.
     * @return {@link org.json.JSONArray}
     */
    private JSONArray downloadJSONArray(final Downloader downloader,
                                        final Uri uri) {
        return downloadJSONArray(downloader, uri, new ArrayList<>());
    }

    /**
     * Download data as {@link org.json.JSONArray}.
     *
     * @param downloader Implementation of the {@link Downloader}.
     * @param uri        Uri to download from.
     * @param parameters List of parameters to attach to connection.
     * @return {@link org.json.JSONArray}
     */
    private JSONArray downloadJSONArray(final Downloader downloader,
                                        final Uri uri,
                                        final List<Pair<String, String>> parameters) {
        JSONArray array = new JSONArray();

        if (!ConnectivityReceiver.checkConnectivityAndNotify(mContext)) {
            return array;
        }

        // Create key to associate response with.
        String responsesMapKey = UrlBuilder.excludeApiToken(uri.toString());
        try {
            responsesMapKey += HTTPDownloaderImpl.getPostParametersQuery(parameters);
        } catch (final UnsupportedEncodingException e) {
            FabricUtils.logException(e);
            responsesMapKey = null;
        }

        // Try to get cache first and return it in case of success.
        array = mApiCache.get(responsesMapKey);
        if (array != null) {
            return array;
        }

        // Declare and initialize variable for response.
        final String response = new String(downloader.downloadDataFromUri(uri, parameters));
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
            FabricUtils.logException(e);
        }

        if (isSuccess) {
            // Remove previous record.
            mApiCache.remove(responsesMapKey);
            // Finally, cache new response.
            mApiCache.put(responsesMapKey, array);
        }

        return array;
    }

    /**
     * Select stream item from the collection of the streams.
     *
     * @param jsonArray Collection of the streams.
     * @return Selected stream.
     */
    @NonNull
    private MediaStream selectStream(final JSONArray jsonArray) {
        final MediaStream mediaStream = MediaStream.makeDefaultInstance();

        if (jsonArray == null) {
            return mediaStream;
        }

        JSONObject object;
        int length = jsonArray.length();
        int bitrate = 0;
        String url = "";
        for (int i = 0; i < length; i++) {
            try {
                object = jsonArray.getJSONObject(i);

                if (object == null) {
                    continue;
                }

                if (object.has(JSONDataParserImpl.KEY_BIT_RATE)) {
                    final Object bitrateObj = object.get(JSONDataParserImpl.KEY_BIT_RATE);
                    if (bitrateObj instanceof Integer) {
                        bitrate = object.getInt(JSONDataParserImpl.KEY_BIT_RATE);
                    }
                    if (bitrateObj instanceof String) {
                        final String bitrateStr = String.valueOf(bitrateObj);
                        if (!TextUtils.isEmpty(bitrateStr) && TextUtils.isDigitsOnly(bitrateStr)) {
                            bitrate = Integer.valueOf(bitrateStr);
                        }
                    }
                }
                if (object.has(JSONDataParserImpl.KEY_STREAM)) {
                    url = object.getString(JSONDataParserImpl.KEY_STREAM);
                }

                if (url == null || url.isEmpty()) {
                    continue;
                }

                if (url.startsWith("htt://")) {
                    url = url.replace("htt://", "http://");
                }

                if (url.startsWith("htyp://")) {
                    url = url.replace("htyp://", "http://");
                }

                mediaStream.setVariant(bitrate, url);

            } catch (final Exception e) {
                FabricUtils.logException(e);
            }
        }

        return mediaStream;
    }

    /**
     * Updates {@link RadioStation} with the values extracted from the JSOn Object.
     *
     * @param radioStation Instance of the {@link RadioStation} to be updated.
     * @param object       JSON object that holds informational parameters.
     * @throws JSONException
     */
    private void updateRadioStation(final RadioStation radioStation, final JSONObject object)
            throws JSONException {

        if (object.has(JSONDataParserImpl.KEY_STATUS)) {
            radioStation.setStatus(object.getInt(JSONDataParserImpl.KEY_STATUS));
        }
        if (object.has(JSONDataParserImpl.KEY_NAME)) {
            radioStation.setName(object.getString(JSONDataParserImpl.KEY_NAME));
        }
        if (object.has(JSONDataParserImpl.KEY_WEBSITE)) {
            radioStation.setWebSite(object.getString(JSONDataParserImpl.KEY_WEBSITE));
        }
        if (object.has(JSONDataParserImpl.KEY_COUNTRY)) {
            radioStation.setCountry(object.getString(JSONDataParserImpl.KEY_COUNTRY));
        }

        if (object.has(JSONDataParserImpl.KEY_STREAMS)) {
            final MediaStream mediaStream
                    = selectStream(object.getJSONArray(JSONDataParserImpl.KEY_STREAMS));
            radioStation.setMediaStream(mediaStream);
        }

        if (object.has(JSONDataParserImpl.KEY_ID)) {
            radioStation.setId(object.getInt(JSONDataParserImpl.KEY_ID));
        }

        if (object.has(JSONDataParserImpl.KEY_IMAGE)) {
            // TODO : Encapsulate Image in the same way as Stream.
            final JSONObject imageObject = object.getJSONObject(JSONDataParserImpl.KEY_IMAGE);

            if (imageObject.has(JSONDataParserImpl.KEY_URL)) {
                radioStation.setImageUrl(imageObject.getString(JSONDataParserImpl.KEY_URL));
            }

            if (imageObject.has(JSONDataParserImpl.KEY_THUMB)) {
                final JSONObject imageThumbObject = imageObject.getJSONObject(
                        JSONDataParserImpl.KEY_THUMB
                );
                if (imageThumbObject.has(JSONDataParserImpl.KEY_URL)) {
                    radioStation.setThumbUrl(imageThumbObject.getString(JSONDataParserImpl.KEY_URL));
                }
            }
        }
    }

    /**
     * Creates and returns list of the quesry search parameters to attach to http connection.
     *
     * @param searchQuery String to use as query.
     * @return List of the query search parameters.
     */
    @NonNull
    public static List<Pair<String, String>> getSearchQueryParameters(final String searchQuery) {
        final List<Pair<String, String>> result = new ArrayList<>();
        result.add(new Pair<>(SEARCH_PARAMETER_KEY, searchQuery));
        return result;
    }
}
