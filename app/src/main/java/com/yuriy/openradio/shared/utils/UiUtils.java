package com.yuriy.openradio.shared.utils;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.yuriy.openradio.shared.view.dialog.AboutDialog;
import com.yuriy.openradio.shared.view.dialog.EditStationDialog;
import com.yuriy.openradio.shared.view.dialog.EqualizerDialog;
import com.yuriy.openradio.shared.view.dialog.GeneralSettingsDialog;
import com.yuriy.openradio.shared.view.dialog.GoogleDriveDialog;
import com.yuriy.openradio.shared.view.dialog.RSSettingsDialog;
import com.yuriy.openradio.shared.view.dialog.SearchDialog;

public final class UiUtils {

    private UiUtils() {
        super();
    }

    /**
     * Clears any active dialog.
     *
     * @param context {@link FragmentActivity}
     * @param transaction Instance of Fragment transaction.
     */
    public static void clearDialogs(final FragmentActivity context, final FragmentTransaction transaction) {
        final FragmentManager manager = context.getSupportFragmentManager();
        Fragment fragment = manager.findFragmentByTag(AboutDialog.DIALOG_TAG);
        if (fragment != null) {
            transaction.remove(fragment);
        }
        fragment = manager.findFragmentByTag(SearchDialog.DIALOG_TAG);
        if (fragment != null) {
            transaction.remove(fragment);
        }
        fragment = manager.findFragmentByTag(EqualizerDialog.DIALOG_TAG);
        if (fragment != null) {
            transaction.remove(fragment);
        }
        fragment = manager.findFragmentByTag(GoogleDriveDialog.DIALOG_TAG);
        if (fragment != null) {
            transaction.remove(fragment);
        }
        fragment = manager.findFragmentByTag(GeneralSettingsDialog.DIALOG_TAG);
        if (fragment != null) {
            transaction.remove(fragment);
        }
        fragment = manager.findFragmentByTag(RSSettingsDialog.DIALOG_TAG);
        if (fragment != null) {
            transaction.remove(fragment);
        }
        fragment = manager.findFragmentByTag(EditStationDialog.DIALOG_TAG);
        if (fragment != null) {
            transaction.remove(fragment);
        }
        transaction.commitNow();
    }
}
