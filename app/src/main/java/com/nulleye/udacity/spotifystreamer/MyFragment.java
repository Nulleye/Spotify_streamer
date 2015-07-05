package com.nulleye.udacity.spotifystreamer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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

}
