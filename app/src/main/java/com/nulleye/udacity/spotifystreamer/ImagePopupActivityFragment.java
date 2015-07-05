package com.nulleye.udacity.spotifystreamer;

//import android.graphics.Matrix;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
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

//    private ImageView image;
//    private ScaleGestureDetector scaleGestureDetector;

    ExtendedViewPager viewPager;
    TouchImageAdapter imageAdapter;
    List<ImagePopupData> data;
    String actionIntentClass;

    public ImagePopupActivityFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View vw = inflater.inflate(R.layout.fragment_image_popup, container, false);

        data  = null;
        int position = 0;
        if (savedInstanceState != null) {
            try {
                data = (List<ImagePopupData>) savedInstanceState.getSerializable(STATE_LIST);
            } catch (Exception e) {}
            position = savedInstanceState.getInt(STATE_LIST_POSITION);
            actionIntentClass = savedInstanceState.getString(STATE_ACTION_INTENT_CLASS);
        } else {
            try {
                data = (List<ImagePopupData>) getActivity().getIntent().getSerializableExtra(MyFragment.ELEMENT_LIST);
            } catch (Exception e) {}
            position = getActivity().getIntent().getIntExtra(MyFragment.ELEMENT_POSITION, 0);
            actionIntentClass = getActivity().getIntent().getStringExtra(MyFragment.ELEMENT_ACTION_INTENT_CLASS);
        }
        imageAdapter = new TouchImageAdapter(data);

        viewPager = (ExtendedViewPager) vw.findViewById(R.id.imageview_pager);
        viewPager.setAdapter(imageAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                updateTitle(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        viewPager.setCurrentItem(position);
        updateTitle(position);  //Force title update as the previous setCurrentItem seems not to call onPageSelected

//        scaleGestureDetector = new ScaleGestureDetector(getActivity(), new ImageScaleGestureListener());

        return vw;
    }


    public void updateTitle(int position) {
        ImagePopupData imageData = data.get(position);
        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(imageData.title);
        actionBar.setSubtitle(imageData.subtitle);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_LIST, (Serializable) data);
        outState.putInt(STATE_LIST_POSITION, viewPager.getCurrentItem());
        outState.putString(STATE_ACTION_INTENT_CLASS, actionIntentClass);
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

        List<ImagePopupData> data;
        List<TouchImageView> images;


        public TouchImageAdapter(List<ImagePopupData> data) {
            this.data = data;
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
                final ImagePopupData imageData = data.get(position);
                img.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (actionIntentClass != null) {
                            try {
                                Class clazz = Class.forName(actionIntentClass);
                                //Explicit intent with selected element id and name
                                Intent intent = new Intent(getActivity(), clazz);
                                intent.putExtra(MyFragment.ELEMENT_ID, imageData.id);
                                intent.putExtra(MyFragment.ELEMENT_NAME, imageData.title);
                                startActivity(intent);
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

}
