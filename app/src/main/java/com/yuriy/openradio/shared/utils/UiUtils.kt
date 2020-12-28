package com.yuriy.openradio.shared.utils

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import com.yuriy.openradio.shared.view.dialog.AboutDialog
import com.yuriy.openradio.shared.view.dialog.EditStationDialog
import com.yuriy.openradio.shared.view.dialog.EqualizerDialog
import com.yuriy.openradio.shared.view.dialog.GeneralSettingsDialog
import com.yuriy.openradio.shared.view.dialog.GoogleDriveDialog
import com.yuriy.openradio.shared.view.dialog.RSSettingsDialog
import com.yuriy.openradio.shared.view.dialog.SearchDialog

object UiUtils {
    /**
     * Clears any active dialog.
     *
     * @param context [FragmentActivity]
     * @param transaction Instance of Fragment transaction.
     */
    @JvmStatic
    fun clearDialogs(context: FragmentActivity, transaction: FragmentTransaction) {
        val manager = context.supportFragmentManager
        var fragment = manager.findFragmentByTag(AboutDialog.DIALOG_TAG)
        if (fragment != null) {
            transaction.remove(fragment)
        }
        fragment = manager.findFragmentByTag(SearchDialog.DIALOG_TAG)
        if (fragment != null) {
            transaction.remove(fragment)
        }
        fragment = manager.findFragmentByTag(EqualizerDialog.DIALOG_TAG)
        if (fragment != null) {
            transaction.remove(fragment)
        }
        fragment = manager.findFragmentByTag(GoogleDriveDialog.DIALOG_TAG)
        if (fragment != null) {
            transaction.remove(fragment)
        }
        fragment = manager.findFragmentByTag(GeneralSettingsDialog.DIALOG_TAG)
        if (fragment != null) {
            transaction.remove(fragment)
        }
        fragment = manager.findFragmentByTag(RSSettingsDialog.DIALOG_TAG)
        if (fragment != null) {
            transaction.remove(fragment)
        }
        fragment = manager.findFragmentByTag(EditStationDialog.DIALOG_TAG)
        if (fragment != null) {
            transaction.remove(fragment)
        }
        transaction.commitNow()
    }
}
