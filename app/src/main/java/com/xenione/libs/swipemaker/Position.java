package com.xenione.libs.swipemaker;

/**
 * Created by Eugeni on 23/04/2016.
 */
public class Position {

    private Anchors mAnchors;
    private int mCurrX;
    public int mSection;
    public float mRelative;
    public float mGlobal;

    public void setAnchors(final Anchors anchors) {
        mAnchors = anchors;
        setCurrPos(0);
    }

    public void setCurrPos(int pos) {
        mCurrX = pos;
        mSection = mAnchors == null ? 0 : mAnchors.sectionFor(pos);
    }

    public void updatePosition(int newX) {
        if (mCurrX == newX) {
            return;
        }
        updateSection(newX);
        mCurrX = newX;
        mRelative = relative(newX);
        mGlobal = global(newX);
    }

    private void decSection() {
        if (mSection == 0) {
            return;
        }
        mSection--;
    }

    private void incSection() {
        if (mSection == mAnchors.size() - 1) {
            return;
        }
        mSection++;
    }

    private boolean moveToLeft(int newX) {
        return mCurrX > newX;
    }

    private boolean moveToRight(int newX) {
        return mCurrX < newX;
    }

    private void updateSection(int newX) {
        if (mAnchors == null) {
            return;
        }
        if (moveToLeft(newX) && (newX <= mAnchors.anchorFor(this.mSection))) {
            decSection();
        } else if (moveToRight(newX) && (newX > mAnchors.anchorFor(this.mSection + 1))) {
            incSection();
        }
    }

    public float global(int posX) {
        return mAnchors == null ? posX : mAnchors.distance(posX);
    }

    public float relative(int posX) {
        return mAnchors == null ? posX : mAnchors.distance(this.mSection, posX);
    }

    public int closeTo(int posX) {
        return mAnchors == null ? posX : mAnchors.closeTo(mSection, posX);
    }

    public int cropInLimits(int posX) {
        return mAnchors == null ? posX : mAnchors.cropInLimits(posX);
    }
}
