package com.yuriy.openradio.service;

import com.yuriy.openradio.api.RadioStationVO;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 14/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class AbstractStorageTest {

    public AbstractStorageTest() {
        super();
    }

    @Test
    public void fromStringToList() throws Exception {
        final String value = "56911<:>{\"Id\":56911,\"Name\":\"Radio El Alfarero\",\"Bitrate\":\"0\",\"Country\":\"CA\",\"Genre\":\"\",\"ImgUrl\":\"https:\\/\\/img.dirble.com\\/station\\/56911\\/1ca6cde6-47ba-4b6e-a784-8c2f91103477.jpg\",\"StreamUrl\":\"http:\\/\\/192.240.102.133:11381\\/index.html?sid=1\",\"Status\":0,\"ThumbUrl\":\"https:\\/\\/img.dirble.com\\/station\\/56911\\/thumb_1ca6cde6-47ba-4b6e-a784-8c2f91103477.jpg\",\"Website\":\"http:\\/\\/radioelalfarero.com\",\"IsLocal\":false,\"SortId\":4}<<::>>57028<:>{\"Id\":57028,\"Name\":\"Radio Cafe Turc\",\"Bitrate\":\"320\",\"Country\":\"TR\",\"Genre\":\"\",\"ImgUrl\":\"https:\\/\\/img.dirble.com\\/station\\/57028\\/6a3255fc-8d55-46e3-ac07-75712fe752e7.jpg\",\"StreamUrl\":\"http:\\/\\/stream.ideocast.fr:8000\\/rct.mp3\",\"Status\":0,\"ThumbUrl\":\"https:\\/\\/img.dirble.com\\/station\\/57028\\/thumb_6a3255fc-8d55-46e3-ac07-75712fe752e7.jpg\",\"Website\":\"http:\\/\\/www.radiocafeturc.com\",\"IsLocal\":false,\"SortId\":2}<<::>>56722<:>{\"Id\":56722,\"Name\":\"Radiowaves\",\"Bitrate\":\"192\",\"Country\":\"CA\",\"Genre\":\"\",\"ImgUrl\":\"https:\\/\\/img.dirble.com\\/station\\/56722\\/3b1f2b68-7f17-4158-96df-976ec41c5f66.jpg\",\"StreamUrl\":\"http:\\/\\/relay.sonixcast.com\\/8897\",\"Status\":0,\"ThumbUrl\":\"https:\\/\\/img.dirble.com\\/station\\/56722\\/thumb_3b1f2b68-7f17-4158-96df-976ec41c5f66.jpg\",\"Website\":\"\",\"IsLocal\":false,\"SortId\":7}<<::>>56973<:>{\"Id\":56973,\"Name\":\"House Nation Toronto Radio\",\"Bitrate\":\"128\",\"Country\":\"CA\",\"Genre\":\"\",\"ImgUrl\":\"https:\\/\\/img.dirble.com\\/station\\/56973\\/bf422823-a224-4436-abb1-e6ee6dad9e08.jpg\",\"StreamUrl\":\"http:\\/\\/soho.wavestreamer.com:7860\",\"Status\":0,\"ThumbUrl\":\"https:\\/\\/img.dirble.com\\/station\\/56973\\/thumb_bf422823-a224-4436-abb1-e6ee6dad9e08.jpg\",\"Website\":\"http:\\/\\/greatdrake72.wixsite.com\\/sand-drift\",\"IsLocal\":false,\"SortId\":0}<<::>>56907<:>{\"Id\":56907,\"Name\":\"Rock 103\",\"Bitrate\":\"128\",\"Country\":\"CA\",\"Genre\":\"\",\"ImgUrl\":\"https:\\/\\/img.dirble.com\\/station\\/56907\\/9e83203b-b5ed-4332-9e1a-2bbf627eda20.jpg\",\"StreamUrl\":\"http:\\/\\/relay.sonixcast.com\\/9697\",\"Status\":0,\"ThumbUrl\":\"https:\\/\\/img.dirble.com\\/station\\/56907\\/thumb_9e83203b-b5ed-4332-9e1a-2bbf627eda20.jpg\",\"Website\":\"http:\\/\\/www.rock103.ca\",\"IsLocal\":false,\"SortId\":5}<<::>>57078<:>{\"Id\":57078,\"Name\":\"Xenxay's 70s & 80s Hits\",\"Bitrate\":\"0\",\"Country\":\"CA\",\"Genre\":\"\",\"ImgUrl\":\"https:\\/\\/img.dirble.com\\/station\\/57078\\/57d61792-f061-486d-b0f4-8c5440fad7d1.jpg\",\"StreamUrl\":\"http:\\/\\/listen.shoutcast.com\\/xenxay-smusicmix\",\"Status\":0,\"ThumbUrl\":\"https:\\/\\/img.dirble.com\\/station\\/57078\\/thumb_57d61792-f061-486d-b0f4-8c5440fad7d1.jpg\",\"Website\":\"http:\\/\\/www.xenxay.xyz\",\"IsLocal\":false,\"SortId\":3}<<::>>56721<:>{\"Id\":56721,\"Name\":\"Hot105 Non Stop\",\"Bitrate\":\"80\",\"Country\":\"CA\",\"Genre\":\"\",\"ImgUrl\":\"https:\\/\\/img.dirble.com\\/station\\/56721\\/Hot105_cover.jpg\",\"StreamUrl\":\"http:\\/\\/comet.shoutca.st:8459\\/stream\",\"Status\":0,\"ThumbUrl\":\"https:\\/\\/img.dirble.com\\/station\\/56721\\/thumb_Hot105_cover.jpg\",\"Website\":\"http:\\/\\/tunein.com\\/radio\\/HOT105-Non-Stop-s275831\\/\",\"IsLocal\":false,\"SortId\":8}<<::>>56670<:>{\"Id\":56670,\"Name\":\"Hot105 Non Stop\",\"Bitrate\":\"24\",\"Country\":\"CA\",\"Genre\":\"\",\"ImgUrl\":\"https:\\/\\/img.dirble.com\\/station\\/56670\\/HOT105_LOGO_NEW_3.jpeg\",\"StreamUrl\":\"http:\\/\\/myradio.doscast.com:8290\\/stream\",\"Status\":0,\"ThumbUrl\":\"https:\\/\\/img.dirble.com\\/station\\/56670\\/thumb_HOT105_LOGO_NEW_3.jpeg\",\"Website\":\"http:\\/\\/hot105worldwide.wixsite.com\\/hot105\\/hot105-non-stop\",\"IsLocal\":false,\"SortId\":6}";
        final List<RadioStationVO> list = AbstractStorage.getAllFromString(value);

        assertThat(list, notNullValue());
        assertThat(list.size(), is(8));

        for (final RadioStationVO radioStation : list) {
            assertThat(radioStation, notNullValue());
            assertThat(radioStation.getStreamURL(), notNullValue());
            assertThat(radioStation.getStreamURL().length(), not(0));
        }
    }
}
