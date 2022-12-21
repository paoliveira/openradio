package com.yuriy.openradio.automotive.dependencies

import com.yuriy.openradio.automotive.ui.AutomotiveSettingsActivity
import com.yuriy.openradio.automotive.ui.AutomotiveSettingsActivityPresenter
import com.yuriy.openradio.automotive.ui.AutomotiveSettingsActivityPresenterImpl
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommon
import java.util.concurrent.atomic.AtomicBoolean

object DependencyRegistryAutomotive {

    private lateinit var sAutomotiveSettingsActivityPresenter: AutomotiveSettingsActivityPresenter

    @Volatile
    private var sInit = AtomicBoolean(false)

    /**
     * Init with application's context only!
     */
    fun init() {
        if (sInit.get()) {
            return
        }

        sAutomotiveSettingsActivityPresenter = AutomotiveSettingsActivityPresenterImpl(
            DependencyRegistryCommon.getLocationStorage()
        )

        sInit.set(true)
    }

    fun inject(dependency: AutomotiveSettingsActivity) {
        dependency.configureWith(sAutomotiveSettingsActivityPresenter)
    }
}
