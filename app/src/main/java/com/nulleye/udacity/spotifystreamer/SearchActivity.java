package com.nulleye.udacity.spotifystreamer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.io.Serializable;


public class SearchActivity extends ActionBarActivity implements
        SearchActivityFragment.Callback,
        ToptracksActivityFragment.Callback,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TOPTRACKS_FRAGMENT = "TOPTRACKS_FRAGMENT";

    SharedPreferences preferences;
    boolean notificationPlayer;

    boolean mTwoPane;

    public static int SELECT_ARTIST = 1000;
    public static int SELECT_TRACK = 1001;

    int resultCode = -1;
    Intent resultIntent = null;

    SearchActivityFragment searchFragment = null;
    ToptracksActivityFragment toptracksFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        if ((mTwoPane = (findViewById(R.id.activity_top_tracks_fragment) != null)) && (savedInstanceState == null)) {
            toptracksFragment = new ToptracksActivityFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.activity_top_tracks_fragment, toptracksFragment, TOPTRACKS_FRAGMENT)
                    .commit();
        }

        searchFragment =  ((SearchActivityFragment)getSupportFragmentManager()
                .findFragmentById(R.id.activity_search_fragment));
        searchFragment.setTwoPaneMode(mTwoPane);

        preferences = PreferenceManager
                .getDefaultSharedPreferences(this.getApplicationContext());
        preferences.registerOnSharedPreferenceChangeListener(this);
        updateNotificationPlayerValue(preferences);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!notificationPlayer) {
            Intent intent = new Intent(this, PlayerService.class);
            intent.setAction(PlayerService.ACTION_SHUTDOWN);
            stopService(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }


    @Override
    public void onItemSelected(String artistId, String artistName, String artistLink, boolean initialLoad) {
        if (mTwoPane) {
            toptracksFragment = ToptracksActivityFragment.newInstance(artistId, artistName, artistLink, initialLoad);
            toptracksFragment.setTwoPane(true);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.activity_top_tracks_fragment, toptracksFragment, TOPTRACKS_FRAGMENT)
                    .commit();
        } else {
            Intent intent = new Intent(this, ToptracksActivity.class);
            intent.putExtra(MyFragment.ELEMENT_ID, artistId);
            intent.putExtra(MyFragment.ELEMENT_NAME, artistName);
            intent.putExtra(MyFragment.ELEMENT_LINK, artistLink);
            startActivity(intent);
        }
    }


    @Override
    public void onImageItemSelected(Serializable itemList, int itemPosition, String intentClass) {
        Intent intent = new Intent(this, ImagePopupActivity.class);
        intent.putExtra(MyFragment.ELEMENT_LIST, itemList);
        intent.putExtra(MyFragment.ELEMENT_POSITION, itemPosition);
        intent.putExtra(MyFragment.ELEMENT_ACTION_INTENT_CLASS, intentClass);
        if (mTwoPane) {
            intent.putExtra(MyFragment.REQUEST_CODE, SELECT_ARTIST);
            startActivityForResult(intent, SELECT_ARTIST);
        }
//Initial idea to apply ImagePopup as a fragement on the right, but finally I dismiss it
//            Bundle args = new Bundle();
//            args.putSerializable(MyFragment.ELEMENT_LIST, itemList);
//            args.putInt(MyFragment.ELEMENT_POSITION, itemPosition);
//            args.putString(MyFragment.ELEMENT_ACTION_INTENT_CLASS, intentClass);
//            ImagePopupActivityFragment fragment = new ImagePopupActivityFragment();
//            fragment.setArguments(args);
//            getSupportFragmentManager().beginTransaction()
//                    .replace(R.id.activity_top_tracks_fragment, fragment, TOPTRACKS_FRAGMENT)
//                    .commit();
        else startActivity(intent);
    }


    @Override
    public void onTrackItemSelected(Serializable itemList, int itemPosition, String extra) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(MyFragment.ELEMENT_LIST, itemList);
        intent.putExtra(MyFragment.ELEMENT_POSITION, itemPosition);
        intent.putExtra(MyFragment.ELEMENT_EXTRA, extra);
        startActivity(intent);
    }


    @Override
    public void onTrackImageItemSelected(Serializable itemList, int itemPosition, String intentClass, String extra) {
        Intent intent = new Intent(this, ImagePopupActivity.class);
        intent.putExtra(MyFragment.ELEMENT_LIST, itemList);
        intent.putExtra(MyFragment.ELEMENT_POSITION, itemPosition);
        intent.putExtra(MyFragment.ELEMENT_ACTION_INTENT_CLASS, intentClass);
        intent.putExtra(MyFragment.ELEMENT_EXTRA, extra);
        if (mTwoPane) {
            intent.putExtra(MyFragment.REQUEST_CODE, SELECT_TRACK);
            startActivityForResult(intent, SELECT_TRACK);
        }
        else startActivity(intent);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == SELECT_ARTIST) || (requestCode == SELECT_TRACK)) {

            //Keep result and wait for onPostResume
            //Prevent Activity state loss: java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
            this.resultCode = requestCode;
            resultIntent = data;

        } else super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    protected void onPostResume() {
        super.onPostResume();
        if ((resultCode > -1) && (resultIntent != null)) {
            if (resultCode == SELECT_ARTIST) {
                if ((resultIntent != null) &&
                        (resultIntent.hasExtra(MyFragment.ELEMENT_POSITION)) &&
                        (resultIntent.hasExtra(MyFragment.ELEMENT_ID)) &&
                        (resultIntent.hasExtra(MyFragment.ELEMENT_NAME)) ) {
                    if (searchFragment != null) {
                        searchFragment.setInitialLoad(resultIntent.getBooleanExtra(MyFragment.ELEMENT_EXTRA2, false));
                        searchFragment.selectItem(resultIntent.getIntExtra(MyFragment.ELEMENT_POSITION, -1));
                    }
//                    onItemSelected(
//                            resultIntent.getStringExtra(MyFragment.ELEMENT_ID),
//                            resultIntent.getStringExtra(MyFragment.ELEMENT_NAME),
//                            resultIntent.getBooleanExtra(MyFragment.ELEMENT_EXTRA2, false)
//                    );
                }
            } else if (resultCode == SELECT_TRACK) {
                if ((resultIntent != null) &&
                        (resultIntent.hasExtra(MyFragment.ELEMENT_POSITION)) &&
                        (resultIntent.hasExtra(MyFragment.ELEMENT_LIST)) &&
                        (resultIntent.hasExtra(MyFragment.ELEMENT_EXTRA))) {
                    if (toptracksFragment != null)
                        toptracksFragment.selectItem(resultIntent.getIntExtra(MyFragment.ELEMENT_POSITION, -1));
//                    onTrackItemSelected(
//                            resultIntent.getSerializableExtra(MyFragment.ELEMENT_LIST),
//                            resultIntent.getIntExtra(MyFragment.ELEMENT_POSITION, -1),
//                            resultIntent.getStringExtra(MyFragment.ELEMENT_EXTRA)
//                    );
                }
            }
            resultCode = -1;
            resultIntent = null;
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (SettingsActivity.PREF_GENERAL_PLAYER_NOTIFICATION.equals(key))
            updateNotificationPlayerValue(sharedPreferences);
    }


    protected void updateNotificationPlayerValue(SharedPreferences sharedPreferences) {
        notificationPlayer = sharedPreferences.getBoolean(SettingsActivity.PREF_GENERAL_PLAYER_NOTIFICATION, true);
    }

}
