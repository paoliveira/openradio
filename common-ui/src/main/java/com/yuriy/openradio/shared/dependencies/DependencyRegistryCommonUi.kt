/*
 * Copyright 2022 The "Open Radio" Project. Author: Chernyshov Yuriy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yuriy.openradio.shared.dependencies

import android.content.Context
import com.yuriy.openradio.shared.model.storage.StorageManagerLayer
import com.yuriy.openradio.shared.model.storage.StorageManagerLayerImpl
import com.yuriy.openradio.shared.model.storage.drive.GoogleDriveManager
import com.yuriy.openradio.shared.presenter.MediaPresenter
import com.yuriy.openradio.shared.presenter.MediaPresenterImpl
import com.yuriy.openradio.shared.view.dialog.AddEditStationDialogPresenter
import com.yuriy.openradio.shared.view.dialog.AddEditStationDialogPresenterImpl
import com.yuriy.openradio.shared.view.dialog.BaseAddEditStationDialog
import com.yuriy.openradio.shared.view.dialog.EditStationDialog
import com.yuriy.openradio.shared.view.dialog.EditStationPresenter
import com.yuriy.openradio.shared.view.dialog.EditStationPresenterImpl
import com.yuriy.openradio.shared.view.dialog.EqualizerDialog
import com.yuriy.openradio.shared.view.dialog.EqualizerPresenter
import com.yuriy.openradio.shared.view.dialog.EqualizerPresenterImpl
import com.yuriy.openradio.shared.view.dialog.NetworkDialog
import com.yuriy.openradio.shared.view.dialog.RemoveStationDialog
import com.yuriy.openradio.shared.view.dialog.RemoveStationDialogPresenter
import com.yuriy.openradio.shared.view.dialog.RemoveStationDialogPresenterImpl
import java.util.concurrent.atomic.AtomicBoolean

object DependencyRegistryCommonUi {

    private lateinit var sMediaPresenter: MediaPresenter
    private lateinit var sEditStationPresenter: EditStationPresenter
    private lateinit var sEqualizerPresenter: EqualizerPresenter
    private lateinit var sRemoveStationDialogPresenter: RemoveStationDialogPresenter
    private lateinit var sAddEditStationDialogPresenter: AddEditStationDialogPresenter
    private lateinit var sStorageManagerLayer: StorageManagerLayer

    @Volatile
    private var sInit = AtomicBoolean(false)

    /**
     * Init with application's context only!
     */
    fun init(context: Context) {
        if (sInit.get()) {
            return
        }
        sMediaPresenter = MediaPresenterImpl(
            context,
            DependencyRegistryCommon.getNetworkLayer(),
            DependencyRegistryCommon.getLocationStorage(),
            DependencyRegistryCommon.getSleepTimerModel()
        )
        sEditStationPresenter = EditStationPresenterImpl(
            DependencyRegistryCommon.getFavoriteStorage(),
            DependencyRegistryCommon.getLocalRadioStationsStorage()
        )
        sStorageManagerLayer = StorageManagerLayerImpl(
            DependencyRegistryCommon.getFavoriteStorage(),
            DependencyRegistryCommon.getLocalRadioStationsStorage()
        )
        sEqualizerPresenter = EqualizerPresenterImpl(DependencyRegistryCommon.getEqualizerLayer())
        sRemoveStationDialogPresenter = RemoveStationDialogPresenterImpl(
            context,
            DependencyRegistryCommon.getRadioStationManagerLayer()
        )
        sAddEditStationDialogPresenter = AddEditStationDialogPresenterImpl(
            context,
            DependencyRegistryCommon.getRadioStationManagerLayer()
        )

        sInit.set(true)
    }

    fun inject(dependency: MediaPresenterDependency) {
        dependency.configureWith(sMediaPresenter)
    }

    fun inject(dependency: GoogleDriveManager) {
        dependency.configureWith(sStorageManagerLayer)
    }

    fun inject(dependency: EditStationDialog) {
        dependency.configureWith(sEditStationPresenter)
    }

    fun inject(dependency: EqualizerDialog) {
        dependency.configureWith(sEqualizerPresenter)
    }

    fun inject(dependency: RemoveStationDialog) {
        dependency.configureWith(sRemoveStationDialogPresenter)
    }

    fun inject(dependency: BaseAddEditStationDialog) {
        dependency.configureWith(sAddEditStationDialogPresenter)
    }

    fun inject(dependency: NetworkDialog) {
        dependency.configureWith(DependencyRegistryCommon.getNetworkSettingsStorage())
    }
}
