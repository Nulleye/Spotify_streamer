package com.nulleye.udacity.spotifystreamer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

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

    public static final String PREF_TOPTRACK_RESULT = "com.nulleye.udacity.spotifystreamer.PREF_TOPTRACK_RESULT";
    public static final String PREF_TOPTRACK_RESULT_SELECTED = "com.nulleye.udacity.spotifystreamer.PREF_TOPTRACK_RESULT_SELECTED";
    public static final String PREF_TOPTRACK_ARTIST_ID = "com.nulleye.udacity.spotifystreamer.PREF_TOPTRACK_ARTIST_ID";
    public static final String PREF_TOPTRACK_ARTIST_NAME = "com.nulleye.udacity.spotifystreamer.PREF_TOPTRACK_ARTIST_NAME";
    public static final String PREF_TOPTRACK_ARTIST_LINK = "com.nulleye.udacity.spotifystreamer.PREF_TOPTRACK_ARTIST_LINK";


    public static final String STATE_ARTIST_ID = "artist_id";
    public static final String STATE_ARTIST_NAME = "artist_name";
    public static final String STATE_ARTIST_LINK = "artist_link";

    ShareActionProvider mShareActionProvider;

    SearchTrack searcher = null;
    TrackAdapter trackAdapter;

    String artistName = null;
    String artistId = null;
    String artistLink = null;

    boolean initialLoad = false;

    boolean mTwoPane = false;

    public void setTwoPane(boolean twoPane) {
        mTwoPane = twoPane;
    }

    public interface Callback {
        public void onTrackItemSelected(Serializable itemList, int itemPosition, String extra);
        public void onTrackImageItemSelected(Serializable itemList, int itemPosition, String intentClass, String extra);
    }


    public static ToptracksActivityFragment newInstance(String artistId, String artistName, String artistLink, boolean initialLoad) {
        ToptracksActivityFragment f = new ToptracksActivityFragment();
        Bundle args = new Bundle();
        args.putString(ELEMENT_ID, artistId);
        args.putString(ELEMENT_NAME, artistName);
        args.putString(ELEMENT_LINK, artistLink);
        args.putBoolean(ELEMENT_EXTRA2, initialLoad); //Tell fragment to load data from storage
        f.setArguments(args);
        return f;
    }


    public ToptracksActivityFragment() {
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            artistId = arguments.getString(MyFragment.ELEMENT_ID);
            artistName = arguments.getString(MyFragment.ELEMENT_NAME);
            artistLink = arguments.getString(MyFragment.ELEMENT_LINK);
            initialLoad = arguments.getBoolean(MyFragment.ELEMENT_EXTRA2);
        }

        View vw = inflater.inflate(R.layout.fragment_toptracks, container, false);
        setupFragment(vw, R.id.topTracks);
        return vw;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();
        int messageId = 0;
        List<Track> tracks = null;
        boolean searching = false;
        currentItem = -1;
        if (artistId != null) {
          //Data taken from arguments
        } else if (savedInstanceState != null) {
            try {
                //Restore previous top tracks list
                Type trackCollectionType = new TypeToken<List<Track>>() {}.getType();
                tracks = new Gson().fromJson(savedInstanceState.getString(STATE_SEARCH_RESULT), trackCollectionType);
            } catch (Exception e) {}
            currentItem = savedInstanceState.getInt(STATE_SEARCH_RESULT_SELECTED);
            //Message id (error, etc)
            messageId = savedInstanceState.getInt(STATE_SEARCH_MESSAGE);
            searching = savedInstanceState.getBoolean(STATE_SEARCHING);
            if (searching) tracks = null;
            artistId = savedInstanceState.getString(STATE_ARTIST_ID);
            artistName = savedInstanceState.getString(STATE_ARTIST_NAME);
            artistLink = savedInstanceState.getString(STATE_ARTIST_LINK);
        } else if (activity.getIntent().hasExtra(ELEMENT_NAME)) {
            Intent intent = activity.getIntent();
            artistId = intent.getStringExtra(ELEMENT_ID);
            artistName = intent.getStringExtra(ELEMENT_NAME);
            artistLink = intent.getStringExtra(ELEMENT_LINK);
        } else {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            try {
                //Restore previous search result
                Type trackCollectionType = new TypeToken<List<Track>>() {}.getType();
                tracks = new Gson().fromJson(prefs.getString(PREF_TOPTRACK_RESULT, null), trackCollectionType);
            } catch(Exception e) {}
            currentItem = prefs.getInt(PREF_TOPTRACK_RESULT_SELECTED, 0);
            artistId = prefs.getString(PREF_TOPTRACK_ARTIST_ID, null);
            artistName = prefs.getString(PREF_TOPTRACK_ARTIST_NAME, null);
            artistLink = prefs.getString(PREF_TOPTRACK_ARTIST_LINK, null);
        }

        if (tracks == null) {
            tracks = new ArrayList<Track>();
            //Get intent data and search for tracks, from storage or intent
            if (artistId != null) {
                if (initialLoad) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    try {
                        //Restore previous search result
                        Type trackCollectionType = new TypeToken<List<Track>>() {}.getType();
                        tracks = new Gson().fromJson(prefs.getString(PREF_TOPTRACK_RESULT, null), trackCollectionType);
                        currentItem = prefs.getInt(PREF_TOPTRACK_RESULT_SELECTED, 0);
                    } catch(Exception e) {}
                }
                if ((tracks == null) || (tracks.size() < 1)) searcher = new SearchTrack();
            }
            //else messageId = R.string.message_error;
        }

        trackAdapter = new TrackAdapter(activity, R.layout.artist_top_track, tracks);
        resultList.setAdapter(trackAdapter);

        if (initialLoad) initialLoad = false;

        if (searcher != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            searcher.execute(artistId,
                    prefs.getString(SettingsActivity.PREF_GENERAL_COUNTRY, Locale.getDefault().getCountry()));
        }
        else if (messageId > 0) setMessage(messageId);

        //Restore selected item
        if ((currentItem < tracks.size()) && (currentItem >= 0))
            resultList.setItemChecked(currentItem,true);
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
        outState.putInt(STATE_SEARCH_RESULT_SELECTED, currentItem); //resultList.getSelectedItemPosition());
        outState.putString(STATE_ARTIST_ID, artistId);
        outState.putString(STATE_ARTIST_NAME, artistName);
        outState.putString(STATE_ARTIST_LINK, artistLink);
    }


    @Override
    public void onResume() {
        super.onResume();
        if (nowPlayingReceiver == null) nowPlayingReceiver = new NowPlayingReceiver();
        IntentFilter intentFilter = new IntentFilter(PlayerService.UPDATE_NOW_PLAYING);
        getActivity().registerReceiver(nowPlayingReceiver, intentFilter);
        bindService();
    }


    @Override
    public void onPause() {
        super.onPause();
        if (nowPlayingReceiver != null) getActivity().unregisterReceiver(nowPlayingReceiver);
        unbindService();
        //Save to permanent storeage
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_TOPTRACK_RESULT, new Gson().toJson(trackAdapter.getData()));
        editor.putInt(PREF_TOPTRACK_RESULT_SELECTED, currentItem); //resultList.getSelectedItemPosition());
        editor.putString(PREF_TOPTRACK_ARTIST_ID, artistId);
        editor.putString(PREF_TOPTRACK_ARTIST_NAME, artistName);
        editor.putString(PREF_TOPTRACK_ARTIST_LINK, artistLink);
        editor.commit();
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        forceHideKeyboard(view);
        currentItem = position;
        if (parent.getId() == R.id.topTracks) {
            Track track = trackAdapter.getItem(position);
            if (track != null) {
                ((Callback) getActivity()).onTrackItemSelected(
                        (Serializable) TrackData.buildFromTrackList(trackAdapter.getData()),
                        position, artistName
                );
            }
        }
    }


    protected class SearchTrack extends AsyncTask<String, Void, List<Track>> {

        SpotifyApi api = new SpotifyApi();
        int error = 0;


        @Override
        protected void onPreExecute() {
            setMessage(R.string.message_loading);
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
            forceHideKeyboard(v);
            ((Callback )getActivity()).onTrackImageItemSelected(
                    (Serializable) TrackData.buildFromTrackList(data),
                    Integer.parseInt((String) v.getTag()),
                    PlayerActivity.class.getCanonicalName(), artistName
            );
        }


    } //TrackAdapter


    @Override
    public void selectItem(int position) {
        if ((position > 0) && (resultList != null) && (trackAdapter != null) &&
                (position < trackAdapter.getCount())) {
            resultList.performItemClick(
                    resultList.getAdapter().getView(position, null, null), position, position);
            //resultList.setSelection(position);
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (artistLink != null) {
            if (mTwoPane) inflater.inflate(R.menu.menu_toptracks_share, menu);
            MenuItem menuItem = menu.findItem(R.id.action_share);
            if (menuItem != null) {
                mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
                if (mShareActionProvider != null)
                    mShareActionProvider.setShareIntent(createShareArtistIntent());
            }
        }
        nowPlayingMenuItem = menu.findItem(R.id.action_now_playing);
        updateNowPlayingMenu();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                break;
            case R.id.action_now_playing:
                switchToPlayer();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }


    protected Intent createShareArtistIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
//        try {
            shareIntent.putExtra(Intent.EXTRA_TEXT, artistLink); // + "#" + URLEncoder.encode(artistName, "utf-8"));
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
        return shareIntent;
    }


}
