package com.yuriy.openradio.api;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/16/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */

/**
 * {@link com.yuriy.openradio.api.RadioStationVO} is a value object that holds information
 * about concrete Radio Station.
 */
public class RadioStationVO {

    private int mId;

    // TODO: Convert to enum
    private int mStatus;

    private String mName = "";

    private String mStreamURL = "";

    private String mWebSite = "";

    // TODO: Convert to enum
    private String mCountry = "";

    // TODO: Convert to enum
    private String mBitRate = "";

    /**
     * Private constructor.
     * Disallow instantiation of this helper class.
     */
    private RadioStationVO() { }

    public int getId() {
        return mId;
    }

    public void setId(final int value) {
        mId = value;
    }

    public int getStatus() {
        return mStatus;
    }

    public void setStatus(final int value) {
        mStatus = value;
    }

    public String getName() {
        return mName;
    }

    public void setName(final String value) {
        mName = value;
    }

    public String getStreamURL() {
        return mStreamURL;
    }

    public void setStreamURL(final String value) {
        mStreamURL = value;
    }

    public String getCountry() {
        return mCountry;
    }

    public void setCountry(final String value) {
        mCountry = value;
    }

    public String getBitRate() {
        return mBitRate;
    }

    public void setBitRate(final String value) {
        mBitRate = value;
    }

    public String getWebSite() {
        return mWebSite;
    }

    public void setWebSite(final String value) {
        mWebSite = value;
    }

    @Override
    public String toString() {
        return "RadioStation{" +
                "id=" + mId +
                ", status=" + mStatus +
                ", name='" + mName + '\'' +
                ", streamURL='" + mStreamURL + '\'' +
                ", webSite='" + mWebSite + '\'' +
                ", country='" + mCountry + '\'' +
                ", bitRate='" + mBitRate + '\'' +
                '}';
    }

    /**
     * Factory method to create instance of the {@link com.yuriy.openradio.api.RadioStationVO}.
     *
     * @return Instance of the {@link com.yuriy.openradio.api.RadioStationVO}.
     */
    public static RadioStationVO makeDefaultInstance() {
        return new RadioStationVO();
    }
}
