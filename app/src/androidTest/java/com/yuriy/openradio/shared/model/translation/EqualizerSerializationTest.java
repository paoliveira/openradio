package com.yuriy.openradio.shared.model.translation;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.yuriy.openradio.shared.vo.EqualizerState;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public final class EqualizerSerializationTest {

    public EqualizerSerializationTest() {
        super();
    }

    @Test
    public void testSerializationOfEqualizer() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final List<String> list = new ArrayList<>();
        list.add("One");
        list.add("Two");
        list.add("Three");

        final EqualizerState state = new EqualizerState();
        state.setPresets(list);

        final short bandRangeLow = -1500;
        final short bandRangeHigh = 1500;
        final short[] bandRange = new short[2];
        bandRange[0] = bandRangeLow;
        bandRange[1] = bandRangeHigh;
        state.setBandLevelRange(bandRange);

        final short bandLevel1 = 100;
        final short bandLevel2 = 200;
        final short bandLevel3 = 300;
        final short bandLevel4 = 400;
        final short bandLevel5 = 500;
        final short[] bandLevels = new short[5];
        bandLevels[0] = bandLevel1;
        bandLevels[1] = bandLevel2;
        bandLevels[2] = bandLevel3;
        bandLevels[3] = bandLevel4;
        bandLevels[4] = bandLevel5;
        state.setBandLevels(bandLevels);

        final EqualizerStateSerializer serializer = new EqualizerJsonStateSerializer();
        final String value = serializer.serialize(state);

        final EqualizerStateDeserializer deserializer = new EqualizerStateJsonDeserializer();
        final EqualizerState newState = deserializer.deserialize(context, value);

        final List<String> newList = newState.getPresets();
        MatcherAssert.assertThat(newList.size(), CoreMatchers.is(3));
        MatcherAssert.assertThat(newList.get(0), CoreMatchers.is("One"));
        MatcherAssert.assertThat(newList.get(1), CoreMatchers.is("Two"));
        MatcherAssert.assertThat(newList.get(2), CoreMatchers.is("Three"));

        final short[] newBandRange = newState.getBandLevelRange();
        MatcherAssert.assertThat(newBandRange.length, CoreMatchers.is(2));
        MatcherAssert.assertThat(newBandRange[0], CoreMatchers.is(bandRangeLow));
        MatcherAssert.assertThat(newBandRange[1], CoreMatchers.is(bandRangeHigh));

        final short[] newBandLevels = newState.getBandLevels();
        MatcherAssert.assertThat(newBandLevels.length, CoreMatchers.is(5));
        MatcherAssert.assertThat(newBandLevels[0], CoreMatchers.is(bandLevel1));
        MatcherAssert.assertThat(newBandLevels[1], CoreMatchers.is(bandLevel2));
        MatcherAssert.assertThat(newBandLevels[2], CoreMatchers.is(bandLevel3));
        MatcherAssert.assertThat(newBandLevels[3], CoreMatchers.is(bandLevel4));
        MatcherAssert.assertThat(newBandLevels[4], CoreMatchers.is(bandLevel5));
    }
}
