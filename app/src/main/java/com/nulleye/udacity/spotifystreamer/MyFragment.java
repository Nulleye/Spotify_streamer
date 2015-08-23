package com.nulleye.udacity.spotifystreamer;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import kaaes.spotify.webapi.android.models.Image;

/**
 * Created by cristian on 3/7/15.
 */
public abstract class MyFragment extends Fragment implements  ListView.OnItemClickListener {

    public static final String ELEMENT_LIST = "element_list";
    public static final String ELEMENT_POSITION = "element_position";
    public static final String ELEMENT_ID = "element_id";
    public static final String ELEMENT_NAME = "element_name";
    public static final String ELEMENT_ACTION_INTENT_CLASS = "action_intent_class";
    public static final String ELEMENT_LINK = "element_link";
    public static final String ELEMENT_EXTRA = "element_extra";
    public static final String ELEMENT_EXTRA2 = "element_extra2";

    public static final String REQUEST_CODE = "request_code";

    public static final String STATE_SEARCH_MESSAGE = "search_message";
    public static final String STATE_SEARCH_RESULT = "search_result";
    public static final String STATE_SEARCH_RESULT_SELECTED = "search_result_selected";
    public static final String STATE_SEARCHING = "searching";

    ListView resultList;

    int currentMessageId = 0;
    View message;
    TextView messageIcon;
    TextView messageText;

    int bestFitImagePixels;

    Animation workingAnimation;

    int currentItem = -1;

    MenuItem nowPlayingMenuItem = null;
    NowPlayingReceiver nowPlayingReceiver;

    public abstract void selectItem(int position);


    public void setupFragment(View parent, int listviewId) {

        resultList = (ListView) parent.findViewById(listviewId);
        resultList.setOnItemClickListener(this);

        message = parent.findViewById(R.id.message_layout);
        messageText = (TextView) parent.findViewById(R.id.message_text);

        messageIcon = (TextView) parent.findViewById(R.id.message_icon);
        messageIcon.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/MaterialIcons-Regular.ttf"));
        messageIcon.setTextColor(messageText.getTextColors().getDefaultColor());
        messageIcon.setTextSize(convertPixelsToDp(messageText.getTextSize() * 1.7F, getActivity()));

        workingAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.working);

        bestFitImagePixels = Math.round(convertDpToPixel(getResources().getDimension(R.dimen.thumbnail_size), getActivity()));
    }


    protected void forceHideKeyboard(View v) {
        //Question: why I need to force this?? (not needed only for the first search)
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }


    protected void showMessage(boolean showHide) {
        try {
            if (showHide) {
                message.setVisibility(View.VISIBLE);
            } else {
                message.setVisibility(View.GONE);
                messageIcon.clearAnimation();
            }
        } catch (Exception e) {}
    }


    protected void setMessage(int messageId) {
        try {
            boolean animate = false;
            int strIdImage;
            switch (messageId) {
                case R.string.message_artist_not_found:
                    strIdImage = R.string.img_not_found;
                    break;
                case R.string.message_no_tracks_found:
                    strIdImage = R.string.img_not_found;
                    break;
                case R.string.message_no_internet:
                    strIdImage = R.string.img_no_internet;
                    break;
                case R.string.message_searching:
                case R.string.message_loading:
                    strIdImage = R.string.img_searching;
                    animate = true;
                    break;
                default:
                    strIdImage = R.string.img_error;
            }
            currentMessageId = messageId;
            if (!animate) messageIcon.clearAnimation();
            messageIcon.setText(getString(strIdImage));
            if (animate) messageIcon.startAnimation(workingAnimation);
            messageText.setText(getString(messageId));
            showMessage(true);
        } catch (Exception e) {}
    }


    public static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }


    public static float convertPixelsToDp(float px, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return dp;
    }


    /**
     * Determine the image that best fits a squared icon of width pixels size
     * @param images Images list
     * @param width squared icon size (device dpi dependant)
     * @return The image
     */
    public static Image getBestFitImage(List<Image> images, int width) {
        if (images == null) return null;
        Image bestFit = null;
        for(Image image:images) {
            if (bestFit == null) bestFit = image;
            else {
                int diffImage = (image.width < image.height)?
                        Math.abs(image.width - width) : Math.abs(image.height - width);
                int diffBest = (bestFit.width < bestFit.height)?
                        Math.abs(bestFit.width - width) : Math.abs(bestFit.height - width);
                if (diffBest > diffImage) bestFit = image;
            }
        }
        if (bestFit != null) return bestFit;
        return null;
    }


    /**
     * Get the largest image from the list, for image popup
     * @param images Images list
     * @return The image
     */
    public static Image getLargestImage(List<Image> images) {
        if (images == null) return null;
        Image largest = null;
        for(Image image:images) {
            if (largest == null) largest = image;
            else {
                int imageDim = Math.max(image.width, image.height);
                int largestDim = Math.max(largest.width, largest.height);
                if (imageDim > largestDim) largest = image;
            }
        }
        if (largest != null) return largest;
        return null;
    }


    protected class NowPlayingReceiver extends BroadcastReceiver {

        public NowPlayingReceiver() {
            super();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(MyFragment.class.getSimpleName(), "onReceive(" + intent + ")");
            ActionBar actionbar = ((ActionBarActivity) getActivity()).getSupportActionBar();
            if ((nowPlayingMenuItem != null) && (actionbar != null) && PlayerService.UPDATE_NOW_PLAYING.equals(intent.getAction())) {
                PlayerService.UpdateNowPlayingType type =
                        (PlayerService.UpdateNowPlayingType) intent.getSerializableExtra(PlayerService.UPDATE_NOW_PLAYING_TYPE);
                nowPlayingMenuItem.setVisible(type == PlayerService.UpdateNowPlayingType.ON);
            }
        }


        @Override
        public IBinder peekService(Context myContext, Intent service) {
            return super.peekService(myContext, service);
        }

    }

    protected boolean mServiceBound = false;
    protected PlayerService mBoundService = null;

    protected ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBoundService = null;
            mServiceBound = false;
            updateNowPlayingMenu();
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(MyFragment.class.getSimpleName(), "onServiceConnected()");
            PlayerService.PlayerServiceBinder binder = (PlayerService.PlayerServiceBinder) service;
            mBoundService = binder.getService();
            mServiceBound = true;
            updateNowPlayingMenu();
        }

    };


    protected void bindService() {
        Intent intent = new Intent(getActivity(),PlayerService.class);
        intent.setAction(PlayerService.ACTION_DUMMY);
        getActivity().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }


    protected void unbindService() {
        getActivity().unbindService(mServiceConnection);
        mServiceBound = false;
    }


    protected void updateNowPlayingMenu() {
        if (nowPlayingMenuItem != null) {
            List<TrackData> tracks = null;
            if (mBoundService != null) tracks = mBoundService.getTracks();
            nowPlayingMenuItem.setVisible((tracks != null) && (tracks.size() > 0));
        }
    }


    protected void switchToPlayer() {
    //    Intent intent = new Intent(getActivity(), PlayerActivity.class);
    //    getActivity().startActivity(intent);
        try {
            buildSwitchToApplicationIntent(getActivity(), getResources().getBoolean(R.bool.large_layout)).send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }


    public static PendingIntent buildSwitchToApplicationIntent(Context context, boolean twoPane) {
        Intent[] intentList;
        Intent mainIntent = new Intent(context, SearchActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Intent playerIntent = new Intent(context, PlayerActivity.class);
        if (!twoPane) intentList = new Intent[] {mainIntent,  new Intent(context, ToptracksActivity.class), playerIntent};
        else intentList = new Intent[] {mainIntent, playerIntent};
        return PendingIntent.getActivities(context, PlayerService.PLAYER_REQUEST_SWITCH, intentList, PendingIntent.FLAG_UPDATE_CURRENT);
    }


}
