package com.xenione.libs.swipemaker

import java.util.*
import kotlin.math.abs

/**
 * Created by Eugeni on 16/04/2016.
 */
class Anchors private constructor(anchor: Array<Int>) {

    private class AnchorHelper(private val mAnchors: Array<Int>) {

        fun next(index: Int): Int {
            return mAnchors[index + 1]
        }

        fun pos(index: Int): Int {
            return mAnchors[index]
        }

        fun sectionFromSup(position: Int): Int {
            var section = 0
            for (i in mAnchors.indices.reversed()) {
                if (mAnchors[i] < position) {
                    section = i
                    break
                }
            }
            return section
        }

        fun size(): Int {
            return mAnchors.size
        }

        val supLimit: Int
            get() = mAnchors[mAnchors.size - 1]
        val infLimit: Int
            get() = mAnchors[0]

        companion object {
            fun distance(init: Int, end: Int): Int {
                return abs(init - end)
            }
        }
    }

    private val mAnchorHelper: AnchorHelper

    /**
     * Gives the amount of anchors that are set.
     *
     * @return amount of anchors.
     */
    fun size(): Int {
        return mAnchorHelper.size()
    }

    /**
     * Gives distance relative (1%) within the superior limit and the
     * Inferior limit from the inferior limit.
     *
     * @param x point from where to get relative distance
     * @return 1% from inf limit
     */
    fun distance(x: Int): Float {
        return distance(x, mAnchorHelper.supLimit, mAnchorHelper.infLimit)
    }

    /**
     * Gives distance relative (1%) within the superior Section limit and the
     * Inferior Section limit from the inferior limit.
     *
     * @param x point from where to get relative distance
     * @return 1% from inf limit
     */
    fun distance(section: Int, x: Int): Float {
        return distance(x, mAnchorHelper.next(section), mAnchorHelper.pos(section))
    }

    private fun distance(x: Int, limitSup: Int, limitInf: Int): Float {
        return (x - limitInf).toFloat() / (limitSup - limitInf)
    }

    fun anchorFor(section: Int): Int {
        return mAnchorHelper.pos(section)
    }

    fun sectionFor(position: Int): Int {
        require(!(position > mAnchorHelper.supLimit || position < mAnchorHelper.infLimit)) { "position exceed limits" }
        return mAnchorHelper.sectionFromSup(position)
    }

    fun closeTo(section: Int, point: Int): Int {
        val distInf = AnchorHelper.distance(point, mAnchorHelper.pos(section))
        val distSup = AnchorHelper.distance(point, mAnchorHelper.next(section))
        return if (distInf < distSup) {
            mAnchorHelper.pos(section)
        } else mAnchorHelper.next(section)
    }

    fun cropInLimits(x: Int): Int {
        var inBounds = x
        if (x < mAnchorHelper.infLimit) {
            inBounds = mAnchorHelper.infLimit
        } else if (x > mAnchorHelper.supLimit) {
            inBounds = mAnchorHelper.supLimit
        }
        return inBounds
    }

    companion object {
        @JvmStatic
        fun make(anchors: Array<Int>): Anchors {
            require(anchors.size >= 2) { "Amount of anchor points provided to SwipeLayout have to be bigger than 2" }
            Arrays.sort(anchors)
            return Anchors(anchors)
        }
    }

    init {
        mAnchorHelper = AnchorHelper(anchor)
    }
}