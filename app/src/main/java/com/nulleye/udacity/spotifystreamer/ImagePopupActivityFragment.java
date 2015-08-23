package com.nulleye.udacity.spotifystreamer;

//import android.graphics.Matrix;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.ortiz.touch.ExtendedViewPager;
import com.ortiz.touch.TouchImageView;
import com.squareup.picasso.Picasso;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */

public class ImagePopupActivityFragment extends Fragment implements View.OnClickListener {

    public static final String STATE_LIST = "list";
    public static final String STATE_LIST_POSITION = "list_position";
    public static final String STATE_ACTION_INTENT_CLASS = "action_intent_class";
    public static final String STATE_EXTRA = "extra";
    public static final String STATE_EXTRA2 = "extra2";
    public static final String STATE_REQUEST_CODE = "request_code";

//    private ImageView image;
//    private ScaleGestureDetector scaleGestureDetector;

    ShareActionProvider mShareActionProvider = null;

    ExtendedViewPager viewPager;
    TouchImageAdapter imageAdapter;

    List<ItemData> data = null;
    int position = -1;
    String actionIntentClass = null;
    String extra = null;
    boolean extra2 = false;

    //Working mode
    int requestCode = -1;

    public ImagePopupActivityFragment() {
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View vw = inflater.inflate(R.layout.fragment_image_popup, container, false);
        viewPager = (ExtendedViewPager) vw.findViewById(R.id.imageview_pager);

        //Currently not used, imagepopup in right pane was dismissed
        Bundle arguments = getArguments();
        if (arguments != null) {
            try {
                data = (List<ItemData>) arguments.getSerializable(MyFragment.ELEMENT_LIST);
            } catch (Exception e) {}
            position = arguments.getInt(MyFragment.ELEMENT_POSITION);
            actionIntentClass = arguments.getString(MyFragment.ELEMENT_ACTION_INTENT_CLASS);
            extra = arguments.getString(MyFragment.ELEMENT_EXTRA);
        }

        return vw;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            try {
                data = (List<ItemData>) savedInstanceState.getSerializable(STATE_LIST);
            } catch (Exception e) {}
            position = savedInstanceState.getInt(STATE_LIST_POSITION);
            actionIntentClass = savedInstanceState.getString(STATE_ACTION_INTENT_CLASS);
            extra = savedInstanceState.getString(STATE_EXTRA);
            extra2 = savedInstanceState.getBoolean(STATE_EXTRA2);
            requestCode = savedInstanceState.getInt(STATE_REQUEST_CODE);
        } else {
            Intent intent = getActivity().getIntent();
            if (intent != null) {
                if (data == null)
                    try {
                        data = (List<ItemData>) intent.getSerializableExtra(MyFragment.ELEMENT_LIST);
                    } catch (Exception e) {
                    }
                if (position < 0)
                    position = intent.getIntExtra(MyFragment.ELEMENT_POSITION, 0);
                if (actionIntentClass == null)
                    actionIntentClass = intent.getStringExtra(MyFragment.ELEMENT_ACTION_INTENT_CLASS);
                if (extra == null)
                    extra = intent.getStringExtra(MyFragment.ELEMENT_EXTRA);
                extra2 = intent.getBooleanExtra(MyFragment.ELEMENT_EXTRA2, false);
                requestCode = intent.getIntExtra(MyFragment.REQUEST_CODE, -1);
            }
        }
        imageAdapter = new TouchImageAdapter(data);

        viewPager.setAdapter(imageAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                updateTitle(position);
                if (mShareActionProvider != null)
                    mShareActionProvider.setShareIntent(createShareItemIntent(data.get(position)));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        viewPager.setCurrentItem(position);
        if (position == 0) updateTitle(position);  //Force title update as the previous setCurrentItem seems not to call onPageSelected

//        scaleGestureDetector = new ScaleGestureDetector(getActivity(), new ImageScaleGestureListener());

    }


    public void updateTitle(int position) {
        ItemData imageData = data.get(position);
        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(imageData.title);
            actionBar.setSubtitle(imageData.subtitle);
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_LIST, (Serializable) data);
        outState.putInt(STATE_LIST_POSITION, viewPager.getCurrentItem());
        outState.putString(STATE_ACTION_INTENT_CLASS, actionIntentClass);
        outState.putString(STATE_EXTRA, extra);
        outState.putBoolean(STATE_EXTRA2, extra2);
        outState.putInt(STATE_REQUEST_CODE, requestCode);
    }


    @Override
    public void onClick(View v) {

    }


//    //Redirect touch event to gesture detector
//    public boolean onTouchEvent(MotionEvent event) {
//       return scaleGestureDetector.onTouchEvent(event);
//    }


//    public class ImageScaleGestureListener extends SimpleOnScaleGestureListener {
//
//        private Matrix matrix = new Matrix();
//        private float scale = 1f;
//        private ScaleGestureDetector SGD;
//
//        @Override
//        public boolean onScale(ScaleGestureDetector detector) {
//            scale *= detector.getScaleFactor();
//            scale = Math.max(0.1f, Math.min(scale, 5.0f));
//            matrix.setScale(scale, scale);
//            image.setImageMatrix(matrix);
//            return true;
//        }
//
//    }


    class TouchImageAdapter extends PagerAdapter {

        List<ItemData> data;
        List<TouchImageView> images;


        public TouchImageAdapter(List<ItemData> data) {
            this.data = data;
            images = new ArrayList<TouchImageView>(data.size());
        }


        @Override
        public int getCount() {
            return data.size();
        }


        @Override
        public View instantiateItem(ViewGroup container, final int position) {
            if (images.size() <= position) for(int i=images.size();i<=position;i++) images.add(null);
            TouchImageView img = images.get(position);
            if (img == null) {
                img = new TouchImageView(container.getContext());
                final ItemData imageData = data.get(position);
                img.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (actionIntentClass != null) {
                            Activity activity = getActivity();
                            try {
                                Class clazz = Class.forName(actionIntentClass);
                                Intent intent = (requestCode > -1)? new Intent() : new Intent(activity, clazz);
                                intent.putExtra(MyFragment.ELEMENT_POSITION, position);
                                if (extra != null) { //Track mode
                                    //Explicit intent with data and currrent position
                                    intent.putExtra(MyFragment.ELEMENT_EXTRA, extra);
                                    intent.putExtra(MyFragment.ELEMENT_LIST, (Serializable) data);
                                } else { //Artist mode
                                    //Explicit intent with selected element id and name
                                    intent.putExtra(MyFragment.ELEMENT_EXTRA2, extra2);
                                    intent.putExtra(MyFragment.ELEMENT_ID, imageData.id);
                                    intent.putExtra(MyFragment.ELEMENT_NAME, imageData.title);
                                }
                                if (requestCode > -1) {
                                    activity.setResult(requestCode, intent);
                                    activity.finish();
                                } else startActivity(intent);
                            } catch(Exception e) {
                                //Class not found!? -> show not implemented message
                                Toast.makeText(getActivity(), getString(R.string.not_implemented), Toast.LENGTH_SHORT).show();;
                            }
                        }
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


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem menuItem = menu.findItem(R.id.action_share);
        if (menuItem != null) mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
    }


    protected Intent createShareItemIntent(ItemData item) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, item.externalUrl);
        return shareIntent;
    }


}
