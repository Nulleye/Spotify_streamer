package com.nulleye.udacity.spotifystreamer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.io.Serializable;


public class ToptracksActivity extends ActionBarActivity implements
        ToptracksActivityFragment.Callback {

    public static final String STATE_ARTIST_NAME = "artist_name";

    String artist = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toptracks);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState != null) artist = savedInstanceState.getString(STATE_ARTIST_NAME);
        else artist = getIntent().getStringExtra(MyFragment.ELEMENT_NAME);
        if (artist == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            artist = prefs.getString(ToptracksActivityFragment.PREF_TOPTRACK_ARTIST_NAME, null);
        }
        if (artist != null) getSupportActionBar().setSubtitle(artist);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_ARTIST_NAME, artist);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_toptracks, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
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
        startActivity(intent);
    }


}
