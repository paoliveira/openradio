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

package com.yuriy.openradio.model.net;

import android.net.Uri;

import com.yuriy.openradio.utils.AppUtils;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 * <p>
 * {@link UrlBuilder} is a helper class which performs
 * build URL of the different API calls.
 */
public final class UrlBuilder {

    /**
     * Id of the first page of the Radio Stations List.
     */
    public static final int FIRST_PAGE_INDEX = 0;

    /**
     * Number of Radio Stations in each page.
     */
    public static final int ITEMS_PER_PAGE = 30;


    private static final int RECENT_POPULAR_PER_PAGE = 50;

    /**
     * Base URL for the API requests.
     */
    private static final String BASE_URL = "http://www.radio-browser.info/webservice/json/";

    /**
     * Google Geo API base url.
     */
    private static final String GOOGLE_GEO_URL = "https://maps.googleapis.com/maps/api/geocode";

    /**
     * Base url for the icons used previously.
     */
    private static final String OLD_IMG_BASE_URL = "cdn.devality.com";

    /**
     * Base url for the icons using currently.
     */
    private static final String NEW_IMG_BASE_URL = "img.dirble.com";

    /**
     * Private constructor.
     * Disallow instantiation of this helper class.
     */
    private UrlBuilder() {
        super();
    }

    /**
     * Get Uri for the Google Geo API which returns location.
     *
     * @param latitude  Latitude of the location.
     * @param longitude Longitude of the location.
     * @return {@link Uri}.
     */
    public static Uri getGoogleGeoAPIUrl(final double latitude, final double longitude) {
        return Uri.parse(GOOGLE_GEO_URL + "/json?latlng=" + latitude + "," + longitude);
    }

    /**
     * Get Uri for the All Categories list.
     *
     * @return {@link Uri}
     */
    public static Uri getAllCategoriesUrl() {
        return Uri.parse(BASE_URL + "tags?reverse=true&order=stationcount");
    }

    /**
     * Get Uri for the All Countries list.
     *
     * @return {@link Uri}
     */
    public static Uri getAllCountriesUrl() {
        return Uri.parse(BASE_URL + "countries");
    }

    /**
     * Get Uri for the list of the Radio Stations in concrete Category.
     *
     * @param categoryId Id of the Category.
     * @return {@link Uri}
     */
    public static Uri getStationsInCategory(final String categoryId,
                                            final int pageNumber,
                                            final int numberPerPage) {
        return Uri.parse(
                BASE_URL + "stations/bytag/" + encodeValue(categoryId) + "?reverse=true&order=stationcount"
                        + "&offset=" + pageNumber
                        + "&limit=" + numberPerPage
        );
    }

    /**
     * Get Uri for the list of the Radio Stations in country.
     *
     * @param countryCode Country Code.
     * @return {@link Uri}
     */
    public static Uri getStationsInCountry(final String countryCode,
                                           final int pageNumber,
                                           final int numberPerPage) {
        final String countryName = AppUtils.COUNTRY_CODE_TO_NAME.get(countryCode);
        return Uri.parse(
                BASE_URL + "stations/bycountry/" + encodeValue(countryName)
                        + "?offset=" + pageNumber
                        + "&limit=" + numberPerPage
        );
    }

    /**
     * Get Uri for the list of the popular Radio Stations.
     *
     * @return {@link Uri}
     */
    public static Uri getPopularStations() {
        return Uri.parse(BASE_URL + "stations/topclick/" + RECENT_POPULAR_PER_PAGE);
    }

    /**
     * Get Uri for the list of the recently added Radio Stations.
     *
     * @return {@link Uri}
     */
    public static Uri getRecentlyAddedStations() {
        return Uri.parse(BASE_URL + "stations/lastchange/" + RECENT_POPULAR_PER_PAGE);
    }

    /**
     * Get Uri for the concrete Radio Station details.
     *
     * @param stationId Id of the Radio Station.
     * @return {@link Uri}
     */
    public static Uri getStation(final String stationId) {
        return Uri.parse(BASE_URL + "station/" + encodeValue(stationId));
    }

    /**
     * Get Uri for the search.
     *
     * @return {@link Uri}.
     */
    public static Uri getSearchUrl(final String query) {
        return Uri.parse(
                BASE_URL + "stations/search?name=" + encodeValue(query)
                        + "&offset=" + 0
                        + "&limit=" + ITEMS_PER_PAGE
                        + "reverse=true&order=clickcount"
        );
    }

    /**
     * Pre-process URI of the Radio Station icon. It checks whether URI contains old base part
     * and replace it with new one.
     *
     * @param uri URI of the icon.
     * @return Modified URI.
     */
    public static Uri preProcessIconUri(final Uri uri) {
        if (uri == null) {
            return null;
        }
        return Uri.parse(preProcessIconUrl(uri.toString()));
    }

    /**
     * Pre-process URL of the Radio Station icon. It checks whether URL contains old base part
     * and replace it with new one.
     *
     * @param url URL of the icon.
     * @return Modified URL.
     */
    public static String preProcessIconUrl(final String url) {
        if (url == null || url.length() == 0) {
            return url;
        }
        if (url.contains(OLD_IMG_BASE_URL)) {
            return url.replace(OLD_IMG_BASE_URL, NEW_IMG_BASE_URL);
        }
        return url;
    }

    /**
     * Method to encode a string value using UTF-8 encoding scheme.
     *
     * @param value
     * @return
     */
    private static String encodeValue(final String value) {
        return value.replaceAll(" ", "%20");
    }
}
