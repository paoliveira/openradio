/*
 * Copyright 2017 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.utils;

import com.yuriy.openradio.shared.vo.RadioStation;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 19/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */

public final class QueueHelperTest {

    public QueueHelperTest() {
        super();
    }

    @Test
    public void merge() {
        final List<RadioStation> listA = new ArrayList<>();
        final List<RadioStation> listB = new ArrayList<>();
        final RadioStation[] radioStations = new RadioStation[10];
        for (int i = 0; i < radioStations.length; i++) {
            final RadioStation radioStation = RadioStation.makeDefaultInstance();
            radioStation.setId(i);
//            radioStation.setStreamURL("Url" + i);

            radioStations[i] = radioStation;
        }

        listA.add(radioStations[0]);
        listA.add(radioStations[1]);
        listA.add(radioStations[2]);
        listA.add(radioStations[3]);
        listA.add(radioStations[4]);

        listB.add(radioStations[3]);
        listB.add(radioStations[4]);
        listB.add(radioStations[5]);

        QueueHelper.merge(listA, listB);

        assertThat(listA.size(), is(6));
        for (int i = 0; i < 6; i++) {
            assertThat(listA.get(i).getId(), is(i));
        }
    }
}
