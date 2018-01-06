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

import android.content.Context;
import android.net.Uri;

import com.yuriy.openradio.utils.ApiKeyLoader;

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
     * Id of the first page (refer to Dirble API for more info) of the Radio Stations List.
     */
    public static final int FIRST_PAGE_INDEX = 1;

    /**
     * Number of Radio Stations in each page (refer to Dirble API for more info).
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
     * IP-API url.
     */
    private static final String IP_API_URL = "http://ip-api.com/json";

    /**
     * URL of the Geo Names service to obtain country's flag.
     */
    private static final String GEO_NAMES_FLAGS = "http://www.geonames.org/flags/";

    /**
     * Base url for the icons used previously by Dirble.
     */
    static final String OLD_IMG_BASE_URL = "cdn.devality.com";

    /**
     * Base url for the icons using currently by Dirble.
     */
    static final String NEW_IMG_BASE_URL = "img.dirble.com";

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
     * Get Uri for the IP API Geo API which returns location.
     *
     * @return {@link Uri}.
     */
    public static Uri getIPAPIUrl() {
        return Uri.parse(IP_API_URL);
    }

    /**
     * Get Uri for the All Categories list.
     *
     * @param context Context of the application.
     * @return {@link Uri}
     */
    public static Uri getAllCategoriesUrl(final Context context) {
        return Uri.parse(BASE_URL + "categories?token=" + ApiKeyLoader.getApiKey(context));
    }

    /**
     * Get Uri for the All Countries list.
     *
     * @param context Context of the application.
     * @return {@link Uri}
     */
    public static Uri getAllCountriesUrl(final Context context) {
        return Uri.parse(BASE_URL + "countries?token=" + ApiKeyLoader.getApiKey(context));
    }

    /**
     * Get Uri for the Child Category's list (list of the categories in the main menu item).
     *
     * @param context   Context of the application.
     * @param primaryId Id of the primary Menu Item
     * @return {@link Uri}
     */
    public static Uri getChildCategoriesUrl(final Context context, final String primaryId) {
        return Uri.parse(
                BASE_URL + "category/" + primaryId + "/childs"
                        + "?token=" + ApiKeyLoader.getApiKey(context)
        );
    }

    /**
     * Get Uri for the list of the Radio Stations in concrete Category.
     *
     * @param context    Context of the application.
     * @param categoryId Id of the Category.
     * @return {@link Uri}
     */
    public static Uri getStationsInCategory(final Context context, final String categoryId,
                                            final int pageNumber, final int numberPerPage) {
        return Uri.parse(
                BASE_URL + "category/" + categoryId + "/stations"
                        + "?token=" + ApiKeyLoader.getApiKey(context)
                        + "&page=" + String.valueOf(pageNumber)
                        + "&per_page=" + String.valueOf(numberPerPage)
        );
    }

    /**
     * Get Uri for the list of the Radio Stations in concrete Category.
     *
     * @param context     Context of the application.
     * @param countryCode Country Code.
     * @return {@link Uri}
     */
    public static Uri getStationsInCountry(final Context context, final String countryCode,
                                           final int pageNumber, final int numberPerPage) {
        return Uri.parse(
                BASE_URL + "countries/" + countryCode + "/stations"
                        + "?token=" + ApiKeyLoader.getApiKey(context)
                        + "&page=" + String.valueOf(pageNumber)
                        + "&per_page=" + String.valueOf(numberPerPage)
        );
    }

    /**
     * Get Uri for the list of the popular Radio Stations.
     *
     * @param context       Context of the application.
     * @param pageNumber    Id of the current page being requested.
     * @param numberPerPage Number of items in one response.
     * @return {@link Uri}
     */
    public static Uri getPopularStations(final Context context, final int pageNumber, final int numberPerPage) {
        return Uri.parse(
                BASE_URL + "stations/popular"
                        + "?token=" + ApiKeyLoader.getApiKey(context)
                        + "&page=" + String.valueOf(pageNumber)
                        + "&per_page=" + String.valueOf(numberPerPage)
        );
    }

    /**
     * Get Uri for the list of the recently added Radio Stations.
     *
     * @param context       Context of the application.
     * @param numberPerPage Number of items in one response.
     * @return {@link Uri}
     */
    public static Uri getRecentlyAddedStations(final Context context, final int pageNumber, final int numberPerPage) {
        return Uri.parse(
                BASE_URL + "stations/recent"
                        + "?token=" + ApiKeyLoader.getApiKey(context)
                        + "&page=" + String.valueOf(pageNumber)
                        + "&per_page=" + String.valueOf(numberPerPage)
        );
    }

    /**
     * Get Uri for the concrete Radio Station details.
     *
     * @param context    Context of the application.
     * @param stationId  Id of the Radio Station.
     * @return {@link Uri}
     */
    public static Uri getStation(final Context context, final String stationId) {
        return Uri.parse(
                BASE_URL + "station/" + stationId
                        + "?token=" + ApiKeyLoader.getApiKey(context)
        );
    }

    /**
     * Get Uri for the search.
     *
     * @param context Context of the application.
     * @return {@link Uri}.
     */
    public static Uri getSearchUrl(final Context context) {
        return Uri.parse(
                BASE_URL + "search/?token=" + ApiKeyLoader.getApiKey(context)
        );
    }

    /**
     * Get Uri for the provided country flag of the small size for the usage in Geo Names service.
     *
     * @param countryCode Country code.
     * @return {@link Uri}
     */
    public static Uri getCountryFlagSmall(final String countryCode) {
        return getCountryFlag(countryCode.toLowerCase(), "l");
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
     * Get Uri for the provided country flag and flag size for the usage in Geo Names service.
     *
     * @param countryCode Country code.
     * @param size        Size of the flag's image. Could be "l" for the small size and
     *                    "x" for the big size.
     * @return {@link Uri}
     */
    private static Uri getCountryFlag(final String countryCode, final String size) {
        return Uri.parse(GEO_NAMES_FLAGS + size + "/" + countryCode.toLowerCase() + ".gif");
    }
}
