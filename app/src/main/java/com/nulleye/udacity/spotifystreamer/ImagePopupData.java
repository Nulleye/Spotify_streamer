package com.nulleye.udacity.spotifystreamer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

/**
 * Common data structure to hold elements for ImagePopupActivity
 *
 * Created by cristian on 3/7/15.
 */
public class ImagePopupData implements Serializable {

    public String id;
    public String title;
    public String subtitle;
    public String imageUrl;
    public int imageWidth;
    public int imageHeight;


    public ImagePopupData() {
        id = null;
        title = null;
        subtitle = null;
        imageUrl = null;
        imageWidth = -1;
        imageHeight = -1;
    }


    public ImagePopupData(Artist artist) {
        subtitle = null;
        if (artist != null) {
            id = artist.id;
            title = artist.name;
            Image image = MyFragment.getLargestImage(artist.images);
            if (image != null) {
                imageUrl = image.url;
                imageWidth = image.width;
                imageHeight = image.height;
                return;
            }
        } else {
            id = null;
            title = null;
        }
        imageUrl = null;
        imageWidth = -1;
        imageHeight = -1;
    }


    public ImagePopupData(Track track) {
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
                    return;
                }
            } else subtitle = null;
        } else {
            id = null;
            title = null;
            subtitle = null;
        }
        imageUrl = null;
        imageWidth = -1;
        imageHeight = -1;
    }


    public static List<ImagePopupData> buildFromArtistList(List<Artist> artists) {
        if (artists != null) {
            List<ImagePopupData> result = new ArrayList<ImagePopupData>(artists.size());
            for(Artist artist:artists) result.add(new ImagePopupData(artist));
            return result;
        }
        return null;
    }


    public static List<ImagePopupData> buildFromTrackList(List<Track> tracks) {
        if (tracks != null) {
            List<ImagePopupData> result = new ArrayList<ImagePopupData>(tracks.size());
            for(Track track:tracks) result.add(new ImagePopupData(track));
            return result;
        }
        return null;
    }


}
