/*
 * Copyright 2016 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.net.Uri;
import android.text.TextUtils;

import com.yuriy.openradio.business.DataParser;
import com.yuriy.openradio.business.JSONDataParserImpl;
import com.yuriy.openradio.net.Downloader;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.CrashlyticsUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */

/**
 * {@link com.yuriy.openradio.api.APIServiceProviderImpl} is the implementation of the
 * {@link com.yuriy.openradio.api.APIServiceProvider} interface.
 */
public class APIServiceProviderImpl implements APIServiceProvider {

    /**
     * Tag string to use in logging messages.
     */
    @SuppressWarnings("unused")
    private static final String CLASS_NAME = APIServiceProviderImpl.class.getSimpleName();

    /**
     * Cache of the API responses. It used in order to avoid API call amount on the server.
     */
    private static final Map<String, JSONArray> RESPONSES_MAP = new Hashtable<>();

    /**
     * Implementation of the {@link com.yuriy.openradio.business.DataParser} allows to
     * parse raw response of the data into different formats.
     */
    private DataParser mDataParser;

    /**
     * Constructor.
     *
     * @param dataParser Implementation of the {@link com.yuriy.openradio.business.DataParser}
     */
    public APIServiceProviderImpl(final DataParser dataParser) {
        mDataParser = dataParser;
    }

    @Override
    public List<CategoryVO> getCategories(final Downloader downloader, final Uri uri) {

        final List<CategoryVO> allCategories = new ArrayList<>();

        if (mDataParser == null) {
            AppLogger.w(CLASS_NAME + " Can not parse data, parser is null");
            return allCategories;
        }

        final JSONArray array = downloadJSONArray(downloader, uri);

        JSONObject object;
        CategoryVO category;
        for (int i = 0; i < array.length(); i++) {
            try {
                object = (JSONObject) array.get(i);

                category = CategoryVO.makeDefaultInstance();

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
                AppLogger.e(CLASS_NAME + " Can not parse Radio Category:" + e.getMessage());
                CrashlyticsUtils.logException(e);
            }
        }

        return allCategories;
    }

    @Override
    public List<CountryVO> getCounties(final Downloader downloader, final Uri uri) {

        final List<CountryVO> allCountries = new ArrayList<>();

        if (mDataParser == null) {
            AppLogger.w(CLASS_NAME + " Can not parse data, parser is null");
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
                                CLASS_NAME + " Can not parse Country name and or Code, " +
                                        "one or both values are not valid"
                        );
                        continue;
                    }

                    allCountries.add(new CountryVO(countryName, countryCode));
                }
            } catch (final JSONException e) {
                AppLogger.e(CLASS_NAME + " Can not parse Country name and or Code:" + e.getMessage());
                CrashlyticsUtils.logException(e);
            }
        }

        return allCountries;
    }

    @Override
    public List<RadioStationVO> getStations(final Downloader downloader, final Uri uri) {

        final List<RadioStationVO> radioStations = new ArrayList<>();

        if (mDataParser == null) {
            AppLogger.w(CLASS_NAME + " Can not parse data, parser is null");
            return radioStations;
        }

        final JSONArray array = downloadJSONArray(downloader, uri);

        JSONObject object;
        RadioStationVO radioStation;
        for (int i = 0; i < array.length(); i++) {
            try {
                object = (JSONObject) array.get(i);

                radioStation = RadioStationVO.makeDefaultInstance();

                updateRadioStation(radioStation, object);

                if (radioStation.getStreamURL().isEmpty()) {
                    continue;
                }

                radioStations.add(radioStation);

            } catch (JSONException e) {
                AppLogger.e(CLASS_NAME + " Can not parse Radio Station:" + e.getMessage());
                CrashlyticsUtils.logException(e);
            }
        }

        /**
         * Begin workaround section against dead Radio Stations
         */

        /*Thread checker;
        final CountDownLatch completeLatch = new CountDownLatch(radioStations.size());
        final CountDownLatch initLatch = new CountDownLatch(1);
        final Set<String> passedUrls = new TreeSet<>();
        final Thread[] checkers = new Thread[radioStations.size()];
        int counter = 0;
        for (final RadioStationVO radioStationVO : radioStations) {
            checker = new RadioStationChecker(
                    radioStationVO.getStreamURL(), initLatch, completeLatch, passedUrls
            );
            checkers[counter++] = checker;
            checker.start();
        }
        initLatch.countDown();
        try {
            completeLatch.await();
        } catch (final InterruptedException e) {
            *//* Ignore *//*
        }

        // Clear "dead" Radio Stations
        for (int i = 0; i < radioStations.size(); i++) {
            radioStation = radioStations.get(i);
            if (!passedUrls.contains(radioStation.getStreamURL())) {
                radioStations.remove(radioStation);
                i--;
            }
        }
        passedUrls.clear();

        // Interrupt checker threads
        for (final Thread checkerToInterrupt : checkers) {
            if (!checkerToInterrupt.isInterrupted()) {
                checkerToInterrupt.interrupt();
            }
        }
        // Clear references
        for (int i = 0; i < checkers.length; i++) {
            checkers[i] = null;
        }*/

        /**
         * End workaround section against dead Radio Stations
         */

        return radioStations;
    }

    @Override
    public RadioStationVO getStation(Downloader downloader, Uri uri) {
        final RadioStationVO radioStation = RadioStationVO.makeDefaultInstance();

        // Download response from the server
        final String response = new String(downloader.downloadDataFromUri(uri));
        AppLogger.i(CLASS_NAME + " Response:\n" + response);

        // Ignore empty response
        if (response.isEmpty()) {
            AppLogger.w(CLASS_NAME + " Can not parse data, response is empty");
            return radioStation;
        }

        JSONObject object;

        try {
            object = new JSONObject(response);
        } catch (JSONException e) {
            AppLogger.e(CLASS_NAME + " Can not convert response to JSON:" + e.getMessage());
            CrashlyticsUtils.logException(e);
            return radioStation;
        }

        try {
            updateRadioStation(radioStation, object);
        } catch (JSONException e) {
            AppLogger.e(CLASS_NAME + " Can not parse Radio Station:" + e.getMessage());
            CrashlyticsUtils.logException(e);
        }

        return radioStation;
    }

    /**
     * Clear responses cache
     */
    public static void clearCache() {
        RESPONSES_MAP.clear();
    }

    /**
     * Download data as {@link org.json.JSONArray}.
     *
     * @param downloader Implementation of the {@link com.yuriy.openradio.net.Downloader}.
     * @param uri        Uri to download from.
     * @return {@link org.json.JSONArray}
     */
    private JSONArray downloadJSONArray(final Downloader downloader, final Uri uri) {
        JSONArray array = new JSONArray();

        // Check cache to avoid unnecessary API call
        if (RESPONSES_MAP.containsKey(uri.toString())) {
            // Return cached value
            AppLogger.i(CLASS_NAME + " Get response from the cache");
            return RESPONSES_MAP.get(uri.toString());
        }

        // Download response from the server
        final String response = new String(downloader.downloadDataFromUri(uri));
        //AppLogger.i(CLASS_NAME + " URI:" + uri);
        //AppLogger.i(CLASS_NAME + " Response:\n" + response);

        // Ignore empty response
        if (response.isEmpty()) {
            AppLogger.w(CLASS_NAME + " Can not parse data, response is empty");
            return array;
        }

        try {
            array = new JSONArray(response);
        } catch (JSONException e) {
            AppLogger.e(CLASS_NAME + " Can not get JSON array:" + e.getMessage());
            CrashlyticsUtils.logException(e);
        }

        // Cache result
        RESPONSES_MAP.put(uri.toString(), array);

        return array;
    }

    /**
     * Select stream item from the collection of the streams.
     *
     * @param jsonArray Collection of the streams.
     * @return Selected stream.
     */
    private StreamVO selectStream(final JSONArray jsonArray) {
        final StreamVO streamVO = StreamVO.makeDefaultInstance();

        if (jsonArray == null) {
            return streamVO;
        }

        JSONObject object;
        int length = jsonArray.length();
        int bitrate = 0;
        int id = 0;
        String stream = "";
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
                    stream = object.getString(JSONDataParserImpl.KEY_STREAM);
                }
                if (object.has(JSONDataParserImpl.KEY_STATION_ID)) {
                    id = object.getInt(JSONDataParserImpl.KEY_STATION_ID);
                }

                if (stream == null || stream.isEmpty()) {
                    continue;
                }

                if (stream.startsWith("htt://")) {
                    stream = stream.replace("htt://", "http://");
                }

                streamVO.setBitrate(bitrate);
                streamVO.setUrl(stream);
                streamVO.setId(id);

                break;

            } catch (final Exception e) {
                AppLogger.e(CLASS_NAME + " Can not parse Stream:" + e.getMessage());
                CrashlyticsUtils.logException(e);
            }
        }

        if (streamVO.getUrl().isEmpty()) {
            AppLogger.w(CLASS_NAME + " Stream has not been selected from:" + jsonArray);
        }

        return streamVO;
    }

    /**
     * Updates {@link RadioStationVO} with the values extraced from the JSOn Object.
     *
     * @param radioStation Instance of the {@link RadioStationVO} to be updated.
     * @param object       JSON object that holds informational parameters.
     * @throws JSONException
     */
    private void updateRadioStation(final RadioStationVO radioStation, final JSONObject object)
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
            final StreamVO streamVO
                    = selectStream(object.getJSONArray(JSONDataParserImpl.KEY_STREAMS));
            radioStation.setStreamURL(streamVO.getUrl());
            radioStation.setBitRate(String.valueOf(streamVO.getBitrate()));
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
}
