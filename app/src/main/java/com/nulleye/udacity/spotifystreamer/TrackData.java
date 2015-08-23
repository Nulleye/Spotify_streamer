package com.nulleye.udacity.spotifystreamer;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

/**
 * Created by cristian on 8/8/15.
 */
public class TrackData extends ItemData {

    //for tracks only
    public boolean playable;
    public long duration;
    public String previewUrl;
    public long lastIndex;
    public long previewDuration;


    public TrackData() {
        super();
        playable = false;
        previewUrl = null;
        duration = -1;
        lastIndex = -1;
        previewDuration = -1;
    }


    public TrackData(Track track) {
        lastIndex = -1;
        previewDuration = -1;
        if (track != null) {
            id = track.id;
            title = track.name;
            if (track.album != null) {
                subtitle = track.album.name;
                Image image = MyFragment.getLargestImage(track.album.images);
                if (image != null) {
                    imageUrl = image.url;
                    imageWidth = image.width;
                    imageHeight = image.height;
                } else {
                    imageUrl = null;
                    imageWidth = -1;
                    imageHeight = -1;
                }
            } else {
                subtitle = null;
                imageUrl = null;
                imageWidth = -1;
                imageHeight = -1;
            }
            playable = (track.is_playable != null)? track.is_playable : false;
            previewUrl = track.preview_url;
            duration = track.duration_ms;
            externalUrl = externalUrl(track.external_urls);
            if (externalUrl == null) externalUrl = externalUrl(track.album.external_urls);
        } else {
            id = null;
            title = null;
            subtitle = null;
            imageUrl = null;
            imageWidth = -1;
            imageHeight = -1;
            playable = false;
            previewUrl = null;
            duration = -1;
            externalUrl = null;
        }
    }


    public static List<ItemData> buildFromTrackList(List<Track> tracks) {
        if (tracks != null) {
            List<ItemData> result = new ArrayList<ItemData>(tracks.size());
            for(Track track:tracks) result.add(new TrackData(track));
            return result;
        }
        return null;
    }


}
