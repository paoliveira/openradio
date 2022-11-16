package com.yuriy.openradio.automotive

import android.content.Context
import com.yuriy.openradio.automotive.dependencies.DependencyRegistryAutomotive
import com.yuriy.openradio.shared.MainAppCommon
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommonUi

class MainAppAutomotive: MainAppCommon() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        DependencyRegistryCommonUi.init(base)
        DependencyRegistryAutomotive.init(base)
    }
}

