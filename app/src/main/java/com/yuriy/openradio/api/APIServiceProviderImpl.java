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

package com.yuriy.openradio.api;

import android.net.Uri;
import android.util.Log;

import com.yuriy.openradio.business.DataParser;
import com.yuriy.openradio.business.JSONDataParserImpl;
import com.yuriy.openradio.net.Downloader;

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
    public List<CategoryVO> getAllCategories(final Downloader downloader, final Uri uri) {

        final List<CategoryVO> allCategories = new ArrayList<>();

        if (mDataParser == null) {
            Log.w(CLASS_NAME, "Can not parse data, parser is null");
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
                Log.e(CLASS_NAME, "Can not parse Radio Category:" + e.getMessage());
            }
        }

        return allCategories;
    }

    @Override
    public List<CategoryVO> getChildCategories(final Downloader downloader, final Uri uri) {

        final List<CategoryVO> childCategories = new ArrayList<>();

        if (mDataParser == null) {
            Log.w(CLASS_NAME, "Can not parse data, parser is null");
            return childCategories;
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
                if (object.has(JSONDataParserImpl.KEY_AMOUNT)) {
                    category.setAmount(object.getInt(JSONDataParserImpl.KEY_AMOUNT));
                }
                if (object.has(JSONDataParserImpl.KEY_TITLE)) {
                    category.setTitle(object.getString(JSONDataParserImpl.KEY_TITLE));
                }
                if (object.has(JSONDataParserImpl.KEY_DESCRIPTION)) {
                    category.setDescription(object.getString(JSONDataParserImpl.KEY_DESCRIPTION));
                }

                childCategories.add(category);

            } catch (JSONException e) {
                Log.e(CLASS_NAME, "Can not parse Radio Category:" + e.getMessage());
            }
        }

        return childCategories;
    }

    @Override
    public List<RadioStationVO> getStationsInCategory(final Downloader downloader, final Uri uri) {

        final List<RadioStationVO> radioStations = new ArrayList<>();

        if (mDataParser == null) {
            Log.w(CLASS_NAME, "Can not parse data, parser is null");
            return radioStations;
        }

        final JSONArray array = downloadJSONArray(downloader, uri);

        JSONObject object;
        RadioStationVO radioStation;
        for (int i = 0; i < array.length(); i++) {
            try {
                object = (JSONObject) array.get(i);

                radioStation = RadioStationVO.makeDefaultInstance();

                // TODO: Use data parser to parse JSON to value object

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
                    radioStation.setId(streamVO.getId());
                }

                if (radioStation.getStreamURL().isEmpty()) {
                    continue;
                }

                radioStations.add(radioStation);

            } catch (JSONException e) {
                Log.e(CLASS_NAME, "Can not parse Radio Station:" + e.getMessage());
            }
        }

        return radioStations;
    }

    @Override
    public RadioStationVO getStation(Downloader downloader, Uri uri) {
        final RadioStationVO radioStation = RadioStationVO.makeDefaultInstance();

        // Download response from the server
        final String response = downloader.downloadDataFromUri(uri);
        Log.i(CLASS_NAME, "Response:\n" + response);

        // Ignore empty response
        if (response.isEmpty()) {
            Log.w(CLASS_NAME, "Can not parse data, response is empty");
            return radioStation;
        }

        JSONObject object;

        try {
            object = new JSONObject(response);
        } catch (JSONException e) {
            Log.e(CLASS_NAME, "Can not convert response to JSON:" + e.getMessage());
            return radioStation;
        }

        // TODO: Use data parser to parse JSON to value object

        try {

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
                radioStation.setId(streamVO.getId());
            }
        } catch (JSONException e) {
            Log.e(CLASS_NAME, "Can not parse Radio Station:" + e.getMessage());
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
            Log.i(CLASS_NAME, "Get response from the cache");
            return RESPONSES_MAP.get(uri.toString());
        }

        // Download response from the server
        final String response = downloader.downloadDataFromUri(uri);
        //Log.i(CLASS_NAME, "URI:" + uri);
        //Log.i(CLASS_NAME, "Response:\n" + response);

        // Ignore empty response
        if (response.isEmpty()) {
            Log.w(CLASS_NAME, "Can not parse data, response is empty");
            return array;
        }

        try {
            array = new JSONArray(response);
        } catch (JSONException e) {
            Log.e(CLASS_NAME, "Can not get JSON array:" + e.getMessage());
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
                    bitrate = object.getInt(JSONDataParserImpl.KEY_BIT_RATE);
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

                streamVO.setBitrate(bitrate);
                streamVO.setUrl(stream);
                streamVO.setId(id);

                break;

            } catch (final JSONException e) {
                Log.e(CLASS_NAME, "Can not parse Stream:" + e.getMessage());
            }
        }

        if (streamVO.getUrl().isEmpty()) {
            Log.w(CLASS_NAME, "Stream has not been selected from:" + jsonArray);
        }

        return streamVO;
    }
}
