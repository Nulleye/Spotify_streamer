package com.nulleye.udacity.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;
import retrofit.RetrofitError;


//TODO ImagePopupActivity in dialog mode (alternative layout with 2 text fields for artist and album+song

/**
 * A placeholder fragment containing a simple view.
 */
public class SearchActivityFragment extends MyFragment implements TextView.OnEditorActionListener {

    public static final String STATE_SEARCH_TEXT = "search_text";

    public static final String PREF_SEARCH_TEXT = "com.nulleye.udacity.spotifystreamer.PREF_SEARCH_TEXT";
    public static final String PREF_SEARCH_MESSAGE = "com.nulleye.udacity.spotifystreamer.PREF_SEARCH_MESSAGE";
    public static final String PREF_SEARCH_RESULT = "com.nulleye.udacity.spotifystreamer.PREF_SEARCH_RESULT";
    public static final String PREF_SEARCH_RESULT_SELECTED = "com.nulleye.udacity.spotifystreamer.PREF_SEARCH_RESULT_SELECTED";
    public static final String PREF_SEARCH_SEARCHING = "com.nulleye.udacity.spotifystreamer.PREF_SEARCH_SEARCHING";

    boolean mTwoPane = false;

    SearchArtist searcher = null;
    ArtistAdapter artistAdapter;

    EditText searchArtist;

    boolean initialLoad = false;


    public void setInitialLoad(boolean initialLoad) {
        this.initialLoad = initialLoad;
    }


    public interface Callback {
        public void onItemSelected(String artistId, String artistName, String artistLink, boolean initialLoad);
        public void onImageItemSelected(Serializable itemList, int itemPosition, String intentClass);
    }


    public SearchActivityFragment() {
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
       View vw = inflater.inflate(R.layout.fragment_search, container, false);
        searchArtist = (EditText) vw.findViewById(R.id.searchArtist);
        searchArtist.setOnEditorActionListener(this);
        setupFragment(vw, R.id.searchResult);
        return vw;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        List<Artist> artists = null;
        String text = null;
        boolean searching = false;
        int messageId = 0;
        currentItem = -1;
        if (savedInstanceState != null) {
            try {
                //Restore previous search result
                Type artistCollectionType = new TypeToken<List<Artist>>() {}.getType();
                artists = new Gson().fromJson(savedInstanceState.getString(STATE_SEARCH_RESULT), artistCollectionType);
            } catch(Exception e) {}
            currentItem = savedInstanceState.getInt(STATE_SEARCH_RESULT_SELECTED);
            text = savedInstanceState.getString(STATE_SEARCH_TEXT);
            searching = savedInstanceState.getBoolean(STATE_SEARCHING);
            messageId = savedInstanceState.getInt(STATE_SEARCH_MESSAGE);
        } else {
            //Load data from permanent storage, if available
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            text = prefs.getString(PREF_SEARCH_TEXT, null);
            try {
                //Restore previous search result
                Type artistCollectionType = new TypeToken<List<Artist>>() {}.getType();
                artists = new Gson().fromJson(prefs.getString(PREF_SEARCH_RESULT, null), artistCollectionType);
                initialLoad = ((artists != null) && (artists.size() > 0) && mTwoPane);
            } catch(Exception e) {}
            currentItem = prefs.getInt(PREF_SEARCH_RESULT_SELECTED, -1);
            searching = prefs.getBoolean(PREF_SEARCH_SEARCHING, false);
            messageId = prefs.getInt(PREF_SEARCH_MESSAGE, 0);
        }

        if (text != null) {
            searchArtist.setText(text);
            searchArtist.setSelection(searchArtist.getText().length());
        }

        if (artists == null) artists = new ArrayList<Artist>();
        artistAdapter = new ArtistAdapter(getActivity(), R.layout.artist_search_result, artists);
        resultList.setAdapter(artistAdapter);

        if (messageId > 0) setMessage(messageId);
        //Search was interrupted (possible orientation change) restart seach
        if (searching) searchArtist(text);

        //Restore selected item
        if ((currentItem < artists.size()) && (currentItem >= 0)) {
            if (initialLoad) selectItem(currentItem);
            else resultList.setItemChecked(currentItem,true);
        }
    }


    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putString(STATE_SEARCH_TEXT, searchArtist.getText().toString());
        if (message.getVisibility() == View.VISIBLE)
            outState.putInt(STATE_SEARCH_MESSAGE, currentMessageId);
        try {
            if ((searcher != null) && (searcher.getStatus() != AsyncTask.Status.FINISHED))
                outState.putBoolean(STATE_SEARCHING, true);
            else outState.putBoolean(STATE_SEARCHING, false);
        } catch(Exception e){}
        try {
            outState.putString(STATE_SEARCH_RESULT, new Gson().toJson(artistAdapter.getData()));
        } catch(Exception e){}
        outState.putInt(STATE_SEARCH_RESULT_SELECTED, currentItem); //resultList.getSelectedItemPosition());
    }


    @Override
    public void onResume() {
        super.onResume();
        if (!mTwoPane) {
            if (nowPlayingReceiver == null) nowPlayingReceiver = new NowPlayingReceiver();
            IntentFilter intentFilter = new IntentFilter(PlayerService.UPDATE_NOW_PLAYING);
            getActivity().registerReceiver(nowPlayingReceiver, intentFilter);
            bindService();
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        if (!mTwoPane) {
            if (nowPlayingReceiver != null) getActivity().unregisterReceiver(nowPlayingReceiver);
            unbindService();
        }
        //Save to permanent storeage
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_SEARCH_TEXT, searchArtist.getText().toString());
        if (message.getVisibility() == View.VISIBLE)
            editor.putInt(PREF_SEARCH_MESSAGE, currentMessageId);
        else editor.putInt(PREF_SEARCH_MESSAGE, 0);
        try {
            if ((searcher != null) && (searcher.getStatus() != AsyncTask.Status.FINISHED))
                editor.putBoolean(PREF_SEARCH_SEARCHING, true);
            else editor.putBoolean(PREF_SEARCH_SEARCHING, false);
        } catch(Exception e){}
        editor.putString(PREF_SEARCH_RESULT, new Gson().toJson(artistAdapter.getData()));
        editor.putInt(PREF_SEARCH_RESULT_SELECTED, currentItem);    //resultList.getSelectedItemPosition());
        editor.commit();
    }


    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if ((v.getId() == R.id.searchArtist) && (actionId == EditorInfo.IME_ACTION_SEARCH)) {
            forceHideKeyboard(v);
            searchArtist(((EditText) v).getText().toString());
            return true;
        }
        return false;
    }


    private void searchArtist(String artist) {
        if ((artist != null) && !artist.isEmpty()) {
            //Cancel active search (if any)
            if ((searcher != null) && (searcher.getStatus() != AsyncTask.Status.FINISHED)) searcher.cancel(true);
            searcher = new SearchArtist();
            searcher.execute(artist);
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        forceHideKeyboard(view);
        currentItem = position;
        if (parent.getId() == R.id.searchResult) {
            Artist artist = artistAdapter.getItem(position);
            if (artist != null) ((Callback) getActivity()).onItemSelected(artist.id, artist.name,
                    ItemData.externalUrl(artist.external_urls), initialLoad);
        }
        if (initialLoad) initialLoad = false;
    }


    public void setTwoPaneMode(boolean twoPaneMode) {
        mTwoPane = twoPaneMode;
    }


    public class SearchArtist extends AsyncTask<String, Void, List<Artist>> {

        SpotifyApi api = new SpotifyApi();
        int error = 0;


        @Override
        protected void onPreExecute() {
            setMessage(R.string.message_searching);
        }


        @Override
        protected List<Artist> doInBackground(String... params) {
            try {
                SpotifyService spotify = api.getService();
                ArtistsPager pager = spotify.searchArtists(params[0]);
                if ((pager != null) && (pager.artists != null)) return pager.artists.items;
            } catch(retrofit.RetrofitError re) {
                if (re.getKind() == RetrofitError.Kind.NETWORK) error = R.string.message_no_internet;
                else error = R.string.message_error;
            } catch(Exception e) {
                error = R.string.message_error;
            }
            return null;
        }


        @Override
        protected void onPostExecute(List<Artist> artists) {
            try {
                if (!isCancelled()) {
                    if ((artists != null) && !artists.isEmpty()) {
                        currentItem = -1;
                        artistAdapter.clear();
                        //Sort by popularity and update adapter
                        Collections.sort(artists, new ArtistComparator(false));
                        artistAdapter.addAll(artists);
                        showMessage(false);
                    } else {
                        //Dont clear adapter to maintain previous good search result
                        if (error == 0) error = R.string.message_artist_not_found;
                        setMessage(error);
                    }
                }
            } catch(Exception e) {
                //Eat any exception here
            }
        }


        @Override
        protected void onCancelled() {
            showMessage(false);
        }

    } //SearchArtist


    /**
     * Comparator to sort artist list by popularity
     */
    public class ArtistComparator implements Comparator<Artist> {

        boolean asc;


        public ArtistComparator(boolean asc) {
            this.asc = asc;
        }


        @Override
        public int compare(Artist first, Artist second) {
            if (first.popularity == second.popularity) return 0;
            else if (first.popularity > second.popularity) return (asc)? 1 : -1;
            else return (asc)? -1 : 1;
        }

    } //ArtistComparator


    public class ArtistAdapter extends ArrayAdapter<Artist> implements View.OnClickListener {

        private int resource;          //hold reference to layout
        private List<Artist> data;     //hold reference to data to store on onSaveInstanceData


        public ArtistAdapter(Context context, int resource, List<Artist> objects) {
            super(context, resource, objects);
            this.resource = resource;
            this.data = objects;
        }


        public List<Artist> getData() {
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
            final Artist artist = getItem(position);
            if ((artist != null) && (result != null)) {
                ImageView iv = (ImageView) result.findViewById(R.id.thumbnail);
                iv.setTag(Integer.toString(position));
                iv.setOnClickListener(this);
                //Fetch artist image using picasso (if available)
                iv.setImageResource(R.drawable.noimage);
                Image image = getBestFitImage(artist.images, bestFitImagePixels);
                if (image != null) Picasso.with(getContext()).load(image.url).into(iv);
                //Set artist name
                TextView tv = (TextView) result.findViewById(R.id.artistName);
                tv.setText(artist.name);
            }
            return result;
        }


        @Override
        public void onClick(View v) {
//            Intent intent = new Intent(getContext(), ImagePopupActivity.class);
//            intent.putExtra(ELEMENT_LIST, (Serializable) ItemData.buildFromArtistList(data));
//            intent.putExtra(ELEMENT_POSITION, Integer.parseInt((String) v.getTag()));
//            intent.putExtra(ELEMENT_ACTION_INTENT_CLASS, ToptracksActivity.class.getCanonicalName());
//            getContext().startActivity(intent);
            forceHideKeyboard(v);
            ((SearchActivityFragment.Callback) getActivity()).onImageItemSelected(
                    (Serializable) ItemData.buildFromArtistList(data),
                    Integer.parseInt((String) v.getTag()), ToptracksActivity.class.getCanonicalName()
            );
        }


    } //ArtistAdapter


    @Override
    public void selectItem(int position) {
        if ((position > 0) && (resultList != null) && (artistAdapter != null) &&
                (position < artistAdapter.getCount())) {
            resultList.performItemClick(
                    resultList.getAdapter().getView(position, null, null), position, position);
            //resultList.setSelection(position);
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        nowPlayingMenuItem = menu.findItem(R.id.action_now_playing);
        if (!mTwoPane) updateNowPlayingMenu();
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


}
