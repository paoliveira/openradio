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

package com.yuriy.openradio.net;

import android.net.Uri;
import android.text.TextUtils;

import com.yuriy.openradio.utils.DirbleApiKeyLoader;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link com.yuriy.openradio.net.UrlBuilder} is a helper class which performs
 * build URL of the different API calls.
 */
public final class UrlBuilder {

    /**
     * Id of the first page of the Radio Stations List.
     */
    public static final int FIRST_PAGE_INDEX = 1;

    /**
     * Number of Radio Stations in each page.
     */
    public static final int ITEMS_PER_PAGE = 30;

    /**
     * Base URL for the API requests.
     */
    private static final String BASE_URL = "http://api.dirble.com/v2/";

    /**
     * Google Geo API base url.
     */
    private static final String GOOGLE_GEO_URL = "https://maps.googleapis.com/maps/api/geocode";

    /**
     * Base url for the icons used previously.
     */
    static final String OLD_IMG_BASE_URL = "cdn.devality.com";

    /**
     * Base url for the icons using currently.
     */
    static final String NEW_IMG_BASE_URL = "img.dirble.com";

    /**
     *
     */
    static final String TOKEN_KEY = "token=";

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
        return Uri.parse(BASE_URL + "categories?" + TOKEN_KEY + DirbleApiKeyLoader.getApiKey());
    }

    /**
     * Get Uri for the All Countries list.
     *
     * @return {@link Uri}
     */
    public static Uri getAllCountriesUrl() {
        return Uri.parse(BASE_URL + "countries?" + TOKEN_KEY + DirbleApiKeyLoader.getApiKey());
    }

    /**
     * Get Uri for the Child Category's list (list of the categories in the main menu item).
     *
     * @param primaryId Id of the primary Menu Item
     * @return {@link Uri}
     */
    public static Uri getChildCategoriesUrl(final String primaryId) {
        return Uri.parse(
                BASE_URL + "category/" + primaryId + "/childs" + "?" + TOKEN_KEY + DirbleApiKeyLoader.getApiKey()
        );
    }

    /**
     * Get Uri for the list of the Radio Stations in concrete Category.
     *
     * @param categoryId Id of the Category.
     * @return {@link Uri}
     */
    public static Uri getStationsInCategory(final String categoryId, final int pageNumber, final int numberPerPage) {
        return Uri.parse(
                BASE_URL + "category/" + categoryId + "/stations"
                        + "?page=" + pageNumber
                        + "&per_page=" + numberPerPage
                        + "&" + TOKEN_KEY + DirbleApiKeyLoader.getApiKey()
        );
    }

    /**
     * Get Uri for the list of the Radio Stations in concrete Category.
     *
     * @param countryCode Country Code.
     * @return {@link Uri}
     */
    public static Uri getStationsInCountry(final String countryCode, final int pageNumber, final int numberPerPage) {
        return Uri.parse(
                BASE_URL + "countries/" + countryCode + "/stations"
                        + "?page=" + pageNumber
                        + "&per_page=" + numberPerPage
                        + "&" + TOKEN_KEY + DirbleApiKeyLoader.getApiKey()
        );
    }

    /**
     * Get Uri for the list of the popular Radio Stations.
     *
     * @param pageNumber    Id of the current page being requested.
     * @param numberPerPage Number of items in one response.
     * @return {@link Uri}
     */
    public static Uri getPopularStations(final int pageNumber, final int numberPerPage) {
        return Uri.parse(
                BASE_URL + "stations/popular"
                        + "?page=" + pageNumber
                        + "&per_page=" + numberPerPage
                        + "&" + TOKEN_KEY + DirbleApiKeyLoader.getApiKey()
        );
    }

    /**
     * Get Uri for the list of the recently added Radio Stations.
     *
     * @param numberPerPage Number of items in one response.
     * @return {@link Uri}
     */
    public static Uri getRecentlyAddedStations(final int pageNumber, final int numberPerPage) {
        return Uri.parse(
                BASE_URL + "stations/recent"
                        + "?page=" + pageNumber
                        + "&per_page=" + numberPerPage
                        + "&" + TOKEN_KEY + DirbleApiKeyLoader.getApiKey()
        );
    }

    /**
     * Get Uri for the concrete Radio Station details.
     *
     * @param stationId  Id of the Radio Station.
     * @return {@link Uri}
     */
    public static Uri getStation(final String stationId) {
        return Uri.parse(BASE_URL + "station/" + stationId + "?" + TOKEN_KEY + DirbleApiKeyLoader.getApiKey());
    }

    /**
     * Get Uri for the search.
     *
     * @return {@link Uri}.
     */
    public static Uri getSearchUrl() {
        return Uri.parse(BASE_URL + "search/?" + TOKEN_KEY + DirbleApiKeyLoader.getApiKey());
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
     * Excludes a value of API token from url.
     *
     * @param url Url of request.
     * @return Url with value of API token excluded.
     */
    public static String excludeApiToken(final String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        if (!url.contains(TOKEN_KEY)) {
            return url;
        }
        return url.substring(0, url.indexOf(TOKEN_KEY) + TOKEN_KEY.length());
    }
}
