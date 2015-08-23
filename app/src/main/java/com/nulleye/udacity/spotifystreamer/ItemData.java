package com.nulleye.udacity.spotifystreamer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Image;

/**
 * Common data structure to hold elements for ImagePopupActivity
 *
 * Created by cristian on 3/7/15.
 */
public class ItemData implements Serializable {

    public final static String SPOTIFY = "spotify";

    public String id;
    public String title;
    public String subtitle;
    public String imageUrl;
    public int imageWidth;
    public int imageHeight;
    public String externalUrl;


    public ItemData() {
        id = null;
        title = null;
        subtitle = null;
        imageUrl = null;
        imageWidth = -1;
        imageHeight = -1;
        externalUrl = null;
    }


    public ItemData(Artist artist) {
        subtitle = null;
        if (artist != null) {
            id = artist.id;
            title = artist.name;
            Image image = MyFragment.getLargestImage(artist.images);
            if (image != null) {
                imageUrl = image.url;
                imageWidth = image.width;
                imageHeight = image.height;
            } else {
                imageUrl = null;
                imageWidth = -1;
                imageHeight = -1;
            }
            externalUrl = externalUrl(artist.external_urls);
        } else {
            id = null;
            title = null;
            imageUrl = null;
            imageWidth = -1;
            imageHeight = -1;
            externalUrl = null;
        }
    }


    public static List<ItemData> buildFromArtistList(List<Artist> artists) {
        if (artists != null) {
            List<ItemData> result = new ArrayList<ItemData>(artists.size());
            for(Artist artist:artists) result.add(new ItemData(artist));
            return result;
        }
        return null;
    }


    public static String externalUrl(Map<String, String> urls) {
        if (urls != null) {
            if (urls.containsKey(SPOTIFY)) return urls.get(SPOTIFY);
            else for(String url:urls.values()) if (url != null) return url;
        }
        return null;
    }


}
