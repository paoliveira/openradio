package com.xenione.libs.swipemaker

/**
 * Created by Eugeni on 23/04/2016.
 */
class Position {

    private var mAnchors: Anchors? = null
    private var mCurrX = 0
    @JvmField
    var mSection = 0
    @JvmField
    var mRelative = 0f
    @JvmField
    var mGlobal = 0f

    fun setAnchors(anchors: Anchors) {
        mAnchors = anchors
        setCurrPos(0)
    }

    fun setCurrPos(pos: Int) {
        mCurrX = pos
        mSection = if (mAnchors == null) 0 else mAnchors!!.sectionFor(pos)
    }

    fun updatePosition(newX: Int) {
        if (mCurrX == newX) {
            return
        }
        updateSection(newX)
        mCurrX = newX
        mRelative = relative(newX)
        mGlobal = global(newX)
    }

    private fun decSection() {
        if (mSection == 0) {
            return
        }
        mSection--
    }

    private fun incSection() {
        if (mSection == mAnchors!!.size() - 1) {
            return
        }
        mSection++
    }

    private fun moveToLeft(newX: Int): Boolean {
        return mCurrX > newX
    }

    private fun moveToRight(newX: Int): Boolean {
        return mCurrX < newX
    }

    private fun updateSection(newX: Int) {
        if (mAnchors == null) {
            return
        }
        if (moveToLeft(newX) && newX <= mAnchors!!.anchorFor(mSection)) {
            decSection()
        } else if (moveToRight(newX) && newX > mAnchors!!.anchorFor(mSection + 1)) {
            incSection()
        }
    }

    private fun global(posX: Int): Float {
        return if (mAnchors == null) posX.toFloat() else mAnchors!!.distance(posX)
    }

    private fun relative(posX: Int): Float {
        return if (mAnchors == null) posX.toFloat() else mAnchors!!.distance(mSection, posX)
    }

    fun closeTo(posX: Int): Int {
        return if (mAnchors == null) posX else mAnchors!!.closeTo(mSection, posX)
    }

    fun cropInLimits(posX: Int): Int {
        return if (mAnchors == null) posX else mAnchors!!.cropInLimits(posX)
    }
}