package com.example.surfaceviewdemo;

import android.content.Context;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public class ZoomSurfaceView extends SurfaceView {

    public static final String TAG = "ZoomSurfaceView";
    public GestureDetector mGestureDetector;
    public ScaleGestureDetector mScaleGestureDetector;
    public int mScreenHeight = 0;
    public int mScreenWidth = 0;
    public static final int ModeDrag = 1;
    public static final int ModeZoon = 2;
    public static final int ModeDoubleClick = 3;
    /**
     * Action模式
     */
    public int mMode = 0;
    public long mFirstTime = 0;
    public static final float ScaleMax = 4.0f;
    public static final float ScaleMid = 2.0f;
    public static final float ScaleMin = 1.0f;
    public static float mScaleTouchSlop = 0;
    public int mStartTop = -1, mStartRight = -1, mStartLeft = -1, mStartBottom = -1;
    public int mStartX, mStartY, mCurrentX, mCurrentY;
    public int mViewWidth = 0;
    public int mViewHeight = 0;
    public int mInitViewWidth = 0;
    public int mInitViewHeight = 0;
    public int mFatherViewW;
    public int mFatherViewH;
    public int mFatherTop;
    public int mFatherBottom;
    public int mDistanceX, mDistanceY;
    public boolean mIsControlVertical, mIsControlHorizal;
    public float mRatio = 0.3f;

    public ZoomSurfaceView(Context context) {
        this(context, null);
    }

    public ZoomSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }


    public void log(String msg) {
        Log.v(TAG, getClass().getSimpleName() + "->" + msg);
    }

    private void init(Context context) {
        mScaleTouchSlop = ViewConfiguration.getTouchSlop();
        setFocusable(true);
        setClickable(true);
        setLongClickable(true);
        mScreenHeight = ScreenUtils.getScreenHeight(getContext());
        mScreenWidth = ScreenUtils.getScreenWidth(getContext());
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), new MyScaleGestueListener());
        mGestureDetector = new GestureDetector(context, new MyGestureDetector());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                onTouchDown(event);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                onPointerDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                onTouchMove(event);
                break;
            case MotionEvent.ACTION_UP:
                mMode = 0;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mMode = 0;
                break;
            default:
                break;
        }
        return mScaleGestureDetector.onTouchEvent(event);
    }

    private void onTouchMove(MotionEvent event) {
        int left = 0, top = 0, right = 0, bottom = 0;
        if (mMode == ModeDrag) {
            left = getLeft();
            right = getRight();
            top = getTop();
            bottom = getBottom();
            mDistanceX = (int) (event.getRawX() - mCurrentX);
            mDistanceY = (int) (event.getRawY() - mCurrentY);
            if (icallBack != null) {
                icallBack.getAngle((int) getX(), this.getWidth());
            }
            if (mScaleTouchSlop <= getDistance(mDistanceX, mDistanceY)) {
                left = left + mDistanceX;
                right = right + mDistanceX;
                bottom = bottom + mDistanceY;
                top = top + mDistanceY;
                // ˮƽ�ж�
                if (mIsControlHorizal) {
                    if (left >= 0) {
                        left = 0;
                        right = this.getWidth();
                    }
                    if (right <= mScreenWidth) {
                        left = mScreenWidth - this.getWidth();
                        right = mScreenWidth;
                    }
                } else {
                    left = getLeft();
                    right = getRight();
                }
                // ��ֱ�ж�
                if (mIsControlVertical) {
                    if (top > 0) {
                        top = 0;
                        bottom = this.getHeight();
                    }
                    if (bottom <= mStartBottom) {
                        bottom = mStartBottom;
                        top = mFatherViewH - this.getWidth();
                    }
                } else {
                    top = this.getTop();
                    bottom = this.getBottom();
                }
                if (mIsControlHorizal || mIsControlVertical) {
                    this.setPosition(left, top, right, bottom);
                }
                mCurrentX = (int) event.getRawX();
                mCurrentY = (int) event.getRawY();
            }
        }
    }

    private void onPointerDown(MotionEvent event) {
        if (event.getPointerCount() == 2) {
            mMode = ModeZoon;
        }
    }


    private void onTouchDown(MotionEvent event) {
        mMode = ModeDrag;
        mStartX = (int) event.getRawX();
        mStartY = (int) event.getRawY();
        mCurrentX = (int) event.getRawX();
        mCurrentY = (int) event.getRawY();
        mViewWidth = getWidth();
        mViewHeight = getHeight();
        if (mViewHeight > mFatherViewH) {
            mIsControlVertical = true;
        } else {
            mIsControlVertical = false;
        }
        if (mViewWidth > mFatherViewW) {
            mIsControlHorizal = true;
        } else {
            mIsControlHorizal = false;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mStartTop == -1) {
            mStartTop = top;
            mStartLeft = left;
            mStartRight = right;
            mStartBottom = bottom;
            mInitViewWidth = getWidth();
            mInitViewHeight = getHeight();
        }
    }

    public class MyScaleGestueListener implements ScaleGestureDetector.OnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            int left = 0, right = 0, top = 0, bottom = 0;
            float length = 0;
            if (mMode == ModeZoon) {
                float ratio = detector.getScaleFactor();
                left = getLeft();
                top = getTop();
                bottom = getBottom();
                right = getRight();
                if (ratio > 1) { // �Ŵ�ײ״̬
                    length = (int) ((getHeight() * (ratio - 1)) / 7.0);
                    left = (int) (left - length / 2);
                    right = (int) (right + length / 2);
                    bottom = (int) (bottom + length / 2);
                    top = (int) (top - length / 2);
                    if (getWidth() <= (mScreenWidth * 3) && getHeight() <= (mFatherViewH * 3)) {
                        setPosition(left, top, right, bottom);
                    }
                } else {
                    length = (int) ((getHeight() * (1 - ratio)) / 7.0);
                    left = (int) (left + length / 2);
                    right = (int) (right - length / 2);
                    bottom = (int) (bottom - length / 2);
                    top = (int) (top + length / 2);
                    if (left >= 0) {
                        left = 0;
                    }
                    if (right <= mScreenWidth) {
                        right = mScreenWidth;
                    }
                    if (top >= 0) {
                        top = 0;
                    }
                    if (bottom <= mFatherViewH) {
                        bottom = mFatherViewH;
                    }
                    if (getWidth() > mInitViewWidth && getHeight() > mFatherViewH) {
                        setPosition(left, top, right, bottom);
                    } else {
                        setPosition(mStartLeft, mStartTop, mStartRight, mStartBottom);
                    }
                }
            }
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }

    }

    public class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
        public boolean onDoubleTap(MotionEvent e) {
            int left = 0, top = 0, right = 0, bottom = 0;
            int length = 0;
            left = getLeft();
            top = getTop();
            bottom = getBottom();
            right = getRight();
            if (getHeight() > mFatherViewH) {
                // ��Сģʽ
                Log.i(TAG, "��Сģʽ");
                while (getHeight() > mFatherViewH) {
                    length = (int) ((getHeight() * mRatio) / 5.0);
                    left = (int) (getLeft() + length / 2);
                    right = (int) (getRight() - length / 2);
                    bottom = (int) (getBottom() - length / 2);
                    top = (int) (getTop() + length / 2);
                    if (left >= 0) {
                        left = 0;
                    }
                    if (right <= mScreenWidth) {
                        right = mScreenWidth;
                    }
                    if (top >= 0) {
                        top = 0;
                    }
                    if (bottom <= mFatherViewH) {
                        bottom = mFatherViewH;
                    }
                    if (getWidth() > mInitViewWidth && getHeight() > mFatherViewH) {
                        setPosition(left, top, right, bottom);
                    } else {
                        setPosition(mStartLeft, mStartTop, mStartRight, mStartBottom);
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            } else {
                Log.i(TAG, "�Ŵ�ģʽ");
                if (getHeight() <= mFatherViewH) {
                    while (getHeight() < mInitViewHeight * 2) {
                        length = (int) ((getHeight() * mRatio) / 5.0);
                        left = (int) (getLeft() - length / 2);
                        right = (int) (getRight() + length / 2);
                        bottom = (int) (getBottom() + length / 2);
                        top = (int) (getTop() - length / 2);
                        if (getWidth() <= (mScreenWidth * 3) && getHeight() <= (mFatherViewH * 3)) {
                            setPosition(left, top, right, bottom);
                        }
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
            return true;
        }

    }

    private void setPosition(int left, int top, int right, int bottom) {
        this.layout(left, top, right, bottom);

    }

    public void setFatherW_H(int fatherView_Width, int fatherView_Height) {
        this.mFatherViewW = fatherView_Width;
        this.mFatherViewH = fatherView_Height;
    }

    public void setLayoutParam(float scale) {
        LinearLayout.LayoutParams layoutParams = (LayoutParams) getLayoutParams();
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.width = (int) (scale * (layoutParams.width));
        layoutParams.height = (int) (scale * (layoutParams.height));
        setLayoutParams(layoutParams);
    }

    private float getDistance(float distanceX, float distanceY) {
        return FloatMath.sqrt(distanceX * distanceX + distanceY * distanceY);
    }

    public void setFatherTopAndBottom(int fatherTop, int fatherBottom) {
        this.mFatherTop = fatherTop;
        this.mFatherBottom = fatherBottom;
    }

    public interface ICoallBack {
        void getAngle(int angle, int viewW);
    }

    ICoallBack icallBack = null;

    public void setEvent(ICoallBack iBack) {
        icallBack = iBack;
    }

}
