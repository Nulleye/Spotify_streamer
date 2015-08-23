package com.nulleye.udacity.spotifystreamer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.ortiz.touch.ExtendedViewPager;
import com.ortiz.touch.TouchImageView;
import com.squareup.picasso.Picasso;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class PlayerActivityFragment extends Fragment implements
        ViewPager.OnPageChangeListener, View.OnClickListener,
        SeekBar.OnSeekBarChangeListener {

    public static final String TAG = PlayerActivityFragment.class.getName();

    //This preference is not controlled on Settings page
    public static final String PREF_GENERAL_CONTINUOUS_PLAY = "com.nulleye.udacity.spotifystreamer.PREF_GENERAL_CONTINUOUS_PLAY";

    public static final String STATE_CURRENT_TRACK = "state_current_track";
    public static final String STATE_TRACKS = "state_tracks";
    public static final String STATE_ARTIST = "state_artist";

    SharedPreferences sharedPreferences;

    boolean mShowShareButton;   //For tablet mode (no action bar)

    ShareActionProvider mShareActionProvider = null;

    List<TrackData> data = null;
    int currentTrack;
    String artist = null;
    boolean continuousPlay;
    boolean ignoreNextItemChange = false;
    boolean playOnStart;

    ExtendedViewPager viewPager;
    TouchImageAdapter imageAdapter;

    SeekBar seekBar;
    Button previousButton;
    Button playButton;
    Button nextButton;
    Button shareButton = null;

    TextView artistName;
    TextView albumName;
    TextView songName;

    TextView currentTime;
    TextView previewTime;
    TextView totalTime;

    TextView bufferingText;
    ToggleButton continuousButton;


    // Connection with PlayerService ---------------------------------------------------------------

    PlayerUpdateReceiver playerReceiver;

    boolean mServiceBound = false;
    PlayerService mBoundService = null;
    Intent initIntent = null;

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
            mBoundService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected(" + playOnStart + ")");
            PlayerService.PlayerServiceBinder binder = (PlayerService.PlayerServiceBinder) service;
            mBoundService = binder.getService();
            mServiceBound = true;

            //Initialize
            artist = mBoundService.getArtistName();
            artistName.setText(artist);
            data = mBoundService.getTracks();
            if (data == null) return;
            currentTrack = mBoundService.getCurrentTrackNumber();
            imageAdapter = new TouchImageAdapter(data);

            if (!playOnStart || isPlaying()) ignoreNextItemChange = true;
           // playOnStart = false;
//            if (playOnStart)
//                currentTrack = initIntent.getIntExtra(PlayerService.ACTION_INIT_DATA_POSITION, currentTrack);

            viewPager.setAdapter(imageAdapter);
            if (currentTrack != 0) viewPager.setCurrentItem(currentTrack);
            else onPageSelected(0);    //Force onPageSelected if 0
        }
    };


    public PlayerActivityFragment() {
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");

        mShowShareButton = (((ActionBarActivity)getActivity()).getSupportActionBar() == null);

        View vw = inflater.inflate(R.layout.fragment_player, container, false);

        sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getActivity().getApplicationContext());
        continuousPlay = sharedPreferences.getBoolean(PREF_GENERAL_CONTINUOUS_PLAY, true);

        Intent intent = getActivity().getIntent();
        if ((savedInstanceState == null) && (intent != null) && (intent.hasExtra(MyFragment.ELEMENT_LIST))) {
            playOnStart = true;
            initIntent = new Intent(getActivity(), PlayerService.class);
            initIntent.setAction(PlayerService.ACTION_INIT);
            initIntent.putExtra(PlayerService.ACTION_INIT_DATA,
                    intent.getSerializableExtra(MyFragment.ELEMENT_LIST));
            initIntent.putExtra(PlayerService.ACTION_INIT_DATA_POSITION,
                    intent.getIntExtra(MyFragment.ELEMENT_POSITION, 0));
            initIntent.putExtra(PlayerService.ACTION_INIT_DATA_PLAY, playOnStart);
            initIntent.putExtra(PlayerService.ACTION_EXTRA_DATA,
                    intent.getStringExtra(MyFragment.ELEMENT_EXTRA));
            //playOnStart = continuousPlay;
            //Log.d(TAG,"from intent");
            getActivity().startService(initIntent);
        } else if (savedInstanceState != null)  {
            playOnStart = false;
            //Starting from savedInstance
            initIntent = new Intent(getActivity(), PlayerService.class);
            initIntent.setAction(PlayerService.ACTION_INIT);
            initIntent.putExtra(PlayerService.ACTION_INIT_DATA,
                    savedInstanceState.getSerializable(STATE_TRACKS));
            initIntent.putExtra(PlayerService.ACTION_INIT_DATA_POSITION,
                    savedInstanceState.getInt(STATE_CURRENT_TRACK, 0));
            initIntent.putExtra(PlayerService.ACTION_EXTRA_DATA,
                    savedInstanceState.getString(STATE_ARTIST));
            //Log.d(TAG,"from saved");
            getActivity().startService(initIntent);
        } else {
            initIntent = new Intent(getActivity(), PlayerService.class);
            initIntent.setAction(PlayerService.ACTION_DUMMY);
        }

        artistName = (TextView) vw.findViewById(R.id.artistName);
        albumName = (TextView) vw.findViewById(R.id.albumName);
        songName = (TextView) vw.findViewById(R.id.songName);

        viewPager = (ExtendedViewPager) vw.findViewById(R.id.imageview_pager);
        viewPager.addOnPageChangeListener(this);

        seekBar = (SeekBar) vw.findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);
        currentTime = (TextView) vw.findViewById(R.id.currentTime);
        previewTime = (TextView) vw.findViewById(R.id.previewTime);
        totalTime = (TextView) vw.findViewById(R.id.totalTime);
        bufferingText = (TextView) vw.findViewById(R.id.bufferingText);

        Typeface typeFace = Typeface.createFromAsset(getActivity().getAssets(),
                "fonts/MaterialIcons-Regular.ttf");
        previousButton = (Button) vw.findViewById(R.id.previousButton);
        previousButton.setTypeface(typeFace);
        previousButton.setOnClickListener(this);
        playButton = (Button) vw.findViewById(R.id.playButton);
        playButton.setTypeface(typeFace);
        playButton.setOnClickListener(this);
        nextButton = (Button) vw.findViewById(R.id.nextButton);
        nextButton.setTypeface(typeFace);
        nextButton.setOnClickListener(this);
        continuousButton = (ToggleButton) vw.findViewById(R.id.continuousButton);
        continuousButton.setTypeface(typeFace);
        continuousButton.setOnClickListener(this);
        continuousButton.setChecked(continuousPlay);

        if (mShowShareButton) {
            shareButton = (Button) vw.findViewById(R.id.shareButton);
            shareButton.setTypeface(typeFace);
            shareButton.setOnClickListener(this);
            shareButton.setVisibility(View.VISIBLE);
        }

        return vw;
    }


    @Override
    public void onSaveInstanceState(final Bundle outState) {
        Log.d(TAG, "onSaveInstanceState()");
        //super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_TRACKS, initIntent.getSerializableExtra(PlayerService.ACTION_INIT_DATA));
        outState.putInt(STATE_CURRENT_TRACK, initIntent.getIntExtra(PlayerService.ACTION_INIT_DATA_POSITION, 0));
        outState.putString(STATE_ARTIST, initIntent.getStringExtra(PlayerService.ACTION_EXTRA_DATA));
    }


    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        if (playerReceiver == null) playerReceiver = new PlayerUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter(PlayerService.UPDATE_PLAYER);
        getActivity().registerReceiver(playerReceiver, intentFilter);
        //Intent intent = new Intent(activity, PlayerService.class);
        //activity.startService(initIntent);
        //ATT: why this initIntent is not always passed to service.onbind or service.onrebind???
        getActivity().bindService(initIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }


    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        if (playerReceiver != null) getActivity().unregisterReceiver(playerReceiver);
        if (mServiceBound) {
            //Store current service data on intent to store it on onSaveInstanceState
            initIntent = new Intent(getActivity(), PlayerService.class);
            initIntent.setAction(PlayerService.ACTION_INIT);
            initIntent.putExtra(PlayerService.ACTION_INIT_DATA,
                    (Serializable) mBoundService.getTracks());
            initIntent.putExtra(PlayerService.ACTION_INIT_DATA_POSITION,
                    mBoundService.getCurrentTrackNumber());
            initIntent.putExtra(PlayerService.ACTION_EXTRA_DATA,
                    mBoundService.getArtistName());
            getActivity().unbindService(mServiceConnection);
            mServiceBound = false;
        }
    }



    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem menuItem = menu.findItem(R.id.action_share);
        if (menuItem != null) mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
    }


    protected Intent createShareTrackIntent(TrackData track) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, track.externalUrl);
        return shareIntent;
    }


    class TouchImageAdapter extends PagerAdapter {

        List<TrackData> data;
        List<TouchImageView> images;


        public TouchImageAdapter(List<TrackData> data) {
            this.data = data;
            refresh();
        }


        public void refresh(){
            images = new ArrayList<TouchImageView>(data.size());
        }


        @Override
        public int getCount() {
            return data.size();
        }


        @Override
        public View instantiateItem(ViewGroup container, int position) {
            if (images.size() <= position) for(int i=images.size();i<=position;i++) images.add(null);
            TouchImageView img = images.get(position);
            if (img == null) {
                img = new TouchImageView(container.getContext());
                final ItemData imageData = data.get(position);
                img.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PlayerActivityFragment.this.onClick(playButton);
                    }
                });
                images.set(position, img);
                img.setImageResource(R.drawable.noimage_big);
                if (imageData.imageUrl != null) Picasso.with(container.getContext()).load(imageData.imageUrl).into(img);
                container.addView(img, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            }
            return img;
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
//            container.removeView((View) object);
        }


        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

    } //TouchImageAdapter


    protected boolean isPlaying() {
        return (mBoundService != null) && mBoundService.isPlaying();
    }


    /**
     * Action buttons
     * @param v
     */
    @Override
    public void onClick(View v) {
        int position = viewPager.getCurrentItem();
        switch(v.getId()) {
            case R.id.previousButton:
                position--;
                if (position >= 0) viewPager.setCurrentItem(position);
                break;
            case R.id.nextButton:
                position++;
                if (position <= (data.size() - 1)) viewPager.setCurrentItem(position);
                break;
            case R.id.playButton:
                if (playButton.isEnabled()) {
                    if (isPlaying()) stopSong();
                    else playSong(position);
                }
                break;
            case R.id.continuousButton:
                continuousPlay = continuousButton.isChecked();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(PREF_GENERAL_CONTINUOUS_PLAY, continuousPlay);
                editor.commit();
                Intent intent = new Intent(getActivity(), PlayerService.class);
                intent.setAction(PlayerService.ACTION_CONTINUOUS_PLAY);
                intent.putExtra(PlayerService.ACTION_EXTRA_DATA, continuousPlay);
                getActivity().startService(intent);
                break;
            case R.id.shareButton:
                Intent shareIntent = createShareTrackIntent(data.get(position));
                startActivity(Intent.createChooser(shareIntent, getString(R.string.action_share_song)));
                break;
            default:
        }
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            int position = progress * 1000;
            if (isPlaying()) {
                Intent intent = new Intent(getActivity(), PlayerService.class);
                intent.setAction(PlayerService.ACTION_SEEK);
                intent.putExtra(PlayerService.ACTION_EXTRA_DATA, position);
                getActivity().startService(intent);
            } else {
                data.get(viewPager.getCurrentItem()).lastIndex = position;
                if (mServiceBound) mBoundService.setTrackProgress(viewPager.getCurrentItem(), position);
            }
            currentTime.setText(getTimeText(position));
        }
    }


    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        //Do nothing
    }


    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //Do nothing
    }


    //PAGER EVENTS ---------------------------------------------------------------------------------


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }


    @Override
    public void onPageSelected(int position) {
        Log.d(TAG, "onPageSelected(" + position + ")");
        if (!ignoreNextItemChange &&
                isPlaying() && (mBoundService.getCurrentTrackNumber() != position)) {
            stopSong();
            playOnStart = true;
        }

        TrackData track = data.get(position);
        updateSongInfo(track, position);

        if (!ignoreNextItemChange && playOnStart &&
                (track.previewUrl != null)) playSong(position);
        else {
            if (mBoundService.getCurrentTrackNumber() != position) mBoundService.setCurrentTrackNumber(position);
            updatePlayButtonState(!isPlaying());
        }

        ignoreNextItemChange = false;
        playOnStart = false;

        if (mShareActionProvider != null) mShareActionProvider.setShareIntent(createShareTrackIntent(track));
    }


    @Override
    public void onPageScrollStateChanged(int state) {
    }


    // MEDIA PLAYER EVENTS -------------------------------------------------------------------------


    private class PlayerUpdateReceiver extends BroadcastReceiver {

        public PlayerUpdateReceiver() {
            super();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "onReceive(" + PlayerService.getUpdateType(intent) + ")");
            if (PlayerService.UPDATE_PLAYER.equals(intent.getAction())) {
                PlayerService.UpdatePlayerType updateType = PlayerService.getUpdateType(intent);
                int trackNumber = PlayerService.getUpdateTrack(intent);
                if (trackNumber < 0) return;        //No track number??
                TrackData trackData;
                boolean isPlaying = false;
                if (updateType != PlayerService.UpdatePlayerType.SHUTDOWN) {
                    if (mBoundService == null) return;  //No service??
                    int track = mBoundService.getCurrentTrackNumber();
                    if (trackNumber != track) return;   //Ignore this event seems to be old
                    if (viewPager.getCurrentItem() != trackNumber) {
                        ignoreNextItemChange = true;
                        viewPager.setCurrentItem(track);
                    }
                    trackData = mBoundService.getTrack(trackNumber);
                    isPlaying = isPlaying();
                } else trackData = data.get(trackNumber);
                switch (updateType) {
                    case SHUTDOWN:
                    case PROGRESS:
                        updateProgressInfo(trackData);
                        updatePlayButtonState(!isPlaying);
                        break;
                    case BUFFERING:
                        int percent = intent.getIntExtra(PlayerService.UPDATE_PLAYER_EXTRA_DATA, -1);
                        if (percent > 0) updateBufferingInfo(percent);
                        break;
                    case ERROR:
                        int error = intent.getIntExtra(PlayerService.UPDATE_PLAYER_EXTRA_DATA, -1);
                        if (error > 0) updateErrorInfo(error);
                        break;
                    case COMPLETE:
                        //updateComplete(mBoundService.getTrack(trackNumber));
                        break;
                    default:
                }
            }
        }


        @Override
        public IBinder peekService(Context myContext, Intent service) {
            return super.peekService(myContext, service);
        }

    }


    protected void playSong(int position) {
        Intent intent = new Intent(getActivity(), PlayerService.class);
        intent.setAction(PlayerService.ACTION_PLAY);
        intent.putExtra(PlayerService.ACTION_EXTRA_DATA, position);
        getActivity().startService(intent);
        updatePlayButtonState(false);
    }


    protected void stopSong() {
        Intent intent = new Intent(getActivity(), PlayerService.class);
        intent.setAction(PlayerService.ACTION_STOP);
        getActivity().startService(intent);
        updatePlayButtonState(true);
    }


    protected void updateSongInfo(TrackData track, int position) {
        if (position <= 0) previousButton.setEnabled(false);
        else if (!previousButton.isEnabled()) previousButton.setEnabled(true);
        if (position >= (data.size() - 1)) nextButton.setEnabled(false);
        else if (!nextButton.isEnabled()) nextButton.setEnabled(true);
        albumName.setText(track.subtitle);
        songName.setText(track.title);
        if (track.previewUrl != null) {
            if (!playButton.isEnabled()) playButton.setEnabled(true);
            if (!seekBar.isEnabled()) seekBar.setEnabled(true);
            totalTime.setText(getTimeText(track.duration));
            //if ((track.previewDuration > -1) && (track.lastIndex == track.previewDuration))
            //    track.lastIndex = -1;
            updateProgressInfo(track);
        } else {
            if (playButton.isEnabled()) playButton.setEnabled(false);
            updatePlayButtonState(true);
            if (seekBar.isEnabled()) seekBar.setEnabled(false);
            if (seekBar.getProgress() != 0) seekBar.setProgress(0);
            currentTime.setText(getTimeText(0));
            totalTime.setText(getTimeText(track.duration));
            if (bufferingText.getVisibility() == View.VISIBLE) bufferingText.setVisibility(View.INVISIBLE);
            if (previewTime.getVisibility() == View.VISIBLE) previewTime.setVisibility(View.INVISIBLE);
        }
    }


    protected void updatePlayButtonState(boolean play) {
        if (play) {
            if (!playButton.getText().equals(getResources().getString(R.string.img_play))) {
                playButton.setText(R.string.img_play);
                playButton.setContentDescription(getResources().getString(R.string.play_song));
            }
        } else {
            if (playButton.getText().equals(getResources().getString(R.string.img_play))) {
                playButton.setText(R.string.img_pause);
                playButton.setContentDescription(getResources().getString(R.string.pause_song));
            }
            if (bufferingText.getVisibility() == View.VISIBLE) bufferingText.setVisibility(View.INVISIBLE);
        }
    }


    protected void updateProgressInfo(TrackData data) {
        long duration = (data.previewDuration > -1)? data.previewDuration : data.duration;
        long durationSeconds = duration / 1000;
        if (seekBar.getMax() != durationSeconds) seekBar.setMax((int) durationSeconds);
        long progress = (data.lastIndex > -1)? data.lastIndex : 0;
        currentTime.setText(getTimeText(progress));
        long progressSeconds = progress / 1000;
        seekBar.setProgress((int) progressSeconds);
        if (data.previewDuration > -1) {
            if (previewTime.getVisibility() != View.VISIBLE) previewTime.setVisibility(View.VISIBLE);
            previewTime.setText(String.format(
                    getResources().getString(R.string.play_preview_text), getTimeText(duration)));
            previewTime.setContentDescription(previewTime.getText());
        } else if (previewTime.getVisibility() == View.VISIBLE) previewTime.setVisibility(View.INVISIBLE);
        //if (bufferingText.getVisibility() == View.VISIBLE) bufferingText.setVisibility(View.INVISIBLE);
        //updatePlayButtonState(!isPlaying());
    }


    protected static String getTimeText(long msTime) {
        if (msTime < 0) return "??:??";
        long hours = (msTime / (1000*60*60));
        long minutes = ((msTime / (1000*60)) % 60);
        long seconds = ((msTime / (1000)) % 60);
        if (hours > 0) return String.format("%d:%02d:%02d", hours, minutes, seconds);
        else return String.format("%02d:%02d", minutes, seconds);
    }


    protected void updateBufferingInfo(int percent) {
        boolean visibility;
        if (percent == 100) visibility = false;
        else {
            bufferingText.setText(percent + "%");
            bufferingText.setContentDescription(String.format(
                    getResources().getString(R.string.play_buffering_text), percent));
            visibility = true;
        }
        if (visibility && (bufferingText.getVisibility() != View.VISIBLE))
            bufferingText.setVisibility(View.VISIBLE);
        else if (!visibility && (bufferingText.getVisibility() == View.VISIBLE))
            bufferingText.setVisibility(View.GONE);
        updatePlayButtonState(!isPlaying());
    }


    protected void updateErrorInfo(int error) {
        bufferingText.setText(R.string.play_error_text);
        if (bufferingText.getVisibility() != View.VISIBLE)
            bufferingText.setVisibility(View.VISIBLE);
        updatePlayButtonState(!isPlaying());
    }


//    protected void updateComplete(TrackData track) {
//        track.lastIndex = track.previewDuration;
//        if (!continuousPlay) {
//            updateProgressInfo(track);
//            updatePlayButtonState(true);
//        } else {
//            int currentItem = mBoundService.getCurrentTrackNumber();
//            int item = currentItem;
//            if (item < (mBoundService.getTracks().size() - 1)) item++;
//            else item = 0;
//            playOnStart = true;
//            if (item != currentItem) viewPager.setCurrentItem(item);
//            else onPageSelected(item);
//        }
//    }


}
