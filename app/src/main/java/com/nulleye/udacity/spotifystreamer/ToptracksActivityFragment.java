package com.nulleye.udacity.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.RetrofitError;


/**
 * A placeholder fragment containing a simple view.
 */
public class ToptracksActivityFragment extends MyFragment {

    public static final String PREF_TOPTRACK_RESULT = "toptrack_result";
    public static final String PREF_TOPTRACK_RESULT_SELECTED = "toptrack_result_selected";

    SearchTrack searcher = null;
    TrackAdapter trackAdapter;


    public ToptracksActivityFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View vw = inflater.inflate(R.layout.fragment_toptracks, container, false);
        setupFragment(vw, R.id.topTracks);
        return vw;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        int messageId = 0;
        List<Track> tracks = null;
        int selectedItem = -1;
        boolean searching = false;
        if (savedInstanceState != null) {
            try {
                //Restore previous top tracks list
                Type trackCollectionType = new TypeToken<List<Track>>() {}.getType();
                tracks = new Gson().fromJson(savedInstanceState.getString(STATE_SEARCH_RESULT), trackCollectionType);
            } catch (Exception e) {}
            selectedItem = savedInstanceState.getInt(STATE_SEARCH_RESULT_SELECTED);
            //Message id (error, etc)
            messageId = savedInstanceState.getInt(STATE_SEARCH_MESSAGE);
            searching = savedInstanceState.getBoolean(STATE_SEARCHING);
            if (searching) tracks = null;
        }

        String id = null;
        if (tracks == null) {
            tracks = new ArrayList<Track>();
            //Get intent data and search for tracks
            id = getActivity().getIntent().getStringExtra(ELEMENT_ID);
            if (id != null) searcher = new SearchTrack();
            else messageId = R.string.message_error;
        }
        trackAdapter = new TrackAdapter(getActivity(), R.layout.artist_top_track, tracks);
        resultList.setAdapter(trackAdapter);


        if (searcher != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            searcher.execute(id,
                    prefs.getString(SettingsActivity.PREF_COUNTRY, Locale.getDefault().getCountry()));
        }
        else if (messageId > 0) setMessage(messageId);

        //Restore selected item
        if ((selectedItem < tracks.size()) && (selectedItem >= 0)) resultList.setSelection(selectedItem);
    }


    @Override
    public void onSaveInstanceState(final Bundle outState) {
        if (message.getVisibility() == View.VISIBLE)
            outState.putInt(STATE_SEARCH_MESSAGE, currentMessageId);
        try {
            if ((searcher != null) && (searcher.getStatus() != AsyncTask.Status.FINISHED))
                outState.putBoolean(STATE_SEARCHING, true);
            else outState.putBoolean(STATE_SEARCHING, false);
        } catch(Exception e){}
        try {
            outState.putString(STATE_SEARCH_RESULT, new Gson().toJson(trackAdapter.getData()));
        } catch(Exception e){}
        outState.putInt(STATE_SEARCH_RESULT_SELECTED, resultList.getSelectedItemPosition());
    }


    @Override
    public void onPause() {
        super.onPause();
        //Save to permanent storeage
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_TOPTRACK_RESULT, new Gson().toJson(trackAdapter.getData()));
        editor.putInt(PREF_TOPTRACK_RESULT_SELECTED, resultList.getSelectedItemPosition());
        editor.commit();
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.topTracks) {
            Track track = trackAdapter.getItem(position);
            if (track != null)
                Toast.makeText(getActivity(), getString(R.string.not_implemented), Toast.LENGTH_SHORT).show();;
        }
    }


    public class SearchTrack extends AsyncTask<String, Void, List<Track>> {

        SpotifyApi api = new SpotifyApi();
        int error = 0;


        @Override
        protected void onPreExecute() {
            setMessage(R.string.message_searching);
        }


        @Override
        protected List<Track> doInBackground(String... params) {
            try {
                SpotifyService spotify = api.getService();
                Map<String,Object> queryParams = new HashMap<String,Object>();
                //Country is mandatory (because we are not registered??)
                //country must be from "ISO 3166-1 alpha-2" (2 char country code) and this is equivalent
                //to Locale().getCountry(); the other country table "ISO 3166-1 alpha-3" (3 char country
                //code) is equivalent to Locale().getISO3Country();
                queryParams.put("country", params[1]);
                Tracks tracks = spotify.getArtistTopTrack(params[0], queryParams);
                if ((tracks != null) && (tracks.tracks != null)) return tracks.tracks;
            } catch(retrofit.RetrofitError re) {
                if (re.getKind() == RetrofitError.Kind.NETWORK) error = R.string.message_no_internet;
                else error = R.string.message_error;
            } catch(Exception e) {
                error = R.string.message_error;
            }
            return null;
        }


        @Override
        protected void onPostExecute(List<Track> tracks) {
            try {
                if (!isCancelled()) {
                    trackAdapter.clear();
                    if ((tracks != null) && !tracks.isEmpty()) {
                        //Sort by popularity and update adapter
                        Collections.sort(tracks, new TrackComparator(false));
                        trackAdapter.addAll(tracks);
                        showMessage(false);
                    } else {
                        if (error == 0) error = R.string.message_no_tracks_found;
                        setMessage(error);
                    }
                }
            } catch(Exception e) {
                //Eat any exception here
            }
        }


        @Override
        protected void onCancelled() {
        }

    } //SearchTrack


    /**
     * Comparator to sort track list by popularity
     */
    public class TrackComparator implements Comparator<Track> {

        boolean asc;


        public TrackComparator(boolean asc) {
            this.asc = asc;
        }


        @Override
        public int compare(Track first, Track second) {
            if (first.popularity == second.popularity) return 0;
            else if (first.popularity > second.popularity) return (asc)? 1 : -1;
            else return (asc)? -1 : 1;
        }

    } //TrackComparator


    public class TrackAdapter extends ArrayAdapter<Track> implements View.OnClickListener {

        private int resource;       //hold reference to layout
        private List<Track> data;  //hold reference to data to store on onSaveInstanceData


        public TrackAdapter(Context context, int resource, List<Track> objects) {
            super(context, resource, objects);
            this.resource = resource;
            this.data = objects;
        }


        public List<Track> getData() {
            return data;
        }


        public View getView(int position, View convertView, ViewGroup parent) {
            View result = convertView;
            //Reuse or create view
            if (result == null) {
                LayoutInflater inflater = (LayoutInflater)
                        getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                result = inflater.inflate(resource, null);
            }
            Track track = getItem(position);
            if ((track != null) && (result != null)) {
                ImageView iv = (ImageView) result.findViewById(R.id.thumbnail);
                iv.setTag(Integer.toString(position));
                iv.setOnClickListener(this);
                //Fetch artist image using picasso (if available)
                iv.setImageResource(R.drawable.noimage);
                if (track.album != null) {
                    Image image = getBestFitImage(track.album.images, bestFitImagePixels);
                    if (image != null) Picasso.with(getContext()).load(image.url).into(iv);
                }
                //Set song name
                TextView tv = (TextView) result.findViewById(R.id.songName);
                tv.setText(track.name);
                //Set album name
                if (track.album != null) {
                    tv = (TextView) result.findViewById(R.id.albumName);
                    tv.setText(track.album.name);
                }
            }
            return result;
        }


        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getContext(), ImagePopupActivity.class);
            intent.putExtra(ELEMENT_LIST, (Serializable) ImagePopupData.buildFromTrackList(data));
            intent.putExtra(ELEMENT_POSITION, Integer.parseInt((String) v.getTag()));
            intent.putExtra(ELEMENT_ACTION_INTENT_CLASS, "[Undefined_Class]");   //Generate exception (not implemented message)
            getContext().startActivity(intent);
        }


    } //TrackAdapter

}
