package com.yuriy.openradio.utils;

import com.yuriy.openradio.api.RadioStationVO;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
    public void merge() throws Exception {
        final List<RadioStationVO> listA = new ArrayList<>();
        final List<RadioStationVO> listB = new ArrayList<>();
        final RadioStationVO[] radioStations = new RadioStationVO[10];
        for (int i = 0; i < radioStations.length; i++) {
            final RadioStationVO radioStation = RadioStationVO.makeDefaultInstance();
            radioStation.setId(i);
            radioStation.setStreamURL("Url" + i);

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
