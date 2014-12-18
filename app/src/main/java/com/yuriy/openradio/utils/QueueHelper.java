package com.yuriy.openradio.utils;

import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.util.Log;

import com.yuriy.openradio.api.RadioStationVO;
import com.yuriy.openradio.business.JSONDataParserImpl;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with Android Studio.
 * Author: Chernyshov Yuriy - Mobile Development
 * Date: 16.12.14
 * Time: 11:44
 */
public class QueueHelper {

    private static final String CLASS_NAME = QueueHelper.class.getSimpleName();

    public static List<MediaSession.QueueItem> getPlayingQueue(
            final Context context,
            final List<RadioStationVO> radioStations) {
        final List<MediaSession.QueueItem> queue = new ArrayList<>();
        int count = 0;
        MediaSession.QueueItem item;
        MediaMetadata track;
        for (RadioStationVO radioStation : radioStations) {

            try {
                track = JSONDataParserImpl.buildMediaMetadataFromRadioStation(context, radioStation);
            } catch (JSONException e) {
                Log.e(CLASS_NAME, "Can not parse Media Metadata:" + e.getMessage());
                continue;
            }

            item = new MediaSession.QueueItem(track.getDescription(), count++);
            queue.add(item);
        }
        return queue;
    }

    public static int getRadioStationIndexOnQueue(final Iterable<MediaSession.QueueItem> queue,
                                                  final String mediaId) {
        int index = 0;
        for (MediaSession.QueueItem item: queue) {
            if (mediaId.equals(item.getDescription().getMediaId())) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public static boolean isIndexPlayable(final int index,
                                          final List<MediaSession.QueueItem> queue) {
        return (queue != null && index >= 0 && index < queue.size());
    }

    public static RadioStationVO getRadioStationById(final String id,
                                                     final List<RadioStationVO> radioStations) {
        for (RadioStationVO radioStation : radioStations) {
            if (radioStation == null) {
                continue;
            }
            if (String.valueOf(radioStation.getId()).equals(id)) {
                return radioStation;
            }
        }

        return null;
    }

    public static <T> void copyCollection(final List<T> destination, final List<T> source) {
        destination.clear();
        for (T sourceItem : source) {
            destination.add(sourceItem);
        }
    }
}