package com.yuriy.openradio.api;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */

/**
 * {@link com.yuriy.openradio.api.CategoryVO} is a value object that holds Radio Category data.
 */
public class CategoryVO {

    private int mId;

    private int mAmount;

    private String mName = "";

    private String mDescription = "";

    /**
     * Private constructor.
     * Disallow instantiation of this helper class.
     */
    private CategoryVO() { }

    public int getId() {
        return mId;
    }

    public void setId(final int value) {
        mId = value;
    }

    public String getName() {
        return mName;
    }

    public void setName(final String value) {
        mName = value;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(final String value) {
        mDescription = value;
    }

    public int getAmount() {
        return mAmount;
    }

    public void setAmount(final int value) {
        mAmount = value;
    }

    /**
     * Factory method to create instance of the {@link com.yuriy.openradio.api.CategoryVO}.
     *
     * @return Instance of the {@link com.yuriy.openradio.api.CategoryVO}.
     */
    public static CategoryVO makeDefaultInstance() {
        return new CategoryVO();
    }
}
