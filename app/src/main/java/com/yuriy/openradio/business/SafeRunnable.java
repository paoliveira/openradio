package com.yuriy.openradio.business;

import java.lang.ref.WeakReference;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 1/10/16
 * E-Mail: chernyshov.yuriy@gmail.com
 */

/**
 * Helper class to manage implementation of the {@link Runnable} interface.
 * It allows to prevent to keep reference to the class which holds callee object.
 *
 * @param <T>
 */
public abstract class SafeRunnable<T> implements Runnable {

    /**
     * Weak reference to the class that holds callee object.
     */
    private final WeakReference<T> mReference;

    /**
     * Constructor.
     *
     * @param reference The reference to the holder class.
     */
    public SafeRunnable(final T reference) {
        super();
        mReference = new WeakReference<>(reference);
    }

    /**
     * Helper method that replace {@link #run()} hook method.
     *
     * @param reference The reference to the holder class.
     */
    public abstract void safeRun(final T reference);

    @Override
    public void run() {
        safeRun(mReference.get());
    }
}
