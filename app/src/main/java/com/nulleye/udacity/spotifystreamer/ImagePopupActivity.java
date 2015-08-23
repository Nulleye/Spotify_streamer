package com.nulleye.udacity.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;


public class ImagePopupActivity extends ActionBarActivity {

//    ImagePopupActivityFragment imagepopup_fragment = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_popup);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

//        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

//        if (savedInstanceState == null)
//            imagepopup_fragment = (ImagePopupActivityFragment) getSupportFragmentManager().findFragmentById(R.id.imagepopup_fragment);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_image_popup, menu);
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


//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        //Redirect touch event to fragment
//        if (imagepopup_fragment != null) return imagepopup_fragment.onTouchEvent(event);
//        return false;
//    }

}
