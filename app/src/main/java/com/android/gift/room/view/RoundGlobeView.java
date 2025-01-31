package com.android.gift.room.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.RelativeLayout;
import com.android.gift.R;
import com.android.gift.manager.VibratorManager;
import com.android.gift.util.AppUtils;
import com.android.gift.view.RadarLayout;

/**
 * Created by TinyHung@outlook.com
 * 2019/7/13
 * 圆球拖拽
 */

public class RoundGlobeView extends RelativeLayout {

    private Context mContext;
    private int mScreenWidth,mScreenHeight,mDefaultX,mDefaultY;
    private float downX,downY;
    private String mTag;
    private boolean mIsVivrate = false;
    private RadarLayout mRadarLayout;

    public RoundGlobeView(Context context) {
        this(context, null);
    }

    public RoundGlobeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundGlobeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View.inflate(context, R.layout.view_index_globe_layout,this);
        this.mContext = context;
        //禁用硬件加速
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mScreenWidth = AppUtils.getInstance().getScreenWidth(context);
        mScreenHeight = AppUtils.getInstance().getScreenHeight(context);
        mRadarLayout = (RadarLayout) findViewById(R.id.radar_layout);
        mRadarLayout.startPlayer();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        if (this.isEnabled()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    //手指按下View的X,Y像素
                    downX = event.getX();
                    downY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    //计算出View所要移动的目标X,Y像素点
                    float mXDistance = event.getRawX() - downX;
                    float mYDistance = event.getRawY() - downY;
                    if (mXDistance < 0) {
                        mXDistance = 0;
                    } else if (mXDistance > (mScreenWidth - getWidth())) {
                        mXDistance = mScreenWidth - getWidth();
                    }
                    if (mYDistance < 0) {
                        mYDistance = 0;
                    } else if (mYDistance > (mScreenHeight - getHeight())) {
                        mYDistance = mScreenHeight - getHeight();
                    }
                    setX(mXDistance);
                    setY(mYDistance);
                    if(null!=mOnGlobeMoveListener){
                        String tag = mOnGlobeMoveListener.continueMove(((int) event.getRawX()), ((int) event.getRawY()));
                        if (null != tag) {
                            if (mTag == null) {
                                mTag = tag;
                                mIsVivrate = true;
                                VibratorManager.getInstance().vibrate(getContext(),50);
                                if(null!=mOnGlobeMoveListener){
                                    mOnGlobeMoveListener.boxEnterView(mTag);
                                }
                                return true;
                            }
                            if (!tag.equals(mTag) && !mIsVivrate) {
                                mTag = tag;
                                mIsVivrate = true;
                                VibratorManager.getInstance().vibrate(getContext(),50);
                                if(null!=mOnGlobeMoveListener){
                                    mOnGlobeMoveListener.boxEnterView(mTag);
                                }
                            }
                        } else {
                            //手持从某个View的区域离开了
                            if(null!=mTag&&null!=mOnGlobeMoveListener){
                                mOnGlobeMoveListener.boxOutView(mTag,false);
                            }
                            mIsVivrate = false;
                            mTag = null;
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    //圆球离开了VIEW区域
                    if(null!=mOnGlobeMoveListener){
                        mOnGlobeMoveListener.boxOutView(mTag,true);
                    }
                    animateXY(((int) event.getRawX()), ((int) event.getRawY()), new OnAnimatorListener() {
                        @Override
                        public void onEnd() {
                            if(null!=mOnGlobeMoveListener&&null!=mTag){
                                mOnGlobeMoveListener.selectedBox(mTag);
                                mIsVivrate = false;
                                mTag = null;
                            }
                        }
                    });
                    break;
            }
            return true;
        }
        return false;
    }

    /**
     * 圆球松手后固定不变的复位终点位置
     * @param endX 终点X
     * @param endY 终点Y
     */
    public void setDefault(int endX, int endY) {
        this.mDefaultX = endX;
        this.mDefaultY = endY;
        setX(mDefaultX);
        setY(mDefaultY);
    }


    @SuppressLint("ObjectAnimatorBinding")
    public void animateXY(int startX, int startY , final OnAnimatorListener onAnimatorListener) {
        ObjectAnimator objectAnimatorX = ObjectAnimator.ofInt(this, "number", startX, mDefaultX);
        ObjectAnimator objectAnimatorY = ObjectAnimator.ofInt(this, "number", startY, mDefaultY);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(objectAnimatorX,objectAnimatorY);
        animatorSet.setDuration(350);
        //BounceInterpolator OvershootInterpolator
        animatorSet.setInterpolator(new OvershootInterpolator());
        objectAnimatorX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animatedValue = (int) animation.getAnimatedValue();
                RoundGlobeView.this.setX(animatedValue);
            }
        });
        objectAnimatorY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animatedValue = (int) animation.getAnimatedValue();
                RoundGlobeView.this.setY(animatedValue);
            }
        });
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (onAnimatorListener!=null) {
                    onAnimatorListener.onEnd();
                }
            }
        });
        animatorSet.start();
    }

    public interface OnGlobeMoveListener {
        String continueMove(int touchRowX, int touchRowY);
        void boxEnterView(String tag);
        void boxOutView(String tag,boolean isGone);
        void selectedBox(String tag);
    }

    private OnGlobeMoveListener mOnGlobeMoveListener;

    public void setOnGlobeMoveListener(OnGlobeMoveListener onGlobeMoveListener) {
        mOnGlobeMoveListener = onGlobeMoveListener;
    }

    public interface OnAnimatorListener{
        void onEnd();
    }

    public void onResume(){
        if(null!=mRadarLayout){
            mRadarLayout.onStart();
        }
    }

    public void onPause(){
        if(null!=mRadarLayout){
            mRadarLayout.onStop();
        }
    }

    public void onDestroy(){
        VibratorManager.getInstance().onDestroy();
        if(null!=mRadarLayout){
            mRadarLayout.onStop();
            mRadarLayout=null;
        }
        mOnGlobeMoveListener=null;mTag=null;mContext=null;
    }
}