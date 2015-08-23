package com.nulleye.udacity.spotifystreamer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by cristian on 8/8/15.
 */
public class PlayerService extends Service implements
        MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnSeekCompleteListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = PlayerService.class.getName();

    public static final String WIFI_LOCK = PlayerService.class.getCanonicalName() + ".WIFI_LOCK";

    //Activity to Service actions
    public static final String ACTION_PLAY = TAG + ".ACTION_PLAY";
    public static final String ACTION_STOP = TAG + ".ACTION_STOP";
    public static final String ACTION_SEEK = TAG + ".ACTION_SEEK";
    public static final String ACTION_SHUTDOWN = TAG + ".ACTION_SHUTDOWN";
    public static final String ACTION_DUMMY = TAG + ".ACTION_DUMMY";
    public static final String ACTION_PREVIOUS = TAG + ".ACTION_PREVIOUS";
    public static final String ACTION_NEXT = TAG + ".ACTION_NEXT";
    public static final String ACTION_CONTINUOUS_PLAY = TAG + ".ACTION_CONTINUOUS_PLAY";
    public static final String ACTION_UPDATE_CLIENTS = TAG + ".ACTION_UPDATE_CLIENTS";
    public static final String ACTION_INIT = TAG + ".ACTION_INIT";
        public static final String ACTION_INIT_DATA = TAG + ".ACTION_INIT_DATA";
        public static final String ACTION_INIT_DATA_POSITION = TAG + ".ACTION_INIT_DATA_POSITION";
        public static final String ACTION_INIT_DATA_PLAY = TAG + ".ACTION_INIT_DATA_PLAY";

        public static final String ACTION_EXTRA_DATA = TAG + ".ACTION_EXTRA_DATA";

    public static final int PLAYER_INIT_ERROR = 10001;
    public static final int PLAYER_START_ERROR = 10002;
    public static final int PLAYER_SEEK_ERROR = 10003;

    //Service to Activity updates
    public static final String UPDATE_PLAYER = PlayerService.class.getCanonicalName() + ".UPDATE_PLAYER";
    public static final String UPDATE_PLAYER_TYPE = TAG + ".UPDATE_PLAYER_TYPE";
    public static final String UPDATE_PLAYER_TRACK = TAG + ".UPDATE_PLAYER_TRACK";
    public static final String UPDATE_PLAYER_EXTRA_DATA = TAG + ".UPDATE_PLAYER_EXTRA_DATA";

    public enum UpdatePlayerType { PROGRESS, BUFFERING, COMPLETE, ERROR, SHUTDOWN }


    public static final String UPDATE_NOW_PLAYING = TAG + ".UPDATE_NOW_PLAYING";
    public static final String UPDATE_NOW_PLAYING_TYPE = TAG + ".UPDATE_NOW_PLAYING_TYPE";

    public enum UpdateNowPlayingType { ON, OFF }


    //Internal enum for setupPlayer function
    protected enum PlayerSetupType { PLAY, STOP, RESET, DESTROY }

    public static final int PLAYER_SERVICE_NOTIFICATION = 10;

    public static final int PLAYER_REQUEST_PREVIOUS = 1001;
    public static final int PLAYER_REQUEST_PLAY = 1002;
    public static final int PLAYER_REQUEST_STOP = 1003;
    public static final int PLAYER_REQUEST_NEXT = 1004;
    public static final int PLAYER_REQUEST_SHUTDOWN = 1005;
    public static final int PLAYER_REQUEST_SWITCH = 1006;


    public static final String PREF_PLAYERSERVICE_ARTIST_NAME = "com.nulleye.udacity.spotifystreamer.PREF_PLAYERSERVICE_ARTIST_NAME";
    public static final String PREF_PLAYERSERVICE_TRACK_LIST = "com.nulleye.udacity.spotifystreamer.PREF_PLAYERSERVICE_TRACK_LIST";
    public static final String PREF_PLAYERSERVICE_CURRENT_TRACK = "com.nulleye.udacity.spotifystreamer.PREF_PLAYERSERVICE_CURRENT_TRACK";

    SharedPreferences preferences;
    boolean continuousPlay;
    boolean notificationPlayer;

    protected MediaPlayer mediaPlayer = null;
    boolean isWorking = false;
    protected WifiManager.WifiLock wifiLock = null;
    protected Timer positionTimer = null;

    protected String artistName = null;
    protected List<TrackData> tracks = null;
    protected int currentTrack = -1;

    protected String lastNotificationTrack = null;
    protected boolean lastNotificationPlayStatus = false;

    //BIND STUFF -----------------------------------------------------------------------------------

    protected boolean isSameData(Intent intent) {
        try {
            String artistNameN = intent.getStringExtra(ACTION_EXTRA_DATA);
            if ((artistName == null) || !artistName.equals(artistNameN)) return false;
            try {
                List<TrackData> tracksN = (List<TrackData>) intent.getSerializableExtra(ACTION_INIT_DATA);
                if (tracks.size() != tracksN.size()) return false;
                for(int i=0;i<tracksN.size();i++)
                    if (!tracksN.get(i).id.equals(tracks.get(i).id)) return false;
            } catch (Exception e) {
                return false;
            }
            if (intent.hasExtra(ACTION_INIT_DATA_PLAY))
                return (getCurrentTrackNumber() == intent.getIntExtra(ACTION_INIT_DATA_POSITION,-1));
            else return true;
            //int requestedTrack = intent.getIntExtra(ACTION_INIT_DATA_POSITION, 0);
            //if (currentTrack != requestedTrack) return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private IBinder mBinder = new PlayerServiceBinder();

    public class PlayerServiceBinder extends Binder {
        PlayerService getService() {
            return PlayerService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind(" + intent.toString() + ")");
//        if (!isSameData(intent)) onStartCommand(intent, 0, 0);
        if (tracks == null) onStartCommand(intent, 0, 0);
        return mBinder;
    }


    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind(" + intent.toString() + ")");
//        if (!isSameData(intent)) onStartCommand(intent, 0, 0);
        if (tracks == null) onStartCommand(intent, 0, 0);
        super.onRebind(intent);
    }


    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind(" + intent.toString() + ")");
        return true;
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (SettingsActivity.PREF_GENERAL_PLAYER_NOTIFICATION.equals(key)) {
            updateNotificationPlayerValue(sharedPreferences);
            if (!notificationPlayer) cancelNotification();
            else updateNotification();
        }
    }


    protected void updateNotificationPlayerValue(SharedPreferences sharedPreferences) {
        notificationPlayer = sharedPreferences.getBoolean(SettingsActivity.PREF_GENERAL_PLAYER_NOTIFICATION, true);
    }


    //BIND STUFF END -------------------------------------------------------------------------------


    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");

        mediaPlayer = new MediaPlayer();

        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);

        mediaPlayer.setLooping(false);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setOnErrorListener(this);
        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, WIFI_LOCK);

        preferences = PreferenceManager
                .getDefaultSharedPreferences(this.getApplicationContext());
        continuousPlay = preferences.getBoolean(PlayerActivityFragment.PREF_GENERAL_CONTINUOUS_PLAY, true);
        try {
            Type trackCollectionType = new TypeToken<List<TrackData>>() {}.getType();
            tracks = new Gson().fromJson(preferences.getString(PREF_PLAYERSERVICE_TRACK_LIST, null), trackCollectionType);
        } catch(Exception e) {}
        currentTrack = preferences.getInt(PREF_PLAYERSERVICE_CURRENT_TRACK, -1);
        artistName = preferences.getString(PREF_PLAYERSERVICE_ARTIST_NAME, null);

        preferences.registerOnSharedPreferenceChangeListener(this);
        updateNotificationPlayerValue(preferences);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = (intent != null)? intent.getAction() : null;
        Log.d(TAG, "onStartCommand(" + ((action != null) ? action : "null") + ")");

        if ((action == null) || ACTION_SHUTDOWN.equals(action)) {
            setupPlayer(PlayerSetupType.STOP);
            stopSelf();
            cancelNotification();
            updateProgress(UpdatePlayerType.SHUTDOWN);
            sendNowPlaying(false);
            return START_NOT_STICKY;
        }

        sendNowPlaying(true);

        if (ACTION_UPDATE_CLIENTS.equals(action)) {
            updateClients();
            return START_STICKY;
        } else if (action.startsWith(ACTION_PLAY)) {
            if (tracks != null) {
                try {
                    int requestedTrack = intent.getIntExtra(ACTION_EXTRA_DATA, -1);
                    if ((requestedTrack > -1) && (requestedTrack < tracks.size()) &&
                            (getTrack(requestedTrack).previewUrl != null)) {
                        currentTrack = requestedTrack;
                        setupPlayer(PlayerSetupType.PLAY);
                        mediaPlayer.setDataSource(getApplicationContext(),
                                Uri.parse(tracks.get(currentTrack).previewUrl));
                        isWorking = true;
                        mediaPlayer.prepareAsync();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    onError(mediaPlayer, PLAYER_START_ERROR, 0);
                }
            }
        } else if (ACTION_STOP.equals(action)) {
            setupPlayer(PlayerSetupType.STOP);
        } else if (ACTION_SEEK.equals(action)) {
            if (currentTrack > -1) {
                try {
                    long seekPosition = intent.getIntExtra(ACTION_EXTRA_DATA, 0);
                    getCurrentTrack().lastIndex = seekPosition;
                    if (mediaPlayer.isPlaying()) mediaPlayer.seekTo((int) seekPosition);
                } catch (Exception e) {
                    e.printStackTrace();
                    onError(mediaPlayer, PLAYER_SEEK_ERROR, 0);
                }
            }
        } else if (action.startsWith(ACTION_PREVIOUS)) {
            if (currentTrack > -1) setCurrentTrackNumber(getCurrentTrackNumber()-1);
            return START_STICKY;
        } else if (action.startsWith(ACTION_NEXT)) {
            if (currentTrack > -1) setCurrentTrackNumber(getCurrentTrackNumber()+1);
            return START_STICKY;
        } else if (ACTION_CONTINUOUS_PLAY.equals(action)) {
            continuousPlay = intent.getBooleanExtra(ACTION_EXTRA_DATA, true);
            return START_STICKY;
        } else if (ACTION_INIT.equals(action)) {
            if (isSameData(intent)) return START_STICKY;
            try {
                setupPlayer(PlayerSetupType.PLAY);
                artistName = intent.getStringExtra(ACTION_EXTRA_DATA);
                try {
                    tracks = (List<TrackData>) intent.getSerializableExtra(ACTION_INIT_DATA);
                } catch (Exception e) {}
                int requestedTrack = intent.getIntExtra(ACTION_INIT_DATA_POSITION, 0);
                if ((requestedTrack > -1) && (requestedTrack < tracks.size()) &&
                        (getTrack(requestedTrack).previewUrl != null)) currentTrack = requestedTrack;
                else currentTrack = 0;
                if ((currentTrack > -1) && intent.getBooleanExtra(ACTION_INIT_DATA_PLAY, false) &&
                        (getTrack(currentTrack).previewUrl != null)) {
                    mediaPlayer.setDataSource(getApplicationContext(),
                            Uri.parse(tracks.get(currentTrack).previewUrl));
                    isWorking = true;
                    mediaPlayer.prepareAsync();
                }
            } catch (Exception e) {
                e.printStackTrace();
                onError(mediaPlayer, PLAYER_INIT_ERROR, 0);
            }
        }
        updateClients();
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_PLAYERSERVICE_TRACK_LIST, new Gson().toJson(tracks));
        editor.putInt(PREF_PLAYERSERVICE_CURRENT_TRACK, currentTrack);
        editor.putString(PREF_PLAYERSERVICE_ARTIST_NAME, artistName);
        editor.commit();

        setupPlayer(PlayerSetupType.DESTROY);
        sendNowPlaying(false);
    }


    /**
     * Setup mediaPleyer object
     * @param type type of setup, 1 to play, 0 to stop, -1 to destroy
     */
    protected void setupPlayer(PlayerSetupType type) {
        try {
            if (mediaPlayer == null) return;
            isWorking = false;
            endTimer();
            try { mediaPlayer.stop(); } catch (IllegalStateException e) {}
            switch (type) {
                case PLAY:
                    if (!wifiLock.isHeld()) wifiLock.acquire();
                    mediaPlayer.reset();
                    break;
                case STOP:
                case RESET:
                    if (wifiLock.isHeld()) wifiLock.release();
                    mediaPlayer.reset();
                    break;
                case DESTROY:
                    if (wifiLock.isHeld()) wifiLock.release();
                    mediaPlayer.release();
                    mediaPlayer = null;
                    break;
                default:
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    protected void endTimer() {
        if (positionTimer != null) positionTimer.cancel();
        positionTimer = null;
    }


    public static void putUpdateType(Intent intent, UpdatePlayerType updateType) {
        intent.putExtra(UPDATE_PLAYER_TYPE, updateType);
    }


    public static UpdatePlayerType getUpdateType(Intent intent) {
        return (UpdatePlayerType) intent.getSerializableExtra(UPDATE_PLAYER_TYPE);
    }


    public static void putUpdateTrack(Intent intent, int track) {
        intent.putExtra(UPDATE_PLAYER_TRACK, track);
    }


    public static int getUpdateTrack(Intent intent) {
        return intent.getIntExtra(UPDATE_PLAYER_TRACK, -1);
    }


    protected Intent buildUpdateIntent(UpdatePlayerType updateType) {
        Intent result = new Intent(UPDATE_PLAYER);
        putUpdateType(result, updateType);
        putUpdateTrack(result, getCurrentTrackNumber());
        return result;
    }


    protected void updateClients() {
        if (notificationPlayer) updateNotification();
        updateProgress(UpdatePlayerType.PROGRESS);
    }


    protected void sendNowPlaying(boolean playing) {
        Intent result = new Intent(UPDATE_NOW_PLAYING);
        result.putExtra(UPDATE_NOW_PLAYING_TYPE, (playing)? UpdateNowPlayingType.ON : UpdateNowPlayingType.OFF);
        sendBroadcast(result);
    }


    // PUBLIC METHODS ------------------------------------------------------------------------------


    public boolean isPlaying() {
        return ((mediaPlayer != null) && (mediaPlayer.isPlaying() || isWorking));
    }


    public String getArtistName() {
        return artistName;
    }


    public List<TrackData> getTracks() {
        return tracks;
    }


    public void setCurrentTrackNumber(int position) {
        if ((tracks != null) &&
                (position > -1) && (position < tracks.size())) {
            currentTrack = position;
            updateClients();
        }
    }


    public int getCurrentTrackNumber() {
        return currentTrack;
    }


    public TrackData getCurrentTrack() {
        return (currentTrack > -1)? getTrack(currentTrack) : null;
    }


    public TrackData getTrack(int number) {
        return ((tracks != null) && (number > -1) && (number < tracks.size()))? tracks.get(number) : null;
    }


    public void setTrackProgress(int track, long progress) {
        if ((tracks != null) && (track > -1) && (track < tracks.size())) {
            tracks.get(track).lastIndex = progress;
            updateClients();
        }
    }


    // EVENTS --------------------------------------------------------------------------------------


    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.d(TAG, "onBufferingUpdate(" + percent + ")");
        Intent intent = buildUpdateIntent(UpdatePlayerType.BUFFERING);
        intent.putExtra(UPDATE_PLAYER_EXTRA_DATA, percent);
        sendBroadcast(intent);
    }


    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "onCompletion()");
        TrackData track = getCurrentTrack();
        if (track != null) {
            if (track.previewDuration > -1) track.lastIndex = track.previewDuration;
            else track.lastIndex = track.duration;
        }
        isWorking = false;
        endTimer();
        //stopSelf();
        setupPlayer(PlayerSetupType.STOP);
        //sendBroadcast(buildUpdateIntent(UpdatePlayerType.COMPLETE));
        //updateNotification();
        updateClients();
        if (continuousPlay) {
            int currentItem = getCurrentTrackNumber();
            int item = currentItem;
            if (item < (getTracks().size() - 1)) item++;
            else item = 0;
            //Autoplay
            Intent intent = new Intent(this, PlayerService.class);
            intent.setAction(ACTION_PLAY);
            intent.putExtra(ACTION_EXTRA_DATA, item);
            onStartCommand(intent, 0, 0);
        }
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "onError(" + what + "," + extra + ")");
        isWorking = false;
        endTimer();
        setupPlayer(PlayerSetupType.RESET);
        Intent intent = buildUpdateIntent(UpdatePlayerType.ERROR);
        intent.putExtra(UPDATE_PLAYER_EXTRA_DATA, what);
        sendBroadcast(intent);
        if (notificationPlayer) updateNotification();
        return false;
    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "onPrepared()");
        try {
            positionTimer = new Timer();
            positionTimer.schedule(new TimerTask() {

                @Override
                public void run() {

                    //Picasso call in updteNotification() fails with
                    //java.lang.IllegalStateException: Method call should happen from the main thread.
                    //updateClients();

                    Intent update = new Intent(PlayerService.this, PlayerService.class);
                    update.setAction(ACTION_UPDATE_CLIENTS);
                    startService(update);

                }

            }, 500, 500);

            //ATT(Discrepancy): Spotify duration is long but MediaPlayer seek is int
            TrackData track = getCurrentTrack();
            if (track == null) return;
            long seekTo = track.lastIndex;
            //Keep things right
            if ((seekTo > 0) &&
                    ((seekTo > track.duration) || ((track.previewDuration > -1) && (seekTo >= track.previewDuration)))) {
                seekTo = 0;
                track.lastIndex = 0;
            }
            if (seekTo > 0) mp.seekTo((int) seekTo);
            else mp.start();
        } finally {
            //isWorking = false;
        }
    }


    @Override
    public void onSeekComplete(MediaPlayer mp) {
        Log.d(TAG, "onSeekComplete()");
        try {
            //Wrong initial seek: no preview duration available and seek was bigger
            //so play from start
            if (mediaPlayer.getCurrentPosition() == mediaPlayer.getDuration()) {
                TrackData track = getCurrentTrack();
                if (track == null) return;
                track.lastIndex = 0;
                mp.seekTo(0);
            } else {
                if (!mp.isPlaying()) mp.start();
                updateClients();
            }
        } finally {
           // isWorking = false;
        }
    }


    protected void updateProgress(UpdatePlayerType progressType) {
        //ATT(Discrepancy): Spotify duration is long but MediaPlayer position is int
        TrackData track = getCurrentTrack();
        if ((track != null) && (mediaPlayer != null) && (mediaPlayer.isPlaying())) {
            track.lastIndex = (long) mediaPlayer.getCurrentPosition();
            track.previewDuration = mediaPlayer.getDuration();
        }
        sendBroadcast(buildUpdateIntent(progressType));
    }


    public void updateNotification() {


        int position = getCurrentTrackNumber();
        TrackData track = getCurrentTrack();
        if (track == null) return;

        boolean isPlaying = isPlaying();

        //Optimization: prevent too much notification updates.
        //Here we only care about the current song and playbutton status
        if ((lastNotificationTrack != null) && (lastNotificationTrack.equals(track.id))
                && (isPlaying == lastNotificationPlayStatus)) return;

        lastNotificationTrack = track.id;
        lastNotificationPlayStatus = isPlaying;

        Log.d(TAG, "updateNotification()");

        // Using RemoteViews to bind custom layouts into Notification
        RemoteViews remoteViews = new RemoteViews(getPackageName(),
                R.layout.custom_notification);

        remoteViews.setTextViewText(R.id.not_songName, track.title);

        if (position == 0) {
            //remoteViews.setImageViewResource(R.id.not_prevButton, R.drawable.ic_action_skip_previousd);
            //remoteViews.setOnClickPendingIntent(R.id.not_prevButton, null);
            remoteViews.setInt(R.id.not_prevButton, "setImageAlpha", 175);
            remoteViews.setOnClickPendingIntent(R.id.not_prevButton, null);
        }
        else {
            remoteViews.setInt(R.id.not_prevButton, "setImageAlpha", 255);
            remoteViews.setImageViewResource(R.id.not_prevButton, R.drawable.ic_action_skip_previous);
            Intent intent = new Intent(this, PlayerService.class);
            if (isPlaying) {
                intent.setAction(PlayerService.ACTION_PLAY + "." + System.currentTimeMillis());
                intent.putExtra(PlayerService.ACTION_EXTRA_DATA, (position - 1));
            } else {
                intent.setAction(PlayerService.ACTION_PREVIOUS + "." + System.currentTimeMillis());
            }
            PendingIntent pIntent = PendingIntent.getService(this, PLAYER_REQUEST_PREVIOUS, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.not_prevButton, pIntent);
        }

        if (isPlaying) {
            remoteViews.setImageViewResource(R.id.not_playButton, R.drawable.ic_action_pause);
            Intent intent = new Intent(this, PlayerService.class);
            intent.setAction(PlayerService.ACTION_STOP);
            PendingIntent pIntent = PendingIntent.getService(this, PLAYER_REQUEST_STOP, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.not_playButton, pIntent);
        } else {
            remoteViews.setImageViewResource(R.id.not_playButton, R.drawable.ic_action_play_arrow);
            Intent intent = new Intent(this, PlayerService.class);
            intent.setAction(PlayerService.ACTION_PLAY + "." + System.currentTimeMillis());
            intent.putExtra(PlayerService.ACTION_EXTRA_DATA, position);
            PendingIntent pIntent = PendingIntent.getService(this, PLAYER_REQUEST_PLAY, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.not_playButton, pIntent);
        }
        if (position < (tracks.size() - 1)) {
            remoteViews.setInt(R.id.not_nextButton, "setImageAlpha", 255);
            remoteViews.setOnClickPendingIntent(R.id.not_nextButton, null);
            remoteViews.setImageViewResource(R.id.not_nextButton, R.drawable.ic_action_skip_next);
            Intent intent = new Intent(this, PlayerService.class);
            if (isPlaying) {
                intent.setAction(PlayerService.ACTION_PLAY + "." + System.currentTimeMillis());
                intent.putExtra(PlayerService.ACTION_EXTRA_DATA, (position + 1));
            } else {
                intent.setAction(PlayerService.ACTION_NEXT + "." + System.currentTimeMillis());
            }
            PendingIntent pIntent = PendingIntent.getService(this, PLAYER_REQUEST_NEXT, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.not_nextButton, pIntent);
        } else {
            //remoteViews.setImageViewResource(R.id.not_nextButton, R.drawable.ic_action_skip_nextd);
            //remoteViews.setOnClickPendingIntent(R.id.not_nextButton, null);
            remoteViews.setInt(R.id.not_nextButton, "setImageAlpha", 175);
            remoteViews.setOnClickPendingIntent(R.id.not_nextButton, null);
        }

//        try {
//            remoteViews.setImageViewUri(R.id.not_all, Uri.parse(track.imageUrl));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        Intent intent = new Intent(this, PlayerService.class);
        intent.setAction(PlayerService.ACTION_SHUTDOWN);
        PendingIntent pIntent = PendingIntent.getService(this, PLAYER_REQUEST_SHUTDOWN, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.not_closeButton, pIntent);

        //intent = new Intent(this, PlayerActivity.class);
        //pIntent = PendingIntent.getActivity(this, PLAYER_REQUEST_SWITCH, intent, PendingIntent.FLAG_ONE_SHOT);
        //remoteViews.setOnClickPendingIntent(R.id.not_all, pIntent);
        remoteViews.setOnClickPendingIntent(R.id.not_all, MyFragment.buildSwitchToApplicationIntent(this, getResources().getBoolean(R.bool.large_layout)));

        android.support.v4.app.NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                // Set Icon
                .setSmallIcon(R.drawable.ic_stat_name)
                .setTicker(String.format(getString(R.string.notification_playing), track.title))
                .setAutoCancel(false)
                .setOngoing(true)
                .setContent(remoteViews);

        NotificationManager notificationmanager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = builder.build();

        Picasso.with(this).load(track.imageUrl).into(remoteViews, R.id.not_all, PLAYER_SERVICE_NOTIFICATION, notification);

        notificationmanager.notify(PLAYER_SERVICE_NOTIFICATION, notification);
    }


    protected void cancelNotification() {
        NotificationManager notificationmanager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationmanager.cancel(PLAYER_SERVICE_NOTIFICATION);
        lastNotificationTrack = null;
        lastNotificationPlayStatus = false;
    }


}
