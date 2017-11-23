package com.practice.loadanimation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;


public class SplashView extends View{
    private ValueAnimator mAnimator;
    // 大圆(里面包含很多小圆的)的半径
    private float mRotationRadius = 90;
    // 每一个小圆的半径
    private float mCircleRadius = 18;
    // 小圆圈的颜色列表，在initialize方法里面初始化
    private int[] mCircleColors;
    // 大圆和小圆旋转的时间
    private long mRotationDuration = 1200; //ms
    // 第二部分动画的执行总时间(包括二个动画时间，各占1/2)
    private long mSplashDuration = 1200; //ms
    // 整体的背景颜色
    private int mSplashBgColor = Color.WHITE;

    /**
     * 参数，保存了一些绘制状态，会被动态地改变* */
    //空心圆初始半径
    private float mHoleRadius = 0F;
    //当前大圆旋转角度(弧度)
    private float mCurrentRotationAngle = 0F;
    //当前大圆的半径
    private float mCurrentRotationRadius = mRotationRadius;

    // 绘制圆的画笔
    private Paint mPaint = new Paint();
    // 绘制背景的画笔(白色背景)
    private Paint mPaintBackground = new Paint();

    // 屏幕正中心点坐标
    private float mCenterX;
    private float mCenterY;
    //屏幕对角线一半
    private float mDiagonalDist;

    public SplashView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        mCircleColors = context.getResources().getIntArray(R.array.splash_circle_colors);
        //画笔初始化
        //消除锯齿
        mPaint.setAntiAlias(true);
        mPaintBackground.setAntiAlias(true);
        //设置样式---边框样式--描边
        mPaintBackground.setStyle(Paint.Style.STROKE);
        mPaintBackground.setColor(mSplashBgColor);
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCenterX = w/2f;
        mCenterY = h/2f;
        mDiagonalDist = (float) Math.sqrt((w*w+h*h))/2f;//勾股定律
    }

    public void splashDisappear(){
        //开启后面两个动画
        //换状态
        if (mState!=null && mState instanceof  RotateState) {
            ((RotateState) mState).cancel();
            post(r);
        }
    }

    Runnable r=new Runnable() {
        //结束选择动画
        @Override
        public void run() {
            mState = new MargingState();
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //开启第一个旋转动画
        if (mState==null) {
            mState=new RotateState();
        }
        //调用绘制方法
        mState.drawState(canvas);
    }
    private SplashState mState = null;

    //策略模式 State - 三种动画
    private abstract class SplashState{
        public abstract void drawState(Canvas canvas);
    }

    /**
     *  1.旋转动画
     *  控制各个小圆的坐标   控制小圆的角度变化   属性动画ValueAnimator
     */
    private class RotateState extends SplashState{

        public RotateState() {
            //1.动画的初始工作            //2.开启动画
            //花1200ms，计算某个时刻当前的角度是多少？0~2π
            mAnimator=ValueAnimator.ofFloat(0f,(float) Math.PI*2);
            mAnimator.setInterpolator(new LinearInterpolator());
            mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurrentRotationAngle = (float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            mAnimator.setDuration(mRotationDuration);
            mAnimator.setRepeatCount(ValueAnimator.INFINITE);//无限重复
            mAnimator.start();

        }

        public void cancel(){
            mAnimator.cancel();
        }

        @Override
        public void drawState(Canvas canvas) {
            //绘制背景，涂成白色
            drawBackground(canvas);
            //绘制小圆
            drawCircle(canvas);
        }
    }


    /**
     *  2.聚合动画
     *  要素：大圆的半径  变大--变小   小圆的半径
     */
    private class MargingState extends SplashState{

        public MargingState() {
            //花1200秒  计算某个时刻当前大圆半径是多少   r~0的过程
            mAnimator = ValueAnimator.ofFloat(mCircleRadius,mRotationRadius);
            mAnimator.setDuration(mRotationDuration/3*2);
            mAnimator.setInterpolator(new OvershootInterpolator(10f));
            mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    //某个时刻当前大圆的半径是多少？
                    mCurrentRotationRadius = (float)animation.getAnimatedValue();
                    invalidate();

                }
            });
            mAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mState = new ExpandState();

                    removeCallbacks(r);

                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                    super.onAnimationRepeat(animation);

                }
            });
//            mAnimator.start();
            mAnimator.reverse();
        }

        @Override
        public void drawState(Canvas canvas) {
            //1.背景--擦黑板，涂成白色
            drawBackground(canvas);
            //2.绘制小圆
            drawCircle(canvas);
        }
    }


    private void drawCircle(Canvas canvas) {
        //每个小圆之间的间隔角度 = 2π/小圆的个数
        float rotationAngle = (float) (2*Math.PI/mCircleColors.length);
        for (int i=0; i < mCircleColors.length; i++){
            /**
             * x = r*cos(a) +centerX
             y=  r*sin(a) + centerY
             每个小圆i*间隔角度 + 旋转的角度 = 当前小圆的真是角度
             */
            double angle = i*rotationAngle + mCurrentRotationAngle;
            //mCurrentRotationRadius是不断变的
            float cx = (float) (mCurrentRotationRadius*Math.cos(angle) + mCenterX);
            float cy = (float) (mCurrentRotationRadius*Math.sin(angle) + mCenterY);
            mPaint.setColor(mCircleColors[i]);
            canvas.drawCircle(cx,cy,mCircleRadius,mPaint);
        }
    }

    private void drawBackground(Canvas canvas) {
        if (mHoleRadius>0f) {
            //得到画笔的宽度 = 对角线/2 - 空心圆的半径
            float strokeWidth = mDiagonalDist - mHoleRadius;
            mPaintBackground.setStrokeWidth(strokeWidth);
            //画圆的半径 = 空心圆的半径 + 画笔的宽度/2
            float radius = mHoleRadius + strokeWidth/2;
            canvas.drawCircle(mCenterX,mCenterY,radius,mPaintBackground);
        }else {
            canvas.drawColor(mSplashBgColor);
        }
    }


    /**
     *  3.水波纹扩散动画
     *  画一个空心圆  -- 画一个园，让它的画笔的粗细变成很大 -- 不断的减小画笔的粗细
     *  空心圆变化的范围：0~对角线/2
     */
    private class ExpandState extends SplashState{
        public ExpandState() {
            //花1200ms，计算某个时刻当前的空心圆的半径是多少？ r~屏幕对角线中的某个值
            mAnimator = ValueAnimator.ofFloat(mCircleRadius, mDiagonalDist);
            mAnimator.setDuration(mRotationDuration);
            mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    //当前的空心圆的半径是多少？
                    mHoleRadius = (float) valueAnimator.getAnimatedValue();
                    invalidate();
                }
            });
            mAnimator.start();
        }
        @Override
        public void drawState(Canvas canvas) {
            drawBackground(canvas);
        }
    }


}
