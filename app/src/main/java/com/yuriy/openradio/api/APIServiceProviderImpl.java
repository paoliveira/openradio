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
import java.util.List;

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
                if (object.has(JSONDataParserImpl.KEY_NAME)) {
                    category.setName(object.getString(JSONDataParserImpl.KEY_NAME));
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
                if (object.has(JSONDataParserImpl.KEY_NAME)) {
                    category.setName(object.getString(JSONDataParserImpl.KEY_NAME));
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

                if (object.has(JSONDataParserImpl.KEY_ID)) {
                    radioStation.setId(object.getInt(JSONDataParserImpl.KEY_ID));
                }
                if (object.has(JSONDataParserImpl.KEY_STATUS)) {
                    radioStation.setStatus(object.getInt(JSONDataParserImpl.KEY_STATUS));
                }
                if (object.has(JSONDataParserImpl.KEY_STREAM_URL)) {
                    radioStation.setStreamURL(object.getString(JSONDataParserImpl.KEY_STREAM_URL));
                }
                if (object.has(JSONDataParserImpl.KEY_NAME)) {
                    radioStation.setName(object.getString(JSONDataParserImpl.KEY_NAME));
                }
                if (object.has(JSONDataParserImpl.KEY_COUNTRY)) {
                    radioStation.setCountry(object.getString(JSONDataParserImpl.KEY_COUNTRY));
                }
                if (object.has(JSONDataParserImpl.KEY_BIT_RATE)) {
                    radioStation.setBitRate(object.getString(JSONDataParserImpl.KEY_BIT_RATE));
                }

                radioStations.add(radioStation);

            } catch (JSONException e) {
                Log.e(CLASS_NAME, "Can not parse Radio Station:" + e.getMessage());
            }
        }

        return radioStations;
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

        // Download response from the server
        final String response = downloader.downloadDataFromUri(uri);
        Log.i(CLASS_NAME, "Response:\n" + response);

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
        return array;
    }
}
