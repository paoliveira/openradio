package com.yuriy.openradio.shared.permission;

import android.content.Context;

import com.yuriy.openradio.shared.view.activity.PermissionsDialogActivity;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionListener implements PermissionStatusListener {

    /**
     * Reference to the enclosing class.
     */
    private final WeakReference<Context> mReference;
    private final Map<String, Double> mMap = new ConcurrentHashMap<>();
    private static final int DELTA = 2000;

    /**
     * Main constructor.
     *
     * @param reference Reference to the enclosing class.
     */
    public PermissionListener(final Context reference) {
        super();
        mReference = new WeakReference<>(reference);
    }

    @Override
    public void onPermissionRequired(final String permissionName) {
        if (mReference.get() == null) {
            return;
        }

        final double currentTime = System.currentTimeMillis();

        if (mMap.containsKey(permissionName)) {
            if (currentTime - mMap.get(permissionName) < DELTA) {
                return;
            }
        }

        mMap.put(permissionName, currentTime);

        mReference.get().startActivity(
                PermissionsDialogActivity.getIntent(mReference.get(), permissionName)
        );
    }
}
