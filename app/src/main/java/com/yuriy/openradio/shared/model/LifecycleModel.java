/*
 * Copyright 2020 The "Open Radio" Project. Author: Chernyshov Yuriy [chernyshov.yuriy@gmail.com]
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

package com.yuriy.openradio.shared.model;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.ViewModel;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Class that monitors and holds current state of the application.
 */
@Singleton
public class LifecycleModel extends ViewModel {

    /**
     * Current state of the application.
     */
    private Lifecycle.Event mEvent;

    @Inject
    public LifecycleModel() {
        super();
    }

    /**
     * @return Current state of the application.
     */
    @Nullable
    public Lifecycle.Event getEvent() {
        return mEvent;
    }

    /**
     * @return {@code true} is application is in background, {@code false} otherwise.
     */
    public boolean isAppInBg() {
        return mEvent != null
                && (mEvent.ordinal() >= Lifecycle.Event.ON_STOP.ordinal()
                && mEvent.ordinal() <= Lifecycle.Event.ON_DESTROY.ordinal());
    }

    public void init() {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(
                (LifecycleEventObserver) (source, event) -> mEvent = event
        );
    }
}
