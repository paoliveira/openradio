package com.yuriy.openradio.view;

import android.view.View;

import java.lang.ref.WeakReference;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 1/10/16
 * E-Mail: chernyshov.yuriy@gmail.com
 */

/**
 * Helper class to manage OnClick events.
 * It allows to prevent to keep reference to the class which holds callee object.
 *
 * @param <T>
 */
public abstract class SafeOnClickListener <T> implements View.OnClickListener {

    /**
     * Weak reference to the class that holds callee object.
     */
    private final WeakReference<T> mReference;

    /**
     * Constructor.
     *
     * @param reference The reference to the holder class.
     */
    public SafeOnClickListener(final T reference) {
        super();
        mReference = new WeakReference<>(reference);
    }

    /**
     * Helper method that replace {@link #onClick(View)} event.
     * It passes back reference to the origin View.
     *
     * @param reference The reference to the holder class.
     * @param view      View, that dispatch the event.
     */
    public abstract void safeOnClick(final T reference, final View view);

    @Override
    public void onClick(final View view) {
        safeOnClick(mReference.get(), view);
    }
}
