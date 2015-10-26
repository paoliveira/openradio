package com.yuriy.openradio.business;

import com.yuriy.openradio.api.RadioStationVO;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/9/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class RadioStationJSONDeserializer implements RadioStationDeserializer {

    @Override
    public final RadioStationVO deserialize(final String value) {
        final RadioStationVO radioStation = RadioStationVO.makeDefaultInstance();
        if (value == null || value.isEmpty()) {
            return radioStation;
        }
        try {
            final JSONObject jsonObject = new JSONObject(value);
            radioStation.setId(getIntValue(jsonObject, RadioStationJSONHelper.KEY_ID));
            radioStation.setName(getStringValue(jsonObject, RadioStationJSONHelper.KEY_NAME));
            radioStation.setBitRate(getStringValue(jsonObject, RadioStationJSONHelper.KEY_BITRATE));
            radioStation.setCountry(getStringValue(jsonObject, RadioStationJSONHelper.KEY_COUNTRY));
            radioStation.setGenre(getStringValue(jsonObject, RadioStationJSONHelper.KEY_GENRE));
            radioStation.setImageUrl(getStringValue(jsonObject, RadioStationJSONHelper.KEY_IMG_URL));
            radioStation.setStreamURL(getStringValue(jsonObject, RadioStationJSONHelper.KEY_STREAM_URL));
            radioStation.setStatus(getIntValue(jsonObject, RadioStationJSONHelper.KEY_STATUS));
            radioStation.setThumbUrl(getStringValue(jsonObject, RadioStationJSONHelper.KEY_THUMB_URL));
            radioStation.setWebSite(getStringValue(jsonObject, RadioStationJSONHelper.KEY_WEB_SITE));
            radioStation.setIsLocal(getBooleanValue(jsonObject, RadioStationJSONHelper.KEY_IS_LOCAL));
        } catch (final JSONException e) {
            /* Ignore this exception */
        }
        return radioStation;
    }

    private String getStringValue(final JSONObject jsonObject, final String key)
            throws JSONException {
        if (jsonObject == null) {
            return "";
        }
        if (jsonObject.has(key)) {
            return jsonObject.getString(key);
        }
        return "";
    }

    private int getIntValue(final JSONObject jsonObject, final String key) throws JSONException {
        if (jsonObject == null) {
            return 0;
        }
        if (jsonObject.has(key)) {
            return jsonObject.getInt(key);
        }
        return 0;
    }

    private boolean getBooleanValue(final JSONObject jsonObject, final String key) throws JSONException {
        if (jsonObject == null) {
            return false;
        }
        if (jsonObject.has(key)) {
            return jsonObject.getBoolean(key);
        }
        return false;
    }
}
