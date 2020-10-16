package com.xenione.libs.swipemaker;

import java.util.Arrays;

/**
 * Created by Eugeni on 16/04/2016.
 */
public class Anchors {

    private static class AnchorHelper {

        private final Integer[] mAnchors;

        private AnchorHelper(final Integer[] anchor) {
            super();
            mAnchors = anchor;
        }

        private int next(int index) {
            return mAnchors[index + 1];
        }

        private int prev(int index) {
            return mAnchors[index - 1];
        }

        private int pos(int index) {
            return mAnchors[index];
        }

        private int sectionFromInf(int position) {
            int section = 0;
            for (int i = 0; i < mAnchors.length - 1; i++) {
                if (mAnchors[i] > position) {
                    section = i;
                    break;
                }
            }

            return section;
        }

        private int sectionFromSup(int position) {
            int section = 0;
            for (int i = mAnchors.length - 1; i >= 0; i--) {
                if (mAnchors[i] < position) {
                    section = i;
                    break;
                }
            }

            return section;
        }

        private int size() {
            return mAnchors.length;
        }

        private int getSupLimit() {
            return mAnchors[mAnchors.length - 1];
        }

        private int getInfLimit() {
            return mAnchors[0];
        }

        private static int distance(int init, int end) {
            return Math.abs(init - end);
        }
    }

    private final AnchorHelper mAnchorHelper;

    private Anchors(final Integer[] anchor) {
        mAnchorHelper = new AnchorHelper(anchor);
    }

    public static Anchors make(Integer[] anchors) {
        if (anchors.length < 2) {
            throw new IllegalArgumentException(
                    "Amount of anchor points provided to SwipeLayout have to be bigger than 2"
            );
        }
        Arrays.sort(anchors);
        return new Anchors(anchors);
    }

    /**
     * Gives the amount of anchors that are set.
     *
     * @return amount of anchors.
     */
    public int size() {
        return mAnchorHelper.size();
    }

    /**
     * Gives distance relative (1%) within the superior limit and the
     * Inferior limit from the inferior limit.
     *
     * @param x point from where to get relative distance
     * @return 1% from inf limit
     */
    public float distance(int x) {
        return distance(x, mAnchorHelper.getSupLimit(), mAnchorHelper.getInfLimit());
    }

    /**
     * Gives distance relative (1%) within the superior Section limit and the
     * Inferior Section limit from the inferior limit.
     *
     * @param x point from where to get relative distance
     * @return 1% from inf limit
     */

    public float distance(int section, int x) {
        return distance(x, mAnchorHelper.next(section), mAnchorHelper.pos(section));
    }

    private float distance(int x, int limitSup, int limitInf) {
        return (float) (x - limitInf) / (limitSup - limitInf);
    }

    public int anchorFor(int section) {
        return mAnchorHelper.pos(section);
    }

    public int sectionFor(int position) {
        if ((position > mAnchorHelper.getSupLimit()) || (position < mAnchorHelper.getInfLimit())) {
            throw new IllegalArgumentException("position exceed limits");
        }
        return mAnchorHelper.sectionFromSup(position);
    }

    public int closeTo(int section, int point) {
        int distInf = AnchorHelper.distance(point, mAnchorHelper.pos(section));
        int distSup = AnchorHelper.distance(point, mAnchorHelper.next(section));
        if (distInf < distSup) {
            return mAnchorHelper.pos(section);
        }
        return mAnchorHelper.next(section);
    }

    public int cropInLimits(int x) {
        int inBounds = x;
        if (x < mAnchorHelper.getInfLimit()) {
            inBounds = mAnchorHelper.getInfLimit();
        } else if (x > mAnchorHelper.getSupLimit()) {
            inBounds = mAnchorHelper.getSupLimit();
        }
        return inBounds;
    }
}
